package com.ax.template.authblueprint.geofence;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code geofence_total
 * {op, outcome}}, both fixed enums — never a subject id, zone id, or tracker id.
 */
@Component
public class GeofenceMetrics {

    private final MeterRegistry registry;

    public GeofenceMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op register | observe ;
     *  @param outcome ok | pending_started | pending_cancelled | confirmed | no_op | invalid */
    public void record(String op, String outcome) {
        registry.counter("geofence_total", "op", op, "outcome", outcome).increment();
    }
}
