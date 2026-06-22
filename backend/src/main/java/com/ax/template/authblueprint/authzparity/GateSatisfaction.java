package com.ax.template.authblueprint.authzparity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One immutable record that a declared MANDATORY companion gate was satisfied for an action
 * (AUTHZPARITY-GATES-001): the gate key, who satisfied it, and the instant. Fully append-only —
 * every column {@code updatable=false}, no public setter. One satisfaction per (action, gateKey)
 * via the unique constraint; a second is idempotent / rejected. A gate the action never declared
 * cannot be satisfied (UNKNOWN_GATE) — the satisfiable set is exactly the action's declared set.
 */
@AggregateMember(root = AuthorizedAction.class)
@Entity
@Table(name = "gate_satisfactions", uniqueConstraints = {
    @UniqueConstraint(name = "uq_gate_action_key", columnNames = {"action_id", "gate_key"})
})
public class GateSatisfaction {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "action_id", nullable = false, updatable = false)
    private UUID actionId;

    @Column(name = "gate_key", nullable = false, updatable = false, length = 100)
    private String gateKey;

    @Column(name = "satisfied_by", nullable = false, updatable = false, length = 200)
    private String satisfiedBy;

    @Column(name = "satisfied_at", nullable = false, updatable = false)
    private Instant satisfiedAt;

    protected GateSatisfaction() {}

    public GateSatisfaction(UUID id, UUID actionId, String gateKey, String satisfiedBy,
                            Instant satisfiedAt) {
        this.id = id;
        this.actionId = actionId;
        this.gateKey = gateKey;
        this.satisfiedBy = satisfiedBy;
        this.satisfiedAt = satisfiedAt;
    }

    public UUID getId() { return id; }
    public UUID getActionId() { return actionId; }
    public String getGateKey() { return gateKey; }
    public String getSatisfiedBy() { return satisfiedBy; }
    public Instant getSatisfiedAt() { return satisfiedAt; }
}
