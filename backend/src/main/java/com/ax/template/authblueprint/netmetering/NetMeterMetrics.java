package com.ax.template.authblueprint.netmetering;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code net_meter_op_total
 * {op, outcome}}, both fixed enums — never a meter id, meter key, value, or net.
 */
@Component
public class NetMeterMetrics {

    private final MeterRegistry registry;

    public NetMeterMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op create | IMPORT | EXPORT | close ;
     *  @param outcome ok | rejected | not_monotone | invalid | net_mismatch | period_closed */
    public void record(String op, String outcome) {
        registry.counter("net_meter_op_total", "op", op, "outcome", outcome).increment();
    }
}
