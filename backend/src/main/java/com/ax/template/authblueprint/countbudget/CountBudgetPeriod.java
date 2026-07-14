package com.ax.template.authblueprint.countbudget;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * periodic-count-budget-l0 per-(subject, periodKey) counter row (PCB-RESET-001 / PCB-CAP-001). Created
 * LAZILY by the first consume request whose derived {@code periodKey} has no existing row — never by an
 * explicit 'close the previous period' action. {@code capAtPeriodStart} is captured ONCE, at first touch,
 * and is IMMUTABLE evidence: a later change to the policy's cap never reshapes an already-touched period
 * (PCB-CAP-001). The consumed count is DERIVED (a COUNT of {@link CountBudgetConsumption} rows), never
 * stored here — there is nothing on this row that could drift from the ledger.
 */
@AggregateMember(root = CountBudgetPolicy.class)
@Entity
@Table(name = "count_budget_periods",
    uniqueConstraints = @UniqueConstraint(name = "uq_count_budget_period_key",
        columnNames = {"policy_id", "period_key"}))
@Check(constraints = "cap_at_period_start > 0")
public class CountBudgetPeriod {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "policy_id", nullable = false, updatable = false)
    private UUID policyId;

    @Column(name = "period_key", nullable = false, updatable = false, length = 20)
    private String periodKey;

    /** Immutable — captured from the policy's cap AT first touch (PCB-CAP-001). */
    @Column(name = "cap_at_period_start", nullable = false, updatable = false)
    private int capAtPeriodStart;

    @Column(name = "first_touched_at", nullable = false, updatable = false)
    private Instant firstTouchedAt;

    protected CountBudgetPeriod() {}

    public CountBudgetPeriod(UUID id, UUID policyId, String periodKey, int capAtPeriodStart,
                             Instant firstTouchedAt) {
        this.id = id;
        this.policyId = policyId;
        this.periodKey = periodKey;
        this.capAtPeriodStart = capAtPeriodStart;
        this.firstTouchedAt = firstTouchedAt;
    }

    public UUID getId() { return id; }
    public UUID getPolicyId() { return policyId; }
    public String getPeriodKey() { return periodKey; }
    public int getCapAtPeriodStart() { return capAtPeriodStart; }
    public Instant getFirstTouchedAt() { return firstTouchedAt; }
}
