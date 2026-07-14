package com.ax.template.authblueprint.saturatingbalance;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code satbal_total
 * {op, outcome}}, both fixed enums — never an ownerId, balance id, or amount.
 */
@Component
public class SaturatingBalanceMetrics {

    private final MeterRegistry registry;

    public SaturatingBalanceMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op accrue | debit ;
     *  @param outcome ok | clamped | rejected */
    public void record(String op, String outcome) {
        registry.counter("satbal_total", "op", op, "outcome", outcome).increment();
    }
}
