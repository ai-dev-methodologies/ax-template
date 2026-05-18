/**
 * @ax-template-meta
 * template_id: backend/billing/PlanRepository
 * layer: backend-domain
 * domain: billing
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — JpaRepository"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/jpa/repositories.html"
 */
package com.example.app.billing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, UUID> {

    Optional<Plan> findByIdAndActiveTrue(UUID id);

    List<Plan> findAllByActiveTrueOrderByAmountAsc();
}
