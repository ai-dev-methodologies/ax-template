---
title: A rounded total split across N buckets MUST be allocated so the parts sum back to the total exactly — never round each part independently
impact: HIGH
impactDescription: "Independently rounding each share of one rounded total loses or creates a residual unit (a 'penny'); the N parts no longer sum to the original and value is silently destroyed or conjured on every proration, fee split, tax allocation, or FX fan-out"
tags:
  - lang
  - precision
  - bigdecimal
  - allocation
  - conservation
  - largest-remainder
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-LANG-005"
verification:
  type: review
  source: "practices/rules/rounded-split-conserves-total-largest-remainder.md (Correct example) + siblings practices/rules/lang-bigdecimal-for-money.md and practices/rules/lang-bigdecimal-for-measured-decimals.md"
  pattern: "Any code that divides ONE rounded total across N buckets (proration, fee distribution, revenue/cost share, tax allocation, FX fan-out, installment split) computes the parts with a conserving allocation — round every part DOWN to the target scale, sum the rounded-down parts, then distribute the leftover minor units one-by-one to the buckets with the largest fractional remainders (largest-remainder method) OR delegates to Money.allocate(); a post-condition asserts sum(parts) == total exactly. The anti-pattern — calling setScale(scale, RoundingMode.HALF_UP) (or .divide(n, scale, mode)) on each share independently and summing the results — is rejected because the residual is unconserved."
upstream:
  - "https://en.wikipedia.org/wiki/Largest_remainder_method"
  - "https://martinfowler.com/eaaCatalog/money.html"
  - "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html"
evidence:
  - source_type: external
    citation: "Wikipedia — Largest remainder method (apportionment: integer floor allocation, then distribute leftover units to the largest fractional remainders)"
    url: "https://en.wikipedia.org/wiki/Largest_remainder_method"
    quote: "To apportion these seats, the parties are then ranked on the basis of their fractional remainders, and the parties with the largest remainders are each allocated one additional seat until all seats have been allocated."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "Martin Fowler — Patterns of Enterprise Application Architecture, Money pattern (the rounding hazard that a conserving allocate() exists to prevent)"
    url: "https://martinfowler.com/eaaCatalog/money.html"
    quote: "The more subtle problem is with rounding. Monetary calculations are often rounded to the smallest currency unit. When you do this it's easy to lose pennies (or your local equivalent) because of rounding errors."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "java.math.BigDecimal — Java SE 21 API documentation (unscaled value + scale; setScale + RoundingMode.FLOOR is the per-bucket round-down primitive the allocation builds on)"
    url: "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html"
    quote: "A BigDecimal consists of an arbitrary precision integer unscaled value and a 32-bit integer scale. If the scale is zero or positive, the scale is the number of digits to the right of the decimal point."
    quoted_at: "2026-06-01"
---

## A rounded total split across N buckets MUST be allocated so the parts sum back to the total exactly — never round each part independently

**Impact: HIGH — independently rounding each share of one rounded total loses or creates a residual unit; the parts stop summing to the total and value is silently destroyed or conjured**

The sibling rule `lang-bigdecimal-for-money.md` governs how a *single* amount is **represented** (use `BigDecimal`, or a committed `long` minor-units model — never `double`). The sibling rule `lang-bigdecimal-for-measured-decimals.md` governs how a *single* aggregated non-money decimal is **scaled and compared** at an aggregation boundary. This rule governs a third, orthogonal correctness property that neither of those covers: **conservation across a split**. When you take ONE total — itself already exact at its target scale — and divide it across N buckets, the sum of the N rounded parts MUST equal the original total *exactly*. This is a property of the *division*, not of any single number's type or scale, which is why it needs its own rule.

The failure is mechanical, not a floating-point artifact (it happens even with perfect `BigDecimal` arithmetic). Split `$100.00` three ways. The exact share is `33.3333…`; rounded to cents each part becomes `33.33`; `33.33 × 3 = 99.99`. One cent has vanished. Split a `100원`-scale total or a 12-month installment plan and the same residual appears with the opposite sign just as easily (`33.34 × 3 = 100.02` if you round half-up). Over a reconciliation run of thousands of prorations, fee distributions, revenue/cost shares, tax allocations, FX fan-outs, or installment splits, these stray units accumulate into a drift that breaks the books — exactly the hazard the Money pattern was built to stop: *Monetary calculations are often rounded to the smallest currency unit. When you do this it's easy to lose pennies (or your local equivalent) because of rounding errors.*

The canonical fix is a **conserving allocation** — the apportionment algorithm political science calls the **largest-remainder method**. Round every bucket's exact share *down* to the target scale (`RoundingMode.FLOOR`), which can only ever leave a non-negative leftover of whole minor units; compute that leftover as `total − Σ floor(share)`; then hand the leftover units out one at a time, in rank order of the fractional remainders that the floor discarded — *the parties are then ranked on the basis of their fractional remainders, and the parties with the largest remainders are each allocated one additional seat until all seats have been allocated.* Because exactly `leftover` units are added back, `Σ parts == total` is guaranteed by construction, and the rounding residual lands on the buckets that were closest to the next unit (the least-arbitrary place to put it). Martin Fowler's `Money.allocate(...)` is the off-the-shelf embodiment of this for currency; prefer it over hand-rolling when a `Money` type is available.

