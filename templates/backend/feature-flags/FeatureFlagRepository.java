/**
 * @ax-template-meta
 * template_id: backend/feature-flags/FeatureFlagRepository
 * layer: backend-domain
 * domain: feature-flags
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — JpaRepository"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/repositories/core-concepts.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   String PK (name) — no UUID generation needed.
 */
package com.example.app.featureflags;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for FeatureFlag entities.
 *
 * <p>Uses name (String) as the PK. Spring Data provides findById, save, delete, existsById.
 * No custom queries needed for basic CRUD.
 *
 * <p>spec_ref: specs/feature-flags-l0.yaml (FF-CRUD-001..FF-CRUD-004)
 */
@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, String> {
}
