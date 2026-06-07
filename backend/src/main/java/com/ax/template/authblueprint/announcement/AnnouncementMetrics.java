package com.ax.template.authblueprint.announcement;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * ANN-OBSERVABILITY-001 — single canonical bounded-cardinality counter
 * {@code announcement_total{transition, outcome}}. transition in {created,published,archived};
 * outcome in {ok,rejected}. NO id/author/title label (composes domain-metrics-bounded-cardinality).
 */
@Component
public class AnnouncementMetrics {

    private final MeterRegistry registry;

    public AnnouncementMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void record(String transition, String outcome) {
        registry.counter("announcement_total", "transition", transition, "outcome", outcome).increment();
    }
}
