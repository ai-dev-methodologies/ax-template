package com.ax.template.authblueprint.commerceorder;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded Micrometer meters for commerce orders.
 * Only {@code result} tag — no order ids in cardinality (METRICS-CARDINALITY-001).
 */
@Component
public class CommerceOrderMetrics {

    private final MeterRegistry registry;

    public CommerceOrderMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordOrderTotal(String result) {
        Counter.builder("commerce_order_total")
            .tag("result", result)
            .register(registry)
            .increment();
    }
}
