package com.ax.template.authblueprint.payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /** IDOR-safe lookup: returns empty when not owned by the caller (controller maps to 404). */
    Optional<Payment> findByIdAndUserId(UUID id, UUID userId);

    Page<Payment> findByUserId(UUID userId, Pageable pageable);
}
