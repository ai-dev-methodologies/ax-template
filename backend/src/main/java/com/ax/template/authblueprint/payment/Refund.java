package com.ax.template.authblueprint.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * Refund row. Each refund references a {@link Payment} via {@link #paymentId}.
 * Sum invariant — sum(refunds.amount) ≤ payment.capturedAmount — is enforced atomically
 * inside RefundService with optimistic locking on the parent Payment.
 *
 * <p>PAYMENT-MONEY-001: amount is {@link BigDecimal}; no float/double fields.
 */
@AggregateRoot
@Entity
@Table(
    name = "refunds",
    indexes = {
        @Index(name = "ix_refunds_payment_id", columnList = "payment_id"),
        @Index(name = "ix_refunds_idempotency_key", columnList = "idempotency_key")
    },
    // PAYMENT-IDEMP-004 (P1-70) — DB backstop for exactly-once refund semantics: at most one
    // refund per (payment, Idempotency-Key). RefundService's lookup short-circuits the normal
    // retry into a replay; this constraint closes the residual concurrent-miss window.
    // NULL keys stay multiply-allowed (SQL NULLs are distinct in a unique constraint).
    // Mirrored by db/migration/V116__refund_idempotency_unique.sql.
    uniqueConstraints = {
        @UniqueConstraint(
            name = "ux_refunds_payment_id_idempotency_key",
            columnNames = {"payment_id", "idempotency_key"})
    }
)
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundState state = RefundState.PROCESSING;

    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Version
    @Column(nullable = false)
    private Long version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public RefundState getState() { return state; }
    public void setState(RefundState state) { this.state = state; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
