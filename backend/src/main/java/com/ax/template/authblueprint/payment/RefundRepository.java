package com.ax.template.authblueprint.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface RefundRepository extends JpaRepository<Refund, UUID> {

    List<Refund> findByPaymentId(UUID paymentId);

    /**
     * Returns the sum of refund amounts for a payment, or null when no refunds exist.
     * Used by RefundService to enforce sum(refunds) ≤ capturedAmount atomically.
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.paymentId = :paymentId"
    )
    BigDecimal sumByPaymentId(@org.springframework.data.repository.query.Param("paymentId") UUID paymentId);
}
