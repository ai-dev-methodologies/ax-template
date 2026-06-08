package com.ax.template.authblueprint.transformation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * transformation-conservation-l0 header — an IMMUTABLE record of one conserving transformation
 * (XFORM-ATOMIC-001). The conserved totals are computed + validated at record time and frozen
 * (@Column updatable=false, no public setter); a correction is a compensating reversal
 * transformation, never an in-place edit (immutable-record-corrected-by-reversal-not-edit).
 * {@code @Version} is present for optimistic-lock discipline though the row is never updated.
 */
@AggregateRoot
@Entity
@Table(name = "transformation_runs")
// XFORM-ACCOUNTED-LOSS-001 — DB backstop applied by ddl-auto (LIVE in tests, also declared in V036
// for Flyway fork-receivers): the persisted totals must conserve exactly, never silent shrinkage.
@Check(constraints = "total_input = total_good + total_residual")
public class TransformationRun {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "created_by", nullable = false, updatable = false, length = 255)
    private String createdBy;

    @Column(name = "base_unit", nullable = false, updatable = false, length = 32)
    private String baseUnit;

    @Column(name = "total_input", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal totalInput;

    @Column(name = "total_good", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal totalGood;

    @Column(name = "total_residual", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal totalResidual;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TransformationRun() {}

    public TransformationRun(UUID id, String createdBy, String baseUnit, BigDecimal totalInput,
                             BigDecimal totalGood, BigDecimal totalResidual, Instant createdAt) {
        this.id = id;
        this.createdBy = createdBy;
        this.baseUnit = baseUnit;
        this.totalInput = totalInput;
        this.totalGood = totalGood;
        this.totalResidual = totalResidual;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getCreatedBy() { return createdBy; }
    public String getBaseUnit() { return baseUnit; }
    public BigDecimal getTotalInput() { return totalInput; }
    public BigDecimal getTotalGood() { return totalGood; }
    public BigDecimal getTotalResidual() { return totalResidual; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
