package com.ax.template.authblueprint.dispatch;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality dispatch counter (domain-metrics-bounded-cardinality). The ONLY meter is
 * {@code dispatch_total{action, outcome}} where both labels are fixed enums — never a request id,
 * provider id, handle, or any unbounded/PII value. {@code outcome} carries the distinct race
 * results (job_taken / driver_busy) so contention is observable without high-cardinality labels.
 */
@Component
public class DispatchMetrics {

    private final MeterRegistry registry;

    public DispatchMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * @param action  one of: offer | accept | decline | expire | cancel  (fixed enum)
     * @param outcome one of: ok | job_taken | driver_busy | not_eligible | expired | rejected (fixed enum)
     */
    public void record(String action, String outcome) {
        registry.counter("dispatch_total", "action", action, "outcome", outcome).increment();
    }
}
