package com.ax.template.authblueprint.rangeownership;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code range_ownership_total
 * {op, outcome}}, both fixed enums — never an ownerRef, identifierValue, or block/assignment id.
 */
@Component
public class RangeOwnershipMetrics {

    private final MeterRegistry registry;

    public RangeOwnershipMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op registerBlock | assign | port ;
     *  @param outcome ok | overlap | not_owned | already_assigned | invalid */
    public void record(String op, String outcome) {
        registry.counter("range_ownership_total", "op", op, "outcome", outcome).increment();
    }
}
