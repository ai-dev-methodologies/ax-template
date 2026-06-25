package com.ax.template.authblueprint.commerceorder;

import com.ax.template.authblueprint.common.AggregateMember;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Check;

import java.util.UUID;

/**
 * One line in a commerce order — snapshot columns are frozen at creation.
 *
 * <p>ORDER-SNAPSHOT-001: {@code sku_id}, {@code name_at_add}, {@code unit_price_at_add}
 * are {@code @Column(updatable=false)}.  Line price is always derived from the snapshot,
 * never from a live catalog lookup, so subsequent product mutations do not rewrite history.
 */
@AggregateMember(root = CommerceOrder.class)
@Entity
@Table(name = "commerce_order_items")
@Check(constraints = "quantity > 0 AND unit_price_at_add >= 0")
public class CommerceOrderItem {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private CommerceOrder order;

    /** Snapshot — immutable once persisted (ORDER-SNAPSHOT-001). */
    @Column(name = "sku_id", nullable = false, updatable = false, length = 255)
    private String skuId;

    /** Product name captured at the moment the item was added (ORDER-SNAPSHOT-001). */
    @Column(name = "name_at_add", nullable = false, updatable = false, length = 400)
    private String nameAtAdd;

    /** Unit price captured at the moment the item was added (ORDER-SNAPSHOT-001). */
    @Column(name = "unit_price_at_add", nullable = false, updatable = false)
    private long unitPriceAtAdd;

    /** Mutable only while the order is IN_PROCESS (via parent aggregate). */
    @Column(name = "quantity", nullable = false)
    private int quantity;

    /** JPA required. */
    protected CommerceOrderItem() {}

    CommerceOrderItem(UUID id, CommerceOrder order, String skuId, String nameAtAdd,
                      long unitPriceAtAdd, int quantity) {
        this.id = id;
        this.order = order;
        this.skuId = skuId;
        this.nameAtAdd = nameAtAdd;
        this.unitPriceAtAdd = unitPriceAtAdd;
        this.quantity = quantity;
    }

    /** Derived — never stored, always computed from the snapshot. Fail-closed on overflow (M2). */
    public long getLineTotal() {
        return Math.multiplyExact(quantity, unitPriceAtAdd);
    }

    /** Package-private — only CommerceOrderService may increment quantity via the aggregate. */
    void incrementQuantity(int delta) {
        this.quantity += delta;
    }

    /** Package-private — only CommerceOrderService may replace quantity via the aggregate. */
    void setQuantity(int qty) {
        this.quantity = qty;
    }

    public UUID getId() { return id; }
    public CommerceOrder getOrder() { return order; }
    public String getSkuId() { return skuId; }
    public String getNameAtAdd() { return nameAtAdd; }
    public long getUnitPriceAtAdd() { return unitPriceAtAdd; }
    public int getQuantity() { return quantity; }
}
