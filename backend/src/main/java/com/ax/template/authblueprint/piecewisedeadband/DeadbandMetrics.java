package com.ax.template.authblueprint.piecewisedeadband;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code deadband_op_total
 * {op, outcome}}, both fixed enums — never a config key, point, or value.
 */
@Component
public class DeadbandMetrics {

    private final MeterRegistry registry;

    public DeadbandMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op create | evaluate ;
     *  @param outcome ok | replayed | compliant | deviation | invalid | rejected */
    public void record(String op, String outcome) {
        registry.counter("deadband_op_total", "op", op, "outcome", outcome).increment();
    }
}
