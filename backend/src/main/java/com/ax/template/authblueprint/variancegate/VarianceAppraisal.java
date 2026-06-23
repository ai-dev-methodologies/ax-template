package com.ax.template.authblueprint.variancegate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * variance-tolerance-band-l0 root (VG-DERIVE-001): one appraisal of a measured ACTUAL against a
 * recorded STANDARD. The variance is DERIVED (actual − standard) by {@link VarianceService} —
 * never an entered field — and persisted IMMUTABLY alongside the standard, the actual, and the
 * tolerance band in force (lowerTolerance, upperTolerance) so the verdict is reconstructible. The
 * band is PINNED per evaluation: bands drift, so the row records exactly which lower/upper
 * governed THIS verdict. The verdict is the ASYMMETRIC two-sided gate (VG-GATE-001). A breach
 * (OUT_OF_TOLERANCE) blocks the dependent operation until disposed (VG-BLOCK/DISPOSE-001); the
 * disposed flag flips ONLY via the package-private {@link #markDisposed()} hook, called by the
 * service under the appraisal's PESSIMISTIC_WRITE row lock (VG-CONCURRENT-001). The verdict is
 * NEVER rewritten — an override records an accountable decision WITHOUT erasing the breach.
 */
@AggregateRoot
@Entity
@Table(name = "variance_appraisals")
@Check(constraints =
    "lower_tolerance >= 0 AND upper_tolerance >= 0"
    + " AND variance = actual_value - standard_value"
    + " AND (disposed = FALSE OR verdict = 'OUT_OF_TOLERANCE')")
public class VarianceAppraisal {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The appraised subject's external reference (cost element, dimension, budget line) — verbatim. */
    @Column(name = "subject", nullable = false, updatable = false, length = 200)
    private String subject;

    @Column(name = "standard_value", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal standardValue;

    @Column(name = "actual_value", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal actualValue;

    /** DERIVED basis — actualValue − standardValue, recorded so the verdict is reconstructible. */
    @Column(name = "variance", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal variance;

    /** The favorable-side allowance magnitude in force at evaluation — pinned (VG-DERIVE-001). */
    @Column(name = "lower_tolerance", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal lowerTolerance;

    /** The unfavorable-side allowance magnitude in force at evaluation — pinned (VG-DERIVE-001). */
    @Column(name = "upper_tolerance", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal upperTolerance;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", nullable = false, updatable = false, length = 20)
    private VarianceVerdict verdict;

    /** Whether an OUT_OF_TOLERANCE breach has an accountable disposition on record (VG-DISPOSE-001). */
    @Column(name = "disposed", nullable = false)
    private boolean disposed;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected VarianceAppraisal() {}

    public VarianceAppraisal(UUID id, String subject, BigDecimal standardValue, BigDecimal actualValue,
                             BigDecimal variance, BigDecimal lowerTolerance, BigDecimal upperTolerance,
                             VarianceVerdict verdict, Instant createdAt) {
        this.id = id;
        this.subject = subject;
        this.standardValue = standardValue;
        this.actualValue = actualValue;
        this.variance = variance;
        this.lowerTolerance = lowerTolerance;
        this.upperTolerance = upperTolerance;
        this.verdict = verdict;
        this.disposed = false;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook — record that an accountable disposition is now on file for the breach.
     *  The verdict is deliberately NOT touched: the appraisal stays OUT_OF_TOLERANCE WITH an
     *  override on record, so the breach never disappears from the audit trail (VG-DISPOSE-001). */
    void markDisposed() {
        this.disposed = true;
    }

    public boolean isBreach() {
        return verdict == VarianceVerdict.OUT_OF_TOLERANCE;
    }

    /** VG-BLOCK-001 — a dependent operation may proceed iff the appraisal is not a breach, or the
     *  breach has an accountable disposition on record. */
    public boolean mayProceed() {
        return !isBreach() || disposed;
    }

    public UUID getId() { return id; }
    public String getSubject() { return subject; }
    public BigDecimal getStandardValue() { return standardValue; }
    public BigDecimal getActualValue() { return actualValue; }
    public BigDecimal getVariance() { return variance; }
    public BigDecimal getLowerTolerance() { return lowerTolerance; }
    public BigDecimal getUpperTolerance() { return upperTolerance; }
    public VarianceVerdict getVerdict() { return verdict; }
    public boolean isDisposed() { return disposed; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
