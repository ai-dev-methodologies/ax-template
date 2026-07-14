package com.ax.template.authblueprint.routelegs;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code route_leg_total
 * {op, outcome}}, both fixed enums — never a route id, leg id, or place code.
 */
@Component
public class RouteLegMetrics {

    private final MeterRegistry registry;

    public RouteLegMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op create | append | insert | remove | replace | reorder ;
     *  @param outcome ok | sequence_violation | gap_violation | concurrent_modification */
    public void record(String op, String outcome) {
        registry.counter("route_leg_total", "op", op, "outcome", outcome).increment();
    }
}
