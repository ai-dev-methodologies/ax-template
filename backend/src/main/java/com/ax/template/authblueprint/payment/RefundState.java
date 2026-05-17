package com.ax.template.authblueprint.payment;

/**
 * Lifecycle of a single Refund row.
 * COMPLETED is the only success path; refunds are processed inline in P3.0.
 */
public enum RefundState {
    PROCESSING,
    COMPLETED,
    FAILED
}
