package com.ax.template.authblueprint.billing;

/**
 * Subscription lifecycle states.
 * <p>
 * Trace: BILLING-STATE-001/002 — transitions are owned by
 * {@link SubscriptionStateMachine}. No other class may invoke
 * {@link Subscription#setStatus(SubscriptionStatus)}; the package-private
 * setter is exercised only via the state machine.
 */
public enum SubscriptionStatus {
    /** Free trial; converts to ACTIVE on TRIAL_END_WEBHOOK. */
    TRIAL,
    /** Paid + current. */
    ACTIVE,
    /** Last payment failed; webhook will retry. */
    PAST_DUE,
    /** Terminal — user cancel or unpaid threshold exceeded. */
    CANCELLED
}
