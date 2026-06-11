package com.ax.template.authblueprint.trueup;

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
 * One immutable forward-posted correction (TUP-DELTA-001): the NET of the recalculated total
 * vs everything previously settled for the source period, posted into an OPEN target period.
 * Fully append-only; uq(run_id) makes a double-post unrepresentable even if the lock
 * discipline regresses (TUP-CONCURRENT-001).
 */
@AggregateMember(root = SettlementRun.class)
@Entity
@Table(name = "trueup_postings", uniqueConstraints = {
    @UniqueConstraint(name = "uq_trueup_run", columnNames = {"run_id"})
})
public class TrueUpPosting {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The run whose recomputation produced this posting — one posting per run. */
    @Column(name = "run_id", nullable = false, updatable = false)
    private UUID runId;

    @Column(name = "source_period_id", nullable = false, updatable = false)
    private UUID sourcePeriodId;

    @Column(name = "target_period_id", nullable = false, updatable = false)
    private UUID targetPeriodId;

    @Column(name = "from_run_version", nullable = false, updatable = false)
    private int fromRunVersion;

    @Column(name = "to_run_version", nullable = false, updatable = false)
    private int toRunVersion;

    @Column(name = "amount", nullable = false, updatable = false, precision = 15, scale = 4)
    private BigDecimal amount;

    @Column(name = "posted_at", nullable = false, updatable = false)
    private Instant postedAt;

    protected TrueUpPosting() {}

    public TrueUpPosting(UUID id, UUID runId, UUID sourcePeriodId, UUID targetPeriodId,
                         int fromRunVersion, int toRunVersion, BigDecimal amount, Instant postedAt) {
        this.id = id;
        this.runId = runId;
        this.sourcePeriodId = sourcePeriodId;
        this.targetPeriodId = targetPeriodId;
        this.fromRunVersion = fromRunVersion;
        this.toRunVersion = toRunVersion;
        this.amount = amount;
        this.postedAt = postedAt;
    }

    public UUID getId() { return id; }
    public UUID getRunId() { return runId; }
    public UUID getSourcePeriodId() { return sourcePeriodId; }
    public UUID getTargetPeriodId() { return targetPeriodId; }
    public int getFromRunVersion() { return fromRunVersion; }
    public int getToRunVersion() { return toRunVersion; }
    public BigDecimal getAmount() { return amount; }
    public Instant getPostedAt() { return postedAt; }
}
