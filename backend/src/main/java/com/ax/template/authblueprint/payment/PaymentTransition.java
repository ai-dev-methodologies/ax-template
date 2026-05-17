package com.ax.template.authblueprint.payment;

/**
 * Transition events for the payment state machine.
 * Mirrors blueprints/payment-manifest.yaml#state_machine event types.
 */
public enum PaymentTransition {
    AUTHORIZE,
    CAPTURE,
    VOID,
    REFUND,
    PARTIAL_REFUND,
    PROVIDER_TIMEOUT,
    NETWORK_RESET,
    PROVIDER_DECLINE,
    PROVIDER_5XX_EXHAUSTED,
    PROVIDER_MALFORMED
}