**Incorrect — each share rounded independently; the parts do not sum back to the total:**

```java
// Split one rounded total across N buckets by rounding each share on its own.
List<BigDecimal> split(BigDecimal total, int n) {
    BigDecimal share = total.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
    List<BigDecimal> parts = new ArrayList<>();
    for (int i = 0; i < n; i++) parts.add(share);   // each part rounded the same way
    return parts;
}
// split(new BigDecimal("100.00"), 3)
//   → [33.33, 33.33, 33.33], sum = 99.99  ❌  one cent destroyed
// HALF_UP on 33.3333 → 33.33; the discarded 0.0033 × 3 is simply lost.
// The same shape over fee shares / tax allocation / FX fan-out drifts the ledger.
```

**Correct — largest-remainder allocation; the parts are conserved to sum exactly to the total:**

```java
static final RoundingMode FLOOR = RoundingMode.FLOOR;

/** Split `total` across `weights` so the parts sum back to `total` exactly. */
List<BigDecimal> allocate(BigDecimal total, List<BigDecimal> weights, int scale) {
    BigDecimal unit = BigDecimal.ONE.movePointLeft(scale);          // smallest unit, e.g. 0.01
    BigDecimal weightSum = weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

    // 1. Floor every share to the target scale (never over-allocates).
    List<BigDecimal> parts = new ArrayList<>();
    BigDecimal allocated = BigDecimal.ZERO;
    for (BigDecimal w : weights) {
        BigDecimal exact = total.multiply(w).divide(weightSum, scale + 10, FLOOR);
        BigDecimal floored = exact.setScale(scale, FLOOR);
        parts.add(floored);
        allocated = allocated.add(floored);
    }

    // 2. Leftover = total − Σ floored, in whole minor units (always >= 0).
    long leftover = total.subtract(allocated).divide(unit).longValueExact();

    // 3. Hand the leftover units to the largest fractional remainders, one each.
    List<Integer> order = remainderRankDescending(total, weights, weightSum, scale);
    for (int k = 0; k < leftover; k++) {
        int b = order.get(k);
        parts.set(b, parts.get(b).add(unit));
    }
    return parts;                                                   // Σ parts == total, exactly
}
// allocate(new BigDecimal("100.00"), List.of(ONE, ONE, ONE), 2)
//   → [33.34, 33.33, 33.33], sum = 100.00  ✅  the stray cent is conserved, not lost
//
// When a Money type is on the classpath, prefer it instead of hand-rolling:
//   Money[] parts = total.allocate(new long[]{1, 1, 1});  // Fowler Money.allocate — same invariant
```

This is **distinct** from the money-representation rule (which only decides the *type* of one amount) and from the measured-decimal rule (which only decides the *scale and compareTo* of one aggregate). A program can obey both of those and still leak a penny on every split. The marker that this rule applies is the shape *one rounded total → N rounded parts*: proration of a charge over a period, distribution of a fee across line items, revenue/cost share across partners, tax allocation across positions, FX conversion fanned out across sub-accounts, or an amount split into installments. Wherever that shape appears, the parts MUST be produced by a conserving allocation and a post-condition MUST assert `sum(parts).compareTo(total) == 0`.

Verification (review-tier): inspect every site that divides a single rounded total across multiple buckets. Confirm it does NOT round each share independently (`setScale(..., HALF_UP)` per part, or `divide(n, scale, mode)` reused for every bucket) and then sum; confirm it instead floors every share, computes the integer leftover as `total − Σ floor`, and distributes the leftover units to the largest fractional remainders (largest-remainder method) or delegates to `Money.allocate(...)`. The canonical regression test a fork-receiver writes per split path feeds an indivisible total (e.g. `100.00 / 3`, `0.10 / 3`, a weighted `7 : 2 : 1` fee split) and asserts the returned parts sum back to the input total *exactly* (`compareTo == 0`) for both even and remainder-bearing divisions.

Reference: [Wikipedia — Largest remainder method](https://en.wikipedia.org/wiki/Largest_remainder_method)

Reference: [Martin Fowler — Money pattern (eaaCatalog)](https://martinfowler.com/eaaCatalog/money.html)

Reference (sibling — single-amount representation, do not conflate): [practices/rules/lang-bigdecimal-for-money.md](lang-bigdecimal-for-money.md)

Reference (sibling — single-aggregate scale/compare, do not conflate): [practices/rules/lang-bigdecimal-for-measured-decimals.md](lang-bigdecimal-for-measured-decimals.md)
