package com.acme.multitenancy;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Request-scoped tenant identity holder.
 *
 * Storage choice (load-bearing): plain ThreadLocal — NOT
 * InheritableThreadLocal. The latter silently propagates the
 * value to child threads, defeating the explicit-propagation
 * contract in #async-propagation. With plain ThreadLocal,
 * crossing an @Async boundary without TenantContextAwareTaskDecorator
 * surfaces a hard failure (empty Optional → exception), which
 * is the only safe behaviour for tenant isolation.
 *
 * Generated from blueprints/multi-tenant-manifest.yaml#context-resolution.tenant_context_skeleton
 * with <root> = acme.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {
        // utility class — no instances
    }

    public static Optional<UUID> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void set(UUID tenantId) {
        Objects.requireNonNull(tenantId, "tenantId");
        CURRENT.set(tenantId);
    }

    public static void clear() {
        CURRENT.remove();
    }
}
