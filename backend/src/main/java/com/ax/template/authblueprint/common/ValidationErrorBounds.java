package com.ax.template.authblueprint.common;

/**
 * Shared response-amplification bounds for validation problem+json bodies. A single request can
 * carry an unbounded number of field / constraint violations, each with a client-influenced
 * message; without a cap an attacker can inflate the error response far beyond the request. Both
 * the global {@code common.GlobalProblemDetailAdvice} and the domain
 * {@code requestvalidation.RequestValidationAdvice} share these bounds so the two twins of the
 * shared validation pattern are bounded identically (mirrors the proven
 * {@code payment.PaymentExceptionHandler} caps).
 */
public final class ValidationErrorBounds {

    private ValidationErrorBounds() {}

    /** Max number of {@code errors[]} entries emitted (bounds many-field amplification). */
    public static final int MAX_FIELD_ERRORS = 10;

    /** Max length of any single client-derived error message, INCLUDING the ellipsis. */
    public static final int MAX_MESSAGE_LEN = 200;

    /**
     * Cap a client-derived message to {@link #MAX_MESSAGE_LEN} characters including the ellipsis,
     * so the promised bound is exact (never off-by-one).
     */
    public static String truncate(String message) {
        if (message == null) {
            return "";
        }
        return message.length() <= MAX_MESSAGE_LEN
                ? message
                : message.substring(0, MAX_MESSAGE_LEN - 1) + "…";
    }
}
