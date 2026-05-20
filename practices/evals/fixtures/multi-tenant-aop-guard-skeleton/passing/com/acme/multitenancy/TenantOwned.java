package com.acme.multitenancy;

/**
 * Marker interface for every JPA @Entity whose rows are
 * tenant-scoped. The tenant_id MUST be populated at insert
 * time from TenantContext.current() — never from request body
 * or query parameter (PROPAGATION-001 forbids forgeable
 * sources). The getter type MUST match #tenant-id-shape
 * (canonical: java.util.UUID).
 *
 * Defense in depth: row-level @Filter + AOP interceptor +
 * TenantOwned marker together close the three vectors —
 * SQL leak (filter), service-call leak (interceptor),
 * static-analysis blind spot (marker).
 *
 * Generated from blueprints/multi-tenant-manifest.yaml#aop-guard.marker_interface
 * with <root> = acme.
 */
public interface TenantOwned {
    java.util.UUID getTenantId();
}
