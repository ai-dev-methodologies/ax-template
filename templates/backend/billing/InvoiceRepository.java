/**
 * @ax-template-meta
 * template_id: backend/billing/InvoiceRepository
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

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Page<Invoice> findBySubscriptionId(UUID subscriptionId, Pageable pageable);

    Page<Invoice> findByUserId(UUID userId, Pageable pageable);
}
