package com.ax.template.authblueprint.tieredeligibility;

import org.springframework.http.HttpStatus;

/**
 * Domain exception for tiered-eligibility. status + RFC 9457 type + machine-readable code.
 */
public class TierException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private TierException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static TierException notFound() {
        return new TierException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Tier ladder not found");
    }

    public static TierException duplicateLadder() {
        return new TierException(HttpStatus.CONFLICT,
            "urn:problem:tiered-eligibility-duplicate", "TIER_DUPLICATE",
            "A tier ladder with this ladder key already exists");
    }

    public static TierException invalidValue() {
        return new TierException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:tiered-eligibility-invalid-value", "TIER_INVALID_VALUE",
            "tierNames must have >= 2 entries; thresholds must be strictly ascending, positive, and one "
                + "fewer than tierNames; an accrual delta must be positive; a restore count must be >= 0 "
                + "and strictly less than the current count");
    }

    /** TIER-DERIVE-001 — the derived capability is denied at the worst tier. */
    public static TierException suspended() {
        return new TierException(HttpStatus.CONFLICT,
            "urn:problem:tiered-eligibility-suspended", "TIER_SUSPENDED",
            "The ladder is at its worst tier; the derived capability is denied until an explicit, "
                + "audited restore occurs");
    }
}
