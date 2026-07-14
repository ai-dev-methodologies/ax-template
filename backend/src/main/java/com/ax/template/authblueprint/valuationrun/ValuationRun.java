package com.ax.template.authblueprint.valuationrun;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * valuation-run-projection-l0 — one IMMUTABLE versioned valuation run (VALRUN-ASOF/IMMUTABLE-001).
 * Pinned to an as-of instant + a recorded basis; carries the computed total AND a persisted
 * {@code output_sum} whose DB @Check (output_sum = total_value) is the fan-out conservation
 * backstop (VALRUN-FANOUT-001). Once computed it is NEVER updated — every column is
 * {@code updatable=false}, there is no public setter, and a correction is a NEW run (a recompute
 * appends version+1) or a rebase (a new baseline with {@code rebasedFromRunVersion} set). The
 * uq(subject_id, run_version) backstop makes a concurrent-recompute loser a deterministic 409
 * (VALRUN-CONCURRENT-001). Linked to its subject by identity ({@code subjectId}) — cross-aggregate
 * reference-by-id, not an object pointer.
 */
@AggregateRoot
@Entity
@Table(name = "valuation_runs", uniqueConstraints = {
    @UniqueConstraint(name = "uq_valuation_subject_version", columnNames = {"subject_id", "run_version"})
})
@Check(constraints = "output_sum = total_value AND run_version >= 1")
public class ValuationRun {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "subject_id", nullable = false, updatable = false)
    private UUID subjectId;

    @Column(name = "run_version", nullable = false, updatable = false)
    private int runVersion;

    /** The instant this run values the subject AS OF — point-in-time read key (VALRUN-ASOF-001). */
    @Column(name = "as_of", nullable = false, updatable = false)
    private Instant asOf;

    /** The input basis (e.g. a corporate-action split ratio) recorded verbatim — reconstructible. */
    @Column(name = "basis", nullable = false, updatable = false, length = 1000)
    private String basis;

    @Column(name = "total_value", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal totalValue;

    /** The persisted Σ of the fan-out outputs; the DB @Check ties it to total_value. */
    @Column(name = "output_sum", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal outputSum;

    /** The version this run rebased FROM — null on a plain recompute, set on a rebase baseline. */
    @Column(name = "rebased_from_run_version", updatable = false)
    private Integer rebasedFromRunVersion;

    /** VALRUN-FALLBACK-001 — WHICH source computed this run (provenance for a fallback read). */
    @Column(name = "source_ref", nullable = false, updatable = false, length = 200)
    private String sourceRef;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ValuationRun() {}

    public ValuationRun(UUID id, UUID subjectId, int runVersion, Instant asOf, String basis,
                        BigDecimal totalValue, BigDecimal outputSum, Integer rebasedFromRunVersion,
                        String sourceRef, Instant createdAt) {
        this.id = id;
        this.subjectId = subjectId;
        this.runVersion = runVersion;
        this.asOf = asOf;
        this.basis = basis;
        this.totalValue = totalValue;
        this.outputSum = outputSum;
        this.rebasedFromRunVersion = rebasedFromRunVersion;
        this.sourceRef = sourceRef;
        this.createdAt = createdAt;
    }

    public boolean isRebaseBaseline() {
        return rebasedFromRunVersion != null;
    }

    public UUID getId() { return id; }
    public UUID getSubjectId() { return subjectId; }
    public int getRunVersion() { return runVersion; }
    public Instant getAsOf() { return asOf; }
    public String getBasis() { return basis; }
    public BigDecimal getTotalValue() { return totalValue; }
    public BigDecimal getOutputSum() { return outputSum; }
    public Integer getRebasedFromRunVersion() { return rebasedFromRunVersion; }
    public String getSourceRef() { return sourceRef; }
    public Instant getCreatedAt() { return createdAt; }
}
