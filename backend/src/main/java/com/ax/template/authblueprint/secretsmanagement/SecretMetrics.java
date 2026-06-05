package com.ax.template.authblueprint.secretsmanagement;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * SECRET-OBSERVABILITY-001 — EXACTLY 3 bounded-cardinality Micrometer metrics:
 * <ul>
 *   <li>{@code secret_access_total{outcome}} — outcome ∈ {granted, denied};</li>
 *   <li>{@code secret_rotation_total{result}} — result ∈ {success, failure};</li>
 *   <li>{@code secret_resolution_failure_total{reason}} —
 *       reason ∈ {not_found, revoked, expired, store_unavailable}.</li>
 * </ul>
 * The ONLY label on each meter is its fixed enum dimension. {@code secret_id}, the secret value, and
 * the principal identity are NEVER labels — those are the high-cardinality / leak axes the spec
 * forbids (so a read records {@code outcome=granted}, not {@code principal=alice, secret_id=db-pw}).
 *
 * <p>Spec: specs/secrets-management-l0.yaml#SECRET-OBSERVABILITY-001.
 */
@Component
public class SecretMetrics {

    public static final String ACCESS = "secret_access_total";
    public static final String ROTATION = "secret_rotation_total";
    public static final String RESOLUTION_FAILURE = "secret_resolution_failure_total";

    static final String TAG_OUTCOME = "outcome";
    static final String TAG_RESULT = "result";
    static final String TAG_REASON = "reason";

    private final MeterRegistry registry;

    public SecretMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** outcome ∈ {granted, denied}. */
    public void access(String outcome) {
        Counter.builder(ACCESS).tag(TAG_OUTCOME, outcome).register(registry).increment();
    }

    /** result ∈ {success, failure}. */
    public void rotation(String result) {
        Counter.builder(ROTATION).tag(TAG_RESULT, result).register(registry).increment();
    }

    /** reason ∈ {not_found, revoked, expired, store_unavailable}. */
    public void resolutionFailure(String reason) {
        Counter.builder(RESOLUTION_FAILURE).tag(TAG_REASON, reason).register(registry).increment();
    }
}
