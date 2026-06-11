package com.ax.template.authblueprint.recordlinkage;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code linkage_total
 * {op, outcome}}, both fixed enums — never a record id, name, score value, or identity.
 */
@Component
public class LinkageMetrics {

    private final MeterRegistry registry;

    public LinkageMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op propose | confirm | reject | resolve ;
     *  @param outcome ok | auto_merged | rejected | invalid */
    public void record(String op, String outcome) {
        registry.counter("linkage_total", "op", op, "outcome", outcome).increment();
    }
}
