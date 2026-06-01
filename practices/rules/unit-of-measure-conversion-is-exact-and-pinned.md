---
title: Cross-unit quantity conversion MUST use an exact, pinned, re-derivable factor — never an ad-hoc float multiply
impact: HIGH
impactDescription: "A quantity received in one unit and stored in another that converts through a scattered float literal drifts, loses or invents units on a non-even split, and cannot be reconstructed because the row never recorded which unit or factor it used"
tags:
  - lang
  - precision
  - bigdecimal
  - unit-of-measure
  - conversion
  - conservation
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-LANG-007"
verification:
  type: review
  source: "practices/rules/unit-of-measure-conversion-is-exact-and-pinned.md (Correct example) + siblings practices/rules/lang-bigdecimal-for-money.md, practices/rules/rounded-split-conserves-total-largest-remainder.md, specs/value-provenance-l0.yaml#PROVENANCE-PINNED-INPUT-001"
  pattern: "Any path that receives/measures a quantity in one unit and stores or consumes it in another (cases↔eaches↔pallets, kg↔g, hours↔minutes, GB↔bytes, kWh↔Wh) declares the conversion factor ONCE as a pinned datum (a stored column or a versioned reference), converts with exact scaled BigDecimal (no float/double), conserves quantity across a round-trip (A→B→A == original) and across a non-even split (whole target units + remainder sum back to the source via the largest-remainder method of PRACTICES-LANG-005), and persists the unit + factor (or factor version) used so the original reading is reconstructable; an ad-hoc float multiply at the call site that stores only the converted value and discards the unit + factor is the rejected anti-pattern"
upstream:
  - "https://www.nist.gov/pml/owm/metric-si/si-units"
  - "https://en.wikipedia.org/wiki/Conversion_of_units"
  - "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html"
evidence:
  - source_type: external
    citation: "NIST PML — SI Units (definition of a unit and of the value of a physical quantity)"
    url: "https://www.nist.gov/pml/owm/metric-si/si-units"
    quote: "The value of a physical quantity is the quantitative expression of a particular physical quantity as the product of a number and a unit, the number being its numerical value."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "Wikipedia — Conversion of units (a multiplicative conversion factor changes the unit without changing the quantity)"
    url: "https://en.wikipedia.org/wiki/Conversion_of_units"
    quote: "Conversion of units is the conversion of the unit of measurement in which a quantity is expressed, typically through a multiplicative conversion factor that changes the unit without changing the quantity."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "java.math.BigDecimal — Java SE 21 API documentation (exact unscaled-value + scale representation float/double lack)"
    url: "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html"
    quote: "A BigDecimal consists of an arbitrary precision integer unscaled value and a 32-bit integer scale. If the scale is zero or positive, the scale is the number of digits to the right of the decimal point."
    quoted_at: "2026-06-01"
---

## Cross-unit quantity conversion MUST use an exact, pinned, re-derivable factor — never an ad-hoc float multiply

**Impact: HIGH — a unit change is a number change, and an unpinned float conversion drifts, leaks units on a non-even split, and cannot be reconstructed**

The NIST definition is the whole reason this rule exists: `The value of a physical quantity is the quantitative expression of a particular physical quantity as the product of a number and a unit, the number being its numerical value.` Change the unit and you change the *number* that expresses the same physical quantity. So the moment a system receives a reading in one unit (a purchase order of `5 cases`) and stores or consumes it in another (`120 eaches` on hand), it has performed an arithmetic operation whose correctness is invisible to the type system — a `double quantity` field looks identical whether it holds cases or eaches, and the compiler cannot tell that `5 * 24.0` was meant to be a case→each expansion. Three failures follow, and AI agents routinely ship all three.

**Failure 1 — the conversion is not exact.** A factor multiplied with `double` drifts: most decimal factors (a 0.45359237 kg/lb, a 3.78541 L/gal, a 1/3 cup) are not representable in binary floating point, so a round-trip `kg → lb → kg` over a few thousand line items no longer returns the original and reconciliation breaks. The fix is `java.math.BigDecimal`, whose exact model is the point of the type: `A BigDecimal consists of an arbitrary precision integer unscaled value and a 32-bit integer scale. If the scale is zero or positive, the scale is the number of digits to the right of the decimal point.` Convert with `source.multiply(factor)` (or `source.divide(factor, scale, mode)` for the inverse) and `setScale(targetScale, declaredMode)` once — never `(double) qty * 24`.

**Failure 2 — the conversion is not pinned.** Wikipedia states the property a correct conversion must preserve: `Conversion of units is the conversion of the unit of measurement in which a quantity is expressed, typically through a multiplicative conversion factor that changes the unit without changing the quantity.` If the row stores only the *converted* number and discards the factor and the source unit, the quantity is no longer reconstructable — a later reader cannot tell whether `120` was `120 eaches` (5 cases × 24) or `120 cases`, and cannot replay the conversion if the packaging hierarchy is ever questioned. Pin the factor as a datum: store the source unit, the target unit, and the factor (or a *version* reference to a packaging-hierarchy / units table) on the row, exactly as `PROVENANCE-PINNED-INPUT-001` pins a time-varying rate. A UOM factor is usually static, but the pin-and-reconstruct discipline is identical — and static does not mean unversioned, because a supplier *can* re-pack a case from 24 to 12 next quarter, and old rows must still re-derive against the factor they were written with.

