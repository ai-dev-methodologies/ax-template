package com.ax.template.authblueprint.authzparity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One immutable four-eyes signoff on an action (AUTHZPARITY-FOUREYES-001): fully append-only —
 * every column {@code updatable=false}, no public setter. The NIST two-person rule — the signer
 * MUST differ from the requester (and, enforced in the service, from any prior signer) — is
 * DB-backstopped via {@code @Check approver_user_id <> requester_user_id} so it holds even under
 * ddl-auto. One signoff per (action, approver) via the unique constraint.
 */
@AggregateMember(root = AuthorizedAction.class)
@Entity
@Table(name = "action_signoffs", uniqueConstraints = {
    @UniqueConstraint(name = "uq_signoff_action_approver", columnNames = {"action_id", "approver_user_id"})
})
// AUTHZPARITY-FOUREYES-001 — the signer is never the requester. LIVE under ddl-auto.
@Check(constraints = "approver_user_id <> requester_user_id")
public class ActionSignoff {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "action_id", nullable = false, updatable = false)
    private UUID actionId;

    @Column(name = "approver_user_id", nullable = false, updatable = false, length = 200)
    private String approverUserId;

    /** Denormalized requester copy — lets the @Check enforce the separation on the row alone. */
    @Column(name = "requester_user_id", nullable = false, updatable = false, length = 200)
    private String requesterUserId;

    @Column(name = "signed_at", nullable = false, updatable = false)
    private Instant signedAt;

    protected ActionSignoff() {}

    public ActionSignoff(UUID id, UUID actionId, String approverUserId, String requesterUserId,
                         Instant signedAt) {
        this.id = id;
        this.actionId = actionId;
        this.approverUserId = approverUserId;
        this.requesterUserId = requesterUserId;
        this.signedAt = signedAt;
    }

    public UUID getId() { return id; }
    public UUID getActionId() { return actionId; }
    public String getApproverUserId() { return approverUserId; }
    public String getRequesterUserId() { return requesterUserId; }
    public Instant getSignedAt() { return signedAt; }
}
