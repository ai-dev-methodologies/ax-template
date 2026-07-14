package com.ax.template.authblueprint.additivefacts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * additive-fact-ledger-l0 forward posting (FACT-LATE-DELTA-POST-002) — a late fact that
 * arrived for a CLOSED origin period posts here, into the CURRENT OPEN period, instead of
 * mutating the origin's frozen aggregate. Fully immutable and append-only; conservation:
 * origin.frozenAggregate + Σ(postings referencing that origin) == Σ of every fact ever
 * assigned to the origin period, including late ones.
 */
@AggregateMember(root = FactPeriod.class)
@Entity
@Table(name = "late_delta_postings")
public class LateDeltaPosting {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The CURRENT open period this posting lives in — its root. */
    @Column(name = "current_period_id", nullable = false, updatable = false)
    private UUID currentPeriodId;

    /** The CLOSED period whose frozen aggregate this posting corrects forward for. */
    @Column(name = "origin_period_id", nullable = false, updatable = false)
    private UUID originPeriodId;

    @Column(name = "fact_id", nullable = false, updatable = false)
    private UUID factId;

    @Column(name = "amount", nullable = false, updatable = false, precision = 15, scale = 4)
    private BigDecimal amount;

    @Column(name = "posted_at", nullable = false, updatable = false)
    private Instant postedAt;

    protected LateDeltaPosting() {}

    public LateDeltaPosting(UUID id, UUID currentPeriodId, UUID originPeriodId, UUID factId,
                            BigDecimal amount, Instant postedAt) {
        this.id = id;
        this.currentPeriodId = currentPeriodId;
        this.originPeriodId = originPeriodId;
        this.factId = factId;
        this.amount = amount;
        this.postedAt = postedAt;
    }

    public UUID getId() { return id; }
    public UUID getCurrentPeriodId() { return currentPeriodId; }
    public UUID getOriginPeriodId() { return originPeriodId; }
    public UUID getFactId() { return factId; }
    public BigDecimal getAmount() { return amount; }
    public Instant getPostedAt() { return postedAt; }
}
