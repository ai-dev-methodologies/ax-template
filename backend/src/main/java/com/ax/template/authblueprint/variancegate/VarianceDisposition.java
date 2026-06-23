package com.ax.template.authblueprint.variancegate;

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
 * One immutable accountable disposition of a variance breach (VG-DISPOSE-001): the decision
 * (OVERRIDE / REJECT), the actor, a non-blank reason, and when. Appended at dispose time — fully
 * append-only; EXACTLY ONE per appraisal, the uq(appraisal_id) DB backstop that makes a re-dispose
 * of an already-disposed appraisal a deterministic 409 even under the concurrent-dispose race
 * (VG-CONCURRENT-001, CWE-362). A disposition NEVER rewrites the appraisal verdict — it records a
 * decision to proceed despite the breach, not an erasure of it.
 */
@AggregateMember(root = VarianceAppraisal.class)
@Entity
@Table(name = "variance_dispositions", uniqueConstraints = {
    @UniqueConstraint(name = "uq_variance_appraisal", columnNames = {"appraisal_id"})
})
public class VarianceDisposition {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "appraisal_id", nullable = false, updatable = false)
    private UUID appraisalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, updatable = false, length = 20)
    private DispositionDecision decision;

    @Column(name = "actor", nullable = false, updatable = false, length = 200)
    private String actor;

    @Column(name = "reason", nullable = false, updatable = false, length = 1000)
    private String reason;

    @Column(name = "decided_at", nullable = false, updatable = false)
    private Instant decidedAt;

    protected VarianceDisposition() {}

    public VarianceDisposition(UUID id, UUID appraisalId, DispositionDecision decision, String actor,
                               String reason, Instant decidedAt) {
        this.id = id;
        this.appraisalId = appraisalId;
        this.decision = decision;
        this.actor = actor;
        this.reason = reason;
        this.decidedAt = decidedAt;
    }

    public UUID getId() { return id; }
    public UUID getAppraisalId() { return appraisalId; }
    public DispositionDecision getDecision() { return decision; }
    public String getActor() { return actor; }
    public String getReason() { return reason; }
    public Instant getDecidedAt() { return decidedAt; }
}
