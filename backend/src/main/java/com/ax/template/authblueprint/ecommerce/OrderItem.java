package com.ax.template.authblueprint.ecommerce;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * Order line item — snapshot of product name + unit_price at purchase time
 * so subsequent product mutations don't rewrite history (ECOM-INV-001 anchor).
 */
@AggregateMember(root = Order.class)
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @Column(name = "id", length = 36, updatable = false)
    private String id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @Column(name = "product_name_at_purchase", nullable = false, length = 200)
    private String productNameAtPurchase;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private long unitPrice;

    @Column(name = "line_total", nullable = false)
    private long lineTotal;

    protected OrderItem() {}

    private OrderItem(String id, Order order, String productId, String productName,
                      int quantity, long unitPrice) {
        this.id = Objects.requireNonNull(id);
        this.order = Objects.requireNonNull(order);
        this.productId = Objects.requireNonNull(productId);
        this.productNameAtPurchase = Objects.requireNonNull(productName);
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = (long) quantity * unitPrice;
    }

    static OrderItem snapshot(Order order, Product product, int quantity) {
        return new OrderItem(UUID.randomUUID().toString(), order, product.getId(),
            product.getName(), quantity, product.getPrice());
    }

    public String getId() { return id; }
    public Order getOrder() { return order; }
    public String getProductId() { return productId; }
    public String getProductNameAtPurchase() { return productNameAtPurchase; }
    public int getQuantity() { return quantity; }
    public long getUnitPrice() { return unitPrice; }
    public long getLineTotal() { return lineTotal; }
}
