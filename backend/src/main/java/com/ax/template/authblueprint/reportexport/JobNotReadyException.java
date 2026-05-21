package com.ax.template.authblueprint.reportexport;

/** EXPORT-LIFECYCLE-003 — mapped to HTTP 409 by the controller. */
public class JobNotReadyException extends RuntimeException {
    public JobNotReadyException(String detail) {
        super(detail);
    }
}
