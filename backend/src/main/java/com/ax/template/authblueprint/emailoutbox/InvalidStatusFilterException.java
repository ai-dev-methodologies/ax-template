package com.ax.template.authblueprint.emailoutbox;

/**
 * P3-78 — thrown ONLY by {@link EmailOutboxAdminController#parseStatusFilter} when the
 * {@code status} query parameter is not PENDING/RETRY/SENT/DLQ/ALL. Deliberately its own
 * type (not a bare {@link IllegalArgumentException}) so
 * {@link EmailOutboxAdminController#handleBadRequest} — which hardcodes the "must be one
 * of ..." detail for this one cause — cannot be reached by an unrelated
 * {@code IllegalArgumentException} raised elsewhere in the controller and misreport its
 * cause. The offending raw value is intentionally not carried on this exception
 * (response-amplification defense; see the handler javadoc).
 */
public class InvalidStatusFilterException extends RuntimeException {
    public InvalidStatusFilterException() {
        super("invalid email outbox status filter");
    }
}
