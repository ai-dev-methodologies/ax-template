package com.ax.template.authblueprint.authzparity;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * authorization-parity-l0 sole orchestrator. Governance APPROVES one artifact into an envelope;
 * EXECUTION is admissible only when (1) the execution parameters re-hash to the SAME canonical
 * parity hash the envelope recorded (executed-matches-authorized), (2) a high-value action carries
 * TWO DISTINCT approver signoffs separated from the requester (four-eyes / NIST two-person rule),
 * and (3) every declared MANDATORY companion gate is recorded present (positive-gates). The execute
 * path takes the action's PESSIMISTIC_WRITE row lock before the check-then-transition so it is
 * atomic and exactly-once under contention (AUTHZPARITY-CONCURRENT-001 / CWE-362). Signoffs,
 * gate-satisfactions and blocked attempts are members: {@link MemberWriter} writes, root-JPQL reads.
 */
@Service
public class AuthorizationParityService {

    private final AuthorizedActionRepository actions;
    private final MemberWriter members;
    private final BlockedAttemptRecorder blockedAttempts;
    private final AuthorizationParityMetrics metrics;
    private final Clock clock;

    public AuthorizationParityService(AuthorizedActionRepository actions, MemberWriter members,
                                      BlockedAttemptRecorder blockedAttempts,
                                      AuthorizationParityMetrics metrics, Clock clock) {
        this.actions = actions;
        this.members = members;
        this.blockedAttempts = blockedAttempts;
        this.metrics = metrics;
        this.clock = clock;
    }

    /** AUTHZPARITY-ENVELOPE-001 — record the envelope: canonical params + parity hash + gate set. */
    @Transactional
    public AuthorizedAction authorize(String actionType, Map<String, String> authorizedParams,
                                      boolean highValue, Set<String> requiredGates, String requester) {
        String canonical = ParityHasher.canonicalize(authorizedParams);
        String hash = ParityHasher.hash(authorizedParams);
        AuthorizedAction a = actions.save(new AuthorizedAction(UUID.randomUUID(), actionType,
            canonical, hash, highValue, requester, requiredGates, Instant.now(clock)));
        metrics.record("authorize", "ok");
        return a;
    }

    /** AUTHZPARITY-FOUREYES-001 — append a distinct-approver signoff; self/duplicate rejected. */
    @Transactional
    public ActionSignoff signoff(UUID actionId, String approver) {
        AuthorizedAction a = actions.findByIdForUpdate(actionId).orElseThrow(AuthorizationParityException::notFound);
        if (a.getStatus() == ActionStatus.EXECUTED) {
            metrics.record("signoff", "rejected");
            throw AuthorizationParityException.alreadyExecuted();
        }
        if (approver.equals(a.getRequesterUserId())) {
            metrics.record("signoff", "invalid");
            throw AuthorizationParityException.selfSignoff();
        }
        boolean duplicate = actions.findSignoffs(actionId).stream()
            .anyMatch(s -> s.getApproverUserId().equals(approver));
        if (duplicate) {
            metrics.record("signoff", "invalid");
            throw AuthorizationParityException.duplicateSignoff();
        }
        ActionSignoff s = members.persistAndFlush(new ActionSignoff(UUID.randomUUID(), actionId,
            approver, a.getRequesterUserId(), Instant.now(clock)));
        metrics.record("signoff", "ok");
        return s;
    }

    /** AUTHZPARITY-GATES-001 — satisfy a DECLARED gate; unknown rejected, re-satisfaction rejected. */
    @Transactional
    public GateSatisfaction satisfyGate(UUID actionId, String gateKey, String by) {
        AuthorizedAction a = actions.findByIdForUpdate(actionId).orElseThrow(AuthorizationParityException::notFound);
        if (a.getStatus() == ActionStatus.EXECUTED) {
            metrics.record("satisfy_gate", "rejected");
            throw AuthorizationParityException.alreadyExecuted();
        }
        if (!a.getRequiredGates().contains(gateKey)) {
            metrics.record("satisfy_gate", "invalid");
            throw AuthorizationParityException.unknownGate(gateKey);
        }
        boolean already = actions.findGates(actionId).stream()
            .anyMatch(g -> g.getGateKey().equals(gateKey));
        if (already) {
            metrics.record("satisfy_gate", "rejected");
            throw AuthorizationParityException.gateAlreadySatisfied(gateKey);
        }
        GateSatisfaction g = members.persistAndFlush(new GateSatisfaction(UUID.randomUUID(), actionId,
            gateKey, by, Instant.now(clock)));
        metrics.record("satisfy_gate", "ok");
        return g;
    }

    /**
     * AUTHZPARITY-EXEC/FOUREYES/GATES/CONCURRENT-001 — under the action's row lock: recompute the
     * parity hash from the ACTUAL execution parameters; on mismatch record a BLOCKED attempt and
     * refuse (409); else enforce four-eyes + positive-gates; then transition to EXECUTED exactly once.
     */
    @Transactional
    public AuthorizedAction execute(UUID actionId, Map<String, String> executionParams, String executor) {
        AuthorizedAction a = actions.findByIdForUpdate(actionId).orElseThrow(AuthorizationParityException::notFound);
        if (a.getStatus() == ActionStatus.EXECUTED) {
            metrics.record("execute", "rejected");
            throw AuthorizationParityException.alreadyExecuted();
        }
        // (1) executed-matches-authorized — a substituted/escalated parameter changes the digest.
        String offered = ParityHasher.hash(executionParams);
        if (!offered.equals(a.getParityHash())) {
            // record in an INDEPENDENT (REQUIRES_NEW) tx — the throw below rolls back THIS tx,
            // so the audit row must commit separately or the refusal would erase its own evidence.
            blockedAttempts.record(actionId, offered, a.getParityHash(), executor);
            metrics.record("execute", "parity_mismatch");
            throw AuthorizationParityException.parityMismatch();
        }
        // (2) four-eyes — two DISTINCT approvers, each separated from the requester.
        if (a.isHighValue()) {
            long distinctApprovers = actions.findSignoffs(actionId).stream()
                .map(ActionSignoff::getApproverUserId).distinct().count();
            if (distinctApprovers < 2) {
                metrics.record("execute", "invalid");
                throw AuthorizationParityException.insufficientSignoffs();
            }
        }
        // (3) positive-gates — every declared mandatory companion gate must be recorded present.
        Set<String> satisfied = actions.findGates(actionId).stream()
            .map(GateSatisfaction::getGateKey).collect(java.util.stream.Collectors.toSet());
        for (String required : a.getRequiredGates()) {
            if (!satisfied.contains(required)) {
                metrics.record("execute", "invalid");
                throw AuthorizationParityException.missingCompanionGate(required);
            }
        }
        a.markExecuted(Instant.now(clock));                     // sole-mutator hook, under the lock
        metrics.record("execute", "executed");
        return a;
    }

    @Transactional(readOnly = true)
    public AuthorizedAction get(UUID id) {
        return actions.findById(id).orElseThrow(AuthorizationParityException::notFound);
    }

    @Transactional(readOnly = true)
    public List<ActionSignoff> signoffs(UUID actionId) {
        get(actionId);                                          // 404 before an empty list
        return actions.findSignoffs(actionId);
    }

    @Transactional(readOnly = true)
    public List<GateSatisfaction> gates(UUID actionId) {
        get(actionId);
        return actions.findGates(actionId);
    }

    @Transactional(readOnly = true)
    public List<BlockedAttempt> blockedAttempts(UUID actionId) {
        get(actionId);
        return actions.findBlockedAttempts(actionId);
    }
}
