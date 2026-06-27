---
title: A composite item (bundle / kit) priced as a CONSERVING roll-up of its children — in ITEM_SUM mode its price is Σ over children of (child.unitPrice × child.quantity) + Σ(bundle fees), for retail / sale / taxable (sale falling back to a child's retail when the child has no sale price); in BUNDLE mode its price is a FIXED base price NOT summed from children — with taxability DERIVED from the children and NO independently-settable rolled-up total column, so a non-conserving composite total is unrepresentable (the COMPOSITION direction, the dual of banded/promotion decomposition)
impact: MEDIUM
impactDescription: "A retail kit, a meal combo, a device+plan telco bundle, a BOM/assembly, a subscription add-on bundle — any composite item whose price is meant to be the roll-up of its parts silently corrupts when the roll-up is stored as a hand-settable total that drifts from the children: a kit shows $37.00 while its parts sum to $41.50 (the displayed bundle price is a lie that no structural check catches — it is non-null and positive), a child is dropped or double-counted, a non-taxable child leaks into the taxable base, or a fixed-price BUNDLE is accidentally summed from its parts (or a summed kit accidentally fixed-priced). Computing the composite price as a PURE derivation of the immutable children + fees (ITEM_SUM) or the immutable fixed base (BUNDLE) on every read — with NO stored total column to hand-edit and taxability DERIVED from the children — makes each drift unrepresentable. This is the COMPOSITION direction the catalog's pricing specs do not cover: banded-pricing segments ONE quantity DOWN across bands and promotion prorates ONE discount DOWN to lines (both decomposition: one → many, Σ parts == the one); a bundle composes MANY children UP into one conserving total (many → one, the total == Σ children + fees). It is also distinct from pricing-pipeline (orders a single line's discount→tax→total) — a bundle's roll-up is across the CHILDREN of one composite line, an allocation-of-transaction-price identity (ASC 606 / IFRS 15)."
tags:
  - pricing
  - aggregate
  - conservation
  - composition
spec_ref: "specs/bundle-pricing-l0.yaml#BUNDLE-ITEMSUM-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/bundlepricing/CompositeItem.java + backend/src/main/java/com/ax/template/authblueprint/bundlepricing/BundlePricingService.java"
  pattern: "A composite item carries its children as immutable @AggregateMember rows (qty + unitRetailPrice + optional unitSalePrice + taxable, all @Column(updatable=false), no public setter) and its bundle fees as an immutable @ElementCollection (amount + taxable); it carries NO stored rolled-up total column. The price is a PURE derivation method on the aggregate, recomputed on every read: in ITEM_SUM mode retailPrice = Σ child.unitRetailPrice×qty + Σ fee.amount, salePrice = Σ (child.unitSalePrice ?? child.unitRetailPrice)×qty + Σ fee.amount, taxablePrice = Σ over taxable children of unitRetailPrice×qty + Σ over taxable fees of amount; in BUNDLE mode retailPrice = the immutable baseRetailPrice (NOT summed), salePrice = baseSalePrice ?? baseRetailPrice. Bundle taxability is DERIVED (taxable iff any child or fee is taxable) — no stored boolean. A @Check makes the mode/base-price shape exclusive (BUNDLE ⇒ base NOT NULL, ITEM_SUM ⇒ base NULL). The stored composite total can never contradict its children because there is no stored total — Σ of the disclosed per-child subtotals + fees reconstructs the authoritative total exactly; arithmetic is exact integer minor units."
upstream:
  - "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/order/domain/BundleOrderItemImpl.java"
  - "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/catalog/service/type/ProductBundlePricingModelType.java"
  - "https://www.ifrs.org/issued-standards/list-of-standards/ifrs-15-revenue-from-contracts-with-customers/"
  - "https://martinfowler.com/eaaCatalog/money.html"
evidence:
  - source_type: external
    citation: "Broadleaf Commerce (develop-7.0.x) BundleOrderItemImpl.getRetailPrice — the ITEM_SUM retail roll-up absorbed: the bundle retail price accumulates each child's retail price × quantity (then adds the bundle fees). Quoted single-line (Fair Use License v1.0 — independent reimplementation, no port)"
    url: "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/order/domain/BundleOrderItemImpl.java"
    quote: "BigDecimal quantityPrice = itemRetailPrice.multiply(new BigDecimal(discreteOrderItem.getQuantity()));"
    quoted_at: "2026-06-27"
  - source_type: external
    citation: "Broadleaf Commerce (develop-7.0.x) BundleOrderItemImpl.shouldSumItems — the mode predicate absorbed: the children are summed only when the product bundle's pricing model is ITEM_SUM; otherwise the fixed base price is used. Quoted single-line (Fair Use License v1.0)"
    url: "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/order/domain/BundleOrderItemImpl.java"
    quote: "return ProductBundlePricingModelType.ITEM_SUM.equals(productBundle.getPricingModel());"
    quoted_at: "2026-06-27"
  - source_type: external
    citation: "Broadleaf Commerce (develop-7.0.x) ProductBundlePricingModelType — the two pricing modes absorbed: ITEM_SUM (sum the children) and BUNDLE (fixed base price). Quoted single-line (Fair Use License v1.0)"
    url: "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/catalog/service/type/ProductBundlePricingModelType.java"
    quote: "public static final ProductBundlePricingModelType ITEM_SUM = new ProductBundlePricingModelType(\"ITEM_SUM\", \"Item Sum\");"
    quoted_at: "2026-06-27"
  - source_type: external
    citation: "IFRS 15 Revenue from Contracts with Customers (¶73-86, Allocating the transaction price to performance obligations; mirrored by FASB ASC 606-10-32) — the conservation standard: the transaction price is allocated across a contract's distinct performance obligations so the amounts allocated to the components sum to the total transaction price. The bundle roll-up is exactly this allocation identity (Σ allocated to components == total)"
    url: "https://www.ifrs.org/issued-standards/list-of-standards/ifrs-15-revenue-from-contracts-with-customers/"
    quote: "Allocating the transaction price to performance obligations"
    quoted_at: "2026-06-27"
  - source_type: external
    citation: "Martin Fowler, Patterns of Enterprise Application Architecture — Money pattern: monetary amounts are integer minor units and rounded once; independently-rounded parts lose pennies. The composite roll-up sums exact integer minor units and rounds once"
    url: "https://martinfowler.com/eaaCatalog/money.html"
    quote: "it's easy to lose pennies (or your local equivalent) because of rounding errors"
    quoted_at: "2026-06-27"
---

## Rule

A **composite item** — a retail kit, a meal combo, a device+plan bundle, a BOM/assembly, a subscription add-on bundle — is an item whose price is meant to be the **roll-up of its parts**. Its hazard is the same as any denormalized aggregate: a stored composite total **drifting** from the children it claims to summarize. This rule absorbs the Broadleaf `BundleOrderItemImpl` roll-up: the composite price is a **pure derivation** of its children, in one of two modes, with **no stored total to hand-edit**.

It is the **COMPOSITION** direction — the dual of the catalog's decomposition pricing specs. `banded-pricing-l0` segments **one** quantity **down** across bands; `promotion-l0` prorates **one** discount **down** to lines — both are one → many (`Σ parts == the one`). A bundle composes **many** children **up** into one conserving total (`total == Σ children + fees`). It is also distinct from `pricing-pipeline` (which orders a single line's discount → tax → total): a bundle's roll-up is **across the children of one composite line**, the allocation-of-transaction-price identity of **ASC 606 / IFRS 15** (the amounts allocated to a bundle's distinct components sum to the total). Three obligations:

