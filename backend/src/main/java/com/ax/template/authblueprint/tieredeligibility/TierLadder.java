package com.ax.template.authblueprint.tieredeligibility;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * tiered-eligibility-l0 ladder root: composes threshold-terminal-derivation-l0's crossing mechanism,
 * generalized from one binary ceiling to N ordered {@link TierDefinition} rungs. {@code currentTierIndex}
 * is ALWAYS the pure derived function of {@code count} against {@link #getTiers()} ({@link
 * #deriveTierIndex}) — the automatic accrual path can only INCREASE count (TIER-MONOTONE-001), and the
 * explicit {@code restore} path is the ONLY one allowed to decrease it, so tier and count can never
 * disagree. {@code count}/{@code currentTierIndex} move ONLY via the package-private {@link #applyCount},
 * called EXCLUSIVELY by {@link TierLadderStateMachine} (sole mutator), always under a PESSIMISTIC_WRITE row
 * lock. {@code @Version} backstops; the tiers list itself is immutable after creation.
 */
@AggregateRoot
@Entity
@Table(name = "tier_ladders")
// TIER-DERIVE-001 — basic sanity backstop. The FULL count-vs-tier consistency check requires the tiers
// list (a child table) and is therefore NOT expressible as a single static @Check here — enforced in
// application code (deriveTierIndex is the single source of truth both writers call).
@Check(constraints = "ladder_count >= 0 AND current_tier_index >= 0")
public class TierLadder {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "ladder_key", nullable = false, updatable = false, length = 200, unique = true)
    private String ladderKey;

    // EAGER — the tiers are read on every request-scope DTO mapping (controller-side, outside the
    // transaction), so LAZY would throw LazyInitializationException once the session closes (mirrors
    // TaxLine's @ElementCollection(fetch = FetchType.EAGER) precedent in this catalog).
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tier_ladder_tiers", joinColumns = @JoinColumn(name = "ladder_id"))
    @OrderColumn(name = "ordinal")
    private List<TierDefinition> tiers;

    @Column(name = "ladder_count", nullable = false)
    private int count;

    @Column(name = "current_tier_index", nullable = false)
    private int currentTierIndex;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TierLadder() {}

    public TierLadder(UUID id, String ladderKey, List<TierDefinition> tiers, int count, Instant createdAt) {
        this.id = id;
        this.ladderKey = ladderKey;
        this.tiers = tiers;
        this.count = count;
        this.currentTierIndex = deriveTierIndex(count);
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook ({@link TierLadderStateMachine} only, under the row lock). */
    void applyCount(int newCount, int newTierIndex) {
        this.count = newCount;
        this.currentTierIndex = newTierIndex;
    }

    /** Pure derivation — the largest tier index whose {@code enterAtCount <= count}. Tier 0's is always 0,
     *  so this is total over {@code count >= 0}. The SINGLE source of truth both accrue and restore use. */
    int deriveTierIndex(int forCount) {
        int idx = 0;
        for (int i = 0; i < tiers.size(); i++) {
            if (tiers.get(i).getEnterAtCount() <= forCount) {
                idx = i;
            }
        }
        return idx;
    }

    public UUID getId() { return id; }
    public String getLadderKey() { return ladderKey; }
    public List<TierDefinition> getTiers() { return tiers; }
    public int getCount() { return count; }
    public int getCurrentTierIndex() { return currentTierIndex; }
    public String getCurrentTierName() { return tiers.get(currentTierIndex).getName(); }
    public boolean isAtWorstTier() { return currentTierIndex == tiers.size() - 1; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
