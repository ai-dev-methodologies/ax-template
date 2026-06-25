---
title: A discount/offer application engine must conserve a prorated order-level discount to the cent (floor-then-distribute-remainder so Σ item shares == order discount exactly), apply offers in a DETERMINISTIC total order (priority then potential-savings, stable tie-break — never collection order), gate co-application on TWO orthogonal flags (stackable ≠ combinable), clamp every discount to the line price (never negative), and enforce max-uses ATOMICALLY via UNIQUE(offer_id, order_ref) plus a pessimistic offer-row lock — never check-then-insert (the Broadleaf max-uses TOCTOU this absorbs and strengthens)
impact: HIGH
impactDescription: "A non-conserving proration loses pennies that propagate into tax and the order total on every multi-item discounted order (reconciliation drift); offers applied in collection/iteration order make the same cart resolve to different totals across runs and nodes (non-reproducible pricing); conflating stackable with combinable compounds discounts a merchant never authorized (margin loss / fraud); an unclamped discount can drive a line below zero so the store pays the customer; and a check-then-insert max-uses cap (Broadleaf's OfferAudit has only non-unique indexes) lets two concurrent redemptions both pass the cap and over-redeem (CWE-362) — the exact race a unique constraint plus a row lock makes unrepresentable"
tags:
  - e-commerce
  - promotion
  - money
  - concurrency
  - determinism
spec_ref: "specs/promotion-l0.yaml#PROMO-MAXUSES-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/commercepromotion/PromotionService.java + backend/src/main/java/com/ax/template/authblueprint/commercepromotion/OfferRedemption.java"
  pattern: "An order-scope discount prorated to items floors each share then distributes leftover minor units one-by-one until Σ shares == order discount exactly; offers sort by (priority ASC, potentialSavings DESC) with a stable tie-break (never collection order); stackable and combinable are separate booleans gating multiples vs other-offer tolerance; each adjustment is clamped to the line price (never negative); redemption inserts an append-only OfferRedemption under UNIQUE(offer_id, order_ref) with the Offer row held PESSIMISTIC_WRITE and the max-uses count checked under that lock, so the over-redemption is unrepresentable; percent math uses Math.multiplyExact on integer minor units (no double); re-apply reconciles adjustments by offer id (idempotent)"
upstream:
  - "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/offer/service/discount/OrderOfferComparator.java"
  - "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/offer/domain/OfferImpl.java"
  - "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/offer/domain/OfferAuditImpl.java"
evidence:
  - source_type: external
    citation: "Broadleaf Commerce (develop-7.0.x) OrderOfferComparator — the deterministic two-key offer ordering absorbed: sort by priority, ties broken by highest potential savings (a total order, not iteration order)"
    url: "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/offer/service/discount/OrderOfferComparator.java"
    quote: "return p2.getPotentialSavings().compareTo(p1.getPotentialSavings());"
    quoted_at: "2026-06-24"
  - source_type: external
    citation: "Broadleaf Commerce (develop-7.0.x) OfferImpl.isCombinableWithOtherOffers — the combinability gate (distinct from stackability) absorbed: whether an offer tolerates other offers co-applying"
    url: "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/offer/domain/OfferImpl.java"
    quote: "return combinableWithOtherOffers == null ? false : combinableWithOtherOffers;"
    quoted_at: "2026-06-24"
  - source_type: external
    citation: "Broadleaf Commerce (develop-7.0.x) OfferAuditImpl — the max-uses GAP this rule strengthens: the redemption audit carries only NON-UNIQUE indexes, so a check-then-insert cap races under concurrency (CWE-362); the absorbed engine adds UNIQUE(offer_id, order_ref) + a pessimistic lock"
    url: "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/offer/domain/OfferAuditImpl.java"
    quote: "@Index(name = \"OFFERAUDIT_OFFER_INDEX\", columnList = \"OFFER_ID\"),"
    quoted_at: "2026-06-24"
---

## Rule

A **discount/offer application engine** (coupons, promotions, loyalty, cart-rules) takes a set of order line items + applicable offers and produces discount adjustments. The correctness cluster a naive engine gets wrong — and that surfaces only at finance-reconciliation time — is:

