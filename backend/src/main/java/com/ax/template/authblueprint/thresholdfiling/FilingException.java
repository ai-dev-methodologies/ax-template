package com.ax.template.authblueprint.thresholdfiling;

import org.springframework.http.HttpStatus;

/** Domain exception for threshold-filing-obligation. status + RFC 9457 type + machine-readable code. */
public class FilingException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private FilingException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static FilingException notFound() {
        return new FilingException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Filing register or filing obligation not found");
    }

    public static FilingException duplicateSubject() {
        return new FilingException(HttpStatus.CONFLICT,
            "urn:problem:filing-duplicate-subject", "FILING_DUPLICATE_SUBJECT",
            "A filing register with this subject key already exists");
    }

    /** TFO-TRIGGER-001 — the register is TRIGGERED: no further accrual, no re-trigger. */
    public static FilingException triggered() {
        return new FilingException(HttpStatus.CONFLICT,
            "urn:problem:filing-triggered", "FILING_TRIGGERED",
            "The register is TRIGGERED (a filing obligation is already bound) — a new reporting "
                + "period is a NEW register");
    }

    /** TFO-TRIGGER-001 — registration/accrual values must be positive and exact. */
    public static FilingException invalidValue() {
        return new FilingException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:filing-invalid-value", "FILING_INVALID_VALUE",
            "threshold must be > 0 and an accrual delta must be > 0");
    }

    /** TFO-DEADLINE-001 — the loop closes exactly once. */
    public static FilingException alreadyAcknowledged() {
        return new FilingException(HttpStatus.CONFLICT,
            "urn:problem:filing-already-acknowledged", "FILING_ALREADY_ACKNOWLEDGED",
            "The filing obligation is already acknowledged — the loop closes once");
    }
}
