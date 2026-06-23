package com.ax.template.authblueprint.statemutation;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code statemutation_total
 * {op, outcome}}, both fixed enums — never a form id, owner, field value, or reason text.
 */
@Component
public class StateMutationMetrics {

    private final MeterRegistry registry;

    public StateMutationMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op open | edit | transition ;
     *  @param outcome ok | edited | transitioned | field_locked | illegal_transition | reopen_no_reason */
    public void record(String op, String outcome) {
        registry.counter("statemutation_total", "op", op, "outcome", outcome).increment();
    }
}
