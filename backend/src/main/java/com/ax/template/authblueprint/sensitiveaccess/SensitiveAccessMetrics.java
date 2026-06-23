package com.ax.template.authblueprint.sensitiveaccess;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code sensitive_access_total
 * {op, outcome}}, both fixed enums — never a record id, record ref, accessor, raw value, or purpose.
 */
@Component
public class SensitiveAccessMetrics {

    private final MeterRegistry registry;

    public SensitiveAccessMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op record | reveal | view | query ;
     *  @param outcome ok | recorded | no_purpose | invalid */
    public void record(String op, String outcome) {
        registry.counter("sensitive_access_total", "op", op, "outcome", outcome).increment();
    }
}
