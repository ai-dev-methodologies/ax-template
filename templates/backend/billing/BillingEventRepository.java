/**
 * @ax-template-meta
 * template_id: backend/billing/BillingEventRepository
 * layer: backend-domain
 * domain: billing
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — JpaRepository"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/jpa/repositories.html"
 */
package com.example.app.billing;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillingEventRepository extends JpaRepository<BillingEvent, UUID> {

    Page<BillingEvent> findBySubscriptionId(UUID subscriptionId, Pageable pageable);

    @Query("SELECT e FROM BillingEvent e WHERE e.subscriptionId IN " +
           "(SELECT s.id FROM Subscription s WHERE s.userId = :userId)")
    Page<BillingEvent> findBySubscriptionUserId(@Param("userId") UUID userId, Pageable pageable);
}
