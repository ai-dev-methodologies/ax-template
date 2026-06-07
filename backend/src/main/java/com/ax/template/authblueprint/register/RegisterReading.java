package com.ax.template.authblueprint.register;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * monotone-register-l0 append-only read. EVERY column is {@code @Column(updatable=false)}, there is no
 * setter, and a row is never UPDATEd or deleted — a correction is a NEW appended read (REG-DELTA-001).
 * {@code delta} is the consumption attributed to this read (≥ 0 always, even on a ROLLOVER wrap);
 * {@code priorAnchor} is the anchor immediately before this read; {@code sequenceNo} is strictly
 * monotonic per register. {@code @Check delta >= 0} is the load-bearing no-negative-consumption backstop.
 */
@Entity
@Table(name = "register_readings",
    uniqueConstraints = @UniqueConstraint(name = "uq_register_reading_seq",
        columnNames = {"register_id", "sequence_no"}))
// REG-MONOTONE-001 / REG-ROLLOVER-001 — consumption is never negative; the raw read is non-negative.
@Check(constraints = "delta >= 0 AND reading_value >= 0")
public class RegisterReading {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "register_id", nullable = false, updatable = false)
    private UUID registerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, updatable = false, length = 20)
    private ReadingKind kind;

    @Column(name = "reading_value", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal readingValue;

    @Column(name = "prior_anchor", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal priorAnchor;

    @Column(name = "delta", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal delta;

    @Column(name = "sequence_no", nullable = false, updatable = false)
    private long sequenceNo;

    /** Mandatory non-blank for ROLLOVER/EXCHANGE (the governed exceptions); null for a NORMAL read. */
    @Column(name = "reason", updatable = false, length = 1000)
    private String reason;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    protected RegisterReading() {}

    public RegisterReading(UUID id, UUID registerId, ReadingKind kind, BigDecimal readingValue,
                           BigDecimal priorAnchor, BigDecimal delta, long sequenceNo, String reason,
                           Instant recordedAt) {
        this.id = id;
        this.registerId = registerId;
        this.kind = kind;
        this.readingValue = readingValue;
        this.priorAnchor = priorAnchor;
        this.delta = delta;
        this.sequenceNo = sequenceNo;
        this.reason = reason;
        this.recordedAt = recordedAt;
    }

    public UUID getId() { return id; }
    public UUID getRegisterId() { return registerId; }
    public ReadingKind getKind() { return kind; }
    public BigDecimal getReadingValue() { return readingValue; }
    public BigDecimal getPriorAnchor() { return priorAnchor; }
    public BigDecimal getDelta() { return delta; }
    public long getSequenceNo() { return sequenceNo; }
    public String getReason() { return reason; }
    public Instant getRecordedAt() { return recordedAt; }
}
