package com.ax.template.authblueprint.piecewisedeadband;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * piecewise-deadband-l0 APPEND-ONLY evaluation (PWDB-IMMUTABLE-001). EVERY column is
 * {@code @Column(updatable=false)}, there is no setter, and a row is never UPDATEd or deleted. Identity for
 * idempotent replay is {@code idempotencyKey} — a deterministic hash of {@code (configId, pointX,
 * actualValue)} — backstopped by a DB unique constraint on {@code (config_id, idempotency_key)} so a race
 * between two identical concurrent evaluate calls cannot produce two rows. {@code obligationTarget} and
 * {@code deadbandWidth} are copied FROM the covering segment at evaluation time (immutable evidence of
 * what the point was actually judged against); {@code deviation = actualValue − obligationTarget} is
 * SIGNED (never just the boolean {@code compliant}).
 */
@AggregateMember(root = DeadbandConfig.class)
@Entity
@Table(name = "deadband_evaluations",
    uniqueConstraints = @UniqueConstraint(name = "uq_deadband_evaluation_idempotency",
        columnNames = {"config_id", "idempotency_key"}))
public class DeadbandEvaluation {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "config_id", nullable = false, updatable = false)
    private UUID configId;

    @Column(name = "segment_id", nullable = false, updatable = false)
    private UUID segmentId;

    @Column(name = "point_x", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal pointX;

    @Column(name = "actual_value", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal actualValue;

    @Column(name = "obligation_target", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal obligationTarget;

    @Column(name = "deadband_width", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal deadbandWidth;

    /** SIGNED deviation = actualValue − obligationTarget. Never just the boolean verdict. */
    @Column(name = "deviation", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal deviation;

    @Column(name = "compliant", nullable = false, updatable = false)
    private boolean compliant;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "sequence_no", nullable = false, updatable = false)
    private long sequenceNo;

    @Column(name = "evaluated_at", nullable = false, updatable = false)
    private Instant evaluatedAt;

    protected DeadbandEvaluation() {}

    public DeadbandEvaluation(UUID id, UUID configId, UUID segmentId, BigDecimal pointX, BigDecimal actualValue,
                              BigDecimal obligationTarget, BigDecimal deadbandWidth, BigDecimal deviation,
                              boolean compliant, String idempotencyKey, long sequenceNo, Instant evaluatedAt) {
        this.id = id;
        this.configId = configId;
        this.segmentId = segmentId;
        this.pointX = pointX;
        this.actualValue = actualValue;
        this.obligationTarget = obligationTarget;
        this.deadbandWidth = deadbandWidth;
        this.deviation = deviation;
        this.compliant = compliant;
        this.idempotencyKey = idempotencyKey;
        this.sequenceNo = sequenceNo;
        this.evaluatedAt = evaluatedAt;
    }

    public UUID getId() { return id; }
    public UUID getConfigId() { return configId; }
    public UUID getSegmentId() { return segmentId; }
    public BigDecimal getPointX() { return pointX; }
    public BigDecimal getActualValue() { return actualValue; }
    public BigDecimal getObligationTarget() { return obligationTarget; }
    public BigDecimal getDeadbandWidth() { return deadbandWidth; }
    public BigDecimal getDeviation() { return deviation; }
    public boolean isCompliant() { return compliant; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public long getSequenceNo() { return sequenceNo; }
    public Instant getEvaluatedAt() { return evaluatedAt; }
}
