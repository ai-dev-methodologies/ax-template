package com.ax.template.authblueprint.payment;

import tools.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;

/**
 * Request body for POST /api/payments/{id}/refund.
 *
 * <p>{@code amount} is optional — when null the full captured amount is refunded.
 * The {@link MoneyDeserializer} rejects JSON floats (PAYMENT-MONEY-002).
 */
public record RefundRequest(
    @JsonDeserialize(using = MoneyDeserializer.class) BigDecimal amount,
    String reason
) {
    /** Empty refund request — refund full captured amount, no reason. */
    public RefundRequest() {
        this(null, null);
    }
}
