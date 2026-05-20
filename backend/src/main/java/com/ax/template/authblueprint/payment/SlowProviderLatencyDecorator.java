package com.ax.template.authblueprint.payment;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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
 * other injection site receive the decorator transparently.
 *
 * <p><b>R14 GAP-A fix (qualifier consistency):</b> the delegate is resolved by the
 * interface {@link PaymentProvider} + {@code @Qualifier("rawPaymentProvider")} bean
 * name — NOT by the concrete {@link MockProvider} class. This unblocks fork-receivers
 * who replace {@code MockProvider} with a real PG adapter (Stripe / Toss / KG Inicis /
 * NICE / KCP): they register the new adapter under the same bean name
 * {@code rawPaymentProvider} and disable the mock via profile/property gating. The
 * decorator wires transparently in both cases. Enforced mechanically by
 * {@code practices/evals/payment_provider_qualifier_consistency_guard.sh} (36th hard guard).
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

    /** Bean-name contract — see class Javadoc. Locked by the 36th hard guard. */
    static final String RAW_PROVIDER_BEAN_NAME = "rawPaymentProvider";

    private final PaymentProvider delegate;
    private final MeterRegistry meterRegistry;
    private final SlowProviderProperties properties;

    public SlowProviderLatencyDecorator(@Qualifier(RAW_PROVIDER_BEAN_NAME) PaymentProvider delegate,
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
