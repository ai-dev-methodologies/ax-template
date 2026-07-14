package com.ax.template.authblueprint.exceptiongate;

import org.springframework.http.HttpStatus;

/** Domain exception for orthogonal-exception-gate. status + RFC 9457 type + machine-readable code. */
public class ExceptionGateException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private ExceptionGateException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static ExceptionGateException notFound() {
        return new ExceptionGateException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "No exception gate for that subject");
    }

    /** EXC-DIM-BLOCK-001 — a gated operation is refused fail-closed while the flag is raised. */
    public static ExceptionGateException blocked(String reason) {
        return new ExceptionGateException(HttpStatus.LOCKED,
            "urn:problem:exception-gate-blocked", "EXCEPTION_GATE_BLOCKED",
            "Operation blocked — the subject's exception dimension is raised"
                + (reason == null ? "" : " (" + reason + ")"));
    }
}
