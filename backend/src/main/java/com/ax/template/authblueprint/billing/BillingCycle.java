package com.ax.template.authblueprint.billing;

/**
 * Plan billing cadence.
 * <p>Trace: blueprints/billing-manifest.yaml#state_machine — periodic renewal
 * cycles are normalized to one of these.
 */
public enum BillingCycle {
    MONTHLY,
    YEARLY
}
