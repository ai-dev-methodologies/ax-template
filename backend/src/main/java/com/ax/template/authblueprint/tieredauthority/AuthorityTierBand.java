package com.ax.template.authblueprint.tieredauthority;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * AuthorityTierBand — one half-open amount band {@code [minAmount, maxAmount)} of an
 * {@link AuthorityTierTable} version, carrying the minimum decider level required to decide an
 * amount inside it (ATA-TIER-001). {@code maxAmount == null} means open-ended (the last band).
 * Bands are validated to tile without gap/overlap at {@link TieredAuthorityService#createTable}
 * time (ATA-BOUNDARY-001) — fully immutable once persisted (the table version itself is
 * append-only; a "change" is a whole new table version, never an edited band).
 */
@AggregateMember(root = AuthorityTierTable.class)
@Entity
@Table(name = "authority_tier_bands")
public class AuthorityTierBand {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "table_id", nullable = false, updatable = false)
    private UUID tableId;

    @Column(name = "order_index", nullable = false, updatable = false)
    private int orderIndex;

    @Column(name = "min_amount", nullable = false, updatable = false, precision = 15, scale = 2)
    private BigDecimal minAmount;

    /** {@code null} = open-ended (only the last band may be open-ended). */
    @Column(name = "max_amount", updatable = false, precision = 15, scale = 2)
    private BigDecimal maxAmount;

    @Column(name = "min_decider_level", nullable = false, updatable = false)
    private int minDeciderLevel;

    protected AuthorityTierBand() {}

    public AuthorityTierBand(UUID id, UUID tableId, int orderIndex, BigDecimal minAmount,
                             BigDecimal maxAmount, int minDeciderLevel) {
        this.id = id;
        this.tableId = tableId;
        this.orderIndex = orderIndex;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.minDeciderLevel = minDeciderLevel;
    }

    public UUID getId() { return id; }
    public UUID getTableId() { return tableId; }
    public int getOrderIndex() { return orderIndex; }
    public BigDecimal getMinAmount() { return minAmount; }
    public BigDecimal getMaxAmount() { return maxAmount; }
    public int getMinDeciderLevel() { return minDeciderLevel; }

    /** ATA-TIER-001 half-open match: {@code minAmount <= amount < maxAmount} (null = +∞). */
    public boolean covers(BigDecimal amount) {
        return amount.compareTo(minAmount) >= 0 && (maxAmount == null || amount.compareTo(maxAmount) < 0);
    }
}
