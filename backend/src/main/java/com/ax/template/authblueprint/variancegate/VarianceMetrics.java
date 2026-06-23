package com.ax.template.authblueprint.variancegate;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code variance_total
 * {op, outcome}}, both fixed enums — never a subject, an appraisal id, an actor, or an amount.
 */
@Component
public class VarianceMetrics {

    private final MeterRegistry registry;

    public VarianceMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op appraise | proceed | dispose ;
     *  @param outcome within | breach | blocked | proceeded | disposed | already_disposed | rejected */
    public void record(String op, String outcome) {
        registry.counter("variance_total", "op", op, "outcome", outcome).increment();
    }
}
