package com.ax.template.authblueprint.decisiongov;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * decision-governance-l0 sole orchestrator. Every re-determination acquires the scope row
 * under PESSIMISTIC_WRITE (DG-CONCURRENT-001), appends version = current + 1 (never an
 * UPDATE — DG-RECOMPUTE-001), and persists the appraisal-sufficient basis in the SAME
 * transaction (DG-BASIS-001). A manual override additionally requires a justification and
 * a four-eyes approver distinct from the requester (DG-OVERRIDE-001) — also DB-backstopped
 * by the entity @Check. Version rows are members: written via {@link MemberWriter}, read
 * via JPQL on {@link DecisionScopeRepository}.
 */
@Service
public class DecisionService {

    static final int MAX_PAGE_SIZE = 200;

    private final DecisionScopeRepository scopes;
    private final MemberWriter members;
    private final DecisionMetrics metrics;
    private final Clock clock;

    public DecisionService(DecisionScopeRepository scopes, MemberWriter members,
                           DecisionMetrics metrics, Clock clock) {
        this.scopes = scopes;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
    }

    /** First determination of a scope — version 1, kind COMPUTED. */
    @Transactional
    public DecisionVersion compute(String scopeKey, String basisJson, String outcome, String actor) {
        requireBasis(basisJson, "compute");
        if (scopes.existsByScopeKey(scopeKey)) {
            metrics.record("compute", "rejected");
            throw DecisionException.duplicateScope();
        }
        try {
            DecisionScope s = scopes.saveAndFlush(
                new DecisionScope(UUID.randomUUID(), scopeKey, Instant.now(clock)));
            DecisionVersion v = members.persist(new DecisionVersion(UUID.randomUUID(), s.getId(), 1,
                DecisionKind.COMPUTED, basisJson, outcome, null, actor, null, Instant.now(clock)));
            metrics.record("compute", "ok");
            return v;
        } catch (DataIntegrityViolationException e) {
            metrics.record("compute", "rejected");
            throw DecisionException.duplicateScope();
        }
    }

    /** Re-determination — NEW version with its OWN basis and a mandatory reason. */
    @Transactional
    public DecisionVersion recompute(String scopeKey, String basisJson, String outcome,
                                     String reason, String actor) {
        requireBasis(basisJson, "recompute");
        requireReason(reason, "recompute");
        DecisionScope s = scopes.findByScopeKeyForUpdate(scopeKey)
            .orElseThrow(DecisionException::notFound);
        int next = s.getCurrentVersion() + 1;
        DecisionVersion v = members.persist(new DecisionVersion(UUID.randomUUID(), s.getId(), next,
            DecisionKind.RECOMPUTED, basisJson, outcome, reason.strip(), actor, null,
            Instant.now(clock)));
        s.advanceVersion(next);
        metrics.record("recompute", "ok");
        return v;
    }

    /** Manual override — justification + four-eyes approver ≠ requester; records the basis deviated FROM. */
    @Transactional
    public DecisionVersion override(String scopeKey, String outcome, String reason,
                                    String actor, String approver) {
        requireReason(reason, "override");
        if (approver == null || approver.isBlank() || approver.strip().equals(actor.strip())) {
            metrics.record("override", "rejected");
            throw DecisionException.fourEyesRequired();
        }
        DecisionScope s = scopes.findByScopeKeyForUpdate(scopeKey)
            .orElseThrow(DecisionException::notFound);
        // defensive: unreachable in normal operation — compute() persists v1 with the scope in one
        // tx and advanceVersion is always paired with a member persist under the same lock
        DecisionVersion prior = scopes.findVersion(s.getId(), s.getCurrentVersion())
            .orElseThrow(DecisionException::notFound);
        int next = s.getCurrentVersion() + 1;
        DecisionVersion v = members.persist(new DecisionVersion(UUID.randomUUID(), s.getId(), next,
            DecisionKind.OVERRIDE, prior.getBasisJson(), outcome, reason.strip(), actor,
            approver.strip(), Instant.now(clock)));
        s.advanceVersion(next);
        metrics.record("override", "ok");
        return v;
    }

    @Transactional(readOnly = true)
    public DecisionVersion latest(String scopeKey) {
        DecisionScope s = scopes.findByScopeKey(scopeKey).orElseThrow(DecisionException::notFound);
        return scopes.findVersion(s.getId(), s.getCurrentVersion())
            .orElseThrow(DecisionException::notFound);
    }

    @Transactional(readOnly = true)
    public Page<DecisionVersion> versions(String scopeKey, int page, int size) {
        DecisionScope s = scopes.findByScopeKey(scopeKey).orElseThrow(DecisionException::notFound);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return scopes.findVersionsPage(s.getId(), PageRequest.of(safePage, safeSize));
    }

    private void requireBasis(String basisJson, String op) {
        if (basisJson == null || basisJson.isBlank()) {
            metrics.record(op, "invalid");
            throw DecisionException.basisRequired();
        }
    }

    private void requireReason(String reason, String op) {
        if (reason == null || reason.isBlank()) {
            metrics.record(op, "invalid");
            throw DecisionException.reasonRequired();
        }
    }
}
