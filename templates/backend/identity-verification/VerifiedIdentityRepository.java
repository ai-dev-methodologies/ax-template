/**
 * @ax-template-meta
 * template_id: backend/identity-verification/VerifiedIdentityRepository
 * layer: backend-domain
 * domain: identity-verification
 * anchors_rule: testing-archunit-repository-shape.md
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — JpaRepository provides findAll(Pageable), save(), and derived query methods"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   VerifiedIdentityRepository provides JPA-based data access for VerifiedIdentity records.
 */
package com.example.app.identityverification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

/**
 * JPA repository for {@link VerifiedIdentity} records.
 *
 * <p>Soft-delete queries must exclude {@code deleted_at IS NOT NULL} rows.
 * Spring Data JPA @SQLDelete on the entity handles soft-delete automatically for
 * {@code repository.delete()} calls.
 */
@Repository
public interface VerifiedIdentityRepository extends JpaRepository<VerifiedIdentity, UUID> {

    /**
     * Find all non-deleted records for a specific provider (IDV-ADMIN-001).
     *
     * @param providerName "pass" | "kcb"
     * @param pageable     pagination and sort
     * @return page of verified identities for the given provider
     */
    Page<VerifiedIdentity> findByProviderName(String providerName, Pageable pageable);
}
