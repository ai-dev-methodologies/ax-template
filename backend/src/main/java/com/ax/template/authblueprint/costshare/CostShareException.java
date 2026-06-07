package com.ax.template.authblueprint.costshare;

import org.springframework.http.HttpStatus;

/**
 * Domain exception for cost-share / accumulator. Carries the HTTP status, the RFC 9457 problem
 * {@code type} URI, and a machine-readable {@code code}. NOTE: an over-the-limit consume is NOT an
 * error here (accumulator-consume-l0 is non-rejecting — it returns a residual); the only rejections
 * are structural (not-found, over-release below zero, malformed amount).
 */
public class CostShareException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private CostShareException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static CostShareException notFound() {
        return new CostShareException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Accumulator not found");
    }

    /** ACC-CLAWBACK-001 — a reversal that would drive used < 0 (refunding more than accumulated). */
    public static CostShareException overRelease() {
        return new CostShareException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:accumulator-over-release", "ACCUMULATOR_OVER_RELEASE",
            "Reversal would drive accumulated usage below zero");
    }

    /** A negative or non-finite amount on a consume/release/allocate. */
    public static CostShareException invalidAmount() {
        return new CostShareException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:accumulator-invalid-amount", "ACCUMULATOR_INVALID_AMOUNT",
            "Amount must be a non-negative decimal");
    }

    /** A waterfall allocate where the deductible and OOP-max resolve to the SAME accumulator. */
    public static CostShareException sameScope() {
        return new CostShareException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:accumulator-same-scope", "ACCUMULATOR_SAME_SCOPE",
            "deductible and out-of-pocket-max must be distinct accumulators");
    }

    /** A duplicate scope_key on create. */
    public static CostShareException duplicateScope() {
        return new CostShareException(HttpStatus.CONFLICT,
            "urn:problem:accumulator-duplicate-scope", "ACCUMULATOR_DUPLICATE_SCOPE",
            "An accumulator already exists for this scope key");
    }
}
