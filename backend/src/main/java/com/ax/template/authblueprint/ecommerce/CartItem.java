package com.ax.template.authblueprint.ecommerce;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @Column(name = "id", length = 36, updatable = false)
    private String id;

    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price_at_added_time", nullable = false)
    private long unitPriceAtAddedTime;

    @Column(name = "line_total", nullable = false)
    private long lineTotal;

    protected CartItem() {}

    private CartItem(String id, Cart cart, String productId, int quantity, long unitPrice) {
        this.id = Objects.requireNonNull(id);
        this.cart = Objects.requireNonNull(cart);
        this.productId = Objects.requireNonNull(productId);
        this.quantity = quantity;
        this.unitPriceAtAddedTime = unitPrice;
        this.lineTotal = quantity * unitPrice;
    }

    static CartItem create(Cart cart, String productId, int quantity, long unitPrice) {
        return new CartItem(UUID.randomUUID().toString(), cart, productId, quantity, unitPrice);
    }

    void setQuantity(int qty) {
        if (qty <= 0) throw new IllegalArgumentException("quantity must be positive");
        this.quantity = qty;
        this.lineTotal = (long) qty * this.unitPriceAtAddedTime;
    }

    public String getId() { return id; }
    public Cart getCart() { return cart; }
    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public long getUnitPriceAtAddedTime() { return unitPriceAtAddedTime; }
    public long getLineTotal() { return lineTotal; }
}
