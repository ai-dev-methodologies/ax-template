---
title: Tiered / marginal / time-of-use pricing must segment the quantity across half-open bands and charge each portion at its own band's rate (charge = Σ qty-in-band × rate) — the bands tile [0,∞) with no gap/overlap, every unit is charged exactly once, and the total is rounded ONCE — never a single blended rate, never a sum of independently-rounded per-band charges
impact: HIGH
impactDescription: "Applying one rate to the whole quantity (instead of marginal banding) over- or under-charges every tiered bill; bands that don't tile the axis (a gap, an overlap, or a bounded top band) leave units un-priced or double-priced; summing independently-rounded per-band charges loses pennies on every bill (the Money-pattern hazard)"
tags:
  - bigdecimal
  - allocation
  - conservation
  - pricing
spec_ref: "specs/banded-pricing-l0.yaml#BAND-SEGMENT-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/common/BandedPricer.java"
  pattern: "A pure function segments quantity Q across an ordered band set: for band i over [lo_i, hi_i) it computes qtyInBand = max(0, min(Q, hi_i) − lo_i) and charge = qtyInBand × rate_i in BigDecimal, summing to an exact total that is setScale(scale, HALF_UP) ONCE at the end (not a sum of per-band rounded charges); it validates that thresholds are strictly increasing and positive, exactly the last band is unbounded, and rates/Q are non-negative (rejecting otherwise); and it asserts Σ qtyInBand == Q (quantity conservation)"
upstream:
  - "https://www.cs.utexas.edu/~EWD/transcriptions/EWD08xx/EWD831.html"
  - "https://martinfowler.com/eaaCatalog/money.html"
evidence:
  - source_type: external
    citation: "E. W. Dijkstra, EWD831 'Why numbering should start at zero' (half-open intervals tile without gap or overlap)"
    url: "https://www.cs.utexas.edu/~EWD/transcriptions/EWD08xx/EWD831.html"
    quote: "two subsequences are adjacent means that the upper bound of the one equals the lower bound of the other"
    quoted_at: "2026-06-08"
  - source_type: external
    citation: "E. W. Dijkstra, EWD831 (preference for an excluded upper bound)"
    url: "https://www.cs.utexas.edu/~EWD/transcriptions/EWD08xx/EWD831.html"
    quote: "for the upper bound we prefer < as in a) and d)."
    quoted_at: "2026-06-08"
  - source_type: external
    citation: "Martin Fowler — Patterns of Enterprise Application Architecture, Money pattern (the penny-conservation rounding hazard)"
    url: "https://martinfowler.com/eaaCatalog/money.html"
    quote: "The more subtle problem is with rounding. Monetary calculations are often rounded to the smallest currency unit. When you do this it's easy to lose pennies (or your local equivalent) because of rounding errors."
    quoted_at: "2026-06-01"
---

## Tiered/marginal pricing segments the quantity across half-open bands and conserves

**Impact: HIGH — a single blended rate over- or under-charges every tiered bill; bands that don't tile the axis leave units un-priced or double-priced; summing independently-rounded per-band charges loses pennies on every bill.**

Marginal/tiered/time-of-use pricing charges *different portions of one quantity at different rates*: the first 10 GB at \$1, the next 10 at \$0.50, the rest at \$0.10. The charge is the **sum of per-band charges**, where each band prices only the quantity that falls in it:

```text
for band i over [lo_i, hi_i):
    qtyInBand_i = max(0, min(Q, hi_i) − lo_i)      // the part of Q inside this half-open band
    charge_i    = qtyInBand_i × rate_i
total = Σ charge_i                                  // exact; round ONCE
// invariants: Σ qtyInBand_i == Q  (every unit charged exactly once)
//             bands tile [0,∞): strictly increasing thresholds, last band unbounded
```

