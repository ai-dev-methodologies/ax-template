package com.acme.multitenancy;

/**
 * Thrown by TenantContextAwareTaskDecorator when an @Async task was
 * submitted from a thread with no TenantContext (typically: the
 * submission happened outside a tenant-aware request, OR the
 * filter that populates TenantContext was bypassed).
 *
 * Pair with TenantBoundaryViolationException — both extend
 * RuntimeException, both live in com.acme.multitenancy, both are
 * handled by MultiTenantProblemDetailAdvice. Boundary → 404
 * (client-side authz fail), ContextMissing → 500 (server bug,
 * ops alert).
 *
 * Generated from blueprints/multi-tenant-manifest.yaml#async-propagation.context_missing_exception_skeleton
 * with <root> = acme.
 */
public class TenantContextMissingException extends RuntimeException {
    public TenantContextMissingException(String message) {
        super(message);
    }
}
