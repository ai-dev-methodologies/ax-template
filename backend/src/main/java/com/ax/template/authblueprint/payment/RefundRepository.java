package com.ax.template.authblueprint.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundRepository extends JpaRepository<Refund, UUID> {

    List<Refund> findByPaymentId(UUID paymentId);

    /**
     * PAYMENT-IDEMP-004 (P1-70): the replay lookup for exactly-once refund semantics.
     * The key is (paymentId, idempotencyKey) — userId is deliberately absent because
     * {@code PaymentRepository.findByIdAndUserId} already enforces the IDOR 404 upstream,
     * so a caller can only ever reach this lookup for a payment they own.
     * Backed by the {@code ux_refunds_payment_id_idempotency_key} unique constraint.
     */
    Optional<Refund> findByPaymentIdAndIdempotencyKey(UUID paymentId, String idempotencyKey);

    /**
     * Returns the sum of refund amounts for a payment, or null when no refunds exist.
     * Used by RefundService to enforce sum(refunds) ≤ capturedAmount atomically.
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.paymentId = :paymentId"
    )
    BigDecimal sumByPaymentId(@org.springframework.data.repository.query.Param("paymentId") UUID paymentId);
}
