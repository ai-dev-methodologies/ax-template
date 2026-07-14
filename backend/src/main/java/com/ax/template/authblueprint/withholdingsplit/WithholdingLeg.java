package com.ax.template.authblueprint.withholdingsplit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One immutable leg of a {@link WithholdingPosting} (WHT-SPLIT-001). Exactly two legs are written
 * per posting — WITHHOLDING and NET — and {@code amount(WITHHOLDING) + amount(NET) == gross} exactly
 * BY CONSTRUCTION (the service derives NET as {@code gross - withholding}, never independently).
 * Append-only: every column {@code updatable = false}, no public setter, no delete path.
 */
@AggregateMember(root = WithholdingPosting.class)
@Entity
@Table(name = "withholding_legs")
public class WithholdingLeg {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "posting_id", nullable = false, updatable = false)
    private UUID postingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "leg_type", nullable = false, updatable = false, length = 20)
    private LegType legType;

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected WithholdingLeg() {}

    public WithholdingLeg(UUID id, UUID postingId, LegType legType, BigDecimal amount, Instant createdAt) {
        this.id = id;
        this.postingId = postingId;
        this.legType = legType;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getPostingId() { return postingId; }
    public LegType getLegType() { return legType; }
    public BigDecimal getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
