package com.ax.template.authblueprint.countbudget;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * periodic-count-budget-l0 APPEND-ONLY consumption ledger row (PCB-AUDIT-001). EVERY column is
 * {@code @Column(updatable=false)}, there is no setter, and a row is never UPDATEd or deleted —
 * {@code sequenceNo} is strictly monotonic per period, and the period's consumed count is the
 * {@code COUNT(*)} of these rows (never a separately-stored, driftable total).
 */
@AggregateMember(root = CountBudgetPolicy.class)
@Entity
@Table(name = "count_budget_consumptions",
    uniqueConstraints = @UniqueConstraint(name = "uq_count_budget_consumption_seq",
        columnNames = {"period_id", "sequence_no"}))
public class CountBudgetConsumption {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "period_id", nullable = false, updatable = false)
    private UUID periodId;

    @Column(name = "sequence_no", nullable = false, updatable = false)
    private long sequenceNo;

    @Column(name = "consumed_at", nullable = false, updatable = false)
    private Instant consumedAt;

    protected CountBudgetConsumption() {}

    public CountBudgetConsumption(UUID id, UUID periodId, long sequenceNo, Instant consumedAt) {
        this.id = id;
        this.periodId = periodId;
        this.sequenceNo = sequenceNo;
        this.consumedAt = consumedAt;
    }

    public UUID getId() { return id; }
    public UUID getPeriodId() { return periodId; }
    public long getSequenceNo() { return sequenceNo; }
    public Instant getConsumedAt() { return consumedAt; }
}
