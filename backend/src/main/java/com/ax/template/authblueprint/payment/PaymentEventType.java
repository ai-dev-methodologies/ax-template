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
    RECONCILIATION_DRIFT,

    // Callback family (dogfood-4 — P1 R4 G7 closure). Redirect-style PG
    // callback events (KG이니시스 / NICE페이먼츠 / KCP / Toss V1). spec
    // anchors: specs/payment-l0.yaml#PAYMENT-CALLBACK-001..003,
    // blueprints/payment-manifest.yaml#callback. Distinct from FAILED
    // (provider decline at authorize/capture) so reconciliation and
    // observability can separate signature-fraud / state-reject paths
    // from genuine provider declines.

    /** Every callback POST entry (signature passed OR failed) — observability anchor for callback throughput. */
    CALLBACK_RECEIVED,

    /** PAYMENT-CALLBACK-001: signature verification failed. Payment state UNCHANGED; audit-only row for forensics. */
    CALLBACK_SIGNATURE_FAIL,

    /** PAYMENT-CALLBACK-003: callback arrived for a Payment in CREATED or REFUNDED state — rejected with HTTP 409. Payment state UNCHANGED. */
    CALLBACK_STATE_REJECTED
}
