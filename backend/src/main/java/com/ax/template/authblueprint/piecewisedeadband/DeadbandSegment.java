package com.ax.template.authblueprint.piecewisedeadband;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * piecewise-deadband-l0 tiling segment (PWDB-SEGMENT-001). Sorted by {@code ordinal}, the segments of one
 * config MUST tile {@code [domainStart, domainEnd)} exactly: the first segment's {@code start} equals the
 * config's {@code domainStart}, each segment's {@code end} equals the next segment's {@code start}
 * (no gap, no overlap), and the last segment's {@code end} equals {@code domainEnd} — validated once, at
 * creation, by {@link DeadbandService}. Every column is {@code @Column(updatable=false)} and there is no
 * setter — a segment is immutable evidence of the curve a point was evaluated against.
 */
@AggregateMember(root = DeadbandConfig.class)
@Entity
@Table(name = "deadband_segments",
    uniqueConstraints = @UniqueConstraint(name = "uq_deadband_segment_ordinal",
        columnNames = {"config_id", "ordinal"}))
// PWDB-SEGMENT-001 — start < end; deadband width non-negative. LIVE under ddl-auto.
@Check(constraints = "segment_start < segment_end AND deadband_width >= 0")
public class DeadbandSegment {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "config_id", nullable = false, updatable = false)
    private UUID configId;

    @Column(name = "ordinal", nullable = false, updatable = false)
    private int ordinal;

    @Column(name = "segment_start", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal start;

    @Column(name = "segment_end", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal end;

    @Column(name = "obligation_target", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal obligationTarget;

    @Column(name = "deadband_width", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal deadbandWidth;

    protected DeadbandSegment() {}

    public DeadbandSegment(UUID id, UUID configId, int ordinal, BigDecimal start, BigDecimal end,
                           BigDecimal obligationTarget, BigDecimal deadbandWidth) {
        this.id = id;
        this.configId = configId;
        this.ordinal = ordinal;
        this.start = start;
        this.end = end;
        this.obligationTarget = obligationTarget;
        this.deadbandWidth = deadbandWidth;
    }

    /** Whether this segment's half-open range {@code [start, end)} covers the given domain point. */
    boolean covers(BigDecimal x) {
        return x.compareTo(start) >= 0 && x.compareTo(end) < 0;
    }

    public UUID getId() { return id; }
    public UUID getConfigId() { return configId; }
    public int getOrdinal() { return ordinal; }
    public BigDecimal getStart() { return start; }
    public BigDecimal getEnd() { return end; }
    public BigDecimal getObligationTarget() { return obligationTarget; }
    public BigDecimal getDeadbandWidth() { return deadbandWidth; }
}
