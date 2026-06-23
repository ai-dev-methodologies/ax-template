package com.ax.template.authblueprint.inventoryreservation;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code
 * inventory_reservation_total {op, outcome}}, both fixed enums — never an item id, sku,
 * reservation id, quantity, or actor.
 */
@Component
public class InventoryReservationMetrics {

    private final MeterRegistry registry;

    public InventoryReservationMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op create | reserve | commit | release ;
     *  @param outcome ok | reserved | committed | released | insufficient | not_held */
    public void record(String op, String outcome) {
        registry.counter("inventory_reservation_total", "op", op, "outcome", outcome).increment();
    }
}
