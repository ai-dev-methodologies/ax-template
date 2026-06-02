---
title: Monetary amounts must use BigDecimal — never float or double
impact: HIGH
impactDescription: "IEEE-754 binary floating point cannot represent most decimal fractions exactly; arithmetic on monetary doubles silently drifts"
tags:
  - lang
  - money
  - precision
  - bigdecimal
spec_ref: "specs/payment-l0.yaml#PAYMENT-MONEY-001"
verification:
  gradle_task: testPayment
  tag: PAYMENT-MONEY-001
upstream:
  - "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html"
  - "https://ieeexplore.ieee.org/document/8766229"
evidence:
  - upstream_id: iso-4217
    section: "Amount representation rules — decimal-string vs minor-units"
    quote: "JSON number with a decimal point"
  - source_type: external
    citation: "Effective Java (3rd ed., Joshua Bloch) — Item 60: Avoid float and double if exact answers are required"
    url: "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html"
  - source_type: external
    citation: "IEEE 754-2019 — Standard for Floating-Point Arithmetic"
    url: "https://ieeexplore.ieee.org/document/8766229"
---

## Monetary amounts must use BigDecimal — never float or double

**Impact: HIGH — IEEE-754 binary floating point cannot represent most decimal fractions exactly; arithmetic on monetary doubles silently drifts**

`double` and `float` are binary floating point — they can represent `0.5`, `0.25`, `0.75` exactly but cannot represent `0.1`, `0.2`, `0.3`, or any tenth that is not a sum of negative powers of two. The classic demonstration `0.1 + 0.2 == 0.30000000000000004` is harmless in a graph but catastrophic in a ledger: a refund computed as `paid - capturedAmount` over a few thousand line items will accumulate sub-cent rounding error that breaks reconciliation, fails audit invariants, and shows up months later as a `recon_drift_detected_total` counter incrementing in production. The Java standard library answer, codified in `java.math.BigDecimal` and recommended verbatim by *Effective Java* Item 60, is unconditional: monetary amounts use `BigDecimal`; never `double`, `float`, or `Number`. The compiler will not catch this — only a rule + a static-analysis scan will.

**Tradeoff — long minor-units integer:** A legitimate alternative is to store an integer in the smallest subdivision of the currency (KRW 1000원 → `1000`, USD $10.99 → `1099`). This is what Stripe, Adyen, and most PSP REST APIs do because integers are exact end-to-end. The tradeoff is binding: **if** the codebase chooses `long` minor-units, **every** monetary field and every arithmetic step must commit to that representation. A mix of `BigDecimal` in some places and `long amountCents` in others reintroduces conversion bugs at every boundary. This rule mandates `BigDecimal` by default; a codebase-wide migration to `long` minor-units is permitted only if (a) documented in `DECISIONS.md`, (b) enforced by a separate ArchUnit rule asserting no `BigDecimal` appears in monetary positions, and (c) the per-currency scale check from `payment-iso-4217-currency.md` is rewritten to assert the integer's implicit scale matches the currency. The mixed form is what this rule rejects.

**#39 reconcile (2026-05-31) — the documented layered boundary is NOT the rejected mixed form.** ax-template runs a deliberate two-layer representation: the storage/domain layer holds `long` minor-units (`currency-amount-precision-explicit`, ArchUnit-enforced on `..billing..`; ecommerce `Product.price`; frontend L0 `money.ts`), and the payment/PG-edge layer holds `BigDecimal` major-units (this rule + `payment-iso-4217-currency`). The two layers are bridged ONLY through `common/Money.toMajorUnits` / `toMinorUnits` (decimal point placed at the ISO-4217 scale) and enforced by `money_boundary_seam_guard.sh`. That arrangement satisfies clauses (a) `DECISIONS.md` ("Money representation — layered boundary"), (b) the billing ArchUnit rule, and (c) `payment-iso-4217-currency.md` — so it is the *reconciled* form, not the rejected one. What this rule still rejects is **undocumented, unscoped mixing with no conversion seam** — the canonical anti-pattern being `BigDecimal.valueOf(order.getTotalAmount())`, which passes a minor-units `long` into a major-units API and silently over-charges 100x on every 2-decimal currency (`money_boundary_seam_guard.sh` blocks exactly this).

**Incorrect — monetary fields typed as double, silent precision drift:**

```java
public class Payment {
    private double amount;            // 0.1 + 0.2 → 0.30000000000000004
    private double capturedAmount;
    // partial-refund check uses subtraction → accumulates rounding error
}
```

**Correct — BigDecimal with explicit scale at construction:**

```java
public class Payment {
    private BigDecimal amount;
    private BigDecimal capturedAmount;

    public static Payment of(BigDecimal raw, String currency) {
        int scale = Currency.getInstance(currency).getDefaultFractionDigits();
        BigDecimal scaled = raw.setScale(scale, RoundingMode.UNNECESSARY);
        return new Payment(scaled, scaled, currency);
    }
}
// raw.setScale(scale, UNNECESSARY) throws ArithmeticException if the input
// already has more decimals than the currency allows — surfaces scale
// violations as 400 RFC 7807 rather than silent truncation.
```

A grep / ArchUnit rule completes the loop: scan the monetary package and assert `float` and `double` do not appear on any monetary-named field. Pair this rule with `payment-iso-4217-currency.md` (per-currency scale validation) and with a Jackson deserializer that rejects JSON `number` tokens with a decimal point (only integer minor units and explicit decimal strings are accepted on the wire).

Verification: `./gradlew testPayment --tests "*Money*"` exercises the BigDecimal field-type check, KRW integer-amount acceptance, JSON float-token rejection, KRW fractional-amount rejection, and USD scale-2 acceptance. (BHD scale-3 and a partial-refund-sum drift invariant are NOT yet covered by PaymentMoneyTest — enforce those at review until added.) Static scan: `grep -rn 'float\|double' backend/src/main/java/.../payment/` returns 0 hits on monetary fields.

Reference: [java.math.BigDecimal — Java SE 21 API documentation](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html)

Reference: [IEEE 754-2019 — Standard for Floating-Point Arithmetic](https://ieeexplore.ieee.org/document/8766229)
