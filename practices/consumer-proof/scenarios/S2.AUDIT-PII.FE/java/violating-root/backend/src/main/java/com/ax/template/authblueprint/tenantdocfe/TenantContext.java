package com.ax.template.authblueprint.tenantdocfe;

import java.util.Optional;
import java.util.UUID;

/**
 * HAND-ROLLED — capability-gap signal.
 *
 * The catalog has NO runtime {@code common/TenantContext} primitive. What it
 * has instead is a design-time SKELETON embedded as a {@code java_skeleton:}
 * block inside {@code blueprints/multi-tenant-manifest.yaml}
 * (#row-level-strategy.filter_activation), and a {@code TenantContext.java}
 * that exists ONLY as a passing/failing FIXTURE under
 * {@code practices/evals/fixtures/multi-tenant-aop-guard-skeleton/} — i.e. a
 * test double for {@code multi_tenant_aop_guard_skeleton_guard.sh} to scan,
 * not a reusable {@code common/} class any real domain can import. Confirmed:
 * {@code find backend/src/main/java/.../common -iname 'TenantContext*'}
 * returns 0 hits. Every domain in {@code backend/src} today is single-tenant.
 *
 * This is a minimal, faithful port of the blueprint's own skeleton
 * (ThreadLocal-scoped, request-lifecycle-bound) so the scenario's clean
 * variant has something real to call. It does NOT modify the real backend.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID tenantId) {
        CURRENT.set(tenantId);
    }

    public static Optional<UUID> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }
}
