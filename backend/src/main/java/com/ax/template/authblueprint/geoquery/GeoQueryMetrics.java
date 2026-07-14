package com.ax.template.authblueprint.geoquery;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code geo_query_total
 * {op, outcome}}, both fixed enums — never a raw lat/lon, external ref, or point id.
 */
@Component
public class GeoQueryMetrics {

    private final MeterRegistry registry;

    public GeoQueryMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op register | search ;
     *  @param outcome ok | invalid */
    public void record(String op, String outcome) {
        registry.counter("geo_query_total", "op", op, "outcome", outcome).increment();
    }
}
