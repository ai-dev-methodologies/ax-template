package com.ax.template.authblueprint.accessgrant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * time-bounded-access-grant-l0 root: a TIME-BOUNDED RELATIONSHIP GRANT (ReBAC). A grant binds
 * a subject to a (resourceRef, relation) PLUS a half-open validity window [validFrom, validUntil)
 * (AGRANT-WINDOW-001). Access is allowed ONLY while the injected Clock's now is in the window AND
 * {@link #status} is ACTIVE — the verdict is RECOMPUTED by {@link #isActiveAt(Instant)} every
 * check; there is deliberately NO stored {@code expired}/{@code active} boolean column that a
 * passing clock could leave stale (the same row is allowed at T and denied at validUntil with no
 * intervening write). Grants are APPEND-ONLY and REVOCABLE: revoking records the actor + instant
 * immutably (AGRANT-REVOKE-001); there is NO delete path. Lifecycle moves ONLY via the
 * package-private {@link #revoke(String, Instant)} hook, called by {@link AccessGrantService}
 * under the grant row's PESSIMISTIC_WRITE lock.
 */
@AggregateRoot
@Entity
@Table(name = "access_grants")
@Check(constraints =
    "valid_until > valid_from"
    + " AND (status = 'ACTIVE' OR status = 'REVOKED')"
    + " AND ((status = 'REVOKED') = (revoked_at IS NOT NULL))"
    + " AND ((revoked_at IS NULL) = (revoked_by IS NULL))")
public class AccessGrant {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The subject the relation is granted to — opaque principal id, recorded verbatim. */
    @Column(name = "subject_id", nullable = false, updatable = false, length = 200)
    private String subjectId;

    /** The resource the relation is over (e.g. a delivery / case / site id) — opaque, verbatim. */
    @Column(name = "resource_ref", nullable = false, updatable = false, length = 200)
    private String resourceRef;

    /** The relation granted (e.g. courier, viewer, operator) — opaque vocabulary, verbatim. */
    @Column(name = "relation", nullable = false, updatable = false, length = 100)
    private String relation;

    @Column(name = "valid_from", nullable = false, updatable = false)
    private Instant validFrom;

    @Column(name = "valid_until", nullable = false, updatable = false)
    private Instant validUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GrantStatus status;

    /** Recorded ONCE on revoke — who revoked. NOT @Column(updatable=false): it is written by the
     *  single REVOKE update (a non-updatable column would be excluded from that UPDATE, leaving it
     *  NULL and violating the @Check). Write-once is enforced by the idempotent {@link #revoke} hook
     *  (it never overwrites a prior revoke) + the @Check, not by JPA updatability (AGRANT-REVOKE-001). */
    @Column(name = "revoked_by", length = 200)
    private String revokedBy;

    /** Recorded ONCE on revoke — when. NOT @Column(updatable=false) for the same reason as revokedBy. */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AccessGrant() {}

    public AccessGrant(UUID id, String subjectId, String resourceRef, String relation,
                       Instant validFrom, Instant validUntil, Instant createdAt) {
        this.id = id;
        this.subjectId = subjectId;
        this.resourceRef = resourceRef;
        this.relation = relation;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.status = GrantStatus.ACTIVE;
        this.createdAt = createdAt;
    }

    /**
     * AGRANT-WINDOW/BOUNDARY-001 — the RECOMPUTED access predicate. Allowed iff the grant is
     * ACTIVE AND {@code now} is in the HALF-OPEN window [validFrom, validUntil): the instant
     * equal to validUntil is OUTSIDE the window (the first instant of the denied side). This is
     * a pure function of (status, validFrom, validUntil, now) — no stored verdict, so the SAME
     * row flips from allowed to denied as the injected Clock crosses validUntil with no write.
     */
    public boolean isActiveAt(Instant now) {
        return status == GrantStatus.ACTIVE
            && !now.isBefore(validFrom)        // now >= validFrom
            && now.isBefore(validUntil);        // now <  validUntil (half-open upper bound)
    }

    /** True iff {@code now} is strictly before validFrom (the not-yet-valid arm of a denial). */
    public boolean isBeforeWindow(Instant now) {
        return now.isBefore(validFrom);
    }

    public boolean isRevoked() {
        return status == GrantStatus.REVOKED;
    }

    /** Sole-mutator hook — revoke the grant, recording the actor + instant immutably. Idempotent:
     *  a second revoke leaves the first (actor, instant) in place. */
    void revoke(String actor, Instant at) {
        if (this.status == GrantStatus.REVOKED) {
            return;                              // idempotent — never overwrite the first revoke
        }
        this.status = GrantStatus.REVOKED;
        this.revokedBy = actor;
        this.revokedAt = at;
    }

    public UUID getId() { return id; }
    public String getSubjectId() { return subjectId; }
    public String getResourceRef() { return resourceRef; }
    public String getRelation() { return relation; }
    public Instant getValidFrom() { return validFrom; }
    public Instant getValidUntil() { return validUntil; }
    public GrantStatus getStatus() { return status; }
    public String getRevokedBy() { return revokedBy; }
    public Instant getRevokedAt() { return revokedAt; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
