package com.ax.template.authblueprint.ecommerce;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * Cart entity — one per user (uk_carts_user). Total is derived from items
 * and recalculated on each mutation (immutable-by-recompute pattern).
 */
@AggregateRoot
@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @Column(name = "id", length = 36, updatable = false)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36, updatable = false, unique = true)
    private String userId;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<CartItem> items = new ArrayList<>();

    protected Cart() {}

    private Cart(String id, String userId, String currency, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.currency = Objects.requireNonNull(currency);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.totalAmount = 0L;
    }

    public static Cart createFor(String userId, String currency) {
        Instant now = Instant.now();
        return new Cart(UUID.randomUUID().toString(), userId, currency, now, now);
    }

    /** Append a CartItem; if the product already exists, sum the quantities. */
    public void addItem(Product product, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        Optional<CartItem> existing = items.stream()
            .filter(ci -> ci.getProductId().equals(product.getId()))
            .findFirst();
        if (existing.isPresent()) {
            CartItem ci = existing.get();
            ci.setQuantity(ci.getQuantity() + quantity);
        } else {
            CartItem ci = CartItem.create(this, product.getId(), quantity, product.getPrice());
            items.add(ci);
        }
        recalculate();
    }

    public void removeItem(String cartItemId) {
        boolean removed = items.removeIf(ci -> ci.getId().equals(cartItemId));
        if (!removed) throw new EcommerceException.CartItemNotFound(cartItemId);
        recalculate();
    }

    public void clear() {
        items.clear();
        recalculate();
    }

    private void recalculate() {
        long sum = 0L;
        for (CartItem ci : items) {
            sum += ci.getLineTotal();
        }
        this.totalAmount = sum;
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public long getTotalAmount() { return totalAmount; }
    public String getCurrency() { return currency; }
    public List<CartItem> getItems() { return List.copyOf(items); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
