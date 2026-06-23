package com.ax.template.authblueprint.mandate;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code mandate_total
 * {op, outcome}}, both fixed enums — never a mandate id, directive, actor, check key, or count.
 */
@Component
public class MandateMetrics {

    private final MeterRegistry registry;

    public MandateMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op issue | complete_task | record_check | satisfy | deemed ;
     *  @param outcome ok | resolved | satisfied | already_resolved | empty_fanout | battery_incomplete
     *                 | unknown_check | invalid | deemed | skipped | rejected */
    public void record(String op, String outcome) {
        registry.counter("mandate_total", "op", op, "outcome", outcome).increment();
    }
}
