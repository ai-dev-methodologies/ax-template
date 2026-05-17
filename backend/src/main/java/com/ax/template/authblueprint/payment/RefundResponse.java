package com.ax.template.authblueprint.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RefundResponse(
    UUID id,
    UUID paymentId,
    BigDecimal amount,
    String currency,
    String state,
    Instant createdAt
) {
    public static RefundResponse from(Refund r) {
        return new RefundResponse(
            r.getId(),
            r.getPaymentId(),
            r.getAmount(),
            r.getCurrency(),
            r.getState().name(),
            r.getCreatedAt()
        );
    }
}
