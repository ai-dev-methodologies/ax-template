package com.ax.template.authblueprint.countbudget;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code count_budget_op_total
 * {op, outcome}}, both fixed enums — never a subject key, period key, or cap value.
 */
@Component
public class CountBudgetMetrics {

    private final MeterRegistry registry;

    public CountBudgetMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op create | update_cap | consume ;
     *  @param outcome ok | first_touch | exhausted | invalid | rejected */
    public void record(String op, String outcome) {
        registry.counter("count_budget_op_total", "op", op, "outcome", outcome).increment();
    }
}
