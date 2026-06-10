package com.ax.template.authblueprint.decisiongov;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code decision_version_total
 * {op, outcome}}, both fixed enums — never a scope key, basis, outcome value, or identity.
 */
@Component
public class DecisionMetrics {

    private final MeterRegistry registry;

    public DecisionMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op compute | recompute | override ;
     *  @param outcome ok | rejected | invalid */
    public void record(String op, String outcome) {
        registry.counter("decision_version_total", "op", op, "outcome", outcome).increment();
    }
}
