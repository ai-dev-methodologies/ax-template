package com.ax.template.authblueprint.dsr;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * The EXACTLY-THREE canonical DSR Micrometer meters (DSR-OBSERVABILITY-001):
 * <ol>
 *   <li>{@code dsr_request_total{tenant,type}} — every opened request;</li>
 *   <li>{@code dsr_sla_breach_total{tenant,type}} — requests flagged over due_at;</li>
 *   <li>{@code dsr_processing_time_seconds{tenant,type}} — histogram of close latency.</li>
 * </ol>
 *
 * <p>Labels are bounded-cardinality ONLY: {@code tenant} (a fixed bounded value)
 * and {@code type} (the fixed 5-value {@link DsrRequestType#metricType()} enum).
 * subject_id / request_id / email / any PII are deliberately NEVER attached —
 * they are both PII and unbounded cardinality (OTel metrics data-model cardinality
 * guidance). The metric names are exposed as constants so the test can assert the
 * registry holds exactly these three and no PII tag keys.
 */
@Component
public class DsrMetrics {

    public static final String REQUEST_TOTAL = "dsr_request_total";
    public static final String SLA_BREACH_TOTAL = "dsr_sla_breach_total";
    public static final String PROCESSING_TIME_SECONDS = "dsr_processing_time_seconds";

    public static final String TAG_TENANT = "tenant";
    public static final String TAG_TYPE = "type";

    /**
     * Reference workload is single-tenant — a fixed bounded label keeps cardinality
     * controlled. A fork-receiver swaps this for its resolved tenant id (still a
     * bounded set), never the subject identifier.
     */
    static final String DEFAULT_TENANT = "default";

    private final MeterRegistry registry;

    public DsrMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** DSR-OBSERVABILITY-001 #1 — increment on every opened request. */
    public void recordRequest(DsrRequestType type) {
        registry.counter(REQUEST_TOTAL, TAG_TENANT, DEFAULT_TENANT, TAG_TYPE, type.metricType())
            .increment();
    }

    /** DSR-OBSERVABILITY-001 #2 — increment when a request is flagged SLA-breaching. */
    public void recordSlaBreach(DsrRequestType type) {
        registry.counter(SLA_BREACH_TOTAL, TAG_TENANT, DEFAULT_TENANT, TAG_TYPE, type.metricType())
            .increment();
    }

    /** DSR-OBSERVABILITY-001 #3 — histogram of receive→close processing time. */
    public void recordProcessingTime(DsrRequestType type, Duration elapsed) {
        Timer.builder(PROCESSING_TIME_SECONDS)
            .tag(TAG_TENANT, DEFAULT_TENANT)
            .tag(TAG_TYPE, type.metricType())
            .publishPercentileHistogram()
            .register(registry)
            .record(elapsed);
    }
}
