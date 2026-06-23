package com.ax.template.authblueprint.valuationrun;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code valuation_run_total
 * {op, outcome}}, both fixed enums — never a subject id, position ref, basis, amount, or as-of.
 */
@Component
public class ValuationRunMetrics {

    private final MeterRegistry registry;

    public ValuationRunMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op create | recompute | rebase | as_of_read ;
     *  @param outcome ok | conflict | not_conserved | not_current | not_found | empty */
    public void record(String op, String outcome) {
        registry.counter("valuation_run_total", "op", op, "outcome", outcome).increment();
    }
}
