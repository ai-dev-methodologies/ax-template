package com.ax.template.authblueprint.ecommerce;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Order entity — snapshot of items at purchase time (ECOM-INV-001).
 * Lifecycle mutated only via {@link OrderStateMachine}.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @Column(name = "id", length = 36, updatable = false)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36, updatable = false)
    private String userId;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private OrderStatus status;

    @Column(name = "payment_id", length = 36)
    private String paymentId;

    @Column(name = "idempotency_key", length = 120, unique = true)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {}

    private Order(String id, String userId, String currency, String idempotencyKey,
                  Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.currency = Objects.requireNonNull(currency);
        this.idempotencyKey = idempotencyKey;
        this.status = OrderStatus.PENDING;
        this.totalAmount = 0L;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Order createPending(String userId, String currency, String idempotencyKey) {
        Instant now = Instant.now();
        return new Order(UUID.randomUUID().toString(), userId, currency, idempotencyKey, now, now);
    }

    public OrderItem addItem(Product product, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        OrderItem oi = OrderItem.snapshot(this, product, quantity);
        items.add(oi);
        recalculate();
        return oi;
    }

    private void recalculate() {
        long sum = 0L;
        for (OrderItem oi : items) {
            sum += oi.getLineTotal();
        }
        this.totalAmount = sum;
        this.updatedAt = Instant.now();
    }

    /**
     * ECOM-INV-001 — verify invariant: total_amount == sum(items.unit_price × quantity).
     * Throws {@link IllegalStateException} if violated.
     */
    public void assertTotalInvariant() {
        long sum = 0L;
        for (OrderItem oi : items) {
            sum += (long) oi.getQuantity() * oi.getUnitPrice();
        }
        if (sum != this.totalAmount) {
            throw new IllegalStateException("ECOM-INV-001 violated: total=" + totalAmount
                + " != sum=" + sum);
        }
    }

    /** Package-private; only {@link OrderStateMachine} may call this. */
    void setStatus(OrderStatus next) {
        this.status = Objects.requireNonNull(next);
        this.updatedAt = Instant.now();
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public long getTotalAmount() { return totalAmount; }
    public String getCurrency() { return currency; }
    public OrderStatus getStatus() { return status; }
    public String getPaymentId() { return paymentId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public List<OrderItem> getItems() { return List.copyOf(items); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
