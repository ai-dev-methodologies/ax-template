package com.ax.template.authblueprint.orderquantization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * order-multiple-quantization-l0 root: one immutable, append-only record of quantizing a required
 * net quantity UP to a procurement constraint (a minimum order quantity + an order multiple). All
 * five basis columns are recorded so the quantization is reconstructible (ORDERQUANT-BASIS-001):
 * the required net quantity, the MOQ used, the order multiple used, the computed orderQuantity, and
 * the computed overage.
 *
 * <p>This domain is DELIBERATELY NON-CONSERVING (ORDERQUANT-OVERAGE-001): {@code orderQuantity >=
 * requiredQuantity} and {@code overage = orderQuantity - requiredQuantity} is the real surplus the
 * lot constraint forces — never hidden. That is the opposite of a conserving rounded-split (whose
 * parts sum back to the whole). The {@link Check} constraints bind {@code overage = order_quantity
 * - required_quantity} and {@code overage >= 0} so a record that fakes or drops the surplus is
 * unrepresentable, and bind {@code moq >= 1 AND order_multiple >= 1 AND required_quantity >= 0}
 * (ORDERQUANT-CONSTRAINT-001) so a meaningless or divide-by-zero constraint cannot be stored.
 *
 * <p>Quantities are exact {@code long} eaches; the columns are {@code order_quantity} /
 * {@code order_multiple} (NEVER a column named {@code value} or {@code order}). The record is
 * immutable — there are no mutator hooks at all and every column is {@code updatable = false};
 * {@code @Version} is present only to satisfy the catalog's optimistic-lock posture.
 */
@AggregateRoot
@Entity
@Table(name = "order_quantizations")
@Check(constraints =
    "required_quantity >= 0 AND moq >= 1 AND order_multiple >= 1"
    + " AND order_quantity >= required_quantity AND order_quantity >= moq"
    + " AND overage >= 0 AND overage = order_quantity - required_quantity")
public class OrderQuantization {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The line/sku/requisition this quantization is for — opaque, recorded verbatim. */
    @Column(name = "item_ref", nullable = false, updatable = false, length = 200)
    private String itemRef;

    /** The required net quantity (what is actually needed) — the quantizer input. */
    @Column(name = "required_quantity", nullable = false, updatable = false)
    private long requiredQuantity;

    /** The supplier minimum order quantity used (>= 1). */
    @Column(name = "moq", nullable = false, updatable = false)
    private long moq;

    /** The supplier order multiple / lot / pack size used (>= 1). */
    @Column(name = "order_multiple", nullable = false, updatable = false)
    private long orderMultiple;

    /** The computed placeable quantity = max(moq, ceil(required / multiple) * multiple). */
    @Column(name = "order_quantity", nullable = false, updatable = false)
    private long orderQuantity;

    /** The recorded NON-CONSERVING surplus = orderQuantity - requiredQuantity (>= 0). */
    @Column(name = "overage", nullable = false, updatable = false)
    private long overage;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OrderQuantization() {}

    /**
     * The orderQuantity and overage are computed by {@link Quantizer} in the service and passed in
     * already-recorded — the entity never recomputes, so the persisted basis IS the computation.
     */
    public OrderQuantization(UUID id, String itemRef, long requiredQuantity, long moq,
                             long orderMultiple, long orderQuantity, long overage, Instant createdAt) {
        this.id = id;
        this.itemRef = itemRef;
        this.requiredQuantity = requiredQuantity;
        this.moq = moq;
        this.orderMultiple = orderMultiple;
        this.orderQuantity = orderQuantity;
        this.overage = overage;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getItemRef() { return itemRef; }
    public long getRequiredQuantity() { return requiredQuantity; }
    public long getMoq() { return moq; }
    public long getOrderMultiple() { return orderMultiple; }
    public long getOrderQuantity() { return orderQuantity; }
    public long getOverage() { return overage; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
