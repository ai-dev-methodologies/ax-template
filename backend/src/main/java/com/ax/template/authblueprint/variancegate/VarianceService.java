package com.ax.template.authblueprint.variancegate;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * variance-tolerance-band-l0 sole orchestrator. The variance is DERIVED (actual − standard) at
 * appraisal time and persisted with the band in force (VG-DERIVE-001); the verdict is the
 * ASYMMETRIC two-sided gate (VG-GATE-001). A dependent operation on a breach is blocked 422 with
 * the variance + band named, until an explicit disposition is recorded (VG-BLOCK/DISPOSE-001). The
 * dispose path takes the appraisal's PESSIMISTIC_WRITE row lock so concurrent dispositions converge
 * to exactly one winner; the uq(appraisal_id) backstop makes the loser a deterministic 409
 * (VG-CONCURRENT-001, CWE-362). VarianceDisposition rows are members: {@link MemberWriter} writes,
 * root-JPQL reads. Nothing in this domain rewrites a verdict or deletes a row.
 */
@Service
public class VarianceService {

    private final VarianceAppraisalRepository appraisals;
    private final MemberWriter members;
    private final VarianceMetrics metrics;
    private final Clock clock;

    public VarianceService(VarianceAppraisalRepository appraisals, MemberWriter members,
                           VarianceMetrics metrics, Clock clock) {
        this.appraisals = appraisals;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
    }

    /** VG-DERIVE-001 + VG-GATE-001 — derive the variance, pin the band, render the asymmetric verdict. */
    @Transactional
    public VarianceAppraisal appraise(String subject, BigDecimal standardValue, BigDecimal actualValue,
                                      BigDecimal lowerTolerance, BigDecimal upperTolerance) {
        if (lowerTolerance.signum() < 0 || upperTolerance.signum() < 0) {
            metrics.record("appraise", "rejected");
            throw VarianceException.invalidBand();             // tolerance magnitudes are non-negative
        }
        BigDecimal variance = actualValue.subtract(standardValue);   // DERIVED — never an entered field
        VarianceVerdict verdict = gate(variance, lowerTolerance, upperTolerance);
        VarianceAppraisal saved = appraisals.save(new VarianceAppraisal(UUID.randomUUID(), subject,
            standardValue, actualValue, variance, lowerTolerance, upperTolerance, verdict,
            Instant.now(clock)));
        metrics.record("appraise", verdict == VarianceVerdict.WITHIN_TOLERANCE ? "within" : "breach");
        return saved;
    }

    /**
     * VG-GATE-001 — the ASYMMETRIC two-sided gate. WITHIN_TOLERANCE iff the variance is
     * ≥ −lowerTolerance AND ≤ +upperTolerance (BigDecimal compareTo, inclusive on both bounds).
     * The favorable (lower) and unfavorable (upper) allowances are independent magnitudes — they
     * are deliberately NOT collapsed into a symmetric ±band.
     */
    private static VarianceVerdict gate(BigDecimal variance, BigDecimal lower, BigDecimal upper) {
        boolean within = variance.compareTo(lower.negate()) >= 0 && variance.compareTo(upper) <= 0;
        return within ? VarianceVerdict.WITHIN_TOLERANCE : VarianceVerdict.OUT_OF_TOLERANCE;
    }

    /**
     * VG-BLOCK-001 — the dependent-operation gate. A WITHIN_TOLERANCE appraisal (or a breach with a
     * disposition on record) proceeds; an undisposed breach is blocked 422 with the variance + band
     * named. Fail-closed: an unknown appraisal is a 404, never an implicit pass.
     */
    @Transactional(readOnly = true)
    public VarianceAppraisal proceed(UUID appraisalId) {
        VarianceAppraisal a = appraisals.findById(appraisalId).orElseThrow(VarianceException::notFound);
        if (!a.mayProceed()) {
            metrics.record("proceed", "blocked");
            throw VarianceException.outOfTolerance(a.getVariance(), a.getLowerTolerance(),
                a.getUpperTolerance());
        }
        metrics.record("proceed", "proceeded");
        return a;
    }

    /**
     * VG-DISPOSE-001 + VG-CONCURRENT-001 — record an accountable disposition (who/when/reason) of a
     * breach under the appraisal's PESSIMISTIC_WRITE row lock; idempotent (one per appraisal). The
     * verdict is NEVER rewritten — the breach stays OUT_OF_TOLERANCE WITH the override on record.
     */
    @Transactional
    public VarianceAppraisal dispose(UUID appraisalId, String actor, String reason) {
        VarianceAppraisal a = appraisals.findByIdForUpdate(appraisalId)
            .orElseThrow(VarianceException::notFound);
        if (!a.isBreach()) {
            metrics.record("dispose", "rejected");
            throw VarianceException.nothingToDispose();        // a within-tolerance appraisal has no breach
        }
        if (reason == null || reason.isBlank()) {
            metrics.record("dispose", "rejected");
            throw VarianceException.blankReason();             // silent acceptance is not permitted
        }
        if (a.isDisposed()) {
            metrics.record("dispose", "disposed");
            return a;                                          // idempotent — already on record, no second row
        }
        Instant now = Instant.now(clock);
        try {
            // uq(appraisal_id) — appending a second disposition is the exactly-once backstop.
            members.persistAndFlush(new VarianceDisposition(UUID.randomUUID(), a.getId(),
                DispositionDecision.OVERRIDE, actor, reason, now));
        } catch (DataIntegrityViolationException dup) {
            metrics.record("dispose", "already_disposed");
            throw VarianceException.alreadyDisposed();         // loser of the concurrent dispose → 409
        }
        a.markDisposed();
        metrics.record("dispose", "disposed");
        return a;
    }

    @Transactional(readOnly = true)
    public VarianceAppraisal get(UUID appraisalId) {
        return appraisals.findById(appraisalId).orElseThrow(VarianceException::notFound);
    }

    @Transactional(readOnly = true)
    public VarianceDisposition dispositionOf(UUID appraisalId) {
        get(appraisalId);                                      // 404 before an empty lookup
        return appraisals.findDisposition(appraisalId).orElse(null);
    }
}
