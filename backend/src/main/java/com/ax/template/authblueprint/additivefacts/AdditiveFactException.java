package com.ax.template.authblueprint.additivefacts;

import org.springframework.http.HttpStatus;

/** Domain exception for additive-fact-ledger. status + RFC 9457 type + machine-readable code. */
public class AdditiveFactException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private AdditiveFactException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static AdditiveFactException notFound() {
        return new AdditiveFactException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Period or fact not found");
    }

    /** FACT-CLOSED-PERIOD-ADD-003 — a period walks OPEN→CLOSED one-way only. */
    public static AdditiveFactException invalidState() {
        return new AdditiveFactException(HttpStatus.CONFLICT,
            "urn:problem:additivefact-invalid-state", "FACT_INVALID_STATE",
            "The period lifecycle walks OPEN→CLOSED one-way only");
    }

    /** FACT-LATE-DELTA-POST-002 — a late fact for a closed period needs a current open target. */
    public static AdditiveFactException currentPeriodRequired() {
        return new AdditiveFactException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:additivefact-current-period-required", "FACT_CURRENT_PERIOD_REQUIRED",
            "A fact for a CLOSED period requires a currentOpenPeriodId to post the delta into");
    }

    /** FACT-LATE-DELTA-POST-002 — the delta lands in an OPEN period only. */
    public static AdditiveFactException currentPeriodNotOpen() {
        return new AdditiveFactException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:additivefact-current-period-not-open", "FACT_CURRENT_PERIOD_NOT_OPEN",
            "The designated current period is not OPEN");
    }
}
