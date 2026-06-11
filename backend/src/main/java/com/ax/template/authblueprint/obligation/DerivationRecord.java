package com.ax.template.authblueprint.obligation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One immutable derivation of an axis candidate (OBL-GROUND-001) — appended on creation and on
 * every usage advance, so an auditor can re-derive every deadline the obligation ever carried
 * from the recorded formulas alone. Fully append-only.
 */
@AggregateMember(root = Obligation.class)
@Entity
@Table(name = "obligation_derivations")
public class DerivationRecord {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "obligation_id", nullable = false, updatable = false)
    private UUID obligationId;

    @Column(name = "axis_id", nullable = false, updatable = false)
    private UUID axisId;

    @Column(name = "candidate_deadline", nullable = false, updatable = false)
    private Instant candidateDeadline;

    @Column(name = "formula", nullable = false, updatable = false, length = 500)
    private String formula;

    @Column(name = "derived_at", nullable = false, updatable = false)
    private Instant derivedAt;

    protected DerivationRecord() {}

    public DerivationRecord(UUID id, UUID obligationId, UUID axisId, Instant candidateDeadline,
                            String formula, Instant derivedAt) {
        this.id = id;
        this.obligationId = obligationId;
        this.axisId = axisId;
        this.candidateDeadline = candidateDeadline;
        this.formula = formula;
        this.derivedAt = derivedAt;
    }

    public UUID getId() { return id; }
    public UUID getObligationId() { return obligationId; }
    public UUID getAxisId() { return axisId; }
    public Instant getCandidateDeadline() { return candidateDeadline; }
    public String getFormula() { return formula; }
    public Instant getDerivedAt() { return derivedAt; }
}
