package com.ax.template.authblueprint.webhooksigning;

import java.time.Duration;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * WHSIGN-VERIFY-001 — the receiver verification contract, executed in the fixed order the spec
 * mandates, BEFORE the body is deserialized or acted upon:
 * <ol>
 *   <li>parse the header → 400 WEBHOOK_SIGNATURE_MALFORMED on failure (WHSIGN-HEADER-001);</li>
 *   <li>check the timestamp window → 400 WEBHOOK_TIMESTAMP_STALE if stale (WHSIGN-TIMESTAMP-001);</li>
 *   <li>constant-time HMAC-SHA256 compare against EACH active secret (current + previous overlap) over
 *       {@code timestamp + '.' + raw_body} → 401 WEBHOOK_SIGNATURE_INVALID if none match
 *       (WHSIGN-HMAC-001 / WHSIGN-SECRET-001);</li>
 *   <li>replay dedup of the event_id within the window → 409 WEBHOOK_EVENT_REPLAYED (WHSIGN-REPLAY-001).</li>
 * </ol>
 * A body-only MAC (no timestamp prefix) cannot match because the signed input ALWAYS includes the
 * timestamp prefix. Spec: specs/webhook-signing-l0.yaml.
 */
@Component
public class InboundSignatureVerifier {

    /** WHSIGN-TIMESTAMP-001 default tolerance window (±300s / 5 min); recipe range 60-900. */
    public static final Duration DEFAULT_TOLERANCE = Duration.ofSeconds(300);

    private final SigningSecretStore secretStore;
    private final ReplayDedupStore replayStore;
    private final WebhookSigningMetrics metrics;

    public InboundSignatureVerifier(SigningSecretStore secretStore,
                                    ReplayDedupStore replayStore,
                                    WebhookSigningMetrics metrics) {
        this.secretStore = secretStore;
        this.replayStore = replayStore;
        this.metrics = metrics;
    }

    /**
     * Run the full four-step contract. Throws {@link WebhookSigningException} (mapped to problem+json by
     * {@link WebhookSigningAdvice}) on any failure; returns normally only when all checks pass.
     *
     * @param endpoint    the per-endpoint signing-secret identifier (WHSIGN-SECRET-001)
     * @param headerValue the raw {@code Webhook-Signature} header
     * @param rawBody     the EXACT bytes on the wire, before any deserialization
     * @param eventId     the per-event identifier for replay dedup (null disables dedup)
     * @param nowEpochSec the receiver's current unix time (seconds)
     * @param nowNanos    a monotonic-ish stamp for the replay TTL bookkeeping
     */
    public void verify(String endpoint, String headerValue, byte[] rawBody, String eventId,
                       long nowEpochSec, long nowNanos) {
        // (1) parse header — MALFORMED → 400
        SignatureHeader header;
        try {
            header = SignatureHeader.parse(headerValue);
        } catch (WebhookSigningException malformed) {
            metrics.verifyFailure(malformed.kind().metricReason);
            throw malformed;
        }

        // (2) timestamp window — STALE → 400 (checked before the costly MAC, per the impl hint)
        if (Math.abs(nowEpochSec - header.timestamp()) > DEFAULT_TOLERANCE.toSeconds()) {
            metrics.verifyFailure(WebhookSigningException.Kind.STALE.metricReason);
            throw new WebhookSigningException(WebhookSigningException.Kind.STALE,
                    "The webhook timestamp is outside the tolerance window.");
        }

        // (3) constant-time MAC compare against each active secret — BAD_MAC → 401
        if (!macMatchesAnyActiveSecret(endpoint, header, rawBody)) {
            metrics.verifyFailure(WebhookSigningException.Kind.BAD_MAC.metricReason);
            throw new WebhookSigningException(WebhookSigningException.Kind.BAD_MAC,
                    "The webhook signature did not verify against any active secret.");
        }

        // (4) replay dedup — REPLAYED → 409 (only after authenticity is established)
        if (eventId != null && !eventId.isBlank()
                && !replayStore.firstSeen(endpoint, eventId, nowNanos, DEFAULT_TOLERANCE)) {
            metrics.replayRejected();
            throw new WebhookSigningException(WebhookSigningException.Kind.REPLAYED,
                    "This webhook event was already delivered within the tolerance window.");
        }
    }

    private boolean macMatchesAnyActiveSecret(String endpoint, SignatureHeader header, byte[] rawBody) {
        byte[] signedInput = Hmac.signedInput(header.timestamp(), rawBody);
        List<byte[]> candidates = secretStore.activeSecrets(endpoint);
        HexFormat hex = HexFormat.of();
        boolean matched = false;
        // Loop ALL candidates (current + previous overlap) AND all provided v1 values without an
        // early break, so verification cost does not branch on a partial match (timing posture).
        for (byte[] secret : candidates) {
            byte[] expected = Hmac.compute(secret, signedInput);
            for (String provided : header.v1Signatures()) {
                byte[] actual;
                try {
                    actual = hex.parseHex(provided);
                } catch (IllegalArgumentException notHex) {
                    continue; // a non-hex v1 simply cannot match a raw MAC
                }
                if (Hmac.constantTimeEquals(expected, actual)) {
                    matched = true;
                }
            }
        }
        return matched;
    }

    /** Convenience for the demo / outbound side: hex(HMAC-SHA256(secret, ts + '.' + body)). */
    static String hexSign(byte[] secret, long timestamp, byte[] rawBody) {
        return HexFormat.of().formatHex(Hmac.compute(secret, Hmac.signedInput(timestamp, rawBody)));
    }
}
