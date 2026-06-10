package com.ax.template.authblueprint.payment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * Payment domain entity. State machine root.
 *
 * <p>PAYMENT-MONEY-001: amount is {@link BigDecimal}; no float/double fields anywhere
 * in this class. {@link #amount} carries the originally requested amount; {@link #capturedAmount}
 * is populated on CAPTURED transition; {@link #balance} is the denormalized refund-derived
 * cache (capturedAmount - sum(refunds)).
 *
 * <p>PCI-DSS SAQ-A: no PAN field stored. Only {@link #paymentMethodToken} (opaque, tokenized
 * upstream by the PCI-certified provider iframe) is persisted.
 *
 * <p>PAYMENT-STATE-002: {@link #version} backs JPA optimistic locking; concurrent transitions
 * surface as {@link org.springframework.orm.ObjectOptimisticLockingFailureException} → 409.
 */
@AggregateRoot
@Entity
@Table(
    name = "payments",
    indexes = {
        @Index(name = "ix_payments_user_id", columnList = "user_id"),
        @Index(name = "ix_payments_order_id", columnList = "order_id"),
        @Index(name = "ix_payments_idempotency_key", columnList = "idempotency_key")
    }
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal amount;

    @Column(name = "captured_amount", precision = 19, scale = 8)
    private BigDecimal capturedAmount;

    /**
     * Refund-derived remaining balance — denormalized cache of
     * (capturedAmount - sum(refunds.amount)). Reconciliation invariant target.
     */
    @Column(precision = 19, scale = 8)
    private BigDecimal balance;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentState state = PaymentState.CREATED;

    /**
     * Opaque tokenized payment method reference. PAN never stored — only the provider's
     * token. PAYMENT-SEC-001: never logged or surfaced in error responses.
     *
     * <p>P5 security-review (US-014 MEDIUM, defense-in-depth): {@code @JsonIgnore}
     * prevents accidental serialization if the entity is ever returned directly from
     * a controller. The current {@code PaymentController#paymentBody} already excludes
     * the field; this annotation guards against future regressions.
     */
    @JsonIgnore
    @Column(name = "payment_method_token", length = 512)
    private String paymentMethodToken;

    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    @Column(name = "decline_reason", length = 128)
    private String declineReason;

    @Column(name = "captured_at")
    private Instant capturedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getCapturedAmount() { return capturedAmount; }
    public void setCapturedAmount(BigDecimal capturedAmount) { this.capturedAmount = capturedAmount; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public PaymentState getState() { return state; }
    /** Package-private — {@link PaymentStateMachine} is the sole mutator (BACKLOG P0-26). */
    void setState(PaymentState state) { this.state = state; }

    public String getPaymentMethodToken() { return paymentMethodToken; }
    public void setPaymentMethodToken(String paymentMethodToken) { this.paymentMethodToken = paymentMethodToken; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getDeclineReason() { return declineReason; }
    public void setDeclineReason(String declineReason) { this.declineReason = declineReason; }

    public Instant getCapturedAt() { return capturedAt; }
    public void setCapturedAt(Instant capturedAt) { this.capturedAt = capturedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
