package com.ax.template.authblueprint.payment;

/**
 * Event types recorded in the immutable payment_events ledger.
 * Mirrors blueprints/payment-manifest.yaml#ledger.
 */
public enum PaymentEventType {
    PAYMENT_CREATED,
    AUTHORIZED,
    CAPTURED,
    VOIDED,
    REFUNDED,
    PARTIAL_REFUNDED,
    UNKNOWN_STATE_REACHED,
    FAILED,
    ADMIN_OVERRIDE,
    RECONCILIATION_DRIFT
}
