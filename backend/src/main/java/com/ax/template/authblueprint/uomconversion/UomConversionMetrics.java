package com.ax.template.authblueprint.uomconversion;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code uom_conversion_total
 * {op, outcome}}, both fixed enums — never a material id, ref, quantity, unit code, or actor.
 */
@Component
public class UomConversionMetrics {

    private final MeterRegistry registry;

    public UomConversionMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op register_material | record_property | convert ;
     *  @param outcome ok | same_dimension | cross_dimension | idempotent | incompatible | unknown_unit | unknown_version */
    public void record(String op, String outcome) {
        registry.counter("uom_conversion_total", "op", op, "outcome", outcome).increment();
    }
}
