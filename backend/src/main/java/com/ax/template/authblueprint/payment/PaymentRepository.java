package com.ax.template.authblueprint.payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /** IDOR-safe lookup: returns empty when not owned by the caller (controller maps to 404). */
    Optional<Payment> findByIdAndUserId(UUID id, UUID userId);

    Page<Payment> findByUserId(UUID userId, Pageable pageable);

    /**
     * PAYMENT-SPLIT-001: sum of amount for all active, successfully-authorized
     * tenders (state IN {AUTHORIZED, CAPTURED}) for the given orderId and currency.
     * Same-currency filter prevents meaningless cross-currency aggregation.
     * COALESCE ensures 0 is returned when no matching rows exist.
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.orderId = :orderId AND p.currency = :currency AND p.state IN " +
           "(com.ax.template.authblueprint.payment.PaymentState.AUTHORIZED, " +
           "com.ax.template.authblueprint.payment.PaymentState.CAPTURED)")
    BigDecimal sumActiveAuthorizedByOrderId(@Param("orderId") String orderId,
                                            @Param("currency") String currency);
}
