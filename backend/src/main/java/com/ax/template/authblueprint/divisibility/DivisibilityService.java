package com.ax.template.authblueprint.divisibility;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * material-divisibility-constraint-l0 sole orchestrator. Declaring a policy APPENDS the next
 * per-material version under the material's PESSIMISTIC_WRITE row lock so two concurrent re-
 * declarations can never mint the same version (DIV-POLICY-001; the uq(material_ref,
 * policy_version) backstop makes any residual-race loser deterministic). A quantity check reads the
 * CURRENT policy, applies the deterministic {@link DivisibilityArithmetic} gate, records an
 * immutable {@link DivisibilityCheck} carrying the verdict + the policy version in force, and —
 * crucially — REJECTS (422) rather than rounding: an INTEGER_ONLY material with a fractional
 * quantity is NON_INTEGRAL (DIV-INTEGRAL-001), a FRACTIONAL material above its max scale is
 * EXCESS_PRECISION (DIV-PRECISION-001). This is the deliberate opposite of order-multiple-
 * quantization, which ROUNDS a requirement up to a lot; THIS gate never changes the number.
 * Check rows are members: {@link MemberWriter} writes, root-JPQL reads (HG-AGG-REPO).
 */
@Service
public class DivisibilityService {

    /** Bounded page for the append-only history reads (the unbounded-list guard requires a Pageable). */
    static final int MAX_HISTORY = 500;

    private final MaterialDivisibilityPolicyRepository policies;
    private final MemberWriter members;
    private final DivisibilityMetrics metrics;
    private final Clock clock;

    public DivisibilityService(MaterialDivisibilityPolicyRepository policies, MemberWriter members,
                               DivisibilityMetrics metrics, Clock clock) {
        this.policies = policies;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
    }

    /**
     * DIV-POLICY-001 — declare (or re-declare) a material's divisibility policy. The first
     * declaration is version 1; a re-declaration appends the next version under the material's
     * row lock, retaining the prior version (append-only, never overwritten).
     */
    @Transactional
    public MaterialDivisibilityPolicy declare(String materialRef, DivisibilityPolicyKind kind, int maxScale) {
        if (kind == DivisibilityPolicyKind.FRACTIONAL && maxScale < 0) {
            metrics.record("declare", "invalid");
            throw DivisibilityException.invalidMaxScale(maxScale);
        }
        long nextVersion = policies.findCurrentForUpdate(materialRef)   // PESSIMISTIC_WRITE: serialize re-declare
            .map(p -> p.getPolicyVersion() + 1L)
            .orElse(1L);
        MaterialDivisibilityPolicy saved = policies.save(new MaterialDivisibilityPolicy(
            UUID.randomUUID(), materialRef, nextVersion, kind, maxScale, Instant.now(clock)));
        metrics.record("declare", "declared");
        return saved;
    }

    /**
     * DIV-INTEGRAL/PRECISION/RECORD/DETERMINISM-001 — check a quantity against the material's current
     * policy. REJECTS (422) a quantity the policy forbids; NEVER rounds or truncates. Records the
     * verdict + the policy version in force on an immutable check record, then — for a rejection —
     * throws after the record is written so the rejection basis is reconstructible.
     */
    // noRollbackFor: a rejection (NON_INTEGRAL / EXCESS_PRECISION) is a RECORDED verdict, not a
    // failed write — the immutable check row MUST survive the 422 (DIV-RECORD-001). Without this,
    // Spring's default rollback-on-RuntimeException would discard the very record the rejection rests on.
    @Transactional(noRollbackFor = DivisibilityException.class)
    public DivisibilityCheck check(String materialRef, BigDecimal quantity) {
        MaterialDivisibilityPolicy policy = policies.findCurrent(materialRef)
            .orElseThrow(DivisibilityException::notFound);
        Instant now = Instant.now(clock);

        CheckVerdict verdict = verdictFor(policy, quantity);
        // Record the verdict (including a rejection) against the policy version in force, verbatim.
        DivisibilityCheck recorded = members.persist(new DivisibilityCheck(UUID.randomUUID(),
            materialRef, quantity, verdict, policy.getPolicyVersion(), now));

        return switch (verdict) {
            case NON_INTEGRAL -> {
                metrics.record("check", "non_integral");
                // The rejection is RECORDED above; throwing here keeps the @Transactional commit
                // so the recorded check survives — the rejection basis is reconstructible.
                throw DivisibilityException.nonIntegral(materialRef, quantity.toPlainString());
            }
            case EXCESS_PRECISION -> {
                metrics.record("check", "excess_precision");
                throw DivisibilityException.excessPrecision(materialRef, quantity.toPlainString(),
                    policy.getMaxScale());
            }
            case ACCEPTED -> {
                metrics.record("check", "accepted");
                yield recorded;
            }
        };
    }

    /** The deterministic gate (DIV-DETERMINISM-001) — reject-not-round, format-independent. */
    private static CheckVerdict verdictFor(MaterialDivisibilityPolicy policy, BigDecimal quantity) {
        return switch (policy.getPolicyKind()) {
            case INTEGER_ONLY -> DivisibilityArithmetic.isIntegral(quantity)
                ? CheckVerdict.ACCEPTED : CheckVerdict.NON_INTEGRAL;
            case FRACTIONAL -> DivisibilityArithmetic.effectiveScale(quantity) <= policy.getMaxScale()
                ? CheckVerdict.ACCEPTED : CheckVerdict.EXCESS_PRECISION;
        };
    }

    @Transactional(readOnly = true)
    public MaterialDivisibilityPolicy current(String materialRef) {
        return policies.findCurrent(materialRef).orElseThrow(DivisibilityException::notFound);
    }

    @Transactional(readOnly = true)
    public List<MaterialDivisibilityPolicy> history(String materialRef) {
        current(materialRef);                                  // 404 before an empty list
        return policies.findHistory(materialRef, PageRequest.of(0, MAX_HISTORY));
    }

    @Transactional(readOnly = true)
    public List<DivisibilityCheck> checks(String materialRef) {
        current(materialRef);                                  // 404 before an empty list
        return policies.findChecks(materialRef, PageRequest.of(0, MAX_HISTORY));
    }
}
