package com.ax.template.authblueprint.thresholdterminal;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code threshold_register_total
 * {op, outcome}}, both fixed enums — never a register id, scope key, anchor, or limit value.
 */
@Component
public class ThresholdMetrics {

    private final MeterRegistry registry;

    public ThresholdMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op create | accrue | use ;
     *  @param outcome ok | crossed | terminal | invalid | rejected */
    public void record(String op, String outcome) {
        registry.counter("threshold_register_total", "op", op, "outcome", outcome).increment();
    }
}
