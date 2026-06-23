package com.ax.template.authblueprint.recurringinterval;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code
 * recurring_interval_total {op, outcome}}, both fixed enums — never an obligation key, a
 * window instant, or a completer identity.
 */
@Component
public class RecurringIntervalMetrics {

    private final MeterRegistry registry;

    public RecurringIntervalMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op create | complete | sweep ;
     *  @param outcome ok | rejected | invalid | advanced | conflict | overdue | current */
    public void record(String op, String outcome) {
        registry.counter("recurring_interval_total", "op", op, "outcome", outcome).increment();
    }
}
