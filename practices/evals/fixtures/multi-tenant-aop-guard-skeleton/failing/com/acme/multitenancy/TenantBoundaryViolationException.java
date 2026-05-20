package com.acme.multitenancy;

/**
 * Thrown by AuthorizedTenantInterceptor when the request principal's
 * TenantContext.current() does not match the resource's tenant_id.
 * Mapped to HTTP 404 via MultiTenantProblemDetailAdvice — NEVER 403
 * (would leak existence of cross-tenant resource).
 *
 * Generated from blueprints/multi-tenant-manifest.yaml#aop-guard.exception_skeleton
 * with <root> = acme.
 */
public class TenantBoundaryViolationException extends RuntimeException {
    public TenantBoundaryViolationException(String message) {
        super(message);
    }
}
