---
title: A pricing pipeline must run phases in a fixed deterministic order — discount BEFORE tax so each item's taxable base is its amount MINUS its prorated order discount (tax on the NET price, never the gross) — and must close the order total as a conserving sum of its disclosed components (total = subTotal − orderAdjustments + shipping + tax + fees) with no penny invented or lost
impact: HIGH
impactDescription: "Charging tax on the gross (pre-discount) line price over-taxes every discounted order — the customer pays tax on money they never spent, and in most jurisdictions a discount reduces the taxable amount, so gross-tax is also non-compliant; a non-deterministic phase order makes the same cart total differently across nodes; and an order total that is not the exact conserving sum of its disclosed parts (sub-total, discount, shipping, tax, fees) — a penny lost between phases or a tax double-counted in both a fulfillment-group total and the order total — produces an unauditable receipt and a reconciliation break at settlement"
tags:
  - e-commerce
  - pricing
  - money
  - determinism
  - tax
spec_ref: "specs/pricing-l0.yaml#PRICING-ORDER-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/commercepricing/PricingPipeline.java"
  pattern: "The pipeline computes phases in a fixed method-body order — apply discount, finalize each item's taxable base as itemAmount minus its prorated order discount, compute tax as taxableBase × rate (round-once), then total = subTotal − orderDiscount + shipping + tax; tax is never computed on the gross line price; money is integer minor units with Math.multiplyExact and a single round; the order total equals subTotal − orderAdjustments + shipping + tax + fees with no penny invented or dropped, and the same total is reconstructible from its disclosed components"
upstream:
  - "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/pricing/service/workflow/FulfillmentItemPricingActivity.java"
  - "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/pricing/service/workflow/TotalActivity.java"
  - "https://martinfowler.com/eaaCatalog/money.html"
evidence:
  - source_type: external
    citation: "Broadleaf Commerce (develop-7.0.x) FulfillmentItemPricingActivity — the discount→tax link absorbed: each item's taxable amount is set to the item amount MINUS its prorated order-level discount, so the downstream tax phase reads the NET (post-discount) base"
    url: "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/pricing/service/workflow/FulfillmentItemPricingActivity.java"
    quote: "fgItem.setTotalItemTaxableAmount(fgItem.getTotalItemAmount().subtract(proratedOrderAdjAmt));"
    quoted_at: "2026-06-24"
  - source_type: external
    citation: "Broadleaf Commerce (develop-7.0.x) TotalActivity — the order-total conserving closure absorbed: the total accumulates the sub-total, subtracts the order adjustments, then adds shipping/tax/fees at a single point"
    url: "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/pricing/service/workflow/TotalActivity.java"
    quote: "total = total.subtract(order.getOrderAdjustmentsValue());"
    quoted_at: "2026-06-24"
  - source_type: external
    citation: "Martin Fowler — Patterns of Enterprise Application Architecture, Money pattern (the penny-conservation rounding hazard the order-total closure must avoid)"
    url: "https://martinfowler.com/eaaCatalog/money.html"
    quote: "The more subtle problem is with rounding. Monetary calculations are often rounded to the smallest currency unit. When you do this it's easy to lose pennies (or your local equivalent) because of rounding errors."
    quoted_at: "2026-06-01"
---

## Rule

A **pricing pipeline** sits above the discount engine and is responsible for two correctness properties a discount engine alone never establishes: the **order of phases** and the **closure of the total**.

1. **Fixed phase order — discount before tax.** Phases run in a deterministic order: apply discount → finalize each item's taxable base → tax → total. Each item's taxable base is `itemAmount − proratedOrderDiscount`, so the tax phase reads the **net** (post-discount) amount. Tax is **never** computed on the gross line price — that over-taxes the customer (and in most jurisdictions a discount reduces the taxable amount, so gross-tax is non-compliant). The order is code-pinned (the method-body order, mirroring Broadleaf's `setOrder()` activity constants) so the same cart resolves identically across runs and nodes.
2. **Conserving total closure.** The order total is `subTotal − orderAdjustments + shipping + tax + fees`, accumulated at a single point with no penny invented or dropped, and the per-fulfillment-group total reconciles to the same components. The displayed total must equal the sum of its disclosed parts exactly — an auditable receipt. This conserves *across* the order-level phases (distinct from per-offer proration, which conserves *within* one discount).

Money is integer minor units throughout, with `Math.multiplyExact` for the tax multiply and a single round (penny-conserving).

**Correct — discount before tax (net base) + conserving order-total closure:**

```java
// backend/.../commercepricing/PricingPipeline.java — phase order IS the determinism source
public PricedOrder priceOrder(List<Line> lines, long orderDiscount, long shipping, int taxBasisPoints) {
    long subTotal = lines.stream().mapToLong(Line::amount).sum();
    long[] prorated = prorate(orderDiscount, lines, subTotal);   // discount FIRST (conserving — promotion engine)
    long totalTax = 0;
    List<PricedLine> priced = new ArrayList<>();
    for (int i = 0; i < lines.size(); i++) {
        long taxable = lines.get(i).amount() - prorated[i];      // taxable base = item amount − prorated discount (NET)
        long tax = Math.multiplyExact(taxable, taxBasisPoints) / 10_000;  // round-once; tax on the NET base, never gross
        totalTax += tax;
        priced.add(new PricedLine(lines.get(i).amount(), prorated[i], taxable, tax));
    }
    long total = subTotal - orderDiscount + shipping + totalTax;  // conserving closure of the disclosed components
    return new PricedOrder(subTotal, orderDiscount, shipping, totalTax, total, priced);
}
```

**Incorrect — tax on the gross price, totals not closed:**

```java
// tax computed on the GROSS line price (before discount) → over-taxes; total not reconstructible from parts
long tax = Math.multiplyExact(line.amount(), taxBasisPoints) / 10_000;  // WRONG: gross base, ignores the discount
long total = subTotal + tax;                                            // WRONG: discount never subtracted; no closure
```

Taxing the gross price charges the customer tax on the discount they were given; a total that is not `subTotal − discount + shipping + tax` cannot be reconstructed from the receipt's disclosed lines. Ordering discount before tax over the net base, and closing the total as the exact sum of its parts, make both defects unrepresentable.

Reference: [Broadleaf FulfillmentItemPricingActivity (taxable base = item − prorated discount)](https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/pricing/service/workflow/FulfillmentItemPricingActivity.java)

Reference: [Broadleaf TotalActivity (order-total conserving closure)](https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/pricing/service/workflow/TotalActivity.java)

Reference: [Martin Fowler — Money pattern (penny conservation)](https://martinfowler.com/eaaCatalog/money.html)
