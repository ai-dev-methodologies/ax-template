package com.ax.template.authblueprint.inputplausibility;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code plausibility_total
 * {op, outcome}}, both fixed enums — never a channel id, subject ref, value, or actor.
 */
@Component
public class PlausibilityMetrics {

    private final MeterRegistry registry;

    public PlausibilityMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op define | submit ;
     *  @param outcome ok | accepted | implausible_range | implausible_rate | invalid */
    public void record(String op, String outcome) {
        registry.counter("plausibility_total", "op", op, "outcome", outcome).increment();
    }
}
