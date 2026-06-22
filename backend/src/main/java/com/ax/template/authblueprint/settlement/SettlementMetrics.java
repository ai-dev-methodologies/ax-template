package com.ax.template.authblueprint.settlement;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code settlement_total
 * {op, outcome}}, both fixed enums — never a trade ref, counterparty name, obligation amount,
 * or instruction id.
 */
@Component
public class SettlementMetrics {

    private final MeterRegistry registry;

    public SettlementMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op settle | novate | fail | retry | buyin ;
     *  @param outcome ok | final_blocked | rejected | invalid */
    public void record(String op, String outcome) {
        registry.counter("settlement_total", "op", op, "outcome", outcome).increment();
    }
}
