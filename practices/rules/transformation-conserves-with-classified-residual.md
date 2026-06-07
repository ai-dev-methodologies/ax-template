---
title: A material transformation must conserve to an ACCOUNTED residual — Σ(input) == Σ(good output) + Σ(classified residual), per base unit, with every residual unit tagged to a governed disposition code — never net-zero, never an unexplained shrinkage
impact: HIGH
impactDescription: "Forcing a yield/backflush posting through the net-zero balanced-posting rule rejects it (422) and pushes the agent to either fake a balancing leg or skip the check; recording good output without accounting the scrap/yield-loss silently destroys inventory (unexplained shrinkage), and rounding the two sides independently leaks a unit on every batch"
tags:
  - conservation
  - bigdecimal
  - allocation
  - manufacturing
  - mass-balance
spec_ref: "specs/transformation-conservation-l0.yaml#XFORM-ACCOUNTED-LOSS-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/transformation/TransformationService.java + backend/src/main/java/com/ax/template/authblueprint/transformation/ConservationCheck.java"
  pattern: "Recording a transformation validates, per base unit, that Σ(input) == Σ(good output) + Σ(residual) as BigDecimal compareTo==0; every residual leg carries a disposition from a closed enum (no miscellaneous bucket); an imbalance -> 422 XFORM_NOT_CONSERVED, an unclassified residual -> rejected; legs are @Column(updatable=false); the whole record is one @Transactional; no net-zero assumption and no independently-rounded sides"
upstream:
  - "https://en.wikipedia.org/wiki/Mass_balance"
  - "https://martinfowler.com/eaaCatalog/money.html"
evidence:
  - source_type: external
    citation: "Wikipedia — Mass balance (material balance: an application of conservation of mass; non-reactive balance form)"
    url: "https://en.wikipedia.org/wiki/Mass_balance"
    quote: "The mass that enters a system must, by conservation of mass, either leave the system or accumulate within the system."
    quoted_at: "2026-06-07"
  - source_type: external
    citation: "Wikipedia — Mass balance (the general reactive balance form, generalizing conservation to accounted generation/consumption)"
    url: "https://en.wikipedia.org/wiki/Mass_balance"
    quote: "Input + Generation = Output + Accumulation + Consumption"
    quoted_at: "2026-06-07"
  - source_type: external
    citation: "Martin Fowler — Patterns of Enterprise Application Architecture, Money pattern (the penny-conservation rounding hazard)"
    url: "https://martinfowler.com/eaaCatalog/money.html"
    quote: "The more subtle problem is with rounding. Monetary calculations are often rounded to the smallest currency unit. When you do this it's easy to lose pennies (or your local equivalent) because of rounding errors."
    quoted_at: "2026-06-01"
---

## A material transformation must conserve to an ACCOUNTED residual, not to zero

**Impact: HIGH — net-zero is the wrong shape for a transformation; recording good output without accounting the scrap/yield-loss silently destroys inventory, and net-zero rejection pushes the agent to fake a balancing leg.**

This rule is the **conserve-with-classified-loss dual** of `value-transfer-must-be-balanced.md` / `balanced-posting-l0`. That contract governs a *transfer*: a value-moving operation must **mint nor destroy nothing** — the signed legs net to **exactly zero** (units leave warehouse A == units enter warehouse B). A *transformation* is the opposite shape: it **consumes** input of one material and **emits a different output**, and the residual is **deliberately non-zero** — flour + water become bread plus trim-scrap plus evaporation-loss. The conserved law here is the **mass balance**: *"The mass that enters a system must, by conservation of mass, either leave the system or accumulate within the system"* — generalized to the accounted form *"Input + Generation = Output + Accumulation + Consumption."* Applied to a stored operation:

```text
Σ(input)  ==  Σ(good output)  +  Σ(residual)        // per base unit, exact BigDecimal
                                  └─ residual = Σ over a CLOSED disposition enum
                                     {SCRAP, REWORK, YIELD_LOSS, WIP_REMAINDER}   (no "misc" bucket)
```

Three defects recur, and one rule closes them.

