package com.ax.template.authblueprint.costshare;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality cost-share counter (domain-metrics-bounded-cardinality). The ONLY meter is
 * {@code cost_share_total{operation, outcome}} — both labels fixed enums, never a scope key, member
 * id, or amount.
 */
@Component
public class CostShareMetrics {

    private final MeterRegistry registry;

    public CostShareMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * @param operation one of: consume | release | reset | allocate | create  (fixed enum)
     * @param outcome   one of: ok | partial | over_release | rejected         (fixed enum)
     */
    public void record(String operation, String outcome) {
        registry.counter("cost_share_total", "operation", operation, "outcome", outcome).increment();
    }
}
