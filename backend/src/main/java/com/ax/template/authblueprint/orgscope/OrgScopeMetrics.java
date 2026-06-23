package com.ax.template.authblueprint.orgscope;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code orgscope_total
 * {op, outcome}}, both fixed enums — never a node id, principal, path, or role value.
 */
@Component
public class OrgScopeMetrics {

    private final MeterRegistry registry;

    public OrgScopeMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op create_node | grant | check ;
     *  @param outcome ok | created | granted | idempotent | allowed | out_of_scope */
    public void record(String op, String outcome) {
        registry.counter("orgscope_total", "op", op, "outcome", outcome).increment();
    }
}
