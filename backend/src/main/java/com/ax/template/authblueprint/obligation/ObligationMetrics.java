package com.ax.template.authblueprint.obligation;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code obligation_total
 * {op, outcome}}, both fixed enums — never an obligation key, axis value, or identity.
 */
@Component
public class ObligationMetrics {

    private final MeterRegistry registry;

    public ObligationMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op create | usage | ack | sweep ;
     *  @param outcome ok | rejected | invalid | fired | skipped */
    public void record(String op, String outcome) {
        registry.counter("obligation_total", "op", op, "outcome", outcome).increment();
    }
}
