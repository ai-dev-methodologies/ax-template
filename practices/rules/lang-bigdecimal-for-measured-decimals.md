---
title: Measured / aggregated non-money decimals must use scaled BigDecimal — never float, double, or money minor-units
impact: HIGH
impactDescription: "IEEE-754 doubles drift across aggregation and a borderline percentage can flip a pass/fail gate; reusing the long minor-units money model mis-scales a 0-100% value"
tags:
  - lang
  - precision
  - bigdecimal
  - percentage
  - aggregation
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-LANG-004"
verification:
  type: review
  source: "practices/rules/lang-bigdecimal-for-measured-decimals.md (Correct example) + sibling practices/rules/lang-bigdecimal-for-money.md"
  pattern: "Score / percentage / ratio / weighted-average / SLA fields are declared BigDecimal; accumulated with BigDecimal.add; finalised ONCE with setScale(scale, RoundingMode) at the aggregation boundary; pass/fail gate uses scaled.compareTo(threshold) >= 0; no float/double on any measured-decimal field; the long minor-units money representation is NOT reused for a 0-100 percentage scale"
upstream:
  - "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html"
  - "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/RoundingMode.html"
  - "https://ieeexplore.ieee.org/document/8766229"
evidence:
  - source_type: external
    citation: "java.math.BigDecimal — Java SE 21 API documentation (scale & cohort/natural-order semantics)"
    url: "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html"
    quote: "A BigDecimal consists of an arbitrary precision integer unscaled value and a 32-bit integer scale. If the scale is zero or positive, the scale is the number of digits to the right of the decimal point."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "java.math.BigDecimal — Java SE 21 API documentation (compareTo treats same-cohort values as equal — the basis for a stable threshold comparison)"
    url: "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html"
    quote: "The natural order of BigDecimal considers members of the same cohort to be equal to each other. In contrast, the equals method requires both the numerical value and representation to be the same for equality to hold."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "java.math.RoundingMode — Java SE 21 API documentation"
    url: "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/RoundingMode.html"
    quote: "Specifies a rounding policy for numerical operations capable of discarding precision."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "Floating-point arithmetic — decimal fractions are not exactly representable and rounding error accumulates across successive operations"
    url: "https://en.wikipedia.org/wiki/Floating-point_arithmetic"
    quote: "the decimal number 0.1 is not representable in binary floating-point of any finite precision"
    quoted_at: "2026-06-01"
---

## Measured / aggregated non-money decimals must use scaled BigDecimal — never float, double, or money minor-units

**Impact: HIGH — doubles drift across aggregation and a borderline percentage flips a pass/fail gate; the money minor-units model mis-scales a 0-100% value**

The sibling rule `lang-bigdecimal-for-money.md` mandates `BigDecimal` (or a committed `long` minor-units model) for *currency*. This rule covers the other half of the decimal surface that AI agents routinely get wrong: **non-money measured or aggregated decimals** — quiz / exam scores, completion percentages, SLA attainment, weighted averages, ratios, pass rates. These have the same IEEE-754 hazard as money but a *different* representation, and conflating the two is its own bug.

The floating-point trap is identical to the money case. `double` is binary floating point, so `0.1` is not representable exactly (`the decimal number 0.1 is not representable in binary floating-point of any finite precision`), and `small errors may accumulate as operations are performed in succession`. Average 40 lesson scores in a `double` accumulator and the running total drifts by a sub-unit; the learner who is *exactly* on the 60.0% pass line lands at `59.99999999999999` and is silently failed — or `60.00000000000001` and silently passed. *Effective Java* Item 60 is unconditional: avoid `float` and `double` when exact answers are required. A score boundary is exactly such a case.

The second, rarer trap is **reusing the money representation**. A developer who has internalised "decimals are `long` minor-units" (the documented ax-template storage model for currency) will store a percentage as `long basisPoints` or scale it by the *currency's* fraction digits. That is the wrong model: a percentage's scale is a property of the *measurement* (typically 1 or 2 decimal places, fixed by the domain), not of any currency's ISO-4217 `getDefaultFractionDigits()`. A `0-100` percentage has no currency, so the minor-units machinery (`Currency.getInstance(...)`, per-currency scale validation) does not apply and silently mis-scales. Keep money and measured decimals as **separate** `BigDecimal` pipelines with their own, explicitly chosen scales.

