package com.ax.template.authblueprint.calendardeadline;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code calendar_deadline_total
 * {op, outcome}}, both fixed enums — never a deadline id, obligation ref, date, or actor.
 */
@Component
public class CalendarDeadlineMetrics {

    private final MeterRegistry registry;

    public CalendarDeadlineMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op create_calendar | edit_calendar | compute | get ;
     *  @param outcome ok | not_found | invalid */
    public void record(String op, String outcome) {
        registry.counter("calendar_deadline_total", "op", op, "outcome", outcome).increment();
    }
}
