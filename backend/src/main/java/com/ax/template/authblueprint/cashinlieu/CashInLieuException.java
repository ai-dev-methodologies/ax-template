package com.ax.template.authblueprint.cashinlieu;

import org.springframework.http.HttpStatus;

/** Domain exception for cash-in-lieu-l0. status + RFC 9457 type + machine-readable code. */
public class CashInLieuException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private CashInLieuException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static CashInLieuException invalidHoldingQuantity() {
        return new CashInLieuException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:cash-in-lieu-invalid-holding-quantity", "CIL_INVALID_HOLDING_QUANTITY",
            "holdingQuantity must be positive");
    }

    public static CashInLieuException invalidRatio() {
        return new CashInLieuException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:cash-in-lieu-invalid-ratio", "CIL_INVALID_RATIO",
            "ratio must be positive");
    }

    public static CashInLieuException invalidCashRate() {
        return new CashInLieuException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:cash-in-lieu-invalid-cash-rate", "CIL_INVALID_CASH_RATE",
            "cashRate must be positive");
    }

    public static CashInLieuException allocationNotFound() {
        return new CashInLieuException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "CIL_ALLOCATION_NOT_FOUND", "No allocation found for that (subject, event)");
    }
}
