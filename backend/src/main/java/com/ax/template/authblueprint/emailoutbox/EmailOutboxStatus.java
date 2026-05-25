package com.ax.template.authblueprint.emailoutbox;

/**
 * Lifecycle status of an {@link EmailOutbox} row.
 *
 * <ul>
 *   <li>{@code PENDING} — newly enqueued; eligible for next processQueue() cycle</li>
 *   <li>{@code RETRY} — last send failed; nextAttemptAt set via exponential backoff
 *       (EMAIL-SEND-002); eligible only when nextAttemptAt &le; now (EMAIL-RETRY-002)</li>
 *   <li>{@code SENT} — terminal success</li>
 *   <li>{@code DLQ} — terminal failure after MAX_RETRIES (EMAIL-RETRY-001)</li>
 * </ul>
 */
public enum EmailOutboxStatus {
    PENDING,
    RETRY,
    SENT,
    DLQ
}
