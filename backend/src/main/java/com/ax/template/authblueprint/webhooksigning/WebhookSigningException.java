package com.ax.template.authblueprint.webhooksigning;

import org.springframework.http.HttpStatus;

/**
 * The closed set of inbound-webhook signature-verification failures, each carrying the HTTP status +
 * stable {@code code} the spec names, in the fixed verification order of WHSIGN-VERIFY-001:
 * <ol>
 *   <li>{@link Kind#MALFORMED} — header unparseable / version-unknown → 400;</li>
 *   <li>{@link Kind#STALE} — timestamp outside the tolerance window → 400;</li>
 *   <li>{@link Kind#BAD_MAC} — no active secret produced a constant-time match → 401;</li>
 *   <li>{@link Kind#REPLAYED} — a fresh-but-already-seen event_id within the window → 409.</li>
 * </ol>
 * The message is ALWAYS value-free — it never carries the secret, the raw signature, or the body
 * (WHSIGN-SECRET-001). The {@code metricReason} (when present) feeds
 * {@link WebhookSigningMetrics#verifyFailure}.
 *
 * <p>Spec: specs/webhook-signing-l0.yaml.
 */
public class WebhookSigningException extends RuntimeException {

    public enum Kind {
        MALFORMED(HttpStatus.BAD_REQUEST, "WEBHOOK_SIGNATURE_MALFORMED", "malformed"),
        STALE(HttpStatus.BAD_REQUEST, "WEBHOOK_TIMESTAMP_STALE", "stale"),
        BAD_MAC(HttpStatus.UNAUTHORIZED, "WEBHOOK_SIGNATURE_INVALID", "bad_mac"),
        REPLAYED(HttpStatus.CONFLICT, "WEBHOOK_EVENT_REPLAYED", null);

        final HttpStatus status;
        final String code;
        /** Mapped {@link WebhookSigningMetrics} verification-failure reason, or null when not a verify failure. */
        final String metricReason;

        Kind(HttpStatus status, String code, String metricReason) {
            this.status = status;
            this.code = code;
            this.metricReason = metricReason;
        }
    }

    private final transient Kind kind;

    public WebhookSigningException(Kind kind, String neutralMessage) {
        super(neutralMessage);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
