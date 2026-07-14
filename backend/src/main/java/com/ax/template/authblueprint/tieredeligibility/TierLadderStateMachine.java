package com.ax.template.authblueprint.tieredeligibility;

import org.springframework.stereotype.Component;

/**
 * tiered-eligibility-l0 sole mutator of {@code count}/{@code currentTierIndex}. Two structurally DISTINCT
 * entry points enforce direction: {@link #degrade} is the AUTOMATIC accrual path (count only ever
 * increases; tier only ever holds or worsens — TIER-MONOTONE-001); {@link #restore} is the ONLY path that
 * may decrease count (and therefore improve tier), reserved for {@link TierLadderService}'s explicit,
 * audited restore action. Both call {@link TierLadder#applyCount} — the entity itself has no other public
 * mutation surface.
 */
@Component
public class TierLadderStateMachine {

    /** TIER-LADDER-001 / TIER-MONOTONE-001 — count strictly increases; the derived tier never improves. */
    void degrade(TierLadder ladder, int newCount, int newTierIndex) {
        if (newCount <= ladder.getCount()) {
            throw new IllegalStateException("degrade() requires a strictly-increasing count (accrual-only path)");
        }
        if (newTierIndex < ladder.getCurrentTierIndex()) {
            throw new IllegalStateException("degrade() can never improve the tier — that is restore()'s job");
        }
        ladder.applyCount(newCount, newTierIndex);
    }

    /** TIER-MONOTONE-001 — the ONLY path that may decrease count; tier can only hold or improve as a result. */
    void restore(TierLadder ladder, int newCount, int newTierIndex) {
        if (newCount >= ladder.getCount()) {
            throw new IllegalStateException("restore() requires a strictly-decreasing count — a no-op restore is not meaningful");
        }
        if (newTierIndex > ladder.getCurrentTierIndex()) {
            throw new IllegalStateException("restore() can never worsen the tier — that is degrade()'s job");
        }
        ladder.applyCount(newCount, newTierIndex);
    }
}
