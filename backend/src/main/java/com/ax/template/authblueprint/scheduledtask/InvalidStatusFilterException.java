package com.ax.template.authblueprint.scheduledtask;

/**
 * P3-78 — thrown ONLY by {@link ScheduledTaskController#parseStatusFilter} when the
 * {@code status} query parameter is not REGISTERED/ENABLED/DISABLED/ALL. Deliberately its
 * own type (not a bare {@link IllegalArgumentException}) so
 * {@link ScheduledTaskController#handleBadRequest} — which hardcodes the "must be one of
 * ..." detail for this one cause — cannot be reached by an unrelated
 * {@code IllegalArgumentException} raised elsewhere in the controller and misreport its
 * cause. The offending raw value is intentionally not carried on this exception
 * (response-amplification defense; see the handler javadoc).
 */
public class InvalidStatusFilterException extends RuntimeException {
    public InvalidStatusFilterException() {
        super("invalid scheduled task status filter");
    }
}
