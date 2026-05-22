package com.ax.template.authblueprint.sessionmanagement;

/** SESS-LIFECYCLE-004 — mapped to HTTP 400 EXPIRES_AT_IN_PAST. */
public class ExpiresAtInPastException extends RuntimeException {
    public ExpiresAtInPastException(String detail) {
        super(detail);
    }
}
