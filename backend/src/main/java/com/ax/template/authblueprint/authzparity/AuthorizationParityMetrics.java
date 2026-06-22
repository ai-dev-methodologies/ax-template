package com.ax.template.authblueprint.authzparity;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code authzparity_total
 * {op, outcome}}, both fixed enums — never an action id, parameter value, hash, or user identity.
 */
@Component
public class AuthorizationParityMetrics {

    private final MeterRegistry registry;

    public AuthorizationParityMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op authorize | signoff | satisfy_gate | execute ;
     *  @param outcome ok | executed | parity_mismatch | rejected | invalid */
    public void record(String op, String outcome) {
        registry.counter("authzparity_total", "op", op, "outcome", outcome).increment();
    }
}
