package com.ax.template.authblueprint.payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Abstraction over external payment processors (Stripe, Toss, etc.). The interface
 * is the "AI2-3 paper exercise" target — both Stripe v2 and Toss v2 must paper-map
 * onto this surface without breaking changes when iterations B/C add real adapters.
 *
 * <p>Result variants ({@link Outcome}) are an explicit sealed-like enum to make
 * provider failure modes (timeout vs decline vs malformed) first-class.
 */
public interface PaymentProvider {

    /**
     * Authorize-and-capture in a single round-trip (tokenization-style PGs:
     * Stripe / Toss V2). Returns a {@link ProviderResponse} describing outcome.
     */
    ProviderResponse authorizeAndCapture(AuthorizationRequest request);

    /**
     * Server-to-server approve/capture after a redirect-style PG callback has
     * already verified the signature and surfaced the provider-issued TID
     * (KG이니시스 / NICE페이먼츠 / KCP / Toss V1).
     *
     * <p>Tokenization-style adapters keep the default — they delegate to
     * {@link #authorizeAndCapture(AuthorizationRequest)} because they never
     * enter the callback path (their {@code paymentMethodToken} is the
     * client-issued token, not a callback TID).
     *
     * <p>Redirect-style adapters override this method to issue the
     * server-to-server REST approval against the PG, using {@code verifiedTid}
     * (the TID already signature-checked by {@code PaymentCallbackVerifier})
     * as the canonical token. The {@code request.paymentMethodToken()} field
     * in the redirect-style flow carries the same {@code verifiedTid}.
     *
     * <p>Spec anchor: specs/payment-l0.yaml#PAYMENT-CALLBACK-001..003,
     * blueprints/payment-manifest.yaml#callback.
     *
     * @param verifiedTid PG-issued TID after callback signature verification
     * @param signedPayload the original (verified) callback payload, opaque to
     *        the interface; adapters may need it for second-stage approval
     * @param request the standard authorization request (amount/currency/etc.);
     *        for redirect-style this is reconstructed by the callback handler
     */
    default ProviderResponse captureFromCallback(
        String verifiedTid,
        String signedPayload,
        AuthorizationRequest request
    ) {
        // Tokenization-style fallback. Redirect-style adapters MUST override.
        return authorizeAndCapture(request);
    }

    enum Outcome {
        APPROVED,
        DECLINED,
        TIMEOUT,
        NETWORK_RESET,
        SERVER_ERROR,
        MALFORMED,
        IDEMPOTENT_REPLAY
    }

    record AuthorizationRequest(
        UUID paymentId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String paymentMethodToken,
        String idempotencyKey,
        FailureMode failureMode
    ) {}

    /**
     * Provider response. Fields:
     * <ul>
     *   <li>{@link #outcome}: one of {@link Outcome}.</li>
     *   <li>{@link #providerRef}: opaque provider-side identifier (mock UUID).</li>
     *   <li>{@link #declineReason}: enum string like INSUFFICIENT_FUNDS / SERVER_ERROR.</li>
     *   <li>{@link #attempts}: number of attempts taken (for 5xx retry telemetry).</li>
     * </ul>
     */
    record ProviderResponse(
        Outcome outcome,
        String providerRef,
        String declineReason,
        int attempts
    ) {}

    /**
     * Injectable failure modes for {@link MockProvider}; mirrors
     * blueprints/payment-manifest.yaml#provider.failure_modes.
     */
    enum FailureMode {
        APPROVED,
        TIMEOUT,
        HTTP_5XX,
        HTTP_4XX_DECLINE,
        MALFORMED_RESPONSE,
        NETWORK_RESET,
        IDEMPOTENCY_REPLAY,
        /**
         * PAYMENT-PROVIDER-007: simulated slow-but-successful response. MockProvider
         * sleeps for a short interval before returning APPROVED. Tests lower the
         * {@code payment.provider.slow-threshold-ms} property so the decorator
         * observes the call as slow and emits a WARN + counter increment.
         */
        SLOW_RESPONSE
    }
}
