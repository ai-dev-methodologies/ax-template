package com.ax.template.authblueprint.derivedstatement;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code derivedstatement_total
 * {outcome}}, a fixed enum — never a subject, period, or basis hash.
 */
@Component
public class DerivedStatementMetrics {

    private final MeterRegistry registry;

    public DerivedStatementMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param outcome created | replayed | raced | rejected */
    public void record(String outcome) {
        registry.counter("derivedstatement_total", "outcome", outcome).increment();
    }
}
