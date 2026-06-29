/**
 * Discount/offer application engine of a commerce platform (coupons, promotions,
 * loyalty, cart-rules): given a set of order line items plus applicable offers,
 * it computes discount adjustments that conserve to the penny, apply in a
 * deterministic order, respect orthogonal stacking gates, and cap redemptions
 * atomically.
 *
 * <h2>Correctness invariant</h2>
 * <ol>
 *   <li><b>Conserving proration (the money keystone).</b> An order-level discount
 *       pushed down to N items conserves exactly:
 *       {@code share_i = floor(orderDiscount * amount_i / total)}, then the
 *       leftover minor units are distributed one-by-one (largest-remainder) until
 *       the shares sum back to the order discount exactly.</li>
 *   <li><b>Deterministic total-order application.</b> Candidate offers sort by
 *       (priority, then potential savings) with a stable tie-break — a total
 *       order computed into scalars, never collection iteration order — so the
 *       same cart resolves identically across runs and nodes.</li>
 *   <li><b>Stackable is not combinable.</b> Two orthogonal gates:
 *       <em>stackable</em> (multiples of one offer) and <em>combinable</em>
 *       (tolerates other offers).</li>
 *   <li><b>Winner-take-all, clamp, atomic cap.</b> The order path and the item
 *       path keep the larger and discard the loser; each discount is clamped to
 *       the line price (never negative); and the max-uses cap is enforced
 *       atomically so an over-redemption is structurally unrepresentable.</li>
 *   <li><b>Idempotent re-apply.</b> Re-application reconciles by offer id and
 *       never double-counts.</li>
 * </ol>
 *
 * <h2>Key components (DDD shape)</h2>
 * <ul>
 *   <li><b>Aggregate root</b> — {@link PromoOffer} owns its {@link PromoOfferCode}
 *       and append-only {@link PromoOfferRedemption} members. Over-redemption is
 *       made unrepresentable by {@code UNIQUE(offer_id, order_ref)} plus a
 *       {@code PESSIMISTIC_WRITE} lock on the offer row over {@code @Version}
 *       optimistic concurrency — never a check-then-insert race.</li>
 *   <li><b>Sole-mutator service</b> — {@link PromotionService} is the only writer
 *       of the offer aggregate and the redemption ledger.</li>
 *   <li><b>Enums / value types</b> — {@link DiscountType} and {@link OfferScope}.</li>
 *   <li><b>Controller surface</b> — {@link PromotionController} (thin HTTP);
 *       {@link PromotionException} renders RFC 9457 Problem Details;
 *       {@link PromotionMetrics} for observability.</li>
 * </ul>
 *
 * <h2>Verification</h2>
 * Run {@code ./gradlew testCommercePromotion} (spec {@code promotion-l0}, 8 items
 * across CONSERVE / STACK / ORDER / MAXSELECT / CLAMP / MAXUSES / IDEMPOTENT /
 * REDEMPTION-IMMUTABLE). The package ships
 * {@code CommercePromotionViolationProofTest} asserting the conservation,
 * atomic-cap, and redemption-immutability invariants are structurally enforced.
 *
 * <h2>External grounding</h2>
 * The atomic max-uses cap closes the concurrent check-then-insert race classified
 * as <a href="https://cwe.mitre.org/data/definitions/362.html">CWE-362</a>
 * (TOCTOU race condition); the penny-conserving distribution is the
 * largest-remainder apportionment method; the error contract is
 * <a href="https://www.rfc-editor.org/rfc/rfc9457">RFC 9457</a> Problem Details.
 */
package com.ax.template.authblueprint.commercepromotion;