1. **ITEM_SUM = conserving roll-up (BUNDLE-ITEMSUM-001).** In `ITEM_SUM` mode the composite price MUST be `Σ over children of (child.unitPrice × child.quantity) + Σ over bundle fees of fee.amount`, computed independently for **retail**, **sale**, and **taxable** prices. The **sale** roll-up falls back to a child's **retail** price when that child has no sale price (per Broadleaf `getSalePrice`). The **taxable** roll-up sums only the **taxable** children and **taxable** fees. Every child unit is counted exactly once at its own `unitPrice × quantity` — Σ of the disclosed per-child subtotals + fees reconstructs the total exactly. Arithmetic is exact integer minor units (round once — the Money pattern).
2. **BUNDLE = fixed base price (BUNDLE-FIXED-001).** In `BUNDLE` mode the composite price MUST be its **fixed base price** (`baseRetailPrice`, and `baseSalePrice` when present else `baseRetailPrice`) — **NOT** summed from the children, even when `Σ children` would differ. The base price is immutable (`@Column(updatable=false)`, no public setter). The mode is **representationally exclusive**: a `@Check` enforces `BUNDLE ⇒ base price NOT NULL` and `ITEM_SUM ⇒ base price NULL`, so the engine can never accidentally sum a fixed-price bundle or fix-price a summed one.
3. **Taxability derived; no stored total (BUNDLE-DERIVED-001).** The bundle's **taxability** MUST be DERIVED from its children (taxable iff at least one child or fee is taxable) — never an independently-settable boolean. Likewise the conserving total MUST NOT be stored as a hand-settable aggregate column: the composite stores **only** the immutable children + fees (ITEM_SUM) or the immutable fixed base (BUNDLE), **never a rolled-up total**. A composite total that contradicts its children is therefore **unrepresentable** — there is no place to store the lie.

