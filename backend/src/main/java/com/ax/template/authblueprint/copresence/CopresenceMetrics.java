package com.ax.template.authblueprint.copresence;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code copresence_gate_total
 * {operation, outcome}}, both fixed enums — never a subject key, concept, label, or reason.
 */
@Component
public class CopresenceMetrics {

    private final MeterRegistry registry;

    public CopresenceMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param operation subject | concept | conflict | member ;
     *  @param outcome ok | rejected | unassessable | absolute | relative | overridden */
    public void record(String operation, String outcome) {
        registry.counter("copresence_gate_total", "operation", operation, "outcome", outcome).increment();
    }
}
