package com.ax.template.authblueprint.reservation;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code reservation_total
 * {operation, outcome}}, both fixed enums — never a balance id, scope key, amount, or actor.
 */
@Component
public class ReservationMetrics {

    private final MeterRegistry registry;

    public ReservationMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param operation create | reserve | settle | release | sweep ;
     *  @param outcome ok | rejected | insufficient | over_settle | already_terminal | expired */
    public void record(String operation, String outcome) {
        registry.counter("reservation_total", "operation", operation, "outcome", outcome).increment();
    }
}
