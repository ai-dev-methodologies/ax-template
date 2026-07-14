package com.ax.template.authblueprint.additivefacts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * additive-fact-ledger-l0 fact row (FACT-ADDITIVE-ACCUM-001) — fully immutable and append-only.
 * Facts ACCUMULATE (unlike remeasurement-trueup's REPLACE-by-supersession readings): a period's
 * total is Σ of the facts assigned to it, never a single latest value. Idempotent on
 * {@code (source, external_fact_id)} (FACT-IDEMPOTENT-004) — the unique constraint is the DB
 * backstop a code regression cannot slip past.
 */
@AggregateMember(root = FactPeriod.class)
@Entity
@Table(name = "facts", uniqueConstraints = {
    @UniqueConstraint(name = "uq_fact_source_external_id", columnNames = {"source", "external_fact_id"})
})
public class Fact {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "period_id", nullable = false, updatable = false)
    private UUID periodId;

    @Column(name = "source", nullable = false, updatable = false, length = 100)
    private String source;

    @Column(name = "external_fact_id", nullable = false, updatable = false, length = 200)
    private String externalFactId;

    @Column(name = "amount", nullable = false, updatable = false, precision = 15, scale = 4)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Fact() {}

    public Fact(UUID id, UUID periodId, String source, String externalFactId, BigDecimal amount,
               Instant createdAt) {
        this.id = id;
        this.periodId = periodId;
        this.source = source;
        this.externalFactId = externalFactId;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getPeriodId() { return periodId; }
    public String getSource() { return source; }
    public String getExternalFactId() { return externalFactId; }
    public BigDecimal getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
