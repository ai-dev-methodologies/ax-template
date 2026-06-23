package com.ax.template.authblueprint.divisibility;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code divisibility_total
 * {op, outcome}}, both fixed enums — never a material ref, a quantity, or an actor.
 */
@Component
public class DivisibilityMetrics {

    private final MeterRegistry registry;

    public DivisibilityMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op declare | check ;
     *  @param outcome declared | accepted | non_integral | excess_precision | invalid */
    public void record(String op, String outcome) {
        registry.counter("divisibility_total", "op", op, "outcome", outcome).increment();
    }
}
