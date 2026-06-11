package com.ax.template.authblueprint.obligation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One fired escalation rung (OBL-LADDER-001) — appended, additive, immutable. The
 * UNIQUE(obligation_id, rung) constraint is the exactly-once DB backstop: a racing second
 * sweep pass cannot double-fire a rung even if the in-code check misses.
 */
@AggregateMember(root = Obligation.class)
@Entity
@Table(name = "obligation_escalations", uniqueConstraints = {
    @UniqueConstraint(name = "uq_obligation_rung", columnNames = {"obligation_id", "rung"})
})
public class EscalationEvent {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "obligation_id", nullable = false, updatable = false)
    private UUID obligationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rung", nullable = false, updatable = false, length = 20)
    private EscalationRung rung;

    @Column(name = "fired_at", nullable = false, updatable = false)
    private Instant firedAt;

    /** The effective deadline at firing time — the ladder's evidence trail. */
    @Column(name = "deadline_at_firing", nullable = false, updatable = false)
    private Instant deadlineAtFiring;

    protected EscalationEvent() {}

    public EscalationEvent(UUID id, UUID obligationId, EscalationRung rung, Instant firedAt,
                           Instant deadlineAtFiring) {
        this.id = id;
        this.obligationId = obligationId;
        this.rung = rung;
        this.firedAt = firedAt;
        this.deadlineAtFiring = deadlineAtFiring;
    }

    public UUID getId() { return id; }
    public UUID getObligationId() { return obligationId; }
    public EscalationRung getRung() { return rung; }
    public Instant getFiredAt() { return firedAt; }
    public Instant getDeadlineAtFiring() { return deadlineAtFiring; }
}
