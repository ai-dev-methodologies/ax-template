package com.ax.template.authblueprint.approvalworkflow;

/** WF-LIFECYCLE-003 — mapped to HTTP 409 REQUEST_TERMINAL when an action targets an already-terminal request. */
public class RequestTerminalException extends RuntimeException {
    public RequestTerminalException(String detail) {
        super(detail);
    }
}
