package com.ax.template.authblueprint.costshare;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * accumulator-consume-l0 entity — a monotone-drawable running total against a limit, per scope
 * (e.g. member+benefit-period+tier). {@code used} moves UP via {@link #advanceUsed} (consume) and
 * DOWN via {@link #decrementUsed} (clawback) and resets via {@link #resetUsed} — it is a bidirectional
 * BALANCE, not a monotone counter, so there is no lifecycle state machine. {@code scopeKey}/
 * {@code createdAt} immutable; mutators are package-private (no public setter — the service in this
 * package is the sole mutator, always under a PESSIMISTIC_WRITE row lock). {@code @Version} backstops.
 */
@AggregateRoot
@Entity
@Table(name = "cost_share_accumulators")
// ACC-RACE-001 / ACC-CLAWBACK-001 — the solvency backstops applied by ddl-auto (so they are LIVE in
// tests, not only declared in V035 for Flyway fork-receivers): a consume can never over-draw past the
// limit, a release can never drive usage below zero.
@Check(constraints = "used >= 0 AND used <= limit_amount")
public class Accumulator {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "scope_key", nullable = false, updatable = false, length = 200, unique = true)
    private String scopeKey;

    /** Money/quantity values are exact BigDecimal — never double (lang-bigdecimal-for-money). */
    @Column(name = "limit_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal limit;

    @Column(name = "used", nullable = false, precision = 19, scale = 4)
    private BigDecimal used;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Accumulator() {}

    public Accumulator(UUID id, String scopeKey, BigDecimal limit, BigDecimal used, Instant createdAt) {
        this.id = id;
        this.scopeKey = scopeKey;
        this.limit = limit;
        this.used = used;
        this.createdAt = createdAt;
    }

    /** ACC-ATOMIC-001 — advance used by exactly `applied` (caller computed applied=min(delta,headroom)). */
    void advanceUsed(BigDecimal applied) {
        this.used = this.used.add(applied);
    }

    /** ACC-CLAWBACK-001 — decrement used (non-monotone); caller guards used>=0. */
    void decrementUsed(BigDecimal amount) {
        this.used = this.used.subtract(amount);
    }

    /** ACC-RESET-001 — period-boundary reset to zero. */
    void resetUsed() {
        this.used = BigDecimal.ZERO.setScale(this.used.scale());
    }

    public UUID getId() { return id; }
    public String getScopeKey() { return scopeKey; }
    public BigDecimal getLimit() { return limit; }
    public BigDecimal getUsed() { return used; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }

    /** Remaining headroom under the limit (never negative). */
    public BigDecimal headroom() {
        BigDecimal h = limit.subtract(used);
        return h.signum() < 0 ? BigDecimal.ZERO.setScale(used.scale()) : h;
    }
}
