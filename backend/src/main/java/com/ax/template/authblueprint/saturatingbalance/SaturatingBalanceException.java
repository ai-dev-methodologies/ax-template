package com.ax.template.authblueprint.saturatingbalance;

import org.springframework.http.HttpStatus;

/** Domain exception for saturating-balance-l0. status + RFC 9457 type + machine-readable code. */
public class SaturatingBalanceException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private SaturatingBalanceException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static SaturatingBalanceException notFound() {
        return new SaturatingBalanceException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Balance not found");
    }

    /** A requested accrual/debit amount must be a positive magnitude — the direction is the operation itself. */
    public static SaturatingBalanceException invalidAmount() {
        return new SaturatingBalanceException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:satbal-invalid-amount", "SATBAL_INVALID_AMOUNT",
            "The requested amount must be a positive magnitude");
    }

    /** A balance's declared cap must be positive. */
    public static SaturatingBalanceException invalidCap() {
        return new SaturatingBalanceException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:satbal-invalid-cap", "SATBAL_INVALID_CAP",
            "The balance cap must be a positive amount");
    }
}
