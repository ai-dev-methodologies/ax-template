package com.acme.multitenancy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.Optional;
import java.util.UUID;

/**
 * Canonical audit / ledger entity fixture for blueprints/multi-tenant-manifest.yaml
 * anchor {@code #ledger-audit-tenant-scope}.
 *
 * <p>Models the GAP-R3-3 closure surface: an append-only event entity
 * that can be inserted OUTSIDE a tenant-scoped request boundary
 * (e.g. a PG callback signature_fail at a {@code permitAll} endpoint
 * where the request has no authenticated principal and therefore no
 * {@code TenantContext}).
 *
 * <p>Three load-bearing properties that distinguish this from a
 * tenant-scoped resource:
 *
 * <ol>
 *   <li><b>Does NOT implement {@link TenantOwned}.</b> Audit/ledger
 *       entities are not tenant-scoped resources in the AOP-boundary
 *       sense; they are operational records whose tenant attribution
 *       is best-effort enrichment, not an isolation boundary.</li>
 *   <li><b>{@code tenant_id} column is nullable.</b> Orphan audit rows
 *       (signature_fail callback with no resolvable Payment) are
 *       preserved with {@code tenant_id = null} rather than dropped
 *       or assigned a sentinel UUID.</li>
 *   <li><b>Getter returns {@code Optional<UUID>}.</b> Bare
 *       {@code UUID getTenantId()} would silently return {@code null}
 *       at call sites — the Optional return forces every reader to
 *       acknowledge the empty case explicitly.</li>
 * </ol>
 *
 * <p>Insert-time contract: the constructor accepts the result of
 * {@code TenantContext.current()} unchanged (NOT
 * {@code TenantContext.current().orElseThrow(...)}). The Optional →
 * null collapse happens exactly once, at this boundary.
 *
 * <p>Schema lockstep: the {@code tenant_id} JPA nullability declared
 * here MUST agree with the Flyway migration. A
 * {@code ledger_audit_tenant_nullable_guard.sh} mirroring
 * {@code ledger_audit_nullability_guard.sh} can enforce this when a
 * fork-receiver wires multi-tenant onto an existing audit table.
 */
@Entity
public class AuditEvent {

    @Id
    @GeneratedValue
    private UUID id;

    /**
     * Tenant attribution — nullable. Orphan audit rows with no
     * tenant context (signature_fail callback at permitAll endpoint)
     * are valid and MUST be preserved. See manifest anchor
     * {@code #ledger-audit-tenant-scope.policy.tenant_id_column_nullability}.
     */
    @Column(name = "tenant_id", nullable = true, updatable = false)
    private UUID tenantId;

    @Column(name = "event_type", nullable = false, updatable = false)
    private String eventType;

    @Column(name = "payload_hash", nullable = false, updatable = false)
    private String payloadHash;

    protected AuditEvent() {
        // JPA
    }

    /**
     * Insert-time constructor. The Optional contract makes the null
     * path explicit at every call site — see anchor
     * {@code #ledger-audit-tenant-scope.policy.append_call_site_contract}.
     *
     * @param tenantId result of {@code TenantContext.current()}; pass
     *                 unchanged from the request-boundary read. NEVER
     *                 unwrap with {@code orElseThrow} — that is the
     *                 contract for tenant-scoped resources, not audit
     *                 entities.
     * @param eventType non-null event discriminator (e.g.
     *                  {@code "SIGNATURE_FAIL"}, {@code "CAPTURE_OK"}).
     * @param payloadHash sha256 of the audit payload.
     */
    public AuditEvent(Optional<UUID> tenantId, String eventType, String payloadHash) {
        this.tenantId = tenantId.orElse(null);
        this.eventType = eventType;
        this.payloadHash = payloadHash;
    }

    public UUID getId() {
        return id;
    }

    /**
     * Returns the tenant attribution. {@link Optional#empty()} signals
     * an orphan audit row appended outside a tenant-scoped request
     * boundary — operational reads MUST treat this case explicitly
     * (do NOT fold it into the current-tenant view).
     */
    public Optional<UUID> getTenantId() {
        return Optional.ofNullable(tenantId);
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayloadHash() {
        return payloadHash;
    }
}
