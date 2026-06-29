package com.ax.template.authblueprint.commerceorder;

import com.ax.template.authblueprint.common.AggregateRoot;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Commerce order (cart + order unified by status — external e-commerce reference pattern).
 *
 * <p>Lifecycle invariants (ORDER-LIFECYCLE-001):
 * <ul>
 *   <li>IN_PROCESS (cart) → SUBMITTED: closes the cart for mutations; freezes totals.</li>
 *   <li>IN_PROCESS → CANCELLED: terminal without submission.</li>
 *   <li>SUBMITTED → CANCELLED: cancel a submitted order.</li>
 *   <li>No re-open edge (SUBMITTED → IN_PROCESS is forbidden).</li>
 * </ul>
 *
 * <p>Mutation rules:
 * <ul>
 *   <li>Status mutated ONLY by {@link CommerceOrderStateMachine}.</li>
 *   <li>Item add/update/remove only when {@code status.editable()} (IN_PROCESS).</li>
 *   <li>Totals (total, subTotal, tax) frozen at submit via the FSM.</li>
 * </ul>
 */
@AggregateRoot
@Entity
@Table(
    name = "commerce_orders",
    indexes = {
        @Index(name = "ix_commerce_orders_user_created",
               columnList = "user_id,created_at"),
        @Index(name = "ix_commerce_orders_status", columnList = "status")
    }
)
@Check(constraints = "total >= 0 AND sub_total >= 0 AND tax >= 0")
public class CommerceOrder {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Scopes every lookup — ORDER-AUTHZ-001. */
    @Column(name = "user_id", nullable = false, updatable = false, length = 255)
    private String userId;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private CommerceOrderStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "total", nullable = false)
    private long total;

    @Column(name = "sub_total", nullable = false)
    private long subTotal;

    @Column(name = "tax", nullable = false)
    private long tax;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<CommerceOrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<CommerceFulfillmentGroup> fulfillmentGroups = new ArrayList<>();

    /**
     * Root directly owns fulfillment group items so the items collection is NOT
     * held by the sibling member {@link CommerceFulfillmentGroup} (DDD-006 HG-AGG-REF fix).
     * Cascade + orphanRemoval here ensures items are deleted when clearFulfillmentGroups() is called.
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @jakarta.persistence.JoinColumn(name = "order_id", nullable = false)
    @OrderBy("id ASC")
    private List<CommerceFulfillmentGroupItem> fulfillmentGroupItems = new ArrayList<>();

    /** JPA required. */
    protected CommerceOrder() {}

    /** Package-private factory — only CommerceOrderService creates orders. */
    CommerceOrder(UUID id, String userId, String currency, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.currency = currency;
        this.status = CommerceOrderStatus.IN_PROCESS;
        this.total = 0L;
        this.subTotal = 0L;
        this.tax = 0L;
        this.createdAt = createdAt;
    }

    // ── item management (package-private — CommerceOrderService is the sole caller) ─

    /**
     * Add item (or merge if sku already present).
     *
     * @return the CommerceOrderItem that carries the quantity (new or existing merged)
     */
    CommerceOrderItem addOrMergeItem(String skuId, String nameAtAdd, long unitPriceAtAdd, int quantity) {
        Optional<CommerceOrderItem> existing = items.stream()
            .filter(i -> i.getSkuId().equals(skuId))
            .findFirst();
        if (existing.isPresent()) {
            existing.get().incrementQuantity(quantity);
            return existing.get();
        }
        CommerceOrderItem item = new CommerceOrderItem(
            UUID.randomUUID(), this, skuId, nameAtAdd, unitPriceAtAdd, quantity);
        items.add(item);
        return item;
    }

    /** Replace quantity on an existing item. Package-private — CommerceOrderService only. */
    void updateItemQuantity(UUID itemId, int newQuantity) {
        CommerceOrderItem item = items.stream()
            .filter(i -> i.getId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new CommerceOrderException(
                "ORDER_ITEM_NOT_FOUND", 404, "Order item not found: " + itemId));
        item.setQuantity(newQuantity);
    }

    /** Remove an item by id. Package-private — CommerceOrderService only. */
    void removeItem(UUID itemId) {
        boolean removed = items.removeIf(i -> i.getId().equals(itemId));
        if (!removed) {
            throw new CommerceOrderException("ORDER_ITEM_NOT_FOUND", 404,
                "Order item not found: " + itemId);
        }
    }

    /** Add a fulfillment group. Package-private — CommerceOrderService only. */
    void addFulfillmentGroup(CommerceFulfillmentGroup group) {
        this.fulfillmentGroups.add(group);
    }

    /** Add a fulfillment group item (owned by root, not sibling member). Package-private. */
    void addFulfillmentGroupItem(CommerceFulfillmentGroupItem item) {
        this.fulfillmentGroupItems.add(item);
    }

    /**
     * Replace fulfillment groups — clears existing groups AND items (orphanRemoval removes them
     * from DB) then lets the service add the new set.  Package-private — CommerceOrderService only.
     * Supports idempotent re-assignment (H2: replace, not append).
     */
    void clearFulfillmentGroups() {
        this.fulfillmentGroups.clear();
        this.fulfillmentGroupItems.clear();
    }

    // ── FSM-only mutators (package-private) ──────────────────────────────────────

    /** Sole mutator of {@link #status} — called only by {@link CommerceOrderStateMachine}. */
    void setStatus(CommerceOrderStatus next) {
        this.status = next;
    }

    /** Called by FSM at submit time to freeze totals (ORDER-TOTAL-SNAPSHOT-001). */
    void freezeTotals(long total, long subTotal, long tax) {
        this.total = total;
        this.subTotal = subTotal;
        this.tax = tax;
        this.submittedAt = Instant.now();
    }

    // ── public readers ────────────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public String getUserId() { return userId; }
    public String getCurrency() { return currency; }
    public CommerceOrderStatus getStatus() { return status; }
    public long getVersion() { return version; }
    public long getTotal() { return total; }
    public long getSubTotal() { return subTotal; }
    public long getTax() { return tax; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public List<CommerceOrderItem> getItems() { return Collections.unmodifiableList(items); }
    public List<CommerceFulfillmentGroup> getFulfillmentGroups() {
        return Collections.unmodifiableList(fulfillmentGroups);
    }
    public List<CommerceFulfillmentGroupItem> getFulfillmentGroupItems() {
        return Collections.unmodifiableList(fulfillmentGroupItems);
    }
}
