/**
 * @ax-template-meta
 * template_id: backend/security/RoleHierarchyConfig
 * layer: backend-cross-cutting
 * anchors_rule: role-hierarchy-subsumes-lower-tiers.md (PRACTICES-SECURITY-004)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Spring Security Reference — Authorization Architecture / Hierarchical Roles (RoleHierarchyImpl)"
 *     url: "https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html"
 *   - source_type: external
 *     citation: "OWASP API Security Top 10 (2023) — API5:2023 Broken Function Level Authorization"
 *     url: "https://owasp.org/API-Security/editions/2023/en/0xa5-broken-function-level-authorization/"
 * usage: |
 *   Use this ONLY when your application has more than two roles AND the tiers
 *   form a subsumption chain (a higher tier can do everything a lower tier can:
 *   ROLE_ADMIN > ROLE_MANAGER > ROLE_MEMBER). The flat two-tier ADMIN-vs-authenticated
 *   model in the reference workload does NOT need this — there is no subsumption to express.
 *
 *   Replace 'com.example.app' with your base package, and edit the single
 *   roleHierarchy() chain below to match YOUR tiers. Once this bean is present:
 *
 *     - Method gates name ONLY the minimum tier, never the superior list:
 *         @PreAuthorize("hasRole('MANAGER')")   // ADMIN admitted automatically
 *     - URL gates inherit the same hierarchy (Spring Security wires the
 *       RoleHierarchy into the authorizeHttpRequests AuthorizationManager
 *       automatically when the bean is present):
 *         .requestMatchers("/api/reports/**").hasRole("MANAGER")  // ADMIN admitted
 *
 *   FORBIDDEN once this bean exists (PRACTICES-SECURITY-004): enumerating the
 *   superior-role list at any gate to mean "or above" —
 *         @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")   // ❌ drifts, fails silently
 *   The hierarchy is the single source of truth; gates name only the floor.
 */
package com.example.app.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Declares a multi-tier RBAC subsumption chain in ONE place.
 *
 * <p>Mandates (PRACTICES-SECURITY-004 — role-hierarchy-subsumes-lower-tiers.md):
 * <ul>
 *   <li>The subsumption chain {@code ROLE_ADMIN > ROLE_MANAGER > ROLE_MEMBER}
 *       is declared exactly once, as data, in {@link #roleHierarchy()}.</li>
 *   <li>The hierarchy is wired into a {@link MethodSecurityExpressionHandler}
 *       so {@code @PreAuthorize} honors it; the web {@code authorizeHttpRequests}
 *       {@code AuthorizationManager} inherits the same bean automatically.</li>
 *   <li>Callers therefore gate on the MINIMUM tier only
 *       ({@code @PreAuthorize("hasRole('MANAGER')")}); superiors are admitted by
 *       the hierarchy — never enumerate {@code hasAnyRole('ADMIN','MANAGER')}.</li>
 * </ul>
 *
 * <p>Per the Spring Security Reference: "A user who is authenticated with
 * {@code ROLE_ADMIN}, will behave as if they have all four roles when security
 * constraints are evaluated against any filter- or method-based rules."
 */
@Configuration
@EnableMethodSecurity
public class RoleHierarchyConfig {

    /**
     * The role subsumption chain — the SINGLE source of truth.
     *
     * <p>Edit this chain to match your tiers. Adding or reordering a tier is a
     * one-line change here; it must NEVER be expressed by editing the role list
     * at individual {@code @PreAuthorize} sites.
     *
     * <p>Equivalent declarative form:
     * <pre>{@code
     * RoleHierarchyImpl.fromHierarchy("""
     *     ROLE_ADMIN > ROLE_MANAGER
     *     ROLE_MANAGER > ROLE_MEMBER
     *     """);
     * }</pre>
     */
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
            .role("ADMIN").implies("MANAGER")
            .role("MANAGER").implies("MEMBER")
            .build();
    }

    /**
     * Wires the {@link #roleHierarchy()} bean into method security so that
     * {@code @PreAuthorize("hasRole('MEMBER')")} admits MANAGER and ADMIN too.
     */
    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }
}
