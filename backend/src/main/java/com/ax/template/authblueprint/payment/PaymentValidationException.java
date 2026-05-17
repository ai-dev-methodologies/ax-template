package com.ax.template.authblueprint.payment;

/**
 * Thrown for validation failures that should surface as 400 + RFC 7807 ProblemDetail.
 */
public class PaymentValidationException extends RuntimeException {
    public PaymentValidationException(String message) {
        super(message);
    }
}
