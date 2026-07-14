package com.ax.template.authblueprint.eventingest;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * INGEST-OBSERVABILITY-001 canonical counter: {@code stale_event_dropped_total{stream, reason}}.
 * Both labels are fixed enums — {@code stream} the {@link IngestStream} name, {@code reason}
 * ∈ {behind_watermark, duplicate} — never an event id, subject id, or event-time (no PII, no
 * unbounded cardinality). Only DROPS increment this counter; a successfully-applied event does
 * not (the spec names exactly one canonical metric, the drop counter).
 */
@Component
public class EventIngestMetrics {

    static final String STALE_EVENT_DROPPED_TOTAL = "stale_event_dropped_total";

    private final MeterRegistry registry;

    public EventIngestMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param reason behind_watermark | duplicate */
    public void dropped(String stream, String reason) {
        registry.counter(STALE_EVENT_DROPPED_TOTAL, "stream", stream, "reason", reason).increment();
    }
}
