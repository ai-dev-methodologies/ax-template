package com.ax.template.authblueprint.register;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code register_reading_total
 * {kind, outcome}}, both fixed enums — never a register id, scope key, value, or reason.
 */
@Component
public class RegisterMetrics {

    private final MeterRegistry registry;

    public RegisterMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param kind create | NORMAL | ROLLOVER | EXCHANGE ;
     *  @param outcome ok | rejected | not_monotone | invalid | reason_required */
    public void record(String kind, String outcome) {
        registry.counter("register_reading_total", "kind", kind, "outcome", outcome).increment();
    }
}
