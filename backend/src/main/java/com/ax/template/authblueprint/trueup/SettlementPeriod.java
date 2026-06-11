package com.ax.template.authblueprint.trueup;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * remeasurement-trueup-l0 settlement period (TUP-SEALED-001): walks OPEN → CLOSED → SEALED
 * one-way only, via the package-private {@link #close} / {@link #seal} sole-mutator hooks.
 * Closing fixes the run-of-record; a CLOSED or SEALED period without one is unrepresentable
 * (@Check backstop). The period row's PESSIMISTIC_WRITE lock is the serialization point for
 * every write touching the period (TUP-CONCURRENT-001). The grid is bounded (gridSlots ≤ 50)
 * so the run basis stays storable inline — larger grids are a fork-receiver swap (hash-only
 * basis + side table).
 */
@AggregateRoot
@Entity
@Table(name = "settlement_periods")
@Check(constraints = "(status = 'OPEN' OR run_of_record_id IS NOT NULL) AND grid_slots >= 1")
public class SettlementPeriod {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "subject", nullable = false, updatable = false, length = 200)
    private String subject;

    @Column(name = "label", nullable = false, updatable = false, length = 100)
    private String label;

    /** Declared grid size — TUP-GRID-001 verifies every slot before a run computes. */
    @Column(name = "grid_slots", nullable = false, updatable = false)
    private int gridSlots;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PeriodStatus status;

    @Column(name = "run_of_record_id")
    private UUID runOfRecordId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SettlementPeriod() {}

    public SettlementPeriod(UUID id, String subject, String label, int gridSlots, Instant createdAt) {
        this.id = id;
        this.subject = subject;
        this.label = label;
        this.gridSlots = gridSlots;
        this.status = PeriodStatus.OPEN;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook — closing fixes the run-of-record (TUP-SEALED-001). */
    void close(UUID runOfRecordId) {
        this.status = PeriodStatus.CLOSED;
        this.runOfRecordId = runOfRecordId;
    }

    /** Sole-mutator hook — sealing trades correction capacity for finality (TUP-SEALED-001). */
    void seal() {
        this.status = PeriodStatus.SEALED;
    }

    public UUID getId() { return id; }
    public String getSubject() { return subject; }
    public String getLabel() { return label; }
    public int getGridSlots() { return gridSlots; }
    public PeriodStatus getStatus() { return status; }
    public UUID getRunOfRecordId() { return runOfRecordId; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
