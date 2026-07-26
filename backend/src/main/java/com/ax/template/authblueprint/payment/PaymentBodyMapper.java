package com.ax.template.authblueprint.payment;

import com.ax.template.authblueprint.common.Money;

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
 * <p>Behavior-preserving lift: the map key set and insertion order are byte-identical to the
 * controller's former body. The controller now delegates every {@code paymentBody(...)} call to
 * {@link #toBody(Payment)}.
 *
 * <h2>Money on the wire (P1-68 closure)</h2>
 * Monetary fields are emitted as integer MINOR units via {@link #minorOrNull} — the sole response
 * encoding, matching {@code contracts/payment-openapi.yaml#MoneyAmount}'s integer branch, the
 * request encoding accepted by {@link MoneyDeserializer}, and {@code money.ts}'s {@code parseMinor}
 * on the client. Previously these were MAJOR-unit {@code BigDecimal}s, so USD emitted
 * {@code "amount":10.99} — a JSON float token that is neither branch of the schema's {@code oneOf}
 * and that the contract's own description says must be REJECTED as input, while {@code parseMinor}
 * silently misread whole-dollar values by 100×. This also subsumes the former {@code canonicalize}
 * trailing-zero strip: {@code 10.90} now emits as {@code 1090}, not {@code 10.9}.
 */
final class PaymentBodyMapper {

    private PaymentBodyMapper() {
    }

    static Map<String, Object> toBody(Payment p) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", p.getId() == null ? null : p.getId().toString());
        body.put("paymentId", p.getId() == null ? null : p.getId().toString());
        body.put("orderId", p.getOrderId());
        body.put("amount", minorOrNull(p.getAmount(), p.getCurrency()));
        body.put("capturedAmount", minorOrNull(p.getCapturedAmount(), p.getCurrency()));
        body.put("balance", minorOrNull(p.getBalance(), p.getCurrency()));
        body.put("currency", p.getCurrency());
        body.put("status", p.getState().name());
        body.put("state", p.getState().name());
        body.put("declineReason", p.getDeclineReason());
        body.put("createdAt", p.getCreatedAt());
        body.put("updatedAt", p.getUpdatedAt());
        return body;
    }

    /**
     * Convert a stored MAJOR-unit {@link BigDecimal} to the integer MINOR units the wire carries,
     * through the canonical {@code common/Money} seam. {@code null} in, {@code null} out
     * (capturedAmount/balance are null before capture).
     *
     * <p>Jackson serializes the returned {@link Long} as a bare JSON integer — the
     * {@code MoneyAmount} integer branch. Trailing zeros disappear by construction
     * ({@code 7000.00000000} → {@code 7000}, USD {@code 10.90} → {@code 1090}), which is why the
     * former {@code canonicalize} strip is gone rather than merely relocated.
     */
    static Long minorOrNull(BigDecimal v, String currency) {
        if (v == null) return null;
        return Money.toMinorUnits(v, currency);
    }
}
