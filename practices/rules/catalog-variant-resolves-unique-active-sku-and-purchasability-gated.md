---
title: A variant-product catalog must give every product exactly one default SKU, resolve a chosen option-value set to EXACTLY ONE active SKU (a duplicate sku-generating signature unrepresentable via UNIQUE(product_id, option_signature) — never an arbitrary iterator().next() pick), gate purchasability on the active-date window AND archival at the cart path (not only at display), and require a sellable SKU to resolve a non-null price at the catalog boundary
impact: HIGH
impactDescription: "Resolving an ambiguous option set by an arbitrary pick (Broadleaf's iterator().next()) charges the customer a non-deterministic price and decrements the wrong SKU's inventory — the same cart resolves differently across runs/nodes; a missing active-window/archival re-gate at the purchase path lets a sunset, not-yet-launched, or archived SKU be bought because only the display layer filtered it; a nullable price on a sellable SKU surfaces as an NPE or a zero-price order at checkout instead of being caught where the data lives; and no default SKU NPEs every variant fallback (name/price/date) — the listed-but-priceless and listed-but-ambiguous defects are exactly the data hazards a variant catalog must make unrepresentable"
tags:
  - catalog
  - e-commerce
  - state-machine
  - data-integrity
  - lifecycle
spec_ref: "specs/catalog-commerce-l0.yaml#CAT-VARIANT-002"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/commercecatalog/CatalogProductService.java + backend/src/main/java/com/ax/template/authblueprint/commercecatalog/Sku.java + backend/src/main/java/com/ax/template/authblueprint/commercecatalog/CatalogProduct.java"
  pattern: "Each product carries a non-null default SKU (create rejects 422 if absent); each variant SKU stores an option_signature (sorted-join of its sku-generating option-value ids) and the table carries UNIQUE(product_id, option_signature) so a duplicate is unrepresentable under ddl-auto; resolveSku computes the requested signature and does an EXACT-match lookup returning exactly one SKU or 404 — never iterator().next() on a multi-candidate set; assertPurchasable gates on now ∈ [active_start, active_end) via an injected Clock AND product-active AND not-archived and is invoked on the purchase path; an active sellable SKU with neither own nor inherited retail price is rejected 422 at the catalog boundary; Sku.product_id/is_default and the option-value xref columns are @Column(updatable=false), @Version guards concurrent variant edits"
upstream:
  - "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/order/service/workflow/service/OrderItemRequestValidationServiceImpl.java"
  - "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/common/src/main/java/org/broadleafcommerce/common/util/DateUtil.java"
  - "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/inventory/service/type/InventoryType.java"
  - "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/inventory/service/InventoryServiceImpl.java"
  - "https://schema.org/Offer"
evidence:
  - source_type: external
    citation: "Broadleaf Commerce (develop-7.0.x) OrderItemRequestValidationServiceImpl.findMatchingSku — the variant-resolution PATTERN this rule absorbs, and the ambiguity GAP it strengthens: Broadleaf resolves a chosen option-value set by an arbitrary pick of the first candidate SKU, with no uniqueness constraint on the SKU↔option-value junction"
    url: "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/order/service/workflow/service/OrderItemRequestValidationServiceImpl.java"
    quote: "matchingSku = catalogService.findSkuById(possibleSkuIds.iterator().next());"
    quoted_at: "2026-06-24"
  - source_type: external
    citation: "Broadleaf Commerce (develop-7.0.x) DateUtil.isActive — the canonical active-date-window semantics absorbed for purchasability: active iff a non-null start has passed and a (nullable) end has not, i.e. now ∈ [start, end)"
    url: "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/common/src/main/java/org/broadleafcommerce/common/util/DateUtil.java"
    quote: "return !(startDate == null || startDate.getTime() >= date || (endDate != null && endDate.getTime() < date));"
    quoted_at: "2026-06-24"
  - source_type: external
    citation: "schema.org/Offer — the standard Product/Offer vocabulary the catalog hooks mirror: an Offer carries a price and an availability status with an availability window (availabilityStarts/availabilityEnds)"
    url: "https://schema.org/Offer"
    quote: "The availability of this item—for example In stock, Out of stock, Pre-order, etc."
    quoted_at: "2026-06-24"
  - source_type: external
    citation: "Broadleaf Commerce (develop-7.0.x) InventoryType — the tri-state inventory POLICY enum absorbed (CAT-INVENTORY-GATE-001): a SKU's availability mode is one of CHECK_QUANTITY / ALWAYS_AVAILABLE / UNAVAILABLE, gating purchasability before any quantity arithmetic"
    url: "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/inventory/service/type/InventoryType.java"
    quote: "public static final InventoryType CHECK_QUANTITY = new InventoryType(\"CHECK_QUANTITY\", \"Check Quantity\");"
    quoted_at: "2026-06-25"
  - source_type: external
    citation: "Broadleaf Commerce (develop-7.0.x) InventoryServiceImpl.checkBasicAvailablility — the UNAVAILABLE hard-block gate absorbed: an UNAVAILABLE SKU is not available regardless of stock, evaluated before any quantity check"
    url: "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/inventory/service/InventoryServiceImpl.java"
    quote: "if (sku.isActive() && !InventoryType.UNAVAILABLE.equals(sku.getInventoryType())) {"
    quoted_at: "2026-06-25"
---

## Rule

A **variant-product catalog** (apparel size/color, electronics configuration, grocery modifiers, marketplace listings) must be modeled so that listing data cannot encode the two defects that surface only at checkout — an *ambiguous* variant and a *priceless* one — and so that a non-purchasable item cannot be bought. Concretely:

