/**
 * Cart-to-order spine of a commerce platform: a single order aggregate that is a
 * mutable <em>cart</em> while {@code IN_PROCESS} and an immutable <em>order</em>
 * once {@code SUBMITTED}, with line-level price snapshots, quantity merging, a
 * one-way submit lifecycle, and a conserving fulfillment partition.
 *
 * <h2>Correctness invariant</h2>
 * <ol>
 *   <li><b>Price snapshot on the line.</b> Each order item freezes its
 *       unit-price, name, and SKU id at add-time as
 *       {@code @Column(updatable=false)}; reads never re-derive the price from
 *       the live catalog, so a later catalog price change cannot rewrite a
 *       customer's existing line.</li>
 *   <li><b>Immutable after submit.</b> add/update/remove-item is allowed only
 *       while editable ({@code IN_PROCESS}); a mutation of a {@code SUBMITTED}
 *       order returns 409. The frozen field set is items, quantities, snapshots,
 *       and total.</li>
 *   <li><b>Quantity merge.</b> Adding an already-present SKU increments that
 *       line's quantity rather than appending a duplicate line — preventing
 *       line-count explosion and per-line proration drift.</li>
 *   <li><b>One-way lifecycle.</b> {@code IN_PROCESS to SUBMITTED} is one-way and
 *       {@code CANCELLED} is terminal (no outgoing edges).</li>
 *   <li><b>Conserving fulfillment partition.</b> For each order item,
 *       the sum of its fulfillment-group-item quantities equals the line
 *       quantity — no unit unassigned, none double-assigned; re-assignment
 *       replaces rather than appends (idempotent); a non-conserving partition is
 *       rejected 422.</li>
 *   <li><b>IDOR-safe scoping.</b> Every order lookup is scoped to the caller's
 *       user id; another user receives 404, never a 403 existence leak.</li>
 * </ol>
 *
 * <h2>Key components (DDD shape)</h2>
 * <ul>
 *   <li><b>Aggregate root</b> — {@link CommerceOrder} unifies cart and order by
 *       status; owns {@link CommerceOrderItem}, {@link CommerceFulfillmentGroup},
 *       and {@link CommerceFulfillmentGroupItem}. Price-snapshot and user-id
 *       columns are immutable; {@code @Version} guards concurrent cart ops.</li>
 *   <li><b>Sole mutator of status</b> — {@link CommerceOrderStateMachine} is the
 *       only writer of {@link CommerceOrderStatus}; {@link CommerceOrderService}
 *       orchestrates the cart/order operations through it.</li>
 *   <li><b>Enums</b> — {@link CommerceOrderStatus}
 *       ({@code IN_PROCESS}/{@code SUBMITTED}/{@code CANCELLED}) and
 *       {@link CommerceFulfillmentGroupStatus}.</li>
 *   <li><b>Controller surface</b> — {@link CommerceOrderController} (thin HTTP);
 *       {@link CommerceOrderExceptionHandler} + {@link CommerceOrderException}
 *       render RFC 9457 Problem Details; {@link CommerceOrderMetrics} for
 *       observability.</li>
 * </ul>
 *
 * <h2>Verification</h2>
 * Run {@code ./gradlew testCommerceOrder} (spec {@code order-l0}, 7 items across
 * SNAPSHOT / IMMUTABLE / MERGE / LIFECYCLE / FULFILL / TOTAL-SNAPSHOT / AUTHZ).
 * The package ships {@code CommerceOrderViolationProofTest}, which asserts the
 * snapshot-immutability, terminal-state, and conservation invariants are
 * structurally enforced. The order-total math composes the pricing pipeline
 * (see {@code com.ax.template.authblueprint.commercepricing}).
 *
 * <h2>External grounding</h2>
 * The lifecycle status codes and idempotent re-assignment follow
 * <a href="https://www.rfc-editor.org/rfc/rfc9110">RFC 9110</a> HTTP Semantics
 * (409 Conflict for an illegal transition; idempotent methods, section 9.2.2);
 * the IDOR-safe user-scoped lookup mitigates
 * <a href="https://cwe.mitre.org/data/definitions/639.html">CWE-639</a>
 * (Authorization Bypass Through User-Controlled Key); the error contract is
 * <a href="https://www.rfc-editor.org/rfc/rfc9457">RFC 9457</a> Problem Details.
 */
package com.ax.template.authblueprint.commerceorder;
