package com.ax.template.authblueprint.netting;

import org.springframework.http.HttpStatus;

/**
 * Domain exception for collection-conservation (netting). status + RFC 9457 type + machine code.
 */
public class NettingException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private NettingException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static NettingException notFound() {
        return new NettingException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Netting run not found");
    }

    public static NettingException duplicateRun() {
        return new NettingException(HttpStatus.CONFLICT,
            "urn:problem:netting-duplicate-run", "NETTING_DUPLICATE_RUN",
            "A netting run with this key already exists");
    }

    /** NET-ONCE-001 — a NETTED run is terminal; the reduction runs at most once. */
    public static NettingException alreadyNetted() {
        return new NettingException(HttpStatus.CONFLICT,
            "urn:problem:netting-already-netted", "NETTING_ALREADY_NETTED",
            "This netting run is already netted; corrections are a new run");
    }

    /** NET-INPUTS-IMMUTABLE-001 — obligations may be added only while the run is OPEN. */
    public static NettingException runNotOpen() {
        return new NettingException(HttpStatus.CONFLICT,
            "urn:problem:netting-run-not-open", "NETTING_RUN_NOT_OPEN",
            "Obligations may be added only while the run is OPEN");
    }

    /** NET-SETWIDE-ZERO-001 — the reduction's member nets did not sum to zero (a dropped/double obligation). */
    public static NettingException notConserved() {
        return new NettingException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:netting-not-conserved", "NETTING_NOT_CONSERVED",
            "The netting reduction is not set-wide conserving (Σ member nets != 0)");
    }

    /** NET-PARTITION-001 — an obligation's currency must match the run's single currency. */
    public static NettingException currencyMismatch() {
        return new NettingException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:netting-currency-mismatch", "NETTING_CURRENCY_MISMATCH",
            "Obligation currency must match the run currency (no cross-currency netting)");
    }

    public static NettingException invalidObligation() {
        return new NettingException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:netting-invalid-obligation", "NETTING_INVALID_OBLIGATION",
            "Obligation amount must be positive and fromMember must differ from toMember");
    }
}
