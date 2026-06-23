package com.ax.template.authblueprint.orderquantization;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code order_quantization_total
 * {outcome}}, a fixed enum — never an item ref, a required amount, an MOQ, a multiple, or an actor.
 */
@Component
public class OrderQuantizationMetrics {

    private final MeterRegistry registry;

    public OrderQuantizationMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param outcome quantized | invalid_constraint */
    public void record(String outcome) {
        registry.counter("order_quantization_total", "outcome", outcome).increment();
    }
}
