package com.ax.template.authblueprint.dunning;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code dunning_total
 * {op, outcome}}, both fixed enums — never a case id, receivable ref, amount, or actor.
 */
@Component
public class DunningMetrics {

    private final MeterRegistry registry;

    public DunningMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op open | advance | reage | pay | cure ;
     *  @param outcome ok | advanced | cured | already_reached | terminal | no_window | invalid */
    public void record(String op, String outcome) {
        registry.counter("dunning_total", "op", op, "outcome", outcome).increment();
    }
}