The invariant has three clauses, all enforced at the **aggregation boundary**:

1. **Carry as `BigDecimal`, accumulate with `BigDecimal.add`** — never a `double`/`float` running total.
2. **Apply scale + `RoundingMode` exactly once, at the boundary** — `result.setScale(scale, RoundingMode.HALF_UP)` (or the domain's documented mode). `RoundingMode` `Specifies a rounding policy for numerical operations capable of discarding precision`; choosing it explicitly is what makes the rounded value reproducible across call sites instead of platform-dependent.
3. **Compare the already-scaled value with `compareTo`** — `scaled.compareTo(threshold) >= 0`. Because `The natural order of BigDecimal considers members of the same cohort to be equal to each other`, comparing the *scaled* value means `33.3` decided here equals `33.3` decided anywhere else; a raw-`double` borderline like `33.333...` can round to `33.3` at one call site and `33.4` at another and flip the verdict.

**Incorrect — double accumulator drifts and a borderline score flips the pass gate:**

```java
public class ScoreService {
    double averagePercent(List<Double> scores) {
        double sum = 0.0;
        for (double s : scores) sum += s;          // IEEE-754 drift accumulates
        return (sum / scores.size()) * 100.0;       // 59.99999999999999 on the 60.0 line
    }

    boolean passed(List<Double> scores) {
        return averagePercent(scores) >= 60.0;      // borderline flips between call sites
    }

    long completionBasisPoints(int done, int total) {
        // ❌ reuses a money-style minor-units integer for a 0-100% scale
        return Math.round((double) done / total * 10_000);
    }
}
```

**Correct — BigDecimal accumulated, scaled once with an explicit RoundingMode, compared with compareTo:**

```java
public class ScoreService {
    private static final int PERCENT_SCALE = 1;                 // domain-chosen, not a currency's
    private static final RoundingMode MODE = RoundingMode.HALF_UP;
    private static final BigDecimal PASS_THRESHOLD =
            new BigDecimal("60.0").setScale(PERCENT_SCALE, MODE);

    BigDecimal averagePercent(List<BigDecimal> scores) {
        BigDecimal sum = scores.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);       // exact accumulation
        return sum.divide(BigDecimal.valueOf(scores.size()), 10, MODE)
                .multiply(new BigDecimal("100"))
                .setScale(PERCENT_SCALE, MODE);                  // scale ONCE at the boundary
    }

    boolean passed(List<BigDecimal> scores) {
        return averagePercent(scores).compareTo(PASS_THRESHOLD) >= 0;  // cohort-stable verdict
    }
}
```

This is **not** the money rule and must not borrow its representation: a percentage carries a domain-fixed scale (here `1`), accumulates as `BigDecimal`, and never passes through `Currency.getInstance(...)` or a `long` minor-units field. If a value is currency, use `lang-bigdecimal-for-money.md` and its ISO-4217 scale path; if it is a measured percentage / score / ratio, use this rule's domain-fixed scale path. Mixing the two — e.g. storing a completion percentage in a `long amountCents`-shaped field — is the cross-domain mistake this rule exists to stop.

Verification (review-tier): inspect every service that averages, ratios, or computes a percentage of user-visible numbers. Each measured-decimal field is `BigDecimal` (no `float`/`double`); accumulation uses `BigDecimal.add`; the result is finalised with a single `setScale(scale, RoundingMode)` at the aggregation boundary; the pass/fail gate uses `compareTo` on the scaled value; and no measured percentage is routed through the currency/minor-units machinery. A fixed-input regression test that feeds scores summing to exactly the threshold and asserts the verdict does not flip is the canonical proof a fork-receiver writes per measured-decimal path.

Reference: [java.math.BigDecimal — Java SE 21 API documentation](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html)

Reference: [java.math.RoundingMode — Java SE 21 API documentation](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/RoundingMode.html)

Reference: [IEEE 754-2019 — Standard for Floating-Point Arithmetic](https://ieeexplore.ieee.org/document/8766229)

Reference (sibling — money, do not conflate): [practices/rules/lang-bigdecimal-for-money.md](lang-bigdecimal-for-money.md)
