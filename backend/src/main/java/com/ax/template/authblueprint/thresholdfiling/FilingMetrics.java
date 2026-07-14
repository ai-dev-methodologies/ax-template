package com.ax.template.authblueprint.thresholdfiling;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code filing_total
 * {op, outcome}}, both fixed enums — never a subject key or accrued value.
 */
@Component
public class FilingMetrics {

    private final MeterRegistry registry;

    public FilingMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op create | accrue | ack ;
     *  @param outcome ok | rejected | invalid | triggered */
    public void record(String op, String outcome) {
        registry.counter("filing_total", "op", op, "outcome", outcome).increment();
    }
}
