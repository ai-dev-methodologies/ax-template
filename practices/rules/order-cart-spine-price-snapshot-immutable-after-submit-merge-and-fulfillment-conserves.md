---
title: A cart→order spine must freeze each line's unit price + name at add-time (the price the customer saw, @Column(updatable=false), never re-derived from the live catalog), reject every add/update/remove on a SUBMITTED order (only an IN_PROCESS cart is editable), MERGE quantity when the same SKU is added again instead of duplicating a line, and partition order units into fulfillment groups conservingly (Σ group-item quantity per line == line quantity)
impact: HIGH
impactDescription: "Re-deriving a line's price from the live catalog at read time lets a later catalog edit silently rewrite a customer's existing order — they are billed a price they never agreed to, and the receipt is unauditable; allowing an add/update/remove on a SUBMITTED (paid) order mutates the order after payment so the charged total no longer matches the order; appending a duplicate line for an already-present SKU explodes the line count and drifts per-line proration; and a fulfillment partition that does not conserve (Σ group-item quantity ≠ line quantity) ships a unit twice or never — the order-spine failure modes a snapshot column, an editable-state guard, a quantity merge, and a partition-conservation check make unrepresentable"
tags:
  - e-commerce
  - order
  - state-machine
  - data-integrity
  - lifecycle
spec_ref: "specs/order-l0.yaml#ORDER-IMMUTABLE-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/commerceorder/CommerceOrder.java + backend/src/main/java/com/ax/template/authblueprint/commerceorder/CommerceOrderItem.java + backend/src/main/java/com/ax/template/authblueprint/commerceorder/CommerceOrderStateMachine.java"
  pattern: "OrderItem carries unit_price_at_add / name_at_add / sku_id as @Column(updatable=false) so the line price is the add-time snapshot, never the live catalog price; add/update/remove first assert status.editable() (true only for IN_PROCESS) and throw 409 ORDER_NOT_EDITABLE otherwise; adding an already-present sku_id increments that line's quantity instead of appending; the sole-mutator OrderStateMachine allows IN_PROCESS→SUBMITTED (one-way) / →CANCELLED (terminal) and freezes the total at submit; assigning fulfillment groups asserts Σ(group-item quantity) per OrderItem == OrderItem quantity (422 otherwise); @Version guards concurrent cart ops; lookups are user-scoped (404, IDOR-safe)"
upstream:
  - "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/order/domain/OrderItem.java"
  - "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/order/service/type/OrderStatus.java"
  - "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/order/domain/Order.java"
evidence:
  - source_type: external
    citation: "Broadleaf Commerce (develop-7.0.x) OrderItem.getRetailPrice — the price-snapshot rationale absorbed: a line's price is captured at add-time and read from the line, not the live catalog, because the catalog price may have moved since"
    url: "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/order/domain/OrderItem.java"
    quote: "could have changed since the item was added"
    quoted_at: "2026-06-24"
  - source_type: external
    citation: "Broadleaf Commerce (develop-7.0.x) OrderStatus — the editable flag absorbed for immutable-after-submit: a SUBMITTED order is constructed non-editable (false), so cart mutations are gated to IN_PROCESS"
    url: "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/order/service/type/OrderStatus.java"
    quote: "public static final OrderStatus SUBMITTED = new OrderStatus(\"SUBMITTED\", \"Submitted\", false);"
    quoted_at: "2026-06-24"
  - source_type: external
    citation: "Broadleaf Commerce (develop-7.0.x) Order.getFulfillmentGroups — the fulfillment-group partition absorbed: an order's items split across groups for multi-address shipping (the conservation Σ group-item qty == line qty is the new correctness content)"
    url: "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/order/domain/Order.java"
    quote: "support multi-address (and multi-type) shipping."
    quoted_at: "2026-06-24"
---

## Rule

