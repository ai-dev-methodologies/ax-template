package com.ax.template.authblueprint.reservation;

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

/**
 * reserve-settle-balance-l0 hold against a {@link ReservableBalance}. {@code amount} (the held value),
 * {@code balanceId}, {@code expiresAt}, {@code createdAt} are immutable; only {@code status}/
 * {@code settledAmount} move, and ONLY via the package-private terminal mutators (no public setter —
 * {@link ReservationService} is the sole mutator). A hold has exactly ONE terminal transition. The
 * {@code @Check settled_amount ≤ amount} is the load-bearing overspend backstop, LIVE under ddl-auto.
 */
@Entity
@Table(name = "reservations")
// RSV-SETTLE-001 — the settled actual can never exceed the held amount; amount is always positive.
@Check(constraints = "amount > 0 AND (settled_amount IS NULL OR (settled_amount >= 0 AND settled_amount <= amount))")
public class Reservation {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "balance_id", nullable = false, updatable = false)
    private UUID balanceId;

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status;

    /** The actual committed on settle (≤ amount); null until SETTLED. */
    @Column(name = "settled_amount", precision = 19, scale = 4)
    private BigDecimal settledAmount;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected Reservation() {}

    public Reservation(UUID id, UUID balanceId, BigDecimal amount, ReservationStatus status,
                       BigDecimal settledAmount, Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.balanceId = balanceId;
        this.amount = amount;
        this.status = status;
        this.settledAmount = settledAmount;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    /** RSV-SETTLE-001 — terminal: commit actual, OUTSTANDING → SETTLED (caller guards actual ≤ amount). */
    void settle(BigDecimal actual) {
        this.status = ReservationStatus.SETTLED;
        this.settledAmount = actual;
    }

    /** RSV-RELEASE-001 — terminal: explicit cancel, OUTSTANDING → RELEASED (whole hold returned). */
    void release() {
        this.status = ReservationStatus.RELEASED;
    }

    /** RSV-SWEEP-001 — terminal: timeout reclaim, OUTSTANDING → EXPIRED (whole hold returned). */
    void expire() {
        this.status = ReservationStatus.EXPIRED;
    }

    public UUID getId() { return id; }
    public UUID getBalanceId() { return balanceId; }
    public BigDecimal getAmount() { return amount; }
    public ReservationStatus getStatus() { return status; }
    public BigDecimal getSettledAmount() { return settledAmount; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }
}
