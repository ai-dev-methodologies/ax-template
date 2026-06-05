package com.ax.template.authblueprint.pagination;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * PAGE-OBSERVABILITY-001 — exactly 3 canonical pagination meters with bounded-cardinality labels:
 *   pagination_requests_total{tenant, mode}        — counter
 *   pagination_drift_warning_rate{tenant}          — counter (deep-offset > 10k events)
 *   pagination_response_time_seconds{tenant, mode} — timer/histogram
 * Labels exclude page/cursor values, offset, and ids (no high-cardinality / PII).
 * Spec: specs/pagination-l0.yaml#PAGE-OBSERVABILITY-001.
 */
@Component
public class PaginationMetrics {

    public static final String REQUESTS = "pagination_requests_total";
    public static final String DRIFT = "pagination_drift_warning_rate";
    public static final String RESPONSE_TIME = "pagination_response_time_seconds";

    private final MeterRegistry registry;

    public PaginationMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void request(String tenant, String mode) {
        Counter.builder(REQUESTS).tag("tenant", tenant).tag("mode", mode).register(registry).increment();
    }

    public void driftWarning(String tenant) {
        Counter.builder(DRIFT).tag("tenant", tenant).register(registry).increment();
    }

    public void responseTime(String tenant, String mode, Duration elapsed) {
        Timer.builder(RESPONSE_TIME).tag("tenant", tenant).tag("mode", mode).register(registry).record(elapsed);
    }
}
