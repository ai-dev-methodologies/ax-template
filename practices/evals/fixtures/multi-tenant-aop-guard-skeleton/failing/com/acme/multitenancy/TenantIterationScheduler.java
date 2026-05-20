package com.acme.multitenancy;

import java.util.List;
import java.util.UUID;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * FAILING fixture for scheduled_task_tenant_scope_guard.sh.
 *
 * Three deliberate violations of
 * blueprints/multi-tenant-manifest.yaml#scheduled-task-tenant-scope:
 *
 *   (1) @SchedulerLock name is bare "reconciliation" — no #tenantId.
 *       Forbidden by #lock_key_contract.forbidden_substrings.
 *       Symptom: all tenants serialize behind one cluster node;
 *       one tenant's hang turns into a fleet-wide outage.
 *
 *   (2) TenantContext.set is called inside the loop without a matching
 *       TenantContext.clear() in a finally block. The set count (1)
 *       does not equal the clear count (0) when the file is scanned.
 *       Symptom: tenantId N leaks into tenantId N+1's iteration; the
 *       canonical cross-tenant write vector.
 *
 *   (3) Tenant enumeration is hardcoded (List.of) instead of going
 *       through tenantCatalog.listActive(). No TenantCatalog field
 *       exists on the class. Deactivated tenants continue receiving
 *       scheduled-task side effects; new tenants require a redeploy.
 *
 * Each violation is independently sufficient to trip the 39th guard.
 */
@Component
public class TenantIterationScheduler {

    @Scheduled(cron = "0 0 3 * * *")
    @SchedulerLock(name = "reconciliation",
                   lockAtMostFor = "PT10M",
                   lockAtLeastFor = "PT30S")
    public void reconcilePayments() {
        // Violation (3): hardcoded tenant list, no TenantCatalog.
        List<UUID> hardcodedTenants = List.of(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000002"));

        for (UUID tenantId : hardcodedTenants) {
            // Violation (2): set without a matching clear() in finally.
            // Worker thread reused across iterations retains the context.
            TenantContext.set(tenantId);
            doReconciliationWork();
            // NO TenantContext.clear() — deliberate leak.
        }
    }

    private void doReconciliationWork() {
        // body intentionally empty for fixture purposes
    }
}
