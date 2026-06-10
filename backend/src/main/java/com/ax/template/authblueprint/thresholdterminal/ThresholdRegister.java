package com.ax.template.authblueprint.thresholdterminal;

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
 * threshold-terminal-derivation-l0 register: a cumulative {@code anchor} with a mandatory immutable
 * {@code limit} (life-limit / usage ceiling). The accrual that makes the anchor reach/cross the limit
 * drives {@code status} to the irreversible terminal {@link ThresholdStatus#EXPIRED} in the SAME
 * transaction (TTD-CROSS-001); the implication is DB-backstopped via @Check (TTD-CHECK-001) so a
 * committed live over-limit row is unrepresentable even under ddl-auto. Columns are
 * {@code anchor_value}/{@code limit_value} — bare {@code limit} is a SQL reserved word (the catalog's
 * {@code value} lesson). The lifecycle moves ONLY via the package-private {@link #markExpired()}
 * called by {@link ThresholdRegisterStateMachine} (sole mutator); the anchor moves ONLY via the
 * package-private {@link #advanceAnchor} called by {@link ThresholdRegisterService} under a
 * PESSIMISTIC_WRITE row lock. {@code @Version} backstops.
 */
@AggregateRoot
@Entity
@Table(name = "threshold_registers")
// TTD-CHECK-001 — anchor ≥ limit IMPLIES terminal; limit positive; anchor non-negative. LIVE under ddl-auto.
@Check(constraints = "limit_value > 0 AND anchor_value >= 0 AND (anchor_value < limit_value OR status = 'EXPIRED')")
public class ThresholdRegister {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "scope_key", nullable = false, updatable = false, length = 200, unique = true)
    private String scopeKey;

    /** Mandatory replacement limit (14 CFR §43.10) — fixed at registration, never raised or lowered. */
    @Column(name = "limit_value", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal limit;

    @Column(name = "anchor_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal anchor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ThresholdStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ThresholdRegister() {}

    public ThresholdRegister(UUID id, String scopeKey, BigDecimal limit, BigDecimal anchor, Instant createdAt) {
        this.id = id;
        this.scopeKey = scopeKey;
        this.limit = limit;
        this.anchor = anchor;
        this.status = ThresholdStatus.ACTIVE;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook (service, under the row lock) — advance the anchor by an accepted accrual. */
    void advanceAnchor(BigDecimal next) {
        this.anchor = next;
    }

    /** Sole-mutator hook ({@link ThresholdRegisterStateMachine} only) — the one-way crossing edge. */
    void markExpired() {
        this.status = ThresholdStatus.EXPIRED;
    }

    public UUID getId() { return id; }
    public String getScopeKey() { return scopeKey; }
    public BigDecimal getLimit() { return limit; }
    public BigDecimal getAnchor() { return anchor; }
    public ThresholdStatus getStatus() { return status; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
