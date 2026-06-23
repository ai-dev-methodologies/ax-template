package com.ax.template.authblueprint.valuationrun;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateMember;

/**
 * valuation-run-projection-l0 — one IMMUTABLE per-position fan-out row of a {@link ValuationRun}
 * (VALRUN-FANOUT-001). One row per position per run version; its value contributes to the run's
 * total, and the sum of all of a run's outputs MUST equal the run total — checked by the run's
 * {@code output_sum} DB @Check AND an independent repo SUM(value) cross-check before commit, so a
 * partial fan-out is unrepresentable. Fully append-only — no public setter, every column
 * {@code updatable=false}; uq(run_id, position_ref) makes a duplicated position deterministic.
 * {@code @AggregateMember} of {@link ValuationRun}: {@code common/MemberWriter} writes, root-JPQL reads.
 */
@AggregateMember(root = ValuationRun.class)
@Entity
@Table(name = "valuation_outputs", uniqueConstraints = {
    @UniqueConstraint(name = "uq_valuation_output_position", columnNames = {"run_id", "position_ref"})
})
public class ValuationOutput {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "run_id", nullable = false, updatable = false)
    private UUID runId;

    @Column(name = "position_ref", nullable = false, updatable = false, length = 200)
    private String positionRef;

    @Column(name = "position_value", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal positionValue;

    protected ValuationOutput() {}

    public ValuationOutput(UUID id, UUID runId, String positionRef, BigDecimal positionValue) {
        this.id = id;
        this.runId = runId;
        this.positionRef = positionRef;
        this.positionValue = positionValue;
    }

    public UUID getId() { return id; }
    public UUID getRunId() { return runId; }
    public String getPositionRef() { return positionRef; }
    public BigDecimal getPositionValue() { return positionValue; }
}