1. **Exactly one default SKU per product.** Every product has a non-null default SKU (create rejects 422 if absent); it is the base of the variant model and the read-time fallback for a variant's null name/price/dates. A null default NPEs every fallback path.
2. **Deterministic exact-match variant resolution.** A chosen sku-generating option-value set resolves to **exactly one** active SKU. Each SKU stores an `option_signature` (the sorted concatenation of its sku-generating option-value ids); the table carries `UNIQUE(product_id, option_signature)` so a duplicate signature is **unrepresentable** (create → 409, DB-backstopped under `ddl-auto`), and `resolveSku` computes the requested signature and does an equality lookup → exactly one SKU, or 404. This **replaces** Broadleaf's `possibleSkuIds.iterator().next()` arbitrary pick (the absorbed gap).
3. **Purchasability gated on window + archival, at the cart path.** `assertPurchasable(sku)` succeeds only when `now ∈ [active_start, active_end)` (via an **injected `Clock`**) AND the product is active AND not archived. The gate is invoked on the *purchase* path, not only at display — a sunset/not-yet-launched/archived SKU returns 409 even though the row exists.
4. **Price-presence at the catalog boundary.** An active sellable SKU must resolve a non-null price (own `retail_price` or the inherited default's), else 422 — caught where the data lives, not deferred to checkout. (`sale_price <= retail_price`, DB `@Check`.) Discount/tax/dynamic pricing belong to the pricing vertical.
5. **Acyclic category graph; immutable identity.** Category edges that would close a cycle are rejected (409). `Sku.product_id`/`is_default` and the option-value xref columns are `@Column(updatable=false)`; `@Version` guards concurrent variant edits.
6. **Tri-state inventory-type policy gate (CAT-INVENTORY-GATE-001).** Each SKU carries an `inventory_type` POLICY flag ∈ {`UNAVAILABLE`, `ALWAYS_AVAILABLE`, `CHECK_QUANTITY`} consulted by `assertPurchasable` *before* any quantity arithmetic: `UNAVAILABLE` → never purchasable regardless of any stock; `ALWAYS_AVAILABLE` (or null) → purchasable without consulting quantity (a service/digital/made-to-order SKU); `CHECK_QUANTITY` → the quantity check is **deferred** to the inventory-reservation vertical (`two-axis-inventory-reservation-l0` owns `available ≥ q`). The catalog carries ONLY the policy flag — **no quantity/stock field** (that axis stays the inventory vertical's; the inventory decrement/increment there is a re-find of `INVRES-COMMIT/RELEASE`, not re-absorbed). This lifts the former blanket inventory deferral to a per-SKU policy gate while preserving the quantity deferral.

**Correct — exact-signature resolution + window/price gates (strengthens the absorbed Broadleaf pattern):**

```java
// backend/.../commercecatalog/CatalogProductService.java
public Sku resolveSku(UUID productId, Map<String, String> chosenOptions) {
    CatalogProduct p = products.findById(productId).orElseThrow(CatalogException::notFound);
    String signature = optionSignature(p, chosenOptions);          // sorted-join of sku-generating value ids
    Sku sku = skus.findByProductIdAndOptionSignature(productId, signature)  // EXACT match — never iterator().next()
        .orElseThrow(CatalogException::noMatchingSku);             // 404, never a silent first-of-many pick
    assertPurchasable(sku);                                        // re-gate at the purchase path
    return sku;
}

void assertPurchasable(Sku sku) {
    Instant now = clock.instant();                                 // injected Clock — deterministic + testable
    CatalogProduct p = products.findById(sku.getProductId()).orElseThrow(CatalogException::notFound);
    boolean inWindow = (sku.getActiveStartDate() == null || !now.isBefore(sku.getActiveStartDate()))
        && (sku.getActiveEndDate() == null || now.isBefore(sku.getActiveEndDate()));   // now ∈ [start, end)
    if (sku.getInventoryType() == InventoryType.UNAVAILABLE) {     // tri-state policy gate, BEFORE any quantity
        throw CatalogException.skuNotPurchasable();                // UNAVAILABLE → never buyable, regardless of stock
    }
    if (!inWindow || p.isArchived() || !p.isActive()) {
        throw CatalogException.skuNotPurchasable();                // 409 — row exists but not buyable
    }
    if (resolvePrice(sku, p) == null) throw CatalogException.priceRequired();   // 422 — no listed-but-priceless
    // ALWAYS_AVAILABLE/null → purchasable here; CHECK_QUANTITY → quantity check deferred to inventory-reservation
}
// Sku: UNIQUE(product_id, option_signature); @Column(updatable=false) product_id, is_default; @Version version
```

**Incorrect — Broadleaf's arbitrary pick + nullable price (the defects this rule absorbs-and-strengthens):**

```java
// ambiguous option set silently resolves to whichever SKU iterates first; sellable SKU may be priceless
if (CollectionUtils.isNotEmpty(possibleSkuIds)) {
    matchingSku = catalogService.findSkuById(possibleSkuIds.iterator().next());  // WRONG: non-deterministic pick
}
return matchingSku;                                              // WRONG: no window/archival re-gate at cart path
// WRONG: retailPrice is nullable, no UNIQUE on the option-value junction, no price-presence gate
```

Two "Red/Large" SKUs with different prices make the customer's charge a coin-flip and decrement the wrong inventory; a sellable SKU with `getPrice() == null` becomes a zero-price order downstream. The catalog boundary is where both must be made unrepresentable.

Reference: [Broadleaf OrderItemRequestValidationServiceImpl (variant resolution + ambiguity gap)](https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/order/service/workflow/service/OrderItemRequestValidationServiceImpl.java)

Reference: [Broadleaf DateUtil.isActive (active-date-window semantics)](https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/common/src/main/java/org/broadleafcommerce/common/util/DateUtil.java)

Reference: [schema.org/Offer (Product/Offer price + availability vocabulary)](https://schema.org/Offer)
