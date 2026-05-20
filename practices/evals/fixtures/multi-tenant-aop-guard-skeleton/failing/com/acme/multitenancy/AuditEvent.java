package com.acme.multitenancy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.UUID;

/**
 * INTENTIONALLY WRONG audit entity — violates all three clauses of
 * {@code blueprints/multi-tenant-manifest.yaml#ledger-audit-tenant-scope}:
 *
 * <ol>
 *   <li>implements {@link TenantOwned} — mandates non-null tenant_id, so
 *       orphan audit rows (signature_fail callback at permitAll endpoint)
 *       cannot persist;</li>
 *   <li>{@code tenant_id} column declares {@code nullable = false} —
 *       insert fails when {@code TenantContext} is empty; external PG
 *       retries indefinitely;</li>
 *   <li>{@code getTenantId()} returns bare {@code UUID} — silently
 *       returns null at call sites; no Optional contract to force
 *       acknowledgement of the empty case.</li>
 * </ol>
 *
 * <p>{@code ledger_audit_tenant_nullable_guard.sh --fixtures} MUST trip
 * on this file. Used to prove the guard can mechanically detect each
 * failure mode the {@code #ledger-audit-tenant-scope} anchor was
 * introduced to prevent.
 */
@Entity
public class AuditEvent implements TenantOwned {

    @Id
    @GeneratedValue
    private UUID id;

    /** WRONG: nullable=false rejects orphan audit rows. */
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "event_type", nullable = false, updatable = false)
    private String eventType;

    protected AuditEvent() {
        // JPA
    }

    public AuditEvent(UUID tenantId, String eventType) {
        // WRONG: bare UUID parameter — call sites silently pass null
        // when TenantContext.current() is empty; the insert then
        // explodes on the NOT NULL constraint, surfacing as a 500.
        this.tenantId = tenantId;
        this.eventType = eventType;
    }

    public UUID getId() {
        return id;
    }

    /** WRONG: bare UUID return — TenantOwned contract honoured but
     *  empty-context detection at call sites is impossible. */
    @Override
    public UUID getTenantId() {
        return tenantId;
    }

    public String getEventType() {
        return eventType;
    }
}
