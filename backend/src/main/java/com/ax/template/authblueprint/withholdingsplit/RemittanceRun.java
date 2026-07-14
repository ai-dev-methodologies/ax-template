package com.ax.template.authblueprint.withholdingsplit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * withholding-split-l0 remittance root: one immutable, FROZEN collection of the withholding legs for
 * one period (WHT-REMIT-003). {@code uq(period)} makes a duplicate remittance run for one period
 * unrepresentable even under a concurrent race — the losing writer catches the constraint violation
 * and returns the winner's row (mirrors material-divisibility-constraint-l0's per-material version
 * and threshold-terminal-derivation-l0's per-scope register). Collecting a period a SECOND time
 * returns this SAME row unchanged — it is never re-summed, even if more postings for the period are
 * created afterward (the collection is a frozen snapshot, not a live recompute).
 */
@AggregateRoot
@Entity
@Table(name = "remittance_runs", uniqueConstraints = {
    @UniqueConstraint(name = "uq_remittance_period", columnNames = {"period"})
})
public class RemittanceRun {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "period", nullable = false, updatable = false, length = 7)
    private String period;

    @Column(name = "total_withheld", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal totalWithheld;

    @Column(name = "posting_count", nullable = false, updatable = false)
    private int postingCount;

    @Column(name = "collected_at", nullable = false, updatable = false)
    private Instant collectedAt;

    protected RemittanceRun() {}

    public RemittanceRun(UUID id, String period, BigDecimal totalWithheld, int postingCount, Instant collectedAt) {
        this.id = id;
        this.period = period;
        this.totalWithheld = totalWithheld;
        this.postingCount = postingCount;
        this.collectedAt = collectedAt;
    }

    public UUID getId() { return id; }
    public String getPeriod() { return period; }
    public BigDecimal getTotalWithheld() { return totalWithheld; }
    public int getPostingCount() { return postingCount; }
    public Instant getCollectedAt() { return collectedAt; }
}
