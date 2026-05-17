package com.ax.template.authblueprint.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response body for POST/GET /api/payments(/{id}).
 *
 * <p>The {@code state} field surfaces the payment state machine value
 * (CREATED/AUTHORIZED/CAPTURED/...) per contracts/payment-openapi.yaml.
 */
public record PaymentResponse(
    UUID id,
    String orderId,
    UUID userId,
    BigDecimal amount,
    BigDecimal capturedAmount,
    BigDecimal balance,
    String currency,
    String state,
    String declineReason,
    Instant createdAt,
    Instant updatedAt
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
            p.getId(),
            p.getOrderId(),
            p.getUserId(),
            p.getAmount(),
            p.getCapturedAmount(),
            p.getBalance(),
            p.getCurrency(),
            p.getState().name(),
            p.getDeclineReason(),
            p.getCreatedAt(),
            p.getUpdatedAt()
        );
    }
}
