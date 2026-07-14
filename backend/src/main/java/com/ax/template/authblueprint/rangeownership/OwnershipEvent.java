package com.ax.template.authblueprint.rangeownership;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateMember;

/**
 * RNG-PORT-003 — one IMMUTABLE, APPEND-ONLY ownership event: the initial assignment
 * ({@code fromOwner} null, {@code reason} "INITIAL_ASSIGNMENT") or a later port
 * ({@code fromOwner} the prior current owner, derived server-side — never client-supplied).
 * Never updated, never deleted. The current owner of an identifier is the {@code toOwner} of the
 * LATEST event ordered by {@code occurredAt}/{@code id}.
 */
@AggregateMember(root = IdentifierAssignment.class)
@Entity
@Table(name = "ownership_events")
public class OwnershipEvent {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "assignment_id", nullable = false, updatable = false)
    private UUID assignmentId;

    /** Null on the initial assignment event; the prior current owner on every subsequent port. */
    @Column(name = "from_owner", updatable = false, length = 200)
    private String fromOwner;

    @Column(name = "to_owner", nullable = false, updatable = false, length = 200)
    private String toOwner;

    @Column(name = "reason", nullable = false, updatable = false, length = 200)
    private String reason;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected OwnershipEvent() {}

    public OwnershipEvent(UUID id, UUID assignmentId, String fromOwner, String toOwner, String reason, Instant occurredAt) {
        this.id = id;
        this.assignmentId = assignmentId;
        this.fromOwner = fromOwner;
        this.toOwner = toOwner;
        this.reason = reason;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public UUID getAssignmentId() { return assignmentId; }
    public String getFromOwner() { return fromOwner; }
    public String getToOwner() { return toOwner; }
    public String getReason() { return reason; }
    public Instant getOccurredAt() { return occurredAt; }
}
