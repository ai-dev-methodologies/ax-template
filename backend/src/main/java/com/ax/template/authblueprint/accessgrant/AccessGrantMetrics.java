package com.ax.template.authblueprint.accessgrant;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code access_grant_total
 * {op, outcome}}, both fixed enums — never a grant id, subject id, resource ref, relation,
 * credential class, or actor.
 */
@Component
public class AccessGrantMetrics {

    private final MeterRegistry registry;

    public AccessGrantMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op grant | check | revoke | credential | eligibility ;
     *  @param outcome ok | allowed | not_yet_valid | expired | revoked | eligible | ineligible */
    public void record(String op, String outcome) {
        registry.counter("access_grant_total", "op", op, "outcome", outcome).increment();
    }
}
