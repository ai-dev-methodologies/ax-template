package com.ax.template.authblueprint.dunning;

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
 * One immutable per-stage dunning transition (DUNNING-LADDER-001): the stage reached, when, the
 * days-overdue at that point, and the actor. Appended at advance time — fully append-only; one
 * transition per (case, stage), the uq(case_id, stage) DB backstop that makes a re-emit of an
 * already-reached stage a deterministic 409 even under the concurrent-advance race
 * (DUNNING-CONCURRENT-001). A halt-on-cure is recorded as a CURED kind on this same row.
 */
@AggregateMember(root = DunningCase.class)
@Entity
@Table(name = "dunning_stage_transitions", uniqueConstraints = {
    @UniqueConstraint(name = "uq_dunning_case_stage", columnNames = {"case_id", "stage", "kind"})
})
public class DunningStageTransition {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "case_id", nullable = false, updatable = false)
    private UUID caseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, updatable = false, length = 20)
    private DunningStage stage;

    /** ADVANCE for a ladder rung, CURED for a recorded halt-on-cure (DUNNING-CURE-001). */
    @Column(name = "kind", nullable = false, updatable = false, length = 20)
    private String kind;

    @Column(name = "days_overdue", nullable = false, updatable = false)
    private long daysOverdue;

    @Column(name = "actor", nullable = false, updatable = false, length = 200)
    private String actor;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected DunningStageTransition() {}

    public DunningStageTransition(UUID id, UUID caseId, DunningStage stage, String kind,
                                  long daysOverdue, String actor, Instant occurredAt) {
        this.id = id;
        this.caseId = caseId;
        this.stage = stage;
        this.kind = kind;
        this.daysOverdue = daysOverdue;
        this.actor = actor;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public UUID getCaseId() { return caseId; }
    public DunningStage getStage() { return stage; }
    public String getKind() { return kind; }
    public long getDaysOverdue() { return daysOverdue; }
    public String getActor() { return actor; }
    public Instant getOccurredAt() { return occurredAt; }
}
