package com.ax.template.authblueprint.webhook;

/**
 * Webhook delivery lifecycle status.
 * <p>
 * Trace:
 * <ul>
 *   <li>WEBHOOK-RETRY-001 — {@link #PENDING_RETRY} during the backoff window.</li>
 *   <li>WEBHOOK-DEAD-LETTER-001 — {@link #FAILED_PERMANENT} once max attempts are exhausted.</li>
 * </ul>
 */
public enum WebhookDeliveryStatus {
    /** Initial state: enqueued, not yet sent. */
    PENDING,
    /** Failed (retriable) attempt; another attempt scheduled at {@code next_attempt_at}. */
    PENDING_RETRY,
    /** Final state: outbound POST returned 2xx. */
    SUCCEEDED,
    /** Final state: retry budget exhausted (WEBHOOK-DEAD-LETTER-001). */
    FAILED_PERMANENT
}
