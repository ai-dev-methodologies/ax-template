package com.ax.template.authblueprint.netmetering;

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
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * signed-dual-register-l0 append-only basis reading. EVERY column is {@code @Column(updatable=false)},
 * there is no setter, and a row is never UPDATEd or deleted — a correction is a NEW appended reading
 * (NETM-NET-001). {@code delta} is the consumption attributed to this reading for its direction (≥ 0
 * always); {@code priorCumulative} is that direction's cumulative immediately before this reading;
 * {@code netAfter}/{@code importAfter}/{@code exportAfter} are the post-append BASIS (the derived net and
 * both cumulatives, recorded so the signed net is independently reconstructible). {@code sequenceNo} is
 * strictly monotonic per (meter, direction). {@code @Check delta >= 0} is the load-bearing
 * no-negative-direction-consumption backstop.
 */
@AggregateMember(root = NetMeter.class)
@Entity
@Table(name = "net_meter_readings",
    uniqueConstraints = @UniqueConstraint(name = "uq_net_meter_reading_seq",
        columnNames = {"meter_id", "direction", "sequence_no"}))
// NETM-DIRECTION-001 — direction consumption is never negative; the raw read is non-negative.
@Check(constraints = "delta >= 0 AND reading_value >= 0")
public class NetMeterReading {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "meter_id", nullable = false, updatable = false)
    private UUID meterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, updatable = false, length = 10)
    private MeterDirection direction;

    @Column(name = "reading_value", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal readingValue;

    @Column(name = "prior_cumulative", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal priorCumulative;

    @Column(name = "delta", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal delta;

    /** Post-append derived net = importAfter − exportAfter (the recorded BASIS). */
    @Column(name = "net_after", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal netAfter;

    @Column(name = "import_after", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal importAfter;

    @Column(name = "export_after", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal exportAfter;

    @Column(name = "sequence_no", nullable = false, updatable = false)
    private long sequenceNo;

    /** The reading's effective billing instant — gates the closed-period backdate check (NETM-PERIOD-001). */
    @Column(name = "effective_at", nullable = false, updatable = false)
    private Instant effectiveAt;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    protected NetMeterReading() {}

    public NetMeterReading(UUID id, UUID meterId, MeterDirection direction, BigDecimal readingValue,
                           BigDecimal priorCumulative, BigDecimal delta, BigDecimal netAfter,
                           BigDecimal importAfter, BigDecimal exportAfter, long sequenceNo,
                           Instant effectiveAt, Instant recordedAt) {
        this.id = id;
        this.meterId = meterId;
        this.direction = direction;
        this.readingValue = readingValue;
        this.priorCumulative = priorCumulative;
        this.delta = delta;
        this.netAfter = netAfter;
        this.importAfter = importAfter;
        this.exportAfter = exportAfter;
        this.sequenceNo = sequenceNo;
        this.effectiveAt = effectiveAt;
        this.recordedAt = recordedAt;
    }

    public UUID getId() { return id; }
    public UUID getMeterId() { return meterId; }
    public MeterDirection getDirection() { return direction; }
    public BigDecimal getReadingValue() { return readingValue; }
    public BigDecimal getPriorCumulative() { return priorCumulative; }
    public BigDecimal getDelta() { return delta; }
    public BigDecimal getNetAfter() { return netAfter; }
    public BigDecimal getImportAfter() { return importAfter; }
    public BigDecimal getExportAfter() { return exportAfter; }
    public long getSequenceNo() { return sequenceNo; }
    public Instant getEffectiveAt() { return effectiveAt; }
    public Instant getRecordedAt() { return recordedAt; }
}
