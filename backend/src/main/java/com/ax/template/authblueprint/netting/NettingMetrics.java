package com.ax.template.authblueprint.netting;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code netting_total
 * {operation, outcome}}, both fixed enums — never a run key, member id, currency, or amount.
 */
@Component
public class NettingMetrics {

    private final MeterRegistry registry;

    public NettingMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param operation create | add | net ;
     *  @param outcome ok | rejected | run_not_open | invalid | currency_mismatch | already_netted | not_conserved */
    public void record(String operation, String outcome) {
        registry.counter("netting_total", "operation", operation, "outcome", outcome).increment();
    }
}
