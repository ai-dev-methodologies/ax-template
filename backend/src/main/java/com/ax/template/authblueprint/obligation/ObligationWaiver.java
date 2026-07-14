package com.ax.template.authblueprint.obligation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One waiver grant (OBL-WAIVER-001/002) — a dual-axis conditional exemption suppressing an
 * obligation's BREACH consequence while valid. Fully immutable once granted (no update
 * endpoint, no mutator here at all): revoking a waiver never touches this row — it appends a
 * separate {@link WaiverRevocation} record instead. Validity is computed by the service from
 * the two immutable bounds ({@link #expiresAt} / {@link #expiresAfterCycles}) plus the absence
 * of a revocation — never stored as a flag on this entity.
 */
@AggregateMember(root = Obligation.class)
@Entity
@Table(name = "obligation_waivers")
public class ObligationWaiver {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "obligation_id", nullable = false, updatable = false)
    private UUID obligationId;

    /** The authenticated caller who granted this waiver — never the declared owner (OBL-WAIVER-002). */
    @Column(name = "granted_by", nullable = false, updatable = false, length = 200)
    private String grantedBy;

    /** The party the waiver is granted FOR — must differ from {@link #grantedBy} (4-eyes). */
    @Column(name = "obligation_owner", nullable = false, updatable = false, length = 200)
    private String obligationOwner;

    @Column(name = "reason", nullable = false, updatable = false, length = 500)
    private String reason;

    @Column(name = "granted_at", nullable = false, updatable = false)
    private Instant grantedAt;

    /** The obligation's usage-cycle count AT grant time — {@link #expiresAfterCycles} counts from here. */
    @Column(name = "granted_at_cycle", nullable = false, updatable = false)
    private long grantedAtCycle;

    /** TIME axis bound. */
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    /** USAGE-CYCLE axis bound — the count of usage-advance events since {@link #grantedAtCycle}. */
    @Column(name = "expires_after_cycles", nullable = false, updatable = false)
    private long expiresAfterCycles;

    protected ObligationWaiver() {}

    public ObligationWaiver(UUID id, UUID obligationId, String grantedBy, String obligationOwner,
                            String reason, Instant grantedAt, long grantedAtCycle,
                            Instant expiresAt, long expiresAfterCycles) {
        this.id = id;
        this.obligationId = obligationId;
        this.grantedBy = grantedBy;
        this.obligationOwner = obligationOwner;
        this.reason = reason;
        this.grantedAt = grantedAt;
        this.grantedAtCycle = grantedAtCycle;
        this.expiresAt = expiresAt;
        this.expiresAfterCycles = expiresAfterCycles;
    }

    /** OBL-WAIVER-001 — valid only while NEITHER axis has lapsed (the earlier bound governs). */
    boolean isValidAt(Instant now, long currentUsageCycleCount) {
        if (!now.isBefore(expiresAt)) {
            return false;
        }
        return (currentUsageCycleCount - grantedAtCycle) < expiresAfterCycles;
    }

    public UUID getId() { return id; }
    public UUID getObligationId() { return obligationId; }
    public String getGrantedBy() { return grantedBy; }
    public String getObligationOwner() { return obligationOwner; }
    public String getReason() { return reason; }
    public Instant getGrantedAt() { return grantedAt; }
    public long getGrantedAtCycle() { return grantedAtCycle; }
    public Instant getExpiresAt() { return expiresAt; }
    public long getExpiresAfterCycles() { return expiresAfterCycles; }
}
