package com.ax.template.authblueprint.taxapplication;

import org.springframework.http.HttpStatus;

/** Domain exception for tax-application. status + RFC 9457 type + machine-readable code. */
public class TaxApplicationException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private TaxApplicationException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    /** Unknown order id — IDOR-safe 404 (never leaks existence). */
    public static TaxApplicationException orderNotFound(String id) {
        return new TaxApplicationException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "TAX_ORDER_NOT_FOUND",
            "Taxable order not found: " + id);
    }

    /** A negative injected rate is not a valid tax rate. */
    public static TaxApplicationException invalidRate(long rateBasisPoints) {
        return new TaxApplicationException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:tax-rate-invalid", "TAX_RATE_INVALID",
            "rate_basis_points must be >= 0: " + rateBasisPoints);
    }

    /** A negative line taxable base is not a valid taxable input. */
    public static TaxApplicationException invalidLine(String detail) {
        return new TaxApplicationException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:tax-line-invalid", "TAX_LINE_INVALID", detail);
    }
}
