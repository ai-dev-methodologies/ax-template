package com.ax.template.authblueprint.inputplausibility;

import org.springframework.http.HttpStatus;

/** Domain exception for input-plausibility. status + RFC 9457 type + machine-readable code. */
public class PlausibilityException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private PlausibilityException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static PlausibilityException notFound() {
        return new PlausibilityException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Plausibility channel not found");
    }

    /** PLAUSIBILITY-RANGE-001 — the self-reported value is outside the channel's configured [min, max]. */
    public static PlausibilityException implausibleRange() {
        return new PlausibilityException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:plausibility-implausible-range", "IMPLAUSIBLE_RANGE",
            "The self-reported value is outside the channel's plausible range");
    }

    /** PLAUSIBILITY-RATE-001 — the jump from the prior accepted value exceeded the channel's max rate. */
    public static PlausibilityException implausibleRate() {
        return new PlausibilityException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:plausibility-implausible-rate", "IMPLAUSIBLE_RATE",
            "The self-reported value changed faster than the channel's plausible rate of change");
    }

    /** A blank/invalid channel config submitted as a domain 422 (not a Bean-Validation 400). */
    public static PlausibilityException invalidChannel(String detail) {
        return new PlausibilityException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:plausibility-invalid-channel", "PLAUSIBILITY_INVALID_CHANNEL", detail);
    }

    /** PLAUSIBILITY-DATE-RANGE/FUTURE-001 — the asserted date is outside the channel's window. */
    public static PlausibilityException implausibleDateRange() {
        return new PlausibilityException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:plausibility-implausible-date-range", "IMPLAUSIBLE_DATE_RANGE",
            "The asserted date is outside the channel's plausible lookback/lookahead window");
    }
}
