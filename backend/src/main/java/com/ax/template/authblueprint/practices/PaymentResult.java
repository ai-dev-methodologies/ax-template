package com.ax.template.authblueprint.practices;

/**
 * Fixture for PRACTICES-LANG-002: a sealed result hierarchy with the two terminal
 * outcomes as records. Exhaustive pattern-matching switch over a `PaymentResult`
 * forces the caller to handle every branch — the compiler refuses to forget one.
 */
public sealed interface PaymentResult permits PaymentResult.PaymentSuccess, PaymentResult.PaymentFailure {

    record PaymentSuccess(String transactionId, long amount) implements PaymentResult {}

    record PaymentFailure(String errorCode, String message) implements PaymentResult {}
}
