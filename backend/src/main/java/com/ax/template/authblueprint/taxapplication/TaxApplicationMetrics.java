package com.ax.template.authblueprint.taxapplication;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality metrics for tax-application. The recompute outcome is recorded as a
 * categorical label (one of a fixed small set — never an order id or amount), so the convergence
 * behavior is observable without unbounded cardinality.
 */
@Component
public class TaxApplicationMetrics {

    private final MeterRegistry registry;

    public TaxApplicationMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param outcome {@code created} | {@code updated} | {@code removed} | {@code exempt_zero}. */
    public void recordRecompute(String outcome) {
        Counter.builder("tax_application_recompute_total")
            .tag("outcome", outcome)
            .register(registry)
            .increment();
    }

    public void recordOrderCreated() {
        Counter.builder("tax_application_order_created_total")
            .register(registry)
            .increment();
    }
}
