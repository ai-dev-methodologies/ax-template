package com.ax.template.authblueprint.exceptiongate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * orthogonal-exception-gate-l0 audit trail (EXC-DIM-LIFT-001) — every raise/lift is recorded
 * append-only with actor + reason. Fully immutable; there is no mutator and no delete path.
 */
@AggregateMember(root = ExceptionGate.class)
@Entity
@Table(name = "exception_audit_entries")
public class ExceptionAuditEntry {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "gate_id", nullable = false, updatable = false)
    private UUID gateId;

    @Column(name = "action", nullable = false, updatable = false, length = 20)
    private String action;

    @Column(name = "reason", updatable = false, length = 500)
    private String reason;

    @Column(name = "actor", nullable = false, updatable = false, length = 200)
    private String actor;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected ExceptionAuditEntry() {}

    public ExceptionAuditEntry(UUID id, UUID gateId, String action, String reason, String actor,
                               Instant occurredAt) {
        this.id = id;
        this.gateId = gateId;
        this.action = action;
        this.reason = reason;
        this.actor = actor;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public UUID getGateId() { return gateId; }
    public String getAction() { return action; }
    public String getReason() { return reason; }
    public String getActor() { return actor; }
    public Instant getOccurredAt() { return occurredAt; }
}
