package com.ax.template.authblueprint.commercepricing;

import org.springframework.http.HttpStatus;

/** Domain exception for the commerce-pricing engine. status + RFC 9457 type + machine-readable code. */
public class PricingException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private PricingException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    /** PRICING-ORDER-001 — taxBasisPoints is outside [0, 10000]. */
    public static PricingException invalidTaxRate(int taxBasisPoints) {
        return new PricingException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:pricing-invalid-tax-rate", "PRICING_INVALID_TAX_RATE",
            "taxBasisPoints must be in [0, 10000] — received: " + taxBasisPoints);
    }

    /** PRICING-ORDER-001 — input is null or structurally invalid. */
    public static PricingException invalidInput(String detail) {
        return new PricingException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:pricing-invalid-input", "PRICING_INVALID_INPUT", detail);
    }
}
