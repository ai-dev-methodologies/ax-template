package com.ax.template.authblueprint.bundlepricing;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.Check;

import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateMember;

/**
 * CompositeComponent — one child line of a {@link CompositeItem} (bundle / kit).
 *
 * <p>An {@code @AggregateMember} of {@link CompositeItem}: loaded and saved only through its
 * root (no own repository). It carries the child's {@code quantity} and unit prices, all of
 * which are IMMUTABLE ({@code @Column(updatable=false)}, no public setter): a child's
 * contribution to the composite roll-up cannot be silently re-priced after creation, which
 * is what makes the conserving total (BUNDLE-ITEMSUM-001) impossible to drift.
 *
 * <p>Invariant traces:
 * <ul>
 *   <li>BUNDLE-ITEMSUM-001 — {@code unitRetailPrice × quantity} is this child's subtotal
 *       in the conserving roll-up; {@code unitSalePrice} falls back to {@code unitRetailPrice}
 *       when null.</li>
 *   <li>BUNDLE-DERIVED-001 — {@code taxable} feeds the bundle's DERIVED taxability and the
 *       taxable-price roll-up.</li>
 * </ul>
 */
@AggregateMember(root = CompositeItem.class)
@Entity
@Table(name = "composite_components")
@Check(constraints = "quantity > 0 AND unit_retail_price >= 0 "
    + "AND (unit_sale_price IS NULL OR (unit_sale_price >= 0 AND unit_sale_price <= unit_retail_price))")
public class CompositeComponent {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "composite_item_id", nullable = false, updatable = false)
    private CompositeItem compositeItem;

    /** Human label for the child line (e.g. SKU name). */
    @Column(name = "name", length = 200, updatable = false)
    private String name;

    /** Quantity of this child in the bundle — immutable, strictly positive. */
    @Column(name = "quantity", nullable = false, updatable = false)
    private int quantity;

    /** Unit retail price in minor currency units (e.g. cents) — immutable. */
    @Column(name = "unit_retail_price", nullable = false, updatable = false)
    private long unitRetailPrice;

    /** Unit sale price (optional override). Null ⇒ falls back to {@code unitRetailPrice}. */
    @Column(name = "unit_sale_price", updatable = false)
    private Long unitSalePrice;

    /** Whether this child contributes to the bundle's taxable price (BUNDLE-DERIVED-001). */
    @Column(name = "taxable", nullable = false, updatable = false)
    private boolean taxable;

    protected CompositeComponent() {}

    CompositeComponent(UUID id, String name, int quantity,
                       long unitRetailPrice, Long unitSalePrice, boolean taxable) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.unitRetailPrice = unitRetailPrice;
        this.unitSalePrice = unitSalePrice;
        this.taxable = taxable;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public long getUnitRetailPrice() { return unitRetailPrice; }
    public Long getUnitSalePrice() { return unitSalePrice; }
    public boolean isTaxable() { return taxable; }

    /** Package-private — set only by {@link CompositeItem#addComponent} when wiring the back-reference. */
    void setCompositeItem(CompositeItem compositeItem) { this.compositeItem = compositeItem; }

    @JsonIgnore
    public CompositeItem getCompositeItem() { return compositeItem; }

    /** This child's retail subtotal in the conserving roll-up: unitRetailPrice × quantity. */
    long retailSubtotal() { return unitRetailPrice * quantity; }

    /** This child's sale subtotal: (unitSalePrice ?? unitRetailPrice) × quantity. */
    long saleSubtotal() {
        long unitSale = (unitSalePrice != null) ? unitSalePrice : unitRetailPrice;
        return unitSale * quantity;
    }
}
