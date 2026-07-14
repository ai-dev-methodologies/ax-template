package com.ax.template.authblueprint.saturatingbalance;

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
 * One immutable ledger row (SATBAL-LEDGER-003): {@link #requestedAmount} is the magnitude the
 * caller asked for (always positive); {@link #appliedAmount} is SIGNED — positive for an
 * {@code ACCRUE}, negative for a {@code DEBIT} — so {@code Σ(appliedAmount)} for a balance
 * always reconciles to its current stored value. When clamping occurred,
 * {@code |appliedAmount| != requestedAmount} and BOTH remain on the permanent record — the
 * clamped remainder is never silently discarded. Fully append-only; no update path.
 */
@AggregateMember(root = Balance.class)
@Entity
@Table(name = "saturating_ledger_entries")
public class LedgerEntry {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "balance_id", nullable = false, updatable = false)
    private UUID balanceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "op", nullable = false, updatable = false, length = 10)
    private LedgerOp op;

    /** What the caller asked for — always a positive magnitude, verbatim. */
    @Column(name = "requested_amount", nullable = false, updatable = false, precision = 15, scale = 4)
    private BigDecimal requestedAmount;

    /** What actually changed the balance, post-clamp — SIGNED (+ accrue, − debit). */
    @Column(name = "applied_amount", nullable = false, updatable = false, precision = 15, scale = 4)
    private BigDecimal appliedAmount;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected LedgerEntry() {}

    public LedgerEntry(UUID id, UUID balanceId, LedgerOp op, BigDecimal requestedAmount,
                       BigDecimal appliedAmount, Instant occurredAt) {
        this.id = id;
        this.balanceId = balanceId;
        this.op = op;
        this.requestedAmount = requestedAmount;
        this.appliedAmount = appliedAmount;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public UUID getBalanceId() { return balanceId; }
    public LedgerOp getOp() { return op; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public BigDecimal getAppliedAmount() { return appliedAmount; }
    public Instant getOccurredAt() { return occurredAt; }
}
