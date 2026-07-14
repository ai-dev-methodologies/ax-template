package com.ax.template.authblueprint.additivefacts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * additive-fact-ledger-l0 period (FACT-CLOSED-PERIOD-ADD-003): while OPEN, its total is
 * derived-on-read as Σ facts assigned to it. Closing FREEZES that sum into
 * {@link #frozenAggregate} via the package-private {@link #close} sole-mutator hook — the
 * column is immutable once set (@Column(updatable=false)), so a CLOSED period's aggregate has
 * no rewrite path even in principle. Late facts still assigned to a CLOSED period do not
 * change {@code frozenAggregate}; their effect posts forward as a {@link LateDeltaPosting}
 * into a caller-designated OPEN period (FACT-LATE-DELTA-POST-002).
 */
@AggregateRoot
@Entity
@Table(name = "fact_periods")
@Check(constraints = "(status = 'OPEN' OR frozen_aggregate IS NOT NULL)")
public class FactPeriod {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "subject", nullable = false, updatable = false, length = 200)
    private String subject;

    @Column(name = "label", nullable = false, updatable = false, length = 100)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FactPeriodStatus status;

    /**
     * Frozen at close time. NOT {@code updatable=false} — this column IS legitimately
     * written exactly once, by the package-private {@link #close} sole-mutator hook; a JPA
     * {@code updatable=false} would block THAT write too (Hibernate excludes the column from
     * every UPDATE, including the first). Immutability-after-close is enforced instead by
     * (a) {@link #close} being the only caller, ever, since a second close attempt is refused
     * before reaching it (status != OPEN), and (b) the @Check backstop below making a CLOSED
     * row without a frozen value unrepresentable (FACT-CLOSED-PERIOD-ADD-003).
     */
    @Column(name = "frozen_aggregate", precision = 15, scale = 4)
    private BigDecimal frozenAggregate;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FactPeriod() {}

    public FactPeriod(UUID id, String subject, String label, Instant createdAt) {
        this.id = id;
        this.subject = subject;
        this.label = label;
        this.status = FactPeriodStatus.OPEN;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook — freezes the aggregate; the column is then immutable forever. */
    void close(BigDecimal frozenAggregate, Instant closedAt) {
        this.status = FactPeriodStatus.CLOSED;
        this.frozenAggregate = frozenAggregate;
        this.closedAt = closedAt;
    }

    public UUID getId() { return id; }
    public String getSubject() { return subject; }
    public String getLabel() { return label; }
    public FactPeriodStatus getStatus() { return status; }
    public BigDecimal getFrozenAggregate() { return frozenAggregate; }
    public Instant getClosedAt() { return closedAt; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
