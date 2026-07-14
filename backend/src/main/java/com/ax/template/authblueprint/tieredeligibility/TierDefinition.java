package com.ax.template.authblueprint.tieredeligibility;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * tiered-eligibility-l0 value object — one rung of the ladder. {@code enterAtCount} is the count at which
 * this tier is entered (tier 0's is always 0). Sorted ascending by {@code enterAtCount} as stored on
 * {@link TierLadder#getTiers()}.
 */
@Embeddable
public class TierDefinition {

    @Column(name = "tier_name", nullable = false, length = 100)
    private String name;

    @Column(name = "enter_at_count", nullable = false)
    private int enterAtCount;

    protected TierDefinition() {}

    public TierDefinition(String name, int enterAtCount) {
        this.name = name;
        this.enterAtCount = enterAtCount;
    }

    public String getName() { return name; }
    public int getEnterAtCount() { return enterAtCount; }
}
