/**
 * @ax-template-meta
 * template_id: backend/billing/SubscriptionRepository
 * layer: backend-domain
 * domain: billing
 * anchors_rule: testing-archunit-repository-shape.md
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — JpaRepository"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/jpa/repositories.html"
 */
package com.example.app.billing;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    /** IDOR-safe lookup: only returns subscription if userId matches. */
    Optional<Subscription> findByIdAndUserId(UUID id, UUID userId);

    Page<Subscription> findByUserId(UUID userId, Pageable pageable);

    Optional<Subscription> findByProviderSubscriptionId(String providerSubscriptionId);
}
