package com.ax.template.authblueprint.problemdetails;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * PROBLEM-OBSERVABILITY-001 — the 3 canonical problem-detail metrics, modeled as 2
 * Micrometer meter names whose tag dimensions yield the three spec slices:
 *
 * <ul>
 *   <li>{@code problem_response_total{problem_type}} — counter sliced by the registered
 *       short type slug (metric 1);</li>
 *   <li>{@code problem_response_total{status_class}} — the SAME counter sliced by status
 *       class ∈ {4xx, 5xx} (metric 2);</li>
 *   <li>{@code problem_response_seconds{status_class}} — handler timing histogram
 *       (metric 3).</li>
 * </ul>
 *
 * <p>Label cardinality is bounded BY CONSTRUCTION: {@code problem_type} draws only from
 * {@link ProblemTypeRegistry#slugs()} (a closed set) and {@code status_class} from the two
 * fixed classes. High-cardinality / PII values — {@code trace_id}, {@code instance},
 * {@code pointer}, user id — are NEVER attached as labels.
 *
 * <p>Anchored to OpenTelemetry HTTP metrics semantic conventions
 * (https://opentelemetry.io/docs/specs/semconv/http/http-metrics/): "The `error.type`
 * value SHOULD be predictable and SHOULD have low cardinality."
 *
 * <p>Spec: specs/problem-details-l0.yaml#PROBLEM-OBSERVABILITY-001.
 */
@Component
public class ProblemMetrics {

    public static final String RESPONSES = "problem_response_total";
    public static final String RESPONSE_TIME = "problem_response_seconds";

    static final String TAG_PROBLEM_TYPE = "problem_type";
    static final String TAG_STATUS_CLASS = "status_class";

    private final MeterRegistry registry;

    public ProblemMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Record one emitted problem detail. {@code slug} MUST be a registered type slug — the
     * spec requires the {@code problem_type} label to draw ONLY from the closed registry, so
     * an unregistered slug FAILS LOUD here (consistent with {@link ProblemTypeRegistry#uri})
     * rather than silently widening the label space with a fallback value outside the registry.
     */
    public void record(String slug, int httpStatus, Duration elapsed) {
        if (!ProblemTypeRegistry.isRegistered(slug)) {
            throw new IllegalArgumentException("unregistered problem type slug for metric label: " + slug);
        }
        String statusClass = statusClass(httpStatus);
        Counter.builder(RESPONSES)
                .tag(TAG_PROBLEM_TYPE, slug)
                .tag(TAG_STATUS_CLASS, statusClass)
                .register(registry)
                .increment();
        Timer.builder(RESPONSE_TIME)
                .tag(TAG_STATUS_CLASS, statusClass)
                .register(registry)
                .record(elapsed);
    }

    /** Fixed two-valued status class — the only values {@code status_class} may take. */
    static String statusClass(int httpStatus) {
        return httpStatus >= 500 ? "5xx" : "4xx";
    }
}