**Failure 3 — the conversion does not conserve quantity on a non-even split.** A multiplicative factor `changes the unit without changing the quantity`; a split must obey the same conservation. Expanding `1 case → 24 eaches` is even, but the inverse direction (and most real splits) are not: `100 eaches` at `24/case` is `4 cases + 4 loose eaches`, and `100 / 24` rounded independently silently loses or invents a unit. When a conversion does not divide evenly, the remainder must be carried, not dropped — compose `rounded-split-conserves-total-largest-remainder.md` (PRACTICES-LANG-005) so the whole target units plus the remainder sum back to the source EXACTLY, and assert it with a post-condition.

This applies cross-industry wherever one quantity is read in one unit and stored/consumed in another: inventory and packaging hierarchies (cases↔eaches↔pallets↔cartons), recipe and batch scaling (kg↔g, L↔mL), data-size accounting (GB↔MB↔bytes), time accounting (hours↔minutes↔seconds), and energy/utility metering (kWh↔Wh, m³↔L). It is **not** the money rule (`lang-bigdecimal-for-money.md` governs the TYPE one single amount uses, not a cross-unit conversion) and **not** the time-varying-rate rule (`PROVENANCE-PINNED-INPUT` pins an FX/tax rate that *moves*); a UOM factor is its own concern — a usually-static, exact, conserving, re-derivable unit change.

**Incorrect — ad-hoc float multiply at the call site; only the converted number is stored; unit + factor lost; non-even split drops a unit:**

```java
public class ReceivingService {
    // factor smeared as a literal at every call site, in double
    private static final double EACHES_PER_CASE = 24.0;

    StockLine receive(int cases) {
        double eaches = cases * EACHES_PER_CASE;     // ❌ double drift; no exact replay
        return new StockLine(eaches);                // ❌ stores only the number; unit + factor gone
    }

    // 100 eaches back to cases — non-even, silently loses the 4 loose units
    int toCases(int eaches) {
        return (int) (eaches / EACHES_PER_CASE);     // ❌ 100/24 → 4; the 4 loose eaches vanish
    }
}
```

**Correct — factor pinned on the row, exact BigDecimal conversion, round-trip + non-even split conserved, unit + factor stored:**

```java
public class ReceivingService {

    StockLine receive(BigDecimal sourceQty, UnitOfMeasure sourceUnit, PackFactor pack) {
        // pack is a pinned, versioned datum: { sourceUnit, targetUnit, factor, version }
        BigDecimal eaches = sourceQty
                .multiply(pack.factor())                       // exact: cases × 24
                .setScale(0, RoundingMode.UNNECESSARY);        // expansion is exact for whole packs
        // store BOTH the converted value AND the unit + factor version it used,
        // so the original reading is reconstructable and a re-export reproduces it
        return new StockLine(eaches, pack.targetUnit(), sourceUnit, pack.version());
    }

    /** Non-even inverse split: 100 eaches at 24/case → 4 cases + 4 loose, conserving every unit. */
    Split toCasesAndRemainder(BigDecimal eaches, PackFactor pack) {
        BigDecimal[] qr = eaches.divideAndRemainder(pack.factor());   // [4, 4]
        BigDecimal wholeCases = qr[0];
        BigDecimal looseEaches = qr[1];
        // post-condition: reconstruct the source EXACTLY (largest-remainder conservation, PRACTICES-LANG-005)
        BigDecimal reconstructed = wholeCases.multiply(pack.factor()).add(looseEaches);
        if (reconstructed.compareTo(eaches) != 0) {
            throw new IllegalStateException("UOM split lost a unit: " + reconstructed + " != " + eaches);
        }
        return new Split(wholeCases, looseEaches, pack.version());
    }
}
```

The loop is closed by a per-path regression test a fork-receiver writes: pin a factor (24 eaches/case), convert a quantity both directions and assert the round-trip is identity, then feed a non-even quantity (100 eaches) and assert `wholeUnits × factor + remainder == source` exactly. The structural half asserts the persisted row carries the source unit, target unit, and factor (or factor version) — never a bare converted number.

Verification (review-tier): inspect every path that converts a quantity from one unit of measure to another. Confirm (1) the factor is declared ONCE as a pinned datum (a stored column or a versioned reference), not a literal smeared across call sites; (2) the arithmetic uses exact scaled `BigDecimal`, with no `float`/`double` on any cross-unit quantity field or in the conversion; (3) the conversion conserves quantity — a round-trip returns the original and a non-even split's whole units plus remainder sum back to the source exactly (composing the largest-remainder allocation of `PRACTICES-LANG-005`); and (4) the stored row records the unit + factor (or factor version) it used so the original reading is reconstructable and a re-export reproduces the same numbers.

Reference: [NIST PML — SI Units (a quantity's value is a number times a unit)](https://www.nist.gov/pml/owm/metric-si/si-units)

Reference: [Wikipedia — Conversion of units (a conversion factor changes the unit without changing the quantity)](https://en.wikipedia.org/wiki/Conversion_of_units)

Reference: [java.math.BigDecimal — Java SE 21 API documentation](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html)

Reference (sibling — money TYPE, do not conflate): [practices/rules/lang-bigdecimal-for-money.md](lang-bigdecimal-for-money.md)

Reference (sibling — conserve a non-even split): [practices/rules/rounded-split-conserves-total-largest-remainder.md](rounded-split-conserves-total-largest-remainder.md)
