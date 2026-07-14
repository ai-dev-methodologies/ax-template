package com.ax.template.authblueprint.tieredeligibility;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code tier_ladder_op_total
 * {op, outcome}}, both fixed enums — never a ladder key, count, or tier name.
 */
@Component
public class TierMetrics {

    private final MeterRegistry registry;

    public TierMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op create | accrue | use | restore ;
     *  @param outcome ok | crossed | suspended | invalid | rejected */
    public void record(String op, String outcome) {
        registry.counter("tier_ladder_op_total", "op", op, "outcome", outcome).increment();
    }
}
