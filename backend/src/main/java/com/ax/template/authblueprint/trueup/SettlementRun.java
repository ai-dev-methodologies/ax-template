package com.ax.template.authblueprint.trueup;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * remeasurement-trueup-l0 settlement run (TUP-RUNVERSION-001): an immutable, versioned
 * computation row recording its INPUT BASIS — the exact reading rows (at slot versions) it
 * consumed, plus the basis hash that makes unchanged-input recompute idempotent. A run has
 * no update path and no mutator: every column is updatable=false; correction means a NEW
 * version (uq backstop), never a rewrite. TrueUpPosting rows are members of this root.
 */
@AggregateRoot
@Entity
@Table(name = "settlement_runs", uniqueConstraints = {
    @UniqueConstraint(name = "uq_run_version", columnNames = {"period_id", "run_version"})
})
public class SettlementRun {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "period_id", nullable = false, updatable = false)
    private UUID periodId;

    @Column(name = "run_version", nullable = false, updatable = false)
    private int runVersion;

    /** The reproducibility trail — which reading rows, at which slot versions (TUP-RUNVERSION-001). */
    @Column(name = "basis_json", nullable = false, updatable = false, length = 4000)
    private String basisJson;

    @Column(name = "basis_hash", nullable = false, updatable = false, length = 64)
    private String basisHash;

    @Column(name = "total_value", nullable = false, updatable = false, precision = 15, scale = 4)
    private BigDecimal totalValue;

    @Column(name = "computed_at", nullable = false, updatable = false)
    private Instant computedAt;

    protected SettlementRun() {}

    public SettlementRun(UUID id, UUID periodId, int runVersion, String basisJson, String basisHash,
                         BigDecimal totalValue, Instant computedAt) {
        this.id = id;
        this.periodId = periodId;
        this.runVersion = runVersion;
        this.basisJson = basisJson;
        this.basisHash = basisHash;
        this.totalValue = totalValue;
        this.computedAt = computedAt;
    }

    public UUID getId() { return id; }
    public UUID getPeriodId() { return periodId; }
    public int getRunVersion() { return runVersion; }
    public String getBasisJson() { return basisJson; }
    public String getBasisHash() { return basisHash; }
    public BigDecimal getTotalValue() { return totalValue; }
    public Instant getComputedAt() { return computedAt; }
}