1. **Conserving proration (the money keystone).** An order-level discount pushed down to N items must conserve: `share_i = floor(orderDiscount * amount_i / total)`, then distribute the leftover minor units one-by-one until `Σ share_i == orderDiscount` exactly. Any derived base (taxable amount) is re-derived from the post-distribution shares. A penny lost here propagates into tax and the order total on every discounted multi-item order.
2. **Deterministic total-order application.** Sort candidate offers by `(priority ASC, potentialSavings DESC)` with a stable tie-break — a total order computed into scalars, never `HashMap`/collection iteration order. The same cart must resolve identically across runs and nodes.
3. **Stackable ≠ combinable.** Two orthogonal gates: `stackable` (multiples of one offer) and `combinable` (tolerates *other* offers). A non-combinable offer, once applied, blocks further offers. Conflating them compounds unauthorized discounts.
4. **Clamp to the line price.** A discount is clamped to the current price and never negative — a line never goes below zero, the order total cannot invert sign.
5. **Atomic max-uses.** Redemption is bounded by `UNIQUE(offer_id, order_ref)` (idempotent — one redemption per offer per order) AND a max-uses cap checked under the offer row's `PESSIMISTIC_WRITE` lock — never check-then-insert. This makes the over-redemption unrepresentable.
6. **Idempotent re-apply.** Re-applying offers reconciles adjustments by offer id (update/remove/add), so a recompute is a no-op — never a second copy stacked on top.

Money is integer minor units throughout (`Math.multiplyExact` for percent, fail-closed on overflow); no floating point.

**Correct — conserving proration + atomic, unique-backstopped max-uses (strengthens the absorbed Broadleaf engine):**

```java
// backend/.../commercepromotion/PromotionService.java
long distributed = 0;
for (LineItem it : items) {                         // floor each share
    long share = Math.multiplyExact(orderDiscount, it.amount()) / total;
    it.setAdjustment(Math.min(share, it.amount())); // CLAMP to line price (never negative/below zero)
    distributed += it.getAdjustment();
}
long remainder = orderDiscount - distributed;       // distribute leftover minor units one-by-one
for (int i = 0; remainder > 0 && i < items.size(); i++, remainder--) {
    items.get(i).addAdjustment(1);                  // until Σ shares == orderDiscount EXACTLY
}
// max-uses: atomic, unique-backstopped — NOT check-then-insert
Offer offer = offers.findByIdForUpdate(offerId)     // PESSIMISTIC_WRITE on the offer row (CWE-362)
    .orElseThrow(PromotionException::offerNotFound);
if (offer.getMaxUses() > 0 && redemptions.countByOffer(offerId) >= offer.getMaxUses()) {
    throw PromotionException.maxUsesExceeded();      // 409 — checked under the lock
}
members.persist(new OfferRedemption(offerId, customerId, orderRef, clock.instant()));  // UNIQUE(offer_id, order_ref)
```

**Incorrect — Broadleaf's non-unique redemption audit + iteration-order application (the TOCTOU + non-determinism this absorbs):**

```java
// max-uses by count-then-insert with no lock and no unique constraint (Broadleaf OfferAudit has only @Index)
if (offerAuditDao.countUsesByCustomer(customerId, offerId) < offer.getMaxUses()) {  // WRONG: TOCTOU
    offerAuditDao.save(new OfferAudit(offerId, customerId, orderId));    // two concurrent checkouts both pass
}
for (Offer o : applicableOffers) { applyDiscount(o); }  // WRONG: iteration order; no conserving proration; no clamp
```

Two concurrent checkouts both read `uses < max` and both insert → the cap is exceeded; the same cart discounts differently per run because the offer set iterates in HashSet order; pennies vanish because the order discount is split without remainder distribution. The unique constraint + row lock + largest-remainder distribution make each defect unrepresentable.

Reference: [Broadleaf OrderOfferComparator (deterministic offer ordering)](https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/offer/service/discount/OrderOfferComparator.java)

Reference: [Broadleaf OfferImpl (combinability gate)](https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/offer/domain/OfferImpl.java)

Reference: [Broadleaf OfferAuditImpl (non-unique redemption audit — the max-uses TOCTOU gap)](https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/offer/domain/OfferAuditImpl.java)
