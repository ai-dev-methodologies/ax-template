package com.ax.template.authblueprint.authzparity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One immutable record of a REFUSED execution attempt against an action (AUTHZPARITY-EXEC-001):
 * a substituted/escalated parameter offered a parity hash that did NOT match the authorized
 * envelope. Captures the offered hash, the authorized hash, and the instant — so an executed-
 * matches-authorized violation is recorded loudly, never silently dropped. Fully append-only.
 * The {@code @Check} backstops that a BLOCKED attempt is genuinely a mismatch (offered <> authorized).
 */
@AggregateMember(root = AuthorizedAction.class)
@Entity
@Table(name = "blocked_attempts")
// AUTHZPARITY-EXEC-001 — a blocked attempt is recorded ONLY for a genuine mismatch. LIVE under ddl-auto.
@Check(constraints = "offered_hash <> authorized_hash")
public class BlockedAttempt {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "action_id", nullable = false, updatable = false)
    private UUID actionId;

    @Column(name = "offered_hash", nullable = false, updatable = false, length = 64)
    private String offeredHash;

    @Column(name = "authorized_hash", nullable = false, updatable = false, length = 64)
    private String authorizedHash;

    @Column(name = "attempted_by", nullable = false, updatable = false, length = 200)
    private String attemptedBy;

    @Column(name = "attempted_at", nullable = false, updatable = false)
    private Instant attemptedAt;

    protected BlockedAttempt() {}

    public BlockedAttempt(UUID id, UUID actionId, String offeredHash, String authorizedHash,
                          String attemptedBy, Instant attemptedAt) {
        this.id = id;
        this.actionId = actionId;
        this.offeredHash = offeredHash;
        this.authorizedHash = authorizedHash;
        this.attemptedBy = attemptedBy;
        this.attemptedAt = attemptedAt;
    }

    public UUID getId() { return id; }
    public UUID getActionId() { return actionId; }
    public String getOfferedHash() { return offeredHash; }
    public String getAuthorizedHash() { return authorizedHash; }
    public String getAttemptedBy() { return attemptedBy; }
    public Instant getAttemptedAt() { return attemptedAt; }
}
