package com.ax.template.authblueprint.reservation;

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
 * reserve-settle-balance-l0 pooled balance with a THIRD term. A claim happens in two phases, so the
 * balance carries {@code committed} (settled/spent) and {@code reserved} (sum of OUTSTANDING holds)
 * separately: {@code available = funded − committed − reserved}. {@code funded}/{@code scopeKey}/
 * {@code createdAt} are immutable; {@code committed}/{@code reserved} mutate ONLY via the package-private
 * mutators (no public setter — {@link ReservationService} is the sole mutator, always under a
 * PESSIMISTIC_WRITE row lock). {@code @Version} backstops; columns use {@code _amount} suffixes because
 * {@code committed}/{@code reserved} risk SQL reserved-word DDL collisions.
 */
@AggregateRoot
@Entity
@Table(name = "reservable_balances")
// RSV-RESERVE-001 / RSV-CONSERVE-001 — solvency backstops applied by ddl-auto (LIVE in tests, not only
// declared in V038 for Flyway fork-receivers): never over-reserve/over-spend, never negative.
@Check(constraints = "committed_amount >= 0 AND reserved_amount >= 0 AND committed_amount + reserved_amount <= funded_amount")
public class ReservableBalance {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "scope_key", nullable = false, updatable = false, length = 200, unique = true)
    private String scopeKey;

    /** Total funded credit — exact BigDecimal, never double (lang-bigdecimal-for-money). Immutable. */
    @Column(name = "funded_amount", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal funded;

    @Column(name = "committed_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal committed;

    @Column(name = "reserved_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal reserved;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ReservableBalance() {}

    public ReservableBalance(UUID id, String scopeKey, BigDecimal funded, BigDecimal committed,
                             BigDecimal reserved, Instant createdAt) {
        this.id = id;
        this.scopeKey = scopeKey;
        this.funded = funded;
        this.committed = committed;
        this.reserved = reserved;
        this.createdAt = createdAt;
    }

    /** RSV-SETTLE-001 — commit actual usage (caller guards actual ≤ the hold's reserved amount). */
    void advanceCommitted(BigDecimal actual) {
        this.committed = this.committed.add(actual);
    }

    /** RSV-RESERVE-001 — place a hold (caller guards amount ≤ available under the row lock). */
    void increaseReserved(BigDecimal amount) {
        this.reserved = this.reserved.add(amount);
    }

    /** RSV-SETTLE-001 / RSV-RELEASE-001 / RSV-SWEEP-001 — return a hold's whole amount to available. */
    void decreaseReserved(BigDecimal amount) {
        this.reserved = this.reserved.subtract(amount);
    }

    public UUID getId() { return id; }
    public String getScopeKey() { return scopeKey; }
    public BigDecimal getFunded() { return funded; }
    public BigDecimal getCommitted() { return committed; }
    public BigDecimal getReserved() { return reserved; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }

    /** Spendable headroom — funded minus committed minus held (RSV-RESERVE-001 reserve ceiling). */
    public BigDecimal available() {
        return funded.subtract(committed).subtract(reserved);
    }
}
