package com.acme.multitenancy;

import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Generated from blueprints/multi-tenant-manifest.yaml#row-level-strategy.filter_activation
 * with <root> = acme.
 *
 * Ordering: MUST run AFTER the security filter chain (so principal is
 * resolved and TenantContext is populated) and BEFORE the first
 * @Transactional boundary (otherwise the first query in the
 * transaction runs without filter).
 */
@Component
public class TenantFilterActivationFilter extends OncePerRequestFilter {

    private final EntityManager entityManager;

    public TenantFilterActivationFilter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {
        Session session = entityManager.unwrap(Session.class);
        try {
            TenantContext.current().ifPresentOrElse(
                tenantId -> session.enableFilter("tenantFilter")
                                   .setParameter("tenantId", tenantId),
                () -> {
                    // No tenant context = unauthenticated / public endpoint.
                    // Filter NOT enabled — relies on Spring Security to gate
                    // the endpoint. Tenant-scoped endpoints MUST be
                    // SecurityFilterChain-protected; otherwise data leaks.
                }
            );
            chain.doFilter(req, res);
        } finally {
            // Disable explicitly — even though session typically closes at
            // request end, OEMIV / pooled sessions can outlive a single
            // request in misconfigured setups.
            if (session.isOpen()) {
                session.disableFilter("tenantFilter");
            }
        }
    }
}
