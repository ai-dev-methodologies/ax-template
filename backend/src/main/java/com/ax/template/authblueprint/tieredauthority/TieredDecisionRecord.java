package com.ax.template.authblueprint.tieredauthority;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * TieredDecisionRecord — an immutable, append-only decision log entry (ATA-SNAPSHOT-001).
 * Every column is {@code updatable=false} and there is no public setter: the record fixes,
 * forever, the tier-table version and band the decision was evaluated against and the
 * decider's authority level AT DECISION TIME, so a later reconfiguration of the
 * {@link AuthorityTierTable} can never retroactively change what this record reports.
 */
@AggregateMember(root = AuthorityTierTable.class)
@Entity
@Table(name = "tiered_decision_records")
public class TieredDecisionRecord {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "table_id", nullable = false, updatable = false)
    private UUID tableId;

    @Column(name = "table_version", nullable = false, updatable = false)
    private int tableVersion;

    @Column(name = "amount", nullable = false, updatable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "band_min_amount", nullable = false, updatable = false, precision = 15, scale = 2)
    private BigDecimal bandMinAmount;

    @Column(name = "band_max_amount", updatable = false, precision = 15, scale = 2)
    private BigDecimal bandMaxAmount;

    @Column(name = "band_min_decider_level", nullable = false, updatable = false)
    private int bandMinDeciderLevel;

    @Column(name = "decider_level", nullable = false, updatable = false)
    private int deciderLevel;

    @Column(name = "outcome", length = 500, updatable = false)
    private String outcome;

    @Column(name = "decided_by", nullable = false, updatable = false, length = 200)
    private String decidedBy;

    @Column(name = "decided_at", nullable = false, updatable = false)
    private Instant decidedAt;

    protected TieredDecisionRecord() {}

    public TieredDecisionRecord(UUID id, UUID tableId, int tableVersion, BigDecimal amount,
                                BigDecimal bandMinAmount, BigDecimal bandMaxAmount, int bandMinDeciderLevel,
                                int deciderLevel, String outcome, String decidedBy, Instant decidedAt) {
        this.id = id;
        this.tableId = tableId;
        this.tableVersion = tableVersion;
        this.amount = amount;
        this.bandMinAmount = bandMinAmount;
        this.bandMaxAmount = bandMaxAmount;
        this.bandMinDeciderLevel = bandMinDeciderLevel;
        this.deciderLevel = deciderLevel;
        this.outcome = outcome;
        this.decidedBy = decidedBy;
        this.decidedAt = decidedAt;
    }

    public UUID getId() { return id; }
    public UUID getTableId() { return tableId; }
    public int getTableVersion() { return tableVersion; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBandMinAmount() { return bandMinAmount; }
    public BigDecimal getBandMaxAmount() { return bandMaxAmount; }
    public int getBandMinDeciderLevel() { return bandMinDeciderLevel; }
    public int getDeciderLevel() { return deciderLevel; }
    public String getOutcome() { return outcome; }
    public String getDecidedBy() { return decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
}