This is NOT the catalog's `ordered-waterfall` (which partitions one *amount* across capped tiers with no per-unit rate) nor `rounded-split` (which splits a *known total* so the parts sum to it by construction). Banding goes the other way: it *builds* the total from a quantity and a rate schedule.

Three defects recur, and one rule closes them.

**Defect 1 — a single blended rate.** Charging `Q × someRate` (or `Q × topBandRate`) instead of summing per-band charges over- or under-bills every customer whose quantity spans more than one band. Each unit must be charged at the rate of the band whose half-open interval contains it.

**Defect 2 — bands that don't tile the axis.** A gap between bands leaves a quantity range un-priced; an overlap double-charges it; a *bounded* top band leaves any quantity beyond it un-priced (a silent revenue hole). Bands must tile `[0, ∞)`: strictly increasing positive thresholds, each band the half-open interval `[prev, this)`, adjacent bands abutting (Dijkstra EWD831: *"two subsequences are adjacent means that the upper bound of the one equals the lower bound of the other"*), and exactly the last band unbounded. Use the half-open convention (*"for the upper bound we prefer <"*) so a quantity exactly at a threshold falls in the upper band, charged once.

**Defect 3 — summing independently-rounded per-band charges.** Rounding each band's charge to the cent and then summing loses pennies versus rounding the exact sum once — the Money pattern's hazard: *"it's easy to lose pennies … because of rounding errors."* Compute each `qtyInBand × rate` at full precision, sum, and round the **total** once.

**Incorrect — blended rate / sum-of-rounded / no tiling check:**

```java
BigDecimal price(BigDecimal q, List<Band> bands) {
    return q.multiply(bands.get(bands.size()-1).rate());     // ❌ DEFECT 1: one rate on the whole quantity
}
```

**Correct — per-band segmentation, half-open tiling, round once:**

```java
static Result price(BigDecimal q, List<Band> bands, int scale) {
    requireTiling(bands);                                    // ✅ DEFECT 2: strictly increasing, last unbounded
    BigDecimal lo = ZERO, total = ZERO, charged = ZERO;
    for (Band b : bands) {
        BigDecimal hi = b.upperExclusive() == null ? q.max(lo) : b.upperExclusive();   // ∞ for the last band
        BigDecimal qtyInBand = q.min(hi).subtract(lo).max(ZERO);   // ✅ DEFECT 1: clamp to [lo,hi)
        total = total.add(qtyInBand.multiply(b.rate()));    // exact, no per-band rounding
        charged = charged.add(qtyInBand);
        lo = hi;
    }
    assert charged.compareTo(q) == 0;                        // ✅ Σ qtyInBand == q (conservation)
    return new Result(total.setScale(scale, HALF_UP), charged, breakdown);   // ✅ DEFECT 3: round ONCE
}
```

Half-open `[lo, hi)` tiling means a quantity exactly on a threshold is charged in exactly one band; the strictly-increasing + unbounded-last validation guarantees full coverage of `[0, ∞)`; rounding the exact total once conserves money. The per-band breakdown is reported at full precision (its own rounding, if a fork-receiver needs per-line cents, composes `rounded-split` against the round-once total).

Verification: review-tier — confirm the pricer charges `qtyInBand × rate` per half-open band and sums (not a blended rate), validates strictly-increasing positive thresholds with exactly the last band unbounded (rejecting gaps/overlaps/bounded-top), computes in `BigDecimal`, asserts `Σ qtyInBand == Q`, and rounds the total once. The canonical proof a fork-receiver writes: `Q=25` over `[0,10)@1, [10,20)@0.5, [20,∞)@0.1` → `15.5`, with `Σ qtyInBand == 25`, plus a fractional fixture asserting round-once ≠ sum-of-rounded.

Reference: [Dijkstra EWD831 — Why numbering should start at zero](https://www.cs.utexas.edu/~EWD/transcriptions/EWD08xx/EWD831.html)

Reference: [Martin Fowler — Money pattern](https://martinfowler.com/eaaCatalog/money.html)
