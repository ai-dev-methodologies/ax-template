package com.ax.template.authblueprint.quorumresolution;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counters (domain-metrics-bounded-cardinality):
 * {@code quorum_resolution_total{outcome}} outcome ∈ {passed, rejected, no_decision}
 * {@code quorum_ballot_total{result}} result ∈ {ok, rejected}
 * NEVER includes voter/motion ids — cardinality is always bounded by fixed enum values.
 */
@Component
public class QuorumMetrics {

    private final MeterRegistry registry;

    public QuorumMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param outcome passed | rejected | no_decision */
    public void recordResolution(String outcome) {
        registry.counter("quorum_resolution_total", "outcome", outcome).increment();
    }

    /** @param result ok | rejected */
    public void recordBallot(String result) {
        registry.counter("quorum_ballot_total", "result", result).increment();
    }
}
