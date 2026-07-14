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
 * tiered-eligibility-l0 APPEND-ONLY restore audit ledger row (TIER-MONOTONE-001). The ONLY record of the
 * ladder ever moving to a BETTER tier — always carries a non-blank {@code reason}, always in a ledger
 * distinct from {@link TierAccrual}. Every column is {@code @Column(updatable=false)}, no setter, never
 * UPDATEd or deleted.
 */
@AggregateMember(root = TierLadder.class)
@Entity
@Table(name = "tier_restore_events",
    uniqueConstraints = @UniqueConstraint(name = "uq_tier_restore_seq", columnNames = {"ladder_id", "sequence_no"}))
@Check(constraints = "reason <> ''")
public class TierRestoreEvent {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "ladder_id", nullable = false, updatable = false)
    private UUID ladderId;

    @Column(name = "count_after", nullable = false, updatable = false)
    private int countAfter;

    @Column(name = "tier_index_after", nullable = false, updatable = false)
    private int tierIndexAfter;

    @Column(name = "reason", nullable = false, updatable = false, length = 500)
    private String reason;

    @Column(name = "sequence_no", nullable = false, updatable = false)
    private long sequenceNo;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    protected TierRestoreEvent() {}

    public TierRestoreEvent(UUID id, UUID ladderId, int countAfter, int tierIndexAfter, String reason,
                            long sequenceNo, Instant recordedAt) {
        this.id = id;
        this.ladderId = ladderId;
        this.countAfter = countAfter;
        this.tierIndexAfter = tierIndexAfter;
        this.reason = reason;
        this.sequenceNo = sequenceNo;
        this.recordedAt = recordedAt;
    }

    public UUID getId() { return id; }
    public UUID getLadderId() { return ladderId; }
    public int getCountAfter() { return countAfter; }
    public int getTierIndexAfter() { return tierIndexAfter; }
    public String getReason() { return reason; }
    public long getSequenceNo() { return sequenceNo; }
    public Instant getRecordedAt() { return recordedAt; }
}
