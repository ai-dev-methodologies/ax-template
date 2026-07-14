package com.ax.template.authblueprint.saturatingbalance;

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
 * saturating-balance-l0 root: a clamp-on-write running balance. Accrual clamps AT
 * {@link #cap} (SATBAL-CEILING-001); debit clamps at zero (SATBAL-FLOOR-002) — neither ever
 * errors. The {@code @Check} backstop makes a stored value outside {@code [0, cap]}
 * unrepresentable even if the mutator logic regresses. {@link #applyAccrual}/{@link #applyDebit}
 * are the SOLE mutators of {@link #current}, always called under this row's
 * {@code PESSIMISTIC_WRITE} lock (SATBAL-CONCURRENT-004) — the caller (service) computes the
 * clamped applied amount and this entity just adds/subtracts it.
 */
@AggregateRoot
@Entity
@Table(name = "saturating_balances")
@Check(constraints = "current_value >= 0 AND current_value <= cap")
public class Balance {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false, length = 200)
    private String ownerId;

    @Column(name = "cap", nullable = false, updatable = false, precision = 15, scale = 4)
    private BigDecimal cap;

    @Column(name = "current_value", nullable = false, precision = 15, scale = 4)
    private BigDecimal current;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Balance() {}

    public Balance(UUID id, String ownerId, BigDecimal cap, Instant createdAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.cap = cap;
        this.current = BigDecimal.ZERO;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook — {@code applied} is the ALREADY-CLAMPED amount (SATBAL-CEILING-001). */
    void applyAccrual(BigDecimal applied) {
        this.current = this.current.add(applied);
    }

    /** Sole-mutator hook — {@code applied} is the ALREADY-CLAMPED amount (SATBAL-FLOOR-002). */
    void applyDebit(BigDecimal applied) {
        this.current = this.current.subtract(applied);
    }

    /** Remaining room to the cap — the ceiling clamp bound (SATBAL-CEILING-001). */
    BigDecimal headroom() {
        return cap.subtract(current);
    }

    public UUID getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public BigDecimal getCap() { return cap; }
    public BigDecimal getCurrent() { return current; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
