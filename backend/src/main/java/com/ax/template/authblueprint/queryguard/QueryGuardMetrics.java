package com.ax.template.authblueprint.queryguard;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code query_guard_total
 * {op, outcome}}, both fixed enums — never a field name (which is caller-controlled and
 * unbounded; emitting it as a tag would be a cardinality-explosion + a reflected-input leak).
 */
@Component
public class QueryGuardMetrics {

    private final MeterRegistry registry;

    public QueryGuardMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op list ;
     *  @param outcome ok | not_sortable | not_filterable | direction_invalid | operator_invalid | filter_malformed */
    public void record(String op, String outcome) {
        registry.counter("query_guard_total", "op", op, "outcome", outcome).increment();
    }
}
