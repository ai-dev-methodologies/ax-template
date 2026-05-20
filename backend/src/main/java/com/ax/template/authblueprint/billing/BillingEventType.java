package com.ax.template.authblueprint.billing;

/**
 * Canonical billing event taxonomy.
 * <p>Trace: blueprints/billing-manifest.yaml#state_machine — every legal
 * transition records exactly one of these as the audit anchor (BILLING-STATE-002).
 */
public enum BillingEventType {
    TRIAL_END,
    PAYMENT_SUCCEEDED,
    PAYMENT_FAILED,
    SUBSCRIPTION_CANCELLED,
    SUBSCRIPTION_RENEWED,
    /** Generic webhook envelope that did not match a known transition. */
    UNHANDLED
}
