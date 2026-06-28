package com.ax.template.authblueprint.offereligibility;

import org.springframework.http.HttpStatus;

/** Domain exception for offer-eligibility. status + RFC 9457 type + machine-readable code. */
public class OfferEligibilityException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private OfferEligibilityException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    /** Unknown offer id — IDOR-safe 404 (never leaks existence). */
    public static OfferEligibilityException offerNotFound(String id) {
        return new OfferEligibilityException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "OFFER_NOT_FOUND",
            "Offer not found: " + id);
    }

    /** Invalid offer declaration (e.g. min_qualifier_qty < 1 or negative discount). */
    public static OfferEligibilityException invalidOffer(String detail) {
        return new OfferEligibilityException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:offer-invalid", "OFFER_INVALID", detail);
    }
}
