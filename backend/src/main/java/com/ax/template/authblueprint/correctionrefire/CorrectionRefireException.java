package com.ax.template.authblueprint.correctionrefire;

import org.springframework.http.HttpStatus;

/** Domain exception for correction-refire-l0. status + RFC 9457 type + machine-readable code. */
public class CorrectionRefireException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private CorrectionRefireException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static CorrectionRefireException notFound() {
        return new CorrectionRefireException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Subject, version, or ack record not found");
    }

    /** CRF-SUPERSEDE-001 — the uq(subject_ref, version) backstop loser of a residual publish race. */
    public static CorrectionRefireException versionConflict() {
        return new CorrectionRefireException(HttpStatus.CONFLICT,
            "urn:problem:correction-version-conflict", "CRF_VERSION_CONFLICT",
            "Another publish created this version first");
    }
}
