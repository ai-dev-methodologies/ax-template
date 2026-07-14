package com.ax.template.authblueprint.bilateralhandoff;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code handoff_total
 * {op, outcome}}, both fixed enums — never a party name or handoff id.
 */
@Component
public class HandoffMetrics {

    private final MeterRegistry registry;

    public HandoffMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op propose | confirm | decline ;
     *  @param outcome ok | idempotent | completed | voided | not_a_party | voided_late | not_open */
    public void record(String op, String outcome) {
        registry.counter("handoff_total", "op", op, "outcome", outcome).increment();
    }
}
