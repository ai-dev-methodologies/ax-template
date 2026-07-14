package com.ax.template.authblueprint.countbudget;

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
 * periodic-count-budget-l0 policy root: a per-subject recurring count budget. {@code cadence} is
 * immutable (changing it mid-flight would silently reshape every future periodKey); {@code cap} is the
 * ONLY mutable field, changed exclusively via the package-private {@link #updateCap} (sole mutator:
 * {@link CountBudgetService}, always under the row's PESSIMISTIC_WRITE lock). PCB-CAP-001: a cap change
 * here affects ONLY periods not yet first-touched — an already-touched {@link CountBudgetPeriod} captured
 * its own cap at creation and never re-reads this field.
 */
@AggregateRoot
@Entity
@Table(name = "count_budget_policies")
@Check(constraints = "cap > 0")
public class CountBudgetPolicy {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "subject_key", nullable = false, updatable = false, length = 200, unique = true)
    private String subjectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "cadence", nullable = false, updatable = false, length = 10)
    private CountBudgetCadence cadence;

    @Column(name = "cap", nullable = false)
    private int cap;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CountBudgetPolicy() {}

    public CountBudgetPolicy(UUID id, String subjectKey, CountBudgetCadence cadence, int cap, Instant createdAt) {
        this.id = id;
        this.subjectKey = subjectKey;
        this.cadence = cadence;
        this.cap = cap;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook ({@link CountBudgetService} only, under the row lock) — PCB-CAP-001. */
    void updateCap(int newCap) {
        this.cap = newCap;
    }

    public UUID getId() { return id; }
    public String getSubjectKey() { return subjectKey; }
    public CountBudgetCadence getCadence() { return cadence; }
    public int getCap() { return cap; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
