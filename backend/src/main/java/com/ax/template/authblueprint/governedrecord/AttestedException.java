package com.ax.template.authblueprint.governedrecord;

import org.springframework.http.HttpStatus;

/**
 * Domain exception for attested-change-record. status + RFC 9457 type + machine-readable code.
 */
public class AttestedException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private AttestedException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static AttestedException notFound() {
        return new AttestedException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Governed datum not found");
    }

    /** ACR-ENVELOPE-001 — a governed edit with a missing/blank reason (rejected before the change). */
    public static AttestedException reasonRequired() {
        return new AttestedException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:attested-reason-required", "ATTESTED_REASON_REQUIRED",
            "A non-blank reason is required for a governed change");
    }

    /** ACR-VOCAB-001 — a reason outside the configured controlled vocabulary. */
    public static AttestedException unknownReason() {
        return new AttestedException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:attested-unknown-reason", "ATTESTED_UNKNOWN_REASON",
            "Reason is not in the active controlled vocabulary");
    }

    public static AttestedException duplicateName() {
        return new AttestedException(HttpStatus.CONFLICT,
            "urn:problem:attested-duplicate-name", "ATTESTED_DUPLICATE_NAME",
            "A governed datum with this name already exists");
    }

    /** ACR-APPEND-ONLY-001 — the per-(datum,field) sequence uniqueness backstop fired (a concurrent
     *  edit lost the allocation race). Deterministic, retryable 409 rather than an opaque 500. */
    public static AttestedException sequenceConflict() {
        return new AttestedException(HttpStatus.CONFLICT,
            "urn:problem:attested-sequence-conflict", "ATTESTED_SEQUENCE_CONFLICT",
            "A concurrent governed change collided on the append sequence; retry the edit");
    }
}
