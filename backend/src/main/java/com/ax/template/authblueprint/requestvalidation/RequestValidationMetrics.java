package com.ax.template.authblueprint.requestvalidation;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * VALIDATION-OBSERVABILITY-001 — the 3 canonical validation metrics, modeled as 2
 * Micrometer counter names whose tag dimensions yield the three spec slices:
 *
 * <ul>
 *   <li>{@code validation_failure_total{field}} — sliced by the offending field TEMPLATE
 *       (metric 1);</li>
 *   <li>{@code validation_failure_total{code}} — same counter sliced by the constraint code
 *       (metric 2);</li>
 *   <li>{@code request_rejected_total{status_class}} — rejected-request counter, status
 *       class ∈ {4xx} (metric 3).</li>
 * </ul>
 *
 * <p>Cardinality is bounded BY CONSTRUCTION. The {@code field} label is the schema-declared
 * field TEMPLATE — collection indices are collapsed to {@code {index}} by
 * {@link #fieldTemplate(String)} and any pointer not in the closed declared set collapses to
 * {@code other}, so an attacker-controlled JSON Pointer (raw index/key) can NEVER widen the
 * label space. The {@code code} label draws from the closed constraint-code set; the offending
 * VALUE, message text, and user id are NEVER attached.
 *
 * <p>Anchored to OpenTelemetry HTTP metrics semantic conventions: "The `error.type` value
 * SHOULD be predictable and SHOULD have low cardinality."
 * Spec: specs/request-validation-l0.yaml#VALIDATION-OBSERVABILITY-001.
 */
@Component
public class RequestValidationMetrics {

    public static final String FAILURES = "validation_failure_total";
    public static final String REJECTED = "request_rejected_total";

    static final String TAG_FIELD = "field";
    static final String TAG_CODE = "code";
    static final String TAG_STATUS_CLASS = "status_class";

    /** The CLOSED set of field templates the reference schema can produce (bounds {@code field}). */
    static final Set<String> DECLARED_FIELD_TEMPLATES = Set.of(
            "/customer", "/amount", "/priority", "/startDate", "/endDate",
            "/address/postalCode", "/address/city",
            "/items/{index}/sku", "/items/{index}/quantity",
            "other");

    private final MeterRegistry registry;

    public RequestValidationMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** Record one field-level violation (metrics 1 + 2): increment by template + code. */
    public void failure(String pointer, String code) {
        Counter.builder(FAILURES)
                .tag(TAG_FIELD, fieldTemplate(pointer))
                .tag(TAG_CODE, code)
                .register(registry)
                .increment();
    }

    /** Record one rejected request (metric 3). */
    public void rejected(int httpStatus) {
        Counter.builder(REJECTED)
                .tag(TAG_STATUS_CLASS, httpStatus >= 500 ? "5xx" : "4xx")
                .register(registry)
                .increment();
    }

    /**
     * Collapse a concrete RFC 6901 pointer to its bounded schema template: numeric path
     * segments → {@code {index}}; an unknown template → {@code other}. This is the guard that
     * keeps the {@code field} label cardinality bounded against attacker-controlled input.
     */
    static String fieldTemplate(String pointer) {
        if (pointer == null || pointer.isBlank()) {
            return "other";
        }
        String template = pointer.replaceAll("/\\d+", "/{index}");
        return DECLARED_FIELD_TEMPLATES.contains(template) ? template : "other";
    }
}
