package com.ax.template.authblueprint.facetcount;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code facetcount_total
 * {op, outcome}}, both fixed enums — never an ownerId, category value, or field name.
 */
@Component
public class FacetCountMetrics {

    private final MeterRegistry registry;

    public FacetCountMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op create | facets ;
     *  @param outcome ok | not_allowed */
    public void record(String op, String outcome) {
        registry.counter("facetcount_total", "op", op, "outcome", outcome).increment();
    }
}
