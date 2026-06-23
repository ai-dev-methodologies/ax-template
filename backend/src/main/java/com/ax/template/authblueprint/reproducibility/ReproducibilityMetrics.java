package com.ax.template.authblueprint.reproducibility;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code reproducibility_total
 * {op, outcome}}, both fixed enums — never a procedure id, input hash, seed, subject, or actor.
 */
@Component
public class ReproducibilityMetrics {

    private final MeterRegistry registry;

    public ReproducibilityMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op draw | classify | replay | unmask ;
     *  @param outcome ok | idempotent | diverged | not_replayable */
    public void record(String op, String outcome) {
        registry.counter("reproducibility_total", "op", op, "outcome", outcome).increment();
    }
}
