package com.ax.template.authblueprint.webhooksigning;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * WHSIGN-OBSERVABILITY-001 — EXACTLY 3 bounded-cardinality Micrometer metrics:
 * <ul>
 *   <li>{@code webhook_signatures_issued_total{endpoint}} — counter of outbound signatures created;</li>
 *   <li>{@code webhook_signature_verification_failure_total{reason}} —
 *       reason ∈ {malformed, stale, bad_mac} (bounded enum);</li>
 *   <li>{@code webhook_replay_rejected_total{}} — counter of dedup-rejected replays.</li>
 * </ul>
 * Labels MUST NOT include the secret, the raw signature, the {@code event_id}, or the body — those are
 * the high-cardinality / leak axes the spec forbids. The verification-failure {@code reason} is a fixed
 * enum; {@code endpoint} on the issued counter is a bounded integration identifier, never a secret.
 *
 * <p>Spec: specs/webhook-signing-l0.yaml#WHSIGN-OBSERVABILITY-001.
 */
@Component
public class WebhookSigningMetrics {

    public static final String ISSUED = "webhook_signatures_issued_total";
    public static final String VERIFY_FAILURE = "webhook_signature_verification_failure_total";
    public static final String REPLAY_REJECTED = "webhook_replay_rejected_total";

    static final String TAG_ENDPOINT = "endpoint";
    static final String TAG_REASON = "reason";

    private final MeterRegistry registry;

    public WebhookSigningMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** One outbound signature created for {@code endpoint}. */
    public void issued(String endpoint) {
        Counter.builder(ISSUED).tag(TAG_ENDPOINT, endpoint).register(registry).increment();
    }

    /** reason ∈ {malformed, stale, bad_mac}. */
    public void verifyFailure(String reason) {
        Counter.builder(VERIFY_FAILURE).tag(TAG_REASON, reason).register(registry).increment();
    }

    /** One dedup-rejected replay (no labels — the spec forbids an event_id dimension). */
    public void replayRejected() {
        Counter.builder(REPLAY_REJECTED).register(registry).increment();
    }
}
