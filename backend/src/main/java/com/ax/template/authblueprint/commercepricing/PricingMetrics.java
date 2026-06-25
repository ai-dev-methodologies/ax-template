package com.ax.template.authblueprint.commercepricing;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality metrics for the pricing engine. Tags use only categorical values
 * (never order ids or SKU refs) to prevent cardinality explosion.
 */
@Component
public class PricingMetrics {

    private final MeterRegistry registry;

    public PricingMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordQuote(String result) {
        Counter.builder("pricing_quote_total")
            .tag("result", result)
            .register(registry)
            .increment();
    }
}
