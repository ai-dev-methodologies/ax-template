package com.ax.template.authblueprint.statemutation;

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
 * One immutable governed state transition of a {@link GovernedForm} (STATEMUTATION-MONOTONE-001):
 * the from-state, the to-state, the kind (FORWARD for a tightening advance, REOPEN for a recorded
 * widening), the reason a re-open required, the actor, and when. Appended at transition time — fully
 * append-only. The uq(form_id, seq) gives every form a gap-free ordered transition trail an auditor
 * can replay to reconstruct what was editable when. A REOPEN row is what makes a widening auditable
 * rather than a silent unlock.
 */
@AggregateMember(root = GovernedForm.class)
@Entity
@Table(name = "form_transitions", uniqueConstraints = {
    @UniqueConstraint(name = "uq_form_transition_seq", columnNames = {"form_id", "seq"})
})
public class FormTransition {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "form_id", nullable = false, updatable = false)
    private UUID formId;

    @Column(name = "seq", nullable = false, updatable = false)
    private long seq;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_state", nullable = false, updatable = false, length = 20)
    private FormState fromState;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_state", nullable = false, updatable = false, length = 20)
    private FormState toState;

    /** FORWARD for a tightening advance, REOPEN for a recorded widening (re-open). */
    @Column(name = "kind", nullable = false, updatable = false, length = 20)
    private String kind;

    /** The reason a re-open required — mandatory for a REOPEN (the audit of WHY a freeze was lifted). */
    @Column(name = "reason", updatable = false, length = 1000)
    private String reason;

    @Column(name = "actor", nullable = false, updatable = false, length = 200)
    private String actor;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected FormTransition() {}

    public FormTransition(UUID id, UUID formId, long seq, FormState fromState, FormState toState,
                          String kind, String reason, String actor, Instant occurredAt) {
        this.id = id;
        this.formId = formId;
        this.seq = seq;
        this.fromState = fromState;
        this.toState = toState;
        this.kind = kind;
        this.reason = reason;
        this.actor = actor;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public UUID getFormId() { return formId; }
    public long getSeq() { return seq; }
    public FormState getFromState() { return fromState; }
    public FormState getToState() { return toState; }
    public String getKind() { return kind; }
    public String getReason() { return reason; }
    public String getActor() { return actor; }
    public Instant getOccurredAt() { return occurredAt; }
}
