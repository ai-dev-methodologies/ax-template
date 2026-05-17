package com.ax.template.authblueprint.payment;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Thin observability decorator around {@link PaymentProvider} that measures
 * wall-clock latency of {@link #authorizeAndCapture} and, when the call exceeds
 * {@code payment.provider.slow-threshold-ms} (default 3000ms per
 * {@code blueprints/payment-manifest.yaml#provider.slow_threshold_ms}), emits a
 * WARN log and increments the {@code payment_provider_slow_total} Micrometer
 * counter.
 *
 * <p>This is the implementation of {@code specs/payment-l0.yaml#PAYMENT-PROVIDER-007}.
 * The decorator is marked {@link Primary @Primary} so {@link PaymentService} and any
 * other injection site receive the decorator transparently; {@link MockProvider} (and
 * future Stripe / Toss adapters) are injected as the delegate via concrete class type.
 *
 * <p>Slow detection is an observability cross-cut — the payment outcome is unchanged.
 * The decorator does not catch, mutate, or interpret the {@link ProviderResponse}; it
 * only times the call and emits side-effect telemetry on the slow path.
 */
@Component
@Primary
public class SlowProviderLatencyDecorator implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(SlowProviderLatencyDecorator.class);

    /** Counter name pinned to {@code blueprints/payment-manifest.yaml#observability.metrics}. */
    static final String SLOW_COUNTER = "payment_provider_slow_total";

    private final PaymentProvider delegate;
    private final MeterRegistry meterRegistry;
    private final SlowProviderProperties properties;

    public SlowProviderLatencyDecorator(MockProvider delegate,
                                        MeterRegistry meterRegistry,
                                        SlowProviderProperties properties) {
        this.delegate = delegate;
        this.meterRegistry = meterRegistry;
        this.properties = properties;
    }

    @Override
    public ProviderResponse authorizeAndCapture(AuthorizationRequest request) {
        long startNanos = System.nanoTime();
        try {
            return delegate.authorizeAndCapture(request);
        } finally {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
            if (elapsedMs > properties.slowThresholdMs()) {
                meterRegistry.counter(SLOW_COUNTER).increment();
                log.warn("slow provider response: elapsed_ms={} threshold_ms={} payment_id={} idempotency_key={}",
                    elapsedMs, properties.slowThresholdMs(),
                    request.paymentId(), request.idempotencyKey());
            }
        }
    }

    /**
     * Bound to manifest path {@code blueprints/payment-manifest.yaml#provider.slow_threshold_ms}.
     * Tests override via {@code @TestPropertySource("payment.provider.slow-threshold-ms=10")}.
     */
    @ConfigurationProperties(prefix = "payment.provider")
    public record SlowProviderProperties(long slowThresholdMs) {
        public SlowProviderProperties {
            if (slowThresholdMs <= 0) {
                slowThresholdMs = 3000L; // manifest default
            }
        }
    }
}