**Correct — pure derivation over immutable children, no stored total, mode-exclusive base:**

```java
// CompositeItem.java — derives its price; NO stored total column, no public price setter
BundlePricing priceRollUp() {
    if (pricingModel == BundlePricingModel.BUNDLE) {                       // fixed base, NOT summed
        long retail = baseRetailPrice;
        long sale = (baseSalePrice != null) ? baseSalePrice : baseRetailPrice;
        boolean taxable = derivedTaxable();                                // derived from children
        return new BundlePricing(retail, sale, taxable ? retail : 0L, taxable);
    }
    long retail = 0, sale = 0, taxablePrice = 0;
    for (CompositeComponent c : components) {                              // ITEM_SUM: Σ child×qty
        retail += c.getUnitRetailPrice() * c.getQuantity();
        long unitSale = (c.getUnitSalePrice() != null) ? c.getUnitSalePrice() : c.getUnitRetailPrice();
        sale += unitSale * c.getQuantity();
        if (c.isTaxable()) taxablePrice += c.getUnitRetailPrice() * c.getQuantity();
    }
    for (BundleFee f : fees) {                                             // + Σ fees
        retail += f.getAmount();
        sale += f.getAmount();
        if (f.isTaxable()) taxablePrice += f.getAmount();
    }
    return new BundlePricing(retail, sale, taxablePrice, derivedTaxable());
}
```

**Incorrect — stored hand-settable total / summed BUNDLE / hand-set taxability:**

```java
composite.setTotalPrice(req.total());        // WRONG: a stored total drifts from the children — no derivation
long retail = sumChildren(children);          // WRONG in BUNDLE mode: a fixed-price bundle must NOT be summed
composite.setTaxable(req.taxable());          // WRONG: taxability must be DERIVED from the children, not hand-set
```

A stored composite total is a number that lies about its parts; summing a fixed-price `BUNDLE` (or fixing the price of an `ITEM_SUM` kit) prices the wrong way; a hand-set taxability contradicts the children. Deriving the price from the immutable children + fees on every read, with no stored total, a mode-exclusive `@Check`, and derived taxability, makes each defect unrepresentable.

Reference: [Broadleaf BundleOrderItemImpl (ITEM_SUM roll-up of children × quantity + fees; BUNDLE fixed base)](https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/order/domain/BundleOrderItemImpl.java)

Reference: [Broadleaf ProductBundlePricingModelType (ITEM_SUM / BUNDLE modes)](https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/catalog/service/type/ProductBundlePricingModelType.java)

Reference: [IFRS 15 — Revenue from Contracts with Customers (allocate the transaction price to the components; Σ allocated == total)](https://www.ifrs.org/issued-standards/list-of-standards/ifrs-15-revenue-from-contracts-with-customers/)

Reference: [Martin Fowler — Money pattern (integer minor units; round once)](https://martinfowler.com/eaaCatalog/money.html)
