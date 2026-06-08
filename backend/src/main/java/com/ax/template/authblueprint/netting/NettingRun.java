package com.ax.template.authblueprint.netting;

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
 * collection-conservation-l0 netting run — one multilateral netting over a single-currency set of
 * gross obligations. {@code netTotal} is the rollup Σ of all members' computed nets and MUST be 0
 * (set-wide closure, NET-SETWIDE-ZERO-001) — DB-backstopped via {@code @Check(net_total = 0)}.
 * {@code runKey}/{@code currency}/{@code createdAt} immutable; {@code status}/{@code netTotal} move
 * ONLY via the package-private {@link #markNetted} (no public setter — {@link NettingService} is the
 * sole mutator, under a PESSIMISTIC_WRITE row lock). {@code @Version} backstops.
 */
@AggregateRoot
@Entity
@Table(name = "netting_runs")
// NET-SETWIDE-ZERO-001 — the rollup of all member nets is ALWAYS exactly zero (LIVE under ddl-auto).
@Check(constraints = "net_total = 0")
public class NettingRun {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "run_key", nullable = false, updatable = false, length = 200, unique = true)
    private String runKey;

    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NettingStatus status;

    @Column(name = "net_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal netTotal;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected NettingRun() {}

    public NettingRun(UUID id, String runKey, String currency, NettingStatus status,
                      BigDecimal netTotal, Instant createdAt) {
        this.id = id;
        this.runKey = runKey;
        this.currency = currency;
        this.status = status;
        this.netTotal = netTotal;
        this.createdAt = createdAt;
    }

    /** NET-ONCE-001 — terminal reduction: OPEN → NETTED, recording the set-wide rollup (must be 0). */
    void markNetted(BigDecimal total) {
        this.status = NettingStatus.NETTED;
        this.netTotal = total;
    }

    public UUID getId() { return id; }
    public String getRunKey() { return runKey; }
    public String getCurrency() { return currency; }
    public NettingStatus getStatus() { return status; }
    public BigDecimal getNetTotal() { return netTotal; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
