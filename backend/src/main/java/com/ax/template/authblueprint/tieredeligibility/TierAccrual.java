package com.ax.template.authblueprint.tieredeligibility;

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
 * tiered-eligibility-l0 APPEND-ONLY accrual ledger row (TIER-LADDER-001). Distinct from
 * {@link TierRestoreEvent} — the two ledgers are never mixed (TIER-MONOTONE-001 requires a restore be
 * recorded in a ledger SEPARATE from accruals). Every column is {@code @Column(updatable=false)}, no
 * setter, never UPDATEd or deleted.
 */
@AggregateMember(root = TierLadder.class)
@Entity
@Table(name = "tier_accruals",
    uniqueConstraints = @UniqueConstraint(name = "uq_tier_accrual_seq", columnNames = {"ladder_id", "sequence_no"}))
@Check(constraints = "delta > 0")
public class TierAccrual {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "ladder_id", nullable = false, updatable = false)
    private UUID ladderId;

    @Column(name = "delta", nullable = false, updatable = false)
    private int delta;

    @Column(name = "count_after", nullable = false, updatable = false)
    private int countAfter;

    @Column(name = "tier_index_after", nullable = false, updatable = false)
    private int tierIndexAfter;

    @Column(name = "sequence_no", nullable = false, updatable = false)
    private long sequenceNo;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    protected TierAccrual() {}

    public TierAccrual(UUID id, UUID ladderId, int delta, int countAfter, int tierIndexAfter,
                       long sequenceNo, Instant recordedAt) {
        this.id = id;
        this.ladderId = ladderId;
        this.delta = delta;
        this.countAfter = countAfter;
        this.tierIndexAfter = tierIndexAfter;
        this.sequenceNo = sequenceNo;
        this.recordedAt = recordedAt;
    }

    public UUID getId() { return id; }
    public UUID getLadderId() { return ladderId; }
    public int getDelta() { return delta; }
    public int getCountAfter() { return countAfter; }
    public int getTierIndexAfter() { return tierIndexAfter; }
    public long getSequenceNo() { return sequenceNo; }
    public Instant getRecordedAt() { return recordedAt; }
}
