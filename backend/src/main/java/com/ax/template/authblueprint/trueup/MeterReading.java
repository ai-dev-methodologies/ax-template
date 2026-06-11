package com.ax.template.authblueprint.trueup;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * remeasurement-trueup-l0 reading row (TUP-SUPERSEDE-001): the value, its source
 * (ESTIMATED|ACTUAL), and the estimation method are immutable once written. A better value is
 * a NEW row with slot_version+1 — this row is then marked SUPERSEDED with a forward pointer
 * via the package-private {@link #supersededBy}, its value retained verbatim. The source-method
 * pairing and the supersession pointer are @Check-backstopped so neither a method-less estimate
 * nor a pointer-less tombstone is representable. Column is reading_value, never value (reserved).
 */
@AggregateRoot
@Entity
@Table(name = "meter_readings", uniqueConstraints = {
    @UniqueConstraint(name = "uq_reading_slot_version", columnNames = {"period_id", "slot_index", "slot_version"})
})
@Check(constraints = "(status <> 'SUPERSEDED' OR superseded_by_id IS NOT NULL)"
    + " AND ((source = 'ESTIMATED' AND estimation_method IS NOT NULL)"
    + " OR (source = 'ACTUAL' AND estimation_method IS NULL))"
    + " AND slot_version >= 1")
public class MeterReading {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "period_id", nullable = false, updatable = false)
    private UUID periodId;

    @Column(name = "slot_index", nullable = false, updatable = false)
    private int slotIndex;

    /** Per-slot supersession counter — uq(period, slot, slot_version) is the race backstop. */
    @Column(name = "slot_version", nullable = false, updatable = false)
    private int slotVersion;

    @Column(name = "reading_value", nullable = false, updatable = false, precision = 15, scale = 4)
    private BigDecimal readingValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, updatable = false, length = 20)
    private ReadingSource source;

    /** Recorded per row when ESTIMATED (TUP-GRID-001) — the gap-fill is a recorded fact. */
    @Column(name = "estimation_method", updatable = false, length = 50)
    private String estimationMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReadingStatus status;

    @Column(name = "superseded_by_id")
    private UUID supersededById;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MeterReading() {}

    public MeterReading(UUID id, UUID periodId, int slotIndex, int slotVersion, BigDecimal readingValue,
                        ReadingSource source, String estimationMethod, Instant createdAt) {
        this.id = id;
        this.periodId = periodId;
        this.slotIndex = slotIndex;
        this.slotVersion = slotVersion;
        this.readingValue = readingValue;
        this.source = source;
        this.estimationMethod = estimationMethod;
        this.status = ReadingStatus.ACTIVE;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook — supersession marks, never rewrites (TUP-SUPERSEDE-001). */
    void supersededBy(UUID nextReadingId) {
        this.status = ReadingStatus.SUPERSEDED;
        this.supersededById = nextReadingId;
    }

    public UUID getId() { return id; }
    public UUID getPeriodId() { return periodId; }
    public int getSlotIndex() { return slotIndex; }
    public int getSlotVersion() { return slotVersion; }
    public BigDecimal getReadingValue() { return readingValue; }
    public ReadingSource getSource() { return source; }
    public String getEstimationMethod() { return estimationMethod; }
    public ReadingStatus getStatus() { return status; }
    public UUID getSupersededById() { return supersededById; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
