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

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Activates Spring Data JPA auditing for all entities carrying {@link AuditingEntityListener}.
 *
 * <p>This configuration is the single point of activation for:
 * <ul>
 *   <li>{@code @CreatedDate} → {@code BaseEntity.createdAt} — set on first persist</li>
 *   <li>{@code @LastModifiedDate} → {@code BaseEntity.updatedAt} — updated on every merge</li>
 *   <li>{@code @CreatedBy} → {@code BaseEntity.createdBy} — set on first persist</li>
 *   <li>{@code @LastModifiedBy} → {@code BaseEntity.lastModifiedBy} — updated on every merge</li>
 * </ul>
 *
 * <p>The {@code @EnableJpaAuditing} annotation must appear on exactly one
 * {@code @Configuration} class in the application context. Placing it here (separate
 * from the main application class) keeps the configuration explicit and testable.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditConfig {

    /**
     * Provides the current authenticated principal's name for {@code @CreatedBy} and
     * {@code @LastModifiedBy} population. Returns {@link Optional#empty()} when no
     * authenticated principal is present (e.g. during unauthenticated requests).
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName);
    }
}
