package com.acme.multitenancy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method that accesses tenant-scoped resources. The
 * AuthorizedTenantInterceptor enforces that every parameter annotated
 * with {@link TenantId} matches TenantContext.current() before the
 * method body runs. Mismatch -> TenantBoundaryViolationException -> 404.
 * Missing context -> TenantContextMissingException -> 500.
 *
 * Generated from blueprints/multi-tenant-manifest.yaml#aop-guard.authorized_tenant_annotation
 * with <root> = acme.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthorizedTenant {
}