A **cart→order spine** models one `Order` that is a mutable **cart** while `IN_PROCESS` and an immutable **order** once `SUBMITTED`. It absorbs four order-spine invariants the live catalog and the discount engine do not provide (it COMPOSES `pricing-l0` for the total math, `thresholdterminal` for terminal-state irreversibility, and `statemutation` for the generic editable-by-state pattern — it does not re-implement them):

1. **Price snapshot on the line.** Each `OrderItem` records `unit_price_at_add`, `name_at_add`, and `sku_id` at add-time as `@Column(updatable=false)`; reads never re-derive the price from the live catalog. A later catalog price change must not rewrite the customer's existing line — the line charges the price the customer saw.
2. **Immutable after submit.** add/update/remove-item first asserts `status.editable()` (true only for `IN_PROCESS`); once `SUBMITTED`, the item set, quantities, snapshots, and total are frozen — a mutation returns 409. (Order-spine specialization of generic state-conditional mutability.)
3. **Quantity merge.** Adding an already-present `sku_id` (same options) to an in-process order increments that line's quantity; it never appends a second line for the same SKU.
4. **Fulfillment partition conserves.** Order units partition into fulfillment groups such that, for each `OrderItem`, `Σ(group-item quantity) == OrderItem.quantity` — no unit unassigned or double-assigned (422 otherwise).

The status FSM (`IN_PROCESS→SUBMITTED` one-way; `→CANCELLED` terminal) is driven by a sole-mutator `OrderStateMachine`; `@Version` guards concurrent cart ops; lookups are user-scoped (404, IDOR-safe).

**Correct — editable-state guard + frozen price snapshot + quantity merge:**

```java
// backend/.../commerceorder/CommerceOrder.java — cart mutation guarded by editable status; same-SKU merges
void addItem(UUID skuId, String name, long unitPrice, int qty) {
    if (!status.editable()) throw CommerceOrderException.notEditable();   // 409 — only IN_PROCESS is a cart
    OrderItem existing = findLine(skuId);                                 // same sku → merge, not duplicate
    if (existing != null) { existing.addQuantity(qty); return; }
    items.add(new OrderItem(skuId, name, unitPrice, qty));                // snapshot frozen on the line
}
// OrderItem.java — the price the customer saw is frozen, never re-read from the catalog
@Column(name = "unit_price_at_add", updatable = false) private long unitPriceAtAdd;
@Column(name = "name_at_add",       updatable = false) private String nameAtAdd;
// OrderStateMachine.java — sole mutator; submit is one-way and freezes the total (composes pricing-l0)
public void submit(Order o, long pricedTotal) {
    assertTransition(o.getStatus(), SUBMITTED);
    o.freezeTotals(pricedTotal);                                         // total snapshot at submit
    o.setStatus(SUBMITTED);
}
```

**Incorrect — append regardless of status, live-catalog price (the toy's gaps this absorbs):**

```java
// toy: addItem ignores status → a SUBMITTED (paid) order still accepts items; always appends; no snapshot
public void addItem(Product p, int qty) {
    orderItems.add(new OrderItem(p, qty));   // WRONG: no editable guard; duplicate line for same SKU
}
long lineTotal() { return catalog.priceOf(skuId) * quantity; }  // WRONG: live price — a catalog edit rewrites history
```

Reading the live catalog price lets a post-purchase catalog edit silently re-bill the customer; appending on a SUBMITTED order mutates a paid order; a duplicate line per SKU drifts proration. The snapshot column, the editable-state guard, the quantity merge, and the partition-conservation check make each defect unrepresentable.

Reference: [Broadleaf OrderItem.getRetailPrice (price snapshot rationale)](https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/order/domain/OrderItem.java)

Reference: [Broadleaf OrderStatus (editable flag — SUBMITTED is frozen)](https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/order/service/type/OrderStatus.java)

Reference: [Broadleaf Order.getFulfillmentGroups (fulfillment partition)](https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/order/domain/Order.java)
