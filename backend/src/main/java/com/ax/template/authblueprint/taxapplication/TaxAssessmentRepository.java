package com.ax.template.authblueprint.taxapplication;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Root repository for the {@link TaxAssessment} aggregate (the single combined tax record). All
 * finders are keyed by order id and return at most one row (the UNIQUE constraint guarantees it),
 * so none returns an unbounded collection.
 */
public interface TaxAssessmentRepository extends JpaRepository<TaxAssessment, UUID> {

    /** The order's combined tax row, if one exists (read path). */
    Optional<TaxAssessment> findByOrderId(UUID orderId);

    /**
     * Pessimistic write lock so a concurrent re-price of the same order cannot create a second row
     * (the find-existing → update-or-create-or-remove convergence is serialized per order).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM TaxAssessment a WHERE a.orderId = :orderId")
    Optional<TaxAssessment> findByOrderIdForUpdate(@Param("orderId") UUID orderId);
}
