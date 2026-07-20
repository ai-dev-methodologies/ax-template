package com.ax.template.authblueprint.payment;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps a {@link Payment} entity to the canonical JSON wire body emitted by
 * {@link PaymentController} for {@code POST/GET /api/payments(/{id})} and the
 * {@code list}/{@code authorize}/{@code capture}/{@code void} endpoints.
 *
 * <p>Extracted verbatim from {@code PaymentController}'s former private
 * {@code paymentBody(Payment)} so the exact wire shape is unit-testable in isolation.
 * {@code MoneyContractParityTest} binds S2.MONEY-QUANTITY.XB to <em>this</em> emitter —
 * the real HTTP boundary — rather than to {@code PaymentResponse}, a DTO no endpoint
 * returns (the controller emits a {@code Map}, not the record). Binding the parity test
 * to the dead record made the money-quantity contract cell VACUOUSLY "covered": the
 * assertions could never observe the amount the endpoint actually serializes.
 *
 * <p>Behavior-preserving lift: the map key set, insertion order, and {@link #canonicalize}
 * amount handling are byte-identical to the controller's former body. The controller now
 * delegates every {@code paymentBody(...)} call to {@link #toBody(Payment)}.
 */
final class PaymentBodyMapper {

    private PaymentBodyMapper() {
    }

    static Map<String, Object> toBody(Payment p) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", p.getId() == null ? null : p.getId().toString());
        body.put("paymentId", p.getId() == null ? null : p.getId().toString());
        body.put("orderId", p.getOrderId());
        body.put("amount", canonicalize(p.getAmount()));
        body.put("capturedAmount", canonicalize(p.getCapturedAmount()));
        body.put("balance", canonicalize(p.getBalance()));
        body.put("currency", p.getCurrency());
        body.put("status", p.getState().name());
        body.put("state", p.getState().name());
        body.put("declineReason", p.getDeclineReason());
        body.put("createdAt", p.getCreatedAt());
        body.put("updatedAt", p.getUpdatedAt());
        return body;
    }

    /**
     * Strip trailing zeros so JSON serialization yields a clean canonical form
     * (e.g., {@code 7000} not {@code 7000.00000000}). Refund balance assertions
     * compare against the canonical form via {@code path("balance").toString()}.
     */
    static BigDecimal canonicalize(BigDecimal v) {
        if (v == null) return null;
        BigDecimal stripped = v.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }
}
