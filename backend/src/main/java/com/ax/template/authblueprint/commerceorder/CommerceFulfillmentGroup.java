package com.ax.template.authblueprint.commerceorder;

import com.ax.template.authblueprint.common.AggregateMember;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * A shipment destination group within an order.
 *
 * <p>ORDER-FULFILL-001: group items are created in the same transaction as assignment;
 * Σ(group-item quantity per order_item_id) must equal each CommerceOrderItem's quantity.
 *
 * <p>This entity holds NO collection of {@link CommerceFulfillmentGroupItem}s —
 * items are cascaded directly from {@link CommerceOrder} (DDD-006 compliance: no member
 * holding a typed collection of another sibling member).  Items are logically grouped
 * by their {@code fulfillment_group_id} UUID reference.
 */
@AggregateMember(root = CommerceOrder.class)
@Entity
@Table(name = "commerce_fulfillment_groups")
public class CommerceFulfillmentGroup {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private CommerceOrder order;

    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CommerceFulfillmentGroupStatus status;

    @Column(name = "merchandise_total", nullable = false)
    private long merchandiseTotal;

    /** JPA required. */
    protected CommerceFulfillmentGroup() {}

    CommerceFulfillmentGroup(UUID id, CommerceOrder order, String address) {
        this.id = id;
        this.order = order;
        this.address = address;
        this.status = CommerceFulfillmentGroupStatus.UNFULFILLED;
        this.merchandiseTotal = 0L;
    }

    void setMerchandiseTotal(long total) {
        this.merchandiseTotal = total;
    }

    public UUID getId() { return id; }
    public CommerceOrder getOrder() { return order; }
    public String getAddress() { return address; }
    public CommerceFulfillmentGroupStatus getStatus() { return status; }
    public long getMerchandiseTotal() { return merchandiseTotal; }
}
