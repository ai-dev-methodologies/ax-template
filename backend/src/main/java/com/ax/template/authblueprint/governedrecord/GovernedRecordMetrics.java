package com.ax.template.authblueprint.governedrecord;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code governed_change_total
 * {operation, outcome}}, both fixed enums — never a datum id, field name, value, or actor.
 */
@Component
public class GovernedRecordMetrics {

    private final MeterRegistry registry;

    public GovernedRecordMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param operation create | change ;
     *  @param outcome ok | rejected | reason_required | unknown_reason | conflict */
    public void record(String operation, String outcome) {
        registry.counter("governed_change_total", "operation", operation, "outcome", outcome).increment();
    }
}
