package com.ax.template.authblueprint.transformation;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality transformation counter (domain-metrics-bounded-cardinality). The ONLY meter is
 * {@code transformation_total{outcome}} — outcome is a fixed enum (recorded + the rejection codes),
 * never a material code, run id, or quantity.
 */
@Component
public class TransformationMetrics {

    private final MeterRegistry registry;

    public TransformationMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param outcome one of: recorded | XFORM_NOT_CONSERVED | XFORM_MIXED_UNIT | XFORM_UNCLASSIFIED_RESIDUAL | XFORM_INVALID_AMOUNT */
    public void record(String outcome) {
        registry.counter("transformation_total", "outcome", outcome).increment();
    }
}
