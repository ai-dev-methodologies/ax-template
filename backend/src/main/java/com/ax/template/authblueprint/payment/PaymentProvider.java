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
     * Authorize-and-capture in a single round-trip (mock-friendly; real providers
     * may split this). Returns a {@link ProviderResponse} describing outcome.
     */
    ProviderResponse authorizeAndCapture(AuthorizationRequest request);

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
