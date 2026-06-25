package com.ax.template.authblueprint.commerceorder;

import com.ax.template.authblueprint.common.AggregateMember;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Check;

import java.util.UUID;

/**
 * Maps a CommerceOrderItem to a CommerceFulfillmentGroup with a split quantity.
 *
 * <p>ORDER-FULFILL-001: Σ(group-item quantity per order_item_id) must == CommerceOrderItem.quantity.
 *
 * <p>All three FK columns ({@code order_id}, {@code fulfillment_group_id}, {@code order_item_id})
 * are stored as plain UUID identity references — NOT object pointers — to satisfy HG-AGG-REF.
 * Cascade and orphanRemoval are managed by {@link CommerceOrder} via its {@code fulfillmentGroupItems}
 * collection (root holds member, not member holds sibling-member).
 */
@AggregateMember(root = CommerceOrder.class)
@Entity
@Table(name = "commerce_fulfillment_group_items")
@Check(constraints = "quantity > 0")
public class CommerceFulfillmentGroupItem {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * FK to {@link CommerceOrder} — written by the owning-side @JoinColumn on
     * {@code CommerceOrder.fulfillmentGroupItems}.
     */
    @Column(name = "order_id", updatable = false, nullable = false,
            insertable = false)
    private UUID orderId;

    /**
     * Identity reference to the parent {@link CommerceFulfillmentGroup} — NOT an object pointer.
     * Avoids HG-AGG-REF sibling-member pointer violation.
     */
    @Column(name = "fulfillment_group_id", updatable = false, nullable = false)
    private UUID fulfillmentGroupId;

    /**
     * Identity reference to the CommerceOrderItem — NOT an object pointer.
     * Avoids HG-AGG-REF cross-aggregate pointer violation.
     */
    @Column(name = "order_item_id", nullable = false, updatable = false)
    private UUID orderItemId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    /** JPA required. */
    protected CommerceFulfillmentGroupItem() {}

    /** Package-private factory — only CommerceOrderService creates items. */
    CommerceFulfillmentGroupItem(UUID id, UUID fulfillmentGroupId, UUID orderItemId, int quantity) {
        this.id = id;
        this.fulfillmentGroupId = fulfillmentGroupId;
        this.orderItemId = orderItemId;
        this.quantity = quantity;
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public UUID getFulfillmentGroupId() { return fulfillmentGroupId; }
    public UUID getOrderItemId() { return orderItemId; }
    public int getQuantity() { return quantity; }
}
