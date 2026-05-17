package com.ax.template.authblueprint.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PAYMENT-RECON-001: append-only ledger. Intentionally exposes no update or delete
 * methods — structural guard against UPDATEs to the immutable audit trail. JpaRepository
 * does carry save/delete methods, but {@link PaymentEventLedger} is the only writer and
 * issues only inserts. The DB-level trigger is the secondary defense.
 */
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, UUID> {

    List<PaymentEvent> findByPaymentIdOrderByOccurredAtAsc(UUID paymentId);

    Optional<PaymentEvent> findFirstByPaymentIdOrderByOccurredAtDesc(UUID paymentId);
}
