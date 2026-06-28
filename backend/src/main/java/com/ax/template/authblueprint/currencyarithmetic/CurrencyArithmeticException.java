package com.ax.template.authblueprint.currencyarithmetic;

import org.springframework.http.HttpStatus;

/** Domain exception for currency-arithmetic. status + RFC 9457 type + machine-readable code. */
public class CurrencyArithmeticException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private CurrencyArithmeticException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    /** Cross-currency add/subtract absent an explicit conversion — the fail-closed core (422). */
    public static CurrencyArithmeticException currencyMismatch(String have, String other) {
        return new CurrencyArithmeticException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:currency-mismatch", "CURRENCY_MISMATCH",
            "cross-currency arithmetic is not allowed without an explicit conversion: " + have + " vs " + other);
    }

    /** A conversion whose fromCurrency does not match the amount being converted (422). */
    public static CurrencyArithmeticException conversionMismatch(String have, String conversionFrom) {
        return new CurrencyArithmeticException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:currency-conversion-mismatch", "CURRENCY_CONVERSION_MISMATCH",
            "conversion fromCurrency must match the amount's currency: " + have + " vs " + conversionFrom);
    }

    /** A currency code that is not a valid ISO-4217 alpha-3 (422). */
    public static CurrencyArithmeticException invalidCurrency(String code) {
        return new CurrencyArithmeticException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:currency-invalid", "CURRENCY_INVALID",
            "not a valid ISO-4217 alpha-3 currency code: " + code);
    }

    /** Unknown ledger id — IDOR-safe 404 (never leaks existence). */
    public static CurrencyArithmeticException ledgerNotFound(String id) {
        return new CurrencyArithmeticException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "CURRENCY_LEDGER_NOT_FOUND",
            "Currency ledger not found: " + id);
    }
}
