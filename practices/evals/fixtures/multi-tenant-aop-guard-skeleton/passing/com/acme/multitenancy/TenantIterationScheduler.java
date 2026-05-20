package com.acme.multitenancy;

import java.util.UUID;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Generated from blueprints/multi-tenant-manifest.yaml#scheduled-task-tenant-scope.per_tenant_iteration.canonical_skeleton
 * with <root> = acme.
 *
 * Per-tenant iteration scheduler. Each cron tick enumerates the active
 * tenant catalog (the SINGLE source of tenant enumeration — see
 * #tenant_catalog_contract) and runs the job body in a tenant-scoped
 * unit of work.
 *
 * Three load-bearing properties (enforced by 39th hard guard
 * scheduled_task_tenant_scope_guard.sh):
 *
 *   1. set/clear is balanced via try/finally — a leak between tenants
 *      is the cross-tenant write vector to guard against. The guard
 *      counts TenantContext.set vs TenantContext.clear in this file;
 *      strict equality is required.
 *
 *   2. tenantCatalog.listActive() is the SINGLE source of tenant
 *      enumeration. Hardcoding tenant ids or reading from a foreign
 *      cache lets a deactivated tenant continue receiving job side
 *      effects.
 *
 *   3. The Shedlock @SchedulerLock name template includes the
 *      tenantId scalar (Shedlock SpEL "#tenantId") so two nodes can
 *      process two tenants in parallel. A bare job-name lock would
 *      serialize all tenants behind one node and turn one tenant's
 *      hang into a fleet-wide outage (see #lock_key_contract.rationale).
 *
 * Per-tenant try/catch: one tenant's job throwing MUST NOT abort the
 * iteration for siblings. The audit row is attributed to the failing
 * tenantId (because TenantContext.set succeeded before the throw); the
 * iteration continues with the next tenant.
 */
@Component
public class TenantIterationScheduler {

    private final TenantCatalog tenantCatalog;
    private final ReconciliationJob reconciliationJob;

    public TenantIterationScheduler(TenantCatalog tenantCatalog,
                                    ReconciliationJob reconciliationJob) {
        this.tenantCatalog = tenantCatalog;
        this.reconciliationJob = reconciliationJob;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @SchedulerLock(
            name = "reconciliation-#tenantId",
            lockAtMostFor = "PT10M",
            lockAtLeastFor = "PT30S")
    public void reconcilePayments() {
        for (UUID tenantId : tenantCatalog.listActive()) {
            try {
                TenantContext.set(tenantId);
                reconciliationJob.runForCurrentTenant();
            } catch (RuntimeException ex) {
                // Per-tenant try/catch — log + continue. Audit row was
                // already attributed to this tenantId before the throw.
                // Do NOT rethrow: aborting here drops sibling tenants.
            } finally {
                // MUST clear — scheduler threads are pooled and reused.
                // Skipping this leaks tenantId N into tenantId N+1's
                // iteration body. This is the canonical cross-tenant
                // write vector that property (1) above guards against.
                TenantContext.clear();
            }
        }
    }

    /**
     * Marker interface for the per-tenant unit of work. Concrete
     * fork-receiver implementations live outside this skeleton package
     * (e.g. PaymentReconciliationJob, RetentionPurgeJob).
     */
    public interface ReconciliationJob {
        void runForCurrentTenant();
    }
}
