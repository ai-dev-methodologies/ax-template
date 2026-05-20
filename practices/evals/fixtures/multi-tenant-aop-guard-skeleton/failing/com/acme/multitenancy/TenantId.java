package com.acme.multitenancy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method parameter whose value is the tenant_id of the resource
 * being accessed. Parameter type MUST be java.util.UUID (canonical -- see
 * blueprints/multi-tenant-manifest.yaml#aop-guard.tenant_id_shape). A
 * different type at runtime causes AuthorizedTenantInterceptor to throw
 * IllegalStateException on the first call (fail-fast wiring bug).
 *
 * Generated from blueprints/multi-tenant-manifest.yaml#aop-guard.tenant_id_marker
 * with <root> = acme.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface TenantId {
}
