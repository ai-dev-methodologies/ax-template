/**
 * @ax-template-meta
 * template_id: backend/data/JpaAuditConfig
 * layer: backend-infrastructure
 * domain: data
 * anchors_rule: soft-delete-only-on-base-entity.md (PRACTICES-PERS-005)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — @EnableJpaAuditing enables AuditingEntityListener to populate @CreatedDate and @LastModifiedDate fields automatically on persist and merge"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/auditing.html"
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — AuditorAware<T> bean provides the current principal for @CreatedBy and @LastModifiedBy; optional when only timestamp auditing is used"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/auditing.html#auditing.auditor-aware"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Add this @Configuration class to activate JPA auditing for all BaseEntity subclasses.
 *   @CreatedDate and @LastModifiedDate on BaseEntity are populated by AuditingEntityListener.
 *   @EnableJpaAuditing MUST be on exactly one @Configuration class in the application context.
 */
package com.example.app.data;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Activates Spring Data JPA auditing for all entities carrying {@link AuditingEntityListener}.
 *
 * <p>This configuration is the single point of activation for:
 * <ul>
 *   <li>{@code @CreatedDate} → {@code BaseEntity.createdAt} — set on first persist</li>
 *   <li>{@code @LastModifiedDate} → {@code BaseEntity.updatedAt} — updated on every merge</li>
 * </ul>
 *
 * <p>The {@code @EnableJpaAuditing} annotation must appear on exactly one
 * {@code @Configuration} class in the application context. Placing it here (separate
 * from the main application class) keeps the configuration explicit and testable.
 *
 * <p>If {@code @CreatedBy} / {@code @LastModifiedBy} auditing is also needed,
 * add an {@code AuditorAware<String>} bean returning the current principal's ID.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditConfig {
    // No beans required for timestamp-only auditing.
    // Add AuditorAware<String> here if @CreatedBy / @LastModifiedBy fields are used.
}
