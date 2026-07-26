package com.ax.template.authblueprint.payment;

import com.ax.template.authblueprint.common.Money;

import java.time.Instant;
import java.util.UUID;

/**
 * Refund wire body returned by {@code PaymentController#refund}.
 *
 * <p>P1-68: {@code amount} is integer MINOR units — the same single response encoding
 * {@link PaymentBodyMapper} emits, matching {@code contracts/payment-openapi.yaml#MoneyAmount}'s
 * integer branch and {@code money.ts}'s {@code parseMinor} on the client. The stored
 * {@code Refund.amount} stays a MAJOR-unit {@link java.math.BigDecimal} (the payment/PG-edge
 * representation); {@code Money.toMinorUnits} is the conversion seam.
 */
public record RefundResponse(
    UUID id,
    UUID paymentId,
    long amount,
    String currency,
    String state,
    Instant createdAt
) {
    public static RefundResponse from(Refund r) {
        return new RefundResponse(
            r.getId(),
            r.getPaymentId(),
            Money.toMinorUnits(r.getAmount(), r.getCurrency()),
            r.getCurrency(),
            r.getState().name(),
            r.getCreatedAt()
        );
    }
}
