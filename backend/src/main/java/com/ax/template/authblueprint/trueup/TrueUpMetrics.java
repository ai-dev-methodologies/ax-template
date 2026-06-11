package com.ax.template.authblueprint.trueup;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code trueup_total
 * {op, outcome}}, both fixed enums — never a period id, label, value, or subject.
 */
@Component
public class TrueUpMetrics {

    private final MeterRegistry registry;

    public TrueUpMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op reading | estimate | recompute | close | seal ;
     *  @param outcome ok | idempotent | trued_up | rejected */
    public void record(String op, String outcome) {
        registry.counter("trueup_total", "op", op, "outcome", outcome).increment();
    }
}
