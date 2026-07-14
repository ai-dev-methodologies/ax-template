package com.ax.template.authblueprint.tieredauthority;

import org.springframework.http.HttpStatus;

/** Domain exception for amount-tiered-authority-l0. status + RFC 9457 type + machine-readable code. */
public class TieredAuthorityException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private TieredAuthorityException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    /** ATA-BOUNDARY-001 — bands must tile without gap/overlap; only the last may be open-ended. */
    public static TieredAuthorityException boundaryInvalid(String detail) {
        return new TieredAuthorityException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:tier-boundary-invalid", "TIER_BOUNDARY_INVALID", detail);
    }

    /** ATA-TIER-001 — no band covers the decided amount (fail-closed, not a silent default). */
    public static TieredAuthorityException noTierMatch(String detail) {
        return new TieredAuthorityException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:tier-not-found", "TIER_NOT_FOUND", detail);
    }

    /** ATA-TIER-001 — decider's level is below the covering band's minimum. Fail-closed, no auto-escalation. */
    public static TieredAuthorityException insufficientAuthority(String detail) {
        return new TieredAuthorityException(HttpStatus.FORBIDDEN,
            "urn:problem:insufficient-authority", "INSUFFICIENT_AUTHORITY", detail);
    }

    public static TieredAuthorityException tableNotFound() {
        return new TieredAuthorityException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "AUTHORITY_TABLE_NOT_FOUND",
            "No authority table has been configured yet");
    }

    public static TieredAuthorityException decisionNotFound() {
        return new TieredAuthorityException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "DECISION_NOT_FOUND", "Decision record not found");
    }
}
