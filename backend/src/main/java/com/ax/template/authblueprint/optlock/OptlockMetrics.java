package com.ax.template.authblueprint.optlock;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * OPTLOCK-OBSERVABILITY-001 — exactly 3 canonical Micrometer metrics with bounded-cardinality
 * labels:
 * <ul>
 *   <li>{@code optlock_conflict_total{resource, outcome}} — outcome ∈ {precondition_failed, lock_conflict};</li>
 *   <li>{@code optlock_precondition_required_total{resource}} — If-Match-absent (428) rejections;</li>
 *   <li>{@code optlock_write_seconds{resource, result}} — write timing, result ∈ {applied, conflict}.</li>
 * </ul>
 * {@code resource} is a FIXED entity-type enum; resource_id / etag / version / user_id are NEVER
 * labels. Spec: specs/optimistic-locking-l0.yaml#OPTLOCK-OBSERVABILITY-001.
 */
@Component
public class OptlockMetrics {

    public static final String CONFLICTS = "optlock_conflict_total";
    public static final String PRECONDITION_REQUIRED = "optlock_precondition_required_total";
    public static final String WRITE_TIME = "optlock_write_seconds";

    static final String TAG_RESOURCE = "resource";
    static final String TAG_OUTCOME = "outcome";
    static final String TAG_RESULT = "result";

    /** The only resource value this reference emits (fixed entity-type enum). */
    static final String RESOURCE = "optlock-resource";

    private final MeterRegistry registry;

    public OptlockMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** outcome ∈ {precondition_failed, lock_conflict}. */
    public void conflict(String outcome) {
        Counter.builder(CONFLICTS).tag(TAG_RESOURCE, RESOURCE).tag(TAG_OUTCOME, outcome)
                .register(registry).increment();
    }

    public void preconditionRequired() {
        Counter.builder(PRECONDITION_REQUIRED).tag(TAG_RESOURCE, RESOURCE)
                .register(registry).increment();
    }

    /** result ∈ {applied, conflict}. */
    public void write(String result, Duration elapsed) {
        Timer.builder(WRITE_TIME).tag(TAG_RESOURCE, RESOURCE).tag(TAG_RESULT, result)
                .register(registry).record(elapsed);
    }
}
