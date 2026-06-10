package com.ax.template.authblueprint.thresholdterminal;

import org.springframework.http.HttpStatus;

/**
 * Domain exception for threshold-terminal-derivation. status + RFC 9457 type + machine-readable code.
 */
public class ThresholdException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private ThresholdException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static ThresholdException notFound() {
        return new ThresholdException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Threshold register not found");
    }

    public static ThresholdException duplicateScope() {
        return new ThresholdException(HttpStatus.CONFLICT,
            "urn:problem:threshold-duplicate-scope", "THRESHOLD_DUPLICATE_SCOPE",
            "A threshold register with this scope key already exists");
    }

    /** TTD-TERMINAL-001 / TTD-DERIVE-001 — the register is EXPIRED: no accrual, no use, no way back. */
    public static ThresholdException terminal() {
        return new ThresholdException(HttpStatus.CONFLICT,
            "urn:problem:threshold-terminal", "THRESHOLD_TERMINAL",
            "The register is EXPIRED (anchor reached its limit) — accruals and uses are rejected; "
                + "a replacement is a NEW register");
    }

    /** TTD-CROSS-001 — registration/accrual values must be positive, in range, and exact. */
    public static ThresholdException invalidValue() {
        return new ThresholdException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:threshold-invalid-value", "THRESHOLD_INVALID_VALUE",
            "limit must be > 0, the initial anchor in [0, limit), and an accrual delta > 0");
    }
}
