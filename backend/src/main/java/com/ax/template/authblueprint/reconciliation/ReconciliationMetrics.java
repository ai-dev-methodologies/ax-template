package com.ax.template.authblueprint.reconciliation;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code reconciliation_total
 * {op, outcome}}, both fixed enums — never a run id, item key, source key, amount, or actor.
 */
@Component
public class ReconciliationMetrics {

    private final MeterRegistry registry;

    public ReconciliationMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op run | dispose | resolve ;
     *  @param outcome created | replayed | disposed | resolved | not_a_break | blank_reason |
     *                 already_disposed | undisposed_break */
    public void record(String op, String outcome) {
        registry.counter("reconciliation_total", "op", op, "outcome", outcome).increment();
    }
}
