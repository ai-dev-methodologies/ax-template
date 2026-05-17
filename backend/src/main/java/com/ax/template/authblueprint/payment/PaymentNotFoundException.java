package com.ax.template.authblueprint.payment;

/**
 * Thrown when a payment is not found OR is found but not owned by the caller.
 * PAYMENT-AUTHZ-003: IDOR-safe — translated to HTTP 404 (not 403) to prevent enumeration.
 */
public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String message) {
        super(message);
    }
}