**Defect 1 — using net-zero (balanced-posting) for a transformation.** The legs of a yield posting do not net to zero (100 kg in, 90 kg good out — the 10 kg is real loss, not a missing credit leg). Run it through the net-zero contract and it is rejected `422 UNBALANCED`. The agent then either invents a phantom balancing leg (corrupting the books) or drops the conservation check entirely (silent shrinkage). The transformation needs its own sum-to-accounted-residual invariant.

**Defect 2 — recording good output without accounting the residual (silent shrinkage).** Persisting only `input` and `good output` and letting the difference vanish is the canonical inventory leak: 100 in, 90 good out, and 10 units of unexplained shrinkage that no report can attribute. Every lost unit MUST land in a **named** disposition category from a **closed** enum — there is no "miscellaneous" bucket — so `Σ(classified residual categories) == total residual`. A residual with no category is rejected.

**Defect 3 — rounding the two sides independently (penny leak per batch).** Computing `Σ(good)+Σ(residual)` with each side separately rounded breaks the exact equality — the Money pattern's *"it's easy to lose pennies … because of rounding errors,"* multiplied across thousands of batches into a drift that breaks the material ledger. Compare exact `BigDecimal` (`compareTo`), and derive nothing by independent rounding.

**Incorrect — net-zero rejects it, or good-only silently shrinks:**

```java
// ❌ A: forcing a transformation through the net-zero transfer contract
balancedPosting.assertNetsToZero(legs);   // 100 in + 90 out = -10 != 0 -> 422 UNBALANCED (wrong shape)

// ❌ B: record good output, let the loss vanish (silent shrinkage, no attribution)
run.setInput(in); run.setGoodOutput(good);   // 100 -> 90; the 10 is gone, attributable to nothing
repo.save(run);
```

**Correct — sum to a classified residual, exact, per base unit, atomic, immutable legs:**

```java
@Transactional
public TransformationRun record(List<Leg> inputs, List<Leg> goods, List<Leg> residuals) {
    ConservationCheck.requireSingleBaseUnit(inputs, goods, residuals);     // XFORM-DIMENSION-001
    for (Leg r : residuals) requireGovernedDisposition(r.disposition());   // closed enum, no "misc"
    BigDecimal in       = sum(inputs);
    BigDecimal good     = sum(goods);
    BigDecimal residual = sum(residuals);
    if (in.compareTo(good.add(residual)) != 0) {                            // ✅ exact, accounted
        throw TransformationException.notConserved(in, good, residual);     // 422 XFORM_NOT_CONSERVED
    }
    return repo.save(TransformationRun.of(inputs, goods, residuals));       // ✅ one tx, immutable legs
}
```

This is the shape the transformation reference workload runs (`ConservationCheck` + `TransformationService.record`): conservation is asserted per base unit as an exact `BigDecimal` equality, every residual leg carries a disposition from the closed `{SCRAP, REWORK, YIELD_LOSS, WIP_REMAINDER}` enum, the record is one transaction, and the legs are immutable (a correction is a reversal, per `immutable-record-corrected-by-reversal-not-edit`, never an edit). A unit-CHANGING transformation (kg → eaches via a yield factor) composes `unit-of-measure-conversion-is-exact-and-pinned`, not naive addition.

Verification: review-tier — confirm the transformation path asserts `Σinput == Σgood + Σresidual` per base unit with exact `BigDecimal`, requires a governed disposition on every residual with no miscellaneous bucket, rejects an imbalance with `422 XFORM_NOT_CONSERVED` (never persisting a half-record), and stores immutable legs. The canonical proof a fork-receiver writes: a `100 -> 90 good + 7 scrap + 3 yield_loss` record succeeds; a `100 -> 90 good + 5 scrap` record is rejected `XFORM_NOT_CONSERVED` and persists nothing.

Reference: [Wikipedia — Mass balance (conservation of mass; input = output + accumulation)](https://en.wikipedia.org/wiki/Mass_balance)

Reference: [Martin Fowler — Money pattern (penny conservation)](https://martinfowler.com/eaaCatalog/money.html)
