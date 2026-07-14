package com.ax.template.authblueprint.obligation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * A waiver revocation (OBL-WAIVER-002) — an APPENDED, immutable record. Revoking a waiver never
 * mutates or deletes {@link ObligationWaiver}; it adds one of these instead, so the original
 * grant survives exactly as issued. UNIQUE(waiver_id) makes a double-revoke unrepresentable at
 * the DB layer, backstopping the service's own 409 check.
 */
@AggregateMember(root = Obligation.class)
@Entity
@Table(name = "obligation_waiver_revocations", uniqueConstraints = {
    @UniqueConstraint(name = "uq_waiver_revocation", columnNames = {"waiver_id"})
})
public class WaiverRevocation {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "waiver_id", nullable = false, updatable = false)
    private UUID waiverId;

    @Column(name = "obligation_id", nullable = false, updatable = false)
    private UUID obligationId;

    @Column(name = "revoked_by", nullable = false, updatable = false, length = 200)
    private String revokedBy;

    @Column(name = "revoked_at", nullable = false, updatable = false)
    private Instant revokedAt;

    protected WaiverRevocation() {}

    public WaiverRevocation(UUID id, UUID waiverId, UUID obligationId, String revokedBy, Instant revokedAt) {
        this.id = id;
        this.waiverId = waiverId;
        this.obligationId = obligationId;
        this.revokedBy = revokedBy;
        this.revokedAt = revokedAt;
    }

    public UUID getId() { return id; }
    public UUID getWaiverId() { return waiverId; }
    public UUID getObligationId() { return obligationId; }
    public String getRevokedBy() { return revokedBy; }
    public Instant getRevokedAt() { return revokedAt; }
}
