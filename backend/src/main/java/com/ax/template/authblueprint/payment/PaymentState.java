package com.ax.template.authblueprint.payment;

/**
 * Payment state machine states. Mirrors blueprints/payment-manifest.yaml#state_machine.states.
 *
 * <p>UNKNOWN is a terminal holding state entered when the provider times out
 * or connection is reset — reconciliation job resolves UNKNOWN asynchronously.
 * FAILED is a terminal state set on declined / 5xx-exhausted / malformed-response paths.
 */
public enum PaymentState {
    CREATED,
    AUTHORIZED,
    CAPTURED,
    VOIDED,
    REFUNDED,
    PARTIAL_REFUNDED,
    UNKNOWN,
    FAILED
}
