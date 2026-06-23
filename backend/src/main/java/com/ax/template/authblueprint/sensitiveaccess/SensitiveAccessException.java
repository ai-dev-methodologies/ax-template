package com.ax.template.authblueprint.sensitiveaccess;

import org.springframework.http.HttpStatus;

/** Domain exception for sensitive-read-audit. status + RFC 9457 type + machine-readable code. */
public class SensitiveAccessException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private SensitiveAccessException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static SensitiveAccessException notFound() {
        return new SensitiveAccessException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Sensitive record not found");
    }

    /** SENSITIVE-PURPOSE-001 — a reveal MUST state a non-blank purpose (AU-2 rationale). */
    public static SensitiveAccessException purposeRequired() {
        return new SensitiveAccessException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:sensitive-purpose-required", "SENSITIVE_PURPOSE_REQUIRED",
            "A non-blank purpose is required to reveal a sensitive value — the access is recorded with its stated purpose");
    }
}
