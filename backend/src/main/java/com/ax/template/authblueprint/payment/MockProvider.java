package com.ax.template.authblueprint.payment;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-process payment provider that simulates the 6 failure modes from
 * blueprints/payment-manifest.yaml#provider.failure_modes plus the default APPROVED path.
 *
 * <p>Failure mode is selected per-call via the {@code failureMode} field on the
 * {@link PaymentProvider.AuthorizationRequest} (typically driven by the
 * X-Test-Provider-Mode request header in tests). HTTP_5XX retries with exponential
 * backoff up to {@link #maxRetries}; tests use short delays to avoid 7+ second waits.
 *
 * <p>IDEMPOTENCY_REPLAY mode caches the first response keyed by idempotencyKey and
 * returns the cached response on subsequent calls — simulating a provider's own
 * idempotency-key replay.
 *
 * <p><b>Bean naming contract (R14 GAP-A closure):</b> registered under the bean name
 * {@code rawPaymentProvider}. {@link SlowProviderLatencyDecorator} resolves the raw
 * (un-decorated) provider via {@code @Qualifier("rawPaymentProvider")} so the decorator
 * never depends on the concrete {@code MockProvider} type. Fork-receivers adding a real
 * PG adapter (Stripe / Toss / KG Inicis / NICE / KCP) MUST register that adapter under
 * the same bean name {@code rawPaymentProvider} (typically {@code @Component("rawPaymentProvider")})
 * and disable {@code MockProvider} via profile/property gating so only one raw provider
 * exists at runtime. Enforced mechanically by
 * {@code practices/evals/payment_provider_qualifier_consistency_guard.sh} (36th hard guard).
 */
@Component("rawPaymentProvider")
public class MockProvider implements PaymentProvider {

    private static final int DEFAULT_MAX_RETRIES = 3;

    /** Per-test-key cached responses for IDEMPOTENCY_REPLAY simulation. */
    private final ConcurrentHashMap<String, ProviderResponse> replayCache = new ConcurrentHashMap<>();

    private final int maxRetries;

    public MockProvider() {
        this(DEFAULT_MAX_RETRIES);
    }

    public MockProvider(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /** Sleep used by {@link FailureMode#SLOW_RESPONSE} to trip the slow-call decorator. */
    private static final long SLOW_RESPONSE_SLEEP_MS = 100L;

    @Override
    public ProviderResponse authorizeAndCapture(AuthorizationRequest request) {
        FailureMode mode = request.failureMode() == null ? FailureMode.APPROVED : request.failureMode();
        return switch (mode) {
            case APPROVED -> approved();
            case TIMEOUT -> new ProviderResponse(Outcome.TIMEOUT, null, null, 1);
            case NETWORK_RESET -> new ProviderResponse(Outcome.NETWORK_RESET, null, null, 1);
            case HTTP_5XX -> retry5xx();
            case HTTP_4XX_DECLINE -> new ProviderResponse(
                Outcome.DECLINED, "mock-ref-" + UUID.randomUUID(), "INSUFFICIENT_FUNDS", 1);
            case MALFORMED_RESPONSE -> new ProviderResponse(
                Outcome.MALFORMED, null, "SERIALIZATION_ERROR", 1);
            case IDEMPOTENCY_REPLAY -> idempotencyReplay(request);
            case SLOW_RESPONSE -> slowApproved();
        };
    }

    private ProviderResponse slowApproved() {
        try {
            Thread.sleep(SLOW_RESPONSE_SLEEP_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return approved();
    }

    private ProviderResponse approved() {
        return new ProviderResponse(Outcome.APPROVED, "mock-ref-" + UUID.randomUUID(), null, 1);
    }

    private ProviderResponse retry5xx() {
        // Exponential backoff simulated with no actual sleep — tests run fast.
        // All attempts return 5xx so we exhaust max retries → FAILED with SERVER_ERROR.
        return new ProviderResponse(Outcome.SERVER_ERROR, null, "SERVER_ERROR", maxRetries);
    }

    private ProviderResponse idempotencyReplay(AuthorizationRequest request) {
        String key = request.idempotencyKey() == null ? "_anon" : request.idempotencyKey();
        return replayCache.computeIfAbsent(key, k -> approved());
    }
}
