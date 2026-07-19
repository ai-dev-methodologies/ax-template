package com.ax.template.authblueprint.checkout;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Order — checkout vertical slice (CRUD L4 composition: cart -> order -> payment ->
 * receipt). Storage layer keeps the money amount as integer minor units (long), per
 * the catalog's money-l0 seam (Fowler Money pattern) — never a raw double/BigDecimal
 * column. See V###__create_order.sql for the matching migration convention
 * (entity_migration_guard.sh).
 */
@Entity
@Table(name = "checkout_order")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long buyerId;

    /** integer minor units — e.g. 10999 == $109.99 for a 2-decimal currency. */
    private long totalAmount;

    private String currency;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    public Long getId() { return id; }
    public Long getBuyerId() { return buyerId; }
    public long getTotalAmount() { return totalAmount; }
    public String getCurrency() { return currency; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
}
