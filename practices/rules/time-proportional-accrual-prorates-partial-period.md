---
title: A quantity earned over time must be accrued as rate × elapsed-fraction-of-period and a partial period MUST be prorated on a declared day-count basis — never granted or charged a full period
impact: HIGH
impactDescription: "Granting or charging a whole period for a partial one (mid-period join/leave/plan-change) over-pays or over-bills; an ad-hoc, undeclared day-count basis makes two call sites disagree on the same partial period; and splitting a period without conserving the whole double-grants or loses a unit at the boundary"
tags:
  - lang
  - accrual
  - proration
  - time
  - bigdecimal
  - conservation
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-LANG-006"
verification:
  type: review
  source: "practices/rules/time-proportional-accrual-prorates-partial-period.md (Correct example) + siblings practices/rules/time-gated-decisions-read-injected-clock.md, practices/rules/rounded-split-conserves-total-largest-remainder.md, practices/rules/lang-bigdecimal-for-measured-decimals.md"
  pattern: "Any quantity EARNED over time — leave accrual, subscription fee, depreciation, interest, SaaS seat-day billing — is computed as rate × (elapsed ÷ period) where the elapsed and period day-counts are produced by a SINGLE declared day-count basis (e.g. actual/actual or 30/360, named once as a constant/enum, never re-derived ad-hoc per call site); a PARTIAL period (mid-period join/leave/plan-change) is PRORATED by that fraction and NEVER billed/granted as a full period; when one period is SPLIT at a boundary (old plan part + new plan part), the prorated parts are produced by a conserving allocation so they sum back to the whole period exactly (no double-grant, no lost unit) — composing PRACTICES-LANG-005 (largest-remainder) for the rounding. The anti-patterns rejected: charging/granting a full period for a partial one; computing elapsed-fraction with an undeclared/ad-hoc divisor that differs between call sites; and rounding each split part independently so the parts no longer sum to the whole."
upstream:
  - "https://en.wikipedia.org/wiki/Day_count_convention"
  - "https://en.wikipedia.org/wiki/Accrual"
  - "https://en.wikipedia.org/wiki/Matching_principle"
evidence:
  - source_type: external
    citation: "Wikipedia — Day count convention (the standardised basis that determines how a quantity accrues over a period; actual/actual vs 30/360 must be the declared, agreed basis — not ad-hoc)"
    url: "https://en.wikipedia.org/wiki/Day_count_convention"
    quote: "In finance, a day count convention determines how interest accrues over time for a variety of investments, including bonds, notes, loans, mortgages, medium-term notes, swaps, and forward rate agreements (FRAs)."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "Wikipedia — Day count convention, comparison of 30/360 and Actual methods (the two bases produce different day counts for the SAME interval, which is why the basis must be declared once and used consistently)"
    url: "https://en.wikipedia.org/wiki/Day_count_convention"
    quote: "Treating a month as 30 days and a year as 360 days was devised for its ease of calculation by hand compared with manually calculating the actual days between two dates."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "Wikipedia — Accrual (accounting recognises an amount earned/incurred over a period of time, independent of when cash moves — the matching basis a time-proportional accrual implements)"
    url: "https://en.wikipedia.org/wiki/Accrual"
    quote: "In accounting and finance, an accrual is an asset or liability that represents revenue or expenses that are receivable or payable but which have not yet been paid."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "Wikipedia — Matching principle (an amount earned/incurred is recognised in the period it belongs to; a partial period gets the partial amount, not a full period's)"
    url: "https://en.wikipedia.org/wiki/Matching_principle"
    quote: "the matching principle (or expense recognition principle) dictates that an expense should be reported in the same period as the corresponding revenue is earned."
    quoted_at: "2026-06-01"
---

## A quantity earned over time must be accrued as rate × elapsed-fraction-of-period and a partial period MUST be prorated on a declared day-count basis — never granted or charged a full period

**Impact: HIGH — granting or charging a whole period for a partial one over-pays/over-bills; an undeclared day-count basis makes two call sites disagree on the same interval; and splitting a period without conserving the whole double-grants or loses a unit at the boundary**

Many domains produce a quantity that is *earned over time* rather than at a single instant: leave/PTO accrual (days earned per month of tenure), a subscription or SaaS fee (charged per billing period), depreciation (cost spread over a useful life), interest (accrued per day on a principal), and seat-day or usage billing (charged per active seat-day). For every one of these the correct quantity for an interval is **`rate × elapsed-fraction-of-period`**, where the elapsed fraction is `elapsed-units ÷ period-units` under a **declared day-count basis**. This is the accounting matching basis made executable — *the matching principle (or expense recognition principle) dictates that an expense should be reported in the same period as the corresponding revenue is earned* — and an accrual is precisely *an asset or liability that represents revenue or expenses that are receivable or payable but which have not yet been paid*. The amount belongs to the slice of time in which it was earned, so a slice that is shorter than a full period earns a proportionally smaller amount.

Three defects recur on these paths, and one rule closes all three.

**Defect 1 — a partial period billed/granted as a full period.** When someone joins on the 20th, cancels mid-month, or changes plan on day 10, the period is *partial*. Charging the full month's fee, granting the full month's accrual, or depreciating a full period's slice over-states the quantity. The partial period MUST be prorated: `fullPeriodAmount × (partialDays ÷ periodDays)`. Granting/charging the full period for a partial one is forbidden.

**Defect 2 — an ad-hoc, undeclared day-count basis.** The *fraction* is only well-defined once you fix what a "day" and a "period" count as. Finance names this explicitly: *a day count convention determines how interest accrues over time*. The two common bases give **different** answers for the same interval — *treating a month as 30 days and a year as 360 days was devised for its ease of calculation by hand compared with manually calculating the actual days between two dates* — so 30/360 and actual/actual disagree on, say, a 28-day February slice. If one call site divides by 30 and another divides by the real days in the month, the same partial period prorates to two different amounts and the books drift. The basis (actual/actual, 30/360, actual/365-fixed, …) MUST be **declared once** as a named constant/enum and used by every site; it is a policy decision, not a per-call improvisation.

**Defect 3 — a split period that does not conserve the whole.** A mid-period plan change splits ONE period into an old-plan part and a new-plan part. The two prorated parts MUST sum back to exactly one whole period — no day counted twice (double-grant at the boundary) and no day dropped. That is the conservation property of `rounded-split-conserves-total-largest-remainder.md`: floor each part, then distribute the leftover minor unit(s) to the largest fractional remainders so `Σ parts == whole` exactly. Independently rounding each part of the split silently loses or conjures a unit at the boundary.

Carry every monetary or measured amount on these paths as `BigDecimal` with an explicit scale and rounding applied **once** at the boundary (see `lang-bigdecimal-for-measured-decimals.md`), read "now" and the period bounds from an injected `Clock` so the proration is deterministically testable (see `time-gated-decisions-read-injected-clock.md`), and never trust a client-supplied join/leave timestamp for the cutoff.

**Incorrect — full period charged for a partial one; ad-hoc divisor; split parts not conserved:**

```java
// Subscription proration + a mid-month plan change.
BigDecimal chargeForJoinMidMonth(BigDecimal monthlyFee, LocalDate joinDate) {
    // ❌ DEFECT 1: ignores that the member joined mid-month — bills a whole month
    return monthlyFee;
}

BigDecimal proratePart(BigDecimal monthlyFee, int activeDays) {
    // ❌ DEFECT 2: divisor is an ad-hoc literal 30; another method here divides by
    //    the real days in the month — the SAME 28-day slice prorates two different ways
    return monthlyFee.multiply(BigDecimal.valueOf(activeDays))
                     .divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
}

List<BigDecimal> splitForPlanChange(BigDecimal monthlyFee, int oldDays, int newDays) {
    // ❌ DEFECT 3: each part rounded independently — oldPart + newPart ≠ monthlyFee
    BigDecimal old = monthlyFee.multiply(BigDecimal.valueOf(oldDays))
            .divide(BigDecimal.valueOf(oldDays + newDays), 2, RoundingMode.HALF_UP);
    BigDecimal neu = monthlyFee.multiply(BigDecimal.valueOf(newDays))
            .divide(BigDecimal.valueOf(oldDays + newDays), 2, RoundingMode.HALF_UP);
    return List.of(old, neu);   // 30.00 split 10:20 → [10.00, 20.00]? often [10.00,20.01] or sums to 29.99
}
```

**Correct — partial period prorated; one declared day-count basis; split conserved:**

```java
/** The ONE declared day-count basis for this domain (policy, named once). */
enum DayCount {
    ACTUAL_ACTUAL,   // real elapsed days ÷ real days in the period
    THIRTY_360;      // month = 30, year = 360
}
static final DayCount BASIS = DayCount.ACTUAL_ACTUAL;   // declared, not ad-hoc

private final Clock clock;   // ✅ injected — proration is deterministically testable

/** elapsed ÷ period under the declared basis — the single source of the fraction. */
BigDecimal elapsedFraction(LocalDate start, LocalDate endExclusive, YearMonth period) {
    long elapsed = ChronoUnit.DAYS.between(start, endExclusive);
    long periodDays = (BASIS == DayCount.THIRTY_360) ? 30 : period.lengthOfMonth();
    return BigDecimal.valueOf(elapsed)
            .divide(BigDecimal.valueOf(periodDays), 10, RoundingMode.HALF_UP);
}

/** ✅ DEFECT 1 closed: a partial period earns rate × fraction, never a full period. */
BigDecimal accrue(BigDecimal ratePerPeriod, LocalDate start, LocalDate endExclusive, YearMonth period) {
    return ratePerPeriod.multiply(elapsedFraction(start, endExclusive, period))
            .setScale(2, RoundingMode.HALF_UP);   // ✅ scale+round once at the boundary
}

/** ✅ DEFECT 3 closed: a plan-change split conserves the whole period exactly. */
List<BigDecimal> splitForPlanChange(BigDecimal periodFee, int oldDays, int newDays) {
    // Delegate to the largest-remainder conserving allocation (PRACTICES-LANG-005):
    List<BigDecimal> parts = allocate(periodFee,
            List.of(BigDecimal.valueOf(oldDays), BigDecimal.valueOf(newDays)), 2);
    assert parts.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                 .compareTo(periodFee) == 0;   // ✅ Σ parts == whole period, exactly
    return parts;
}
```

The shape that triggers this rule is *a quantity that grows with elapsed time*: PTO days per month of service, a recurring fee per billing cycle, an asset's cost over its useful life, interest per day on a balance, or seat-days of usage. Wherever that shape appears, the per-interval amount MUST be `rate × (elapsed ÷ period)` under a day-count basis that is **declared once** (actual/actual or 30/360, not re-invented per call site), a partial period MUST be prorated by that fraction (never billed/granted whole), and a split of one period across a boundary MUST conserve the whole (`Σ parts == whole`, via the largest-remainder allocation).

Verification (review-tier): inspect every path that earns a quantity over time (leave accrual, subscription/SaaS fee, depreciation, interest, seat-day billing). Confirm (1) the amount is computed as `rate × elapsed-fraction`, not a flat full-period amount when the period is partial; (2) the elapsed and period day-counts come from a SINGLE declared day-count basis (a named constant/enum — actual/actual, 30/360, …), not an ad-hoc divisor that differs between call sites; (3) when one period is split at a join/leave/plan-change boundary, the parts are produced by a conserving allocation (largest-remainder) and a post-condition asserts `Σ parts == whole period`. The canonical per-path regression test a fork-receiver writes pins a `Clock.fixed(...)`, prorates a mid-period join (e.g. join on day 20 of a 30-day month → `fee × 10/30`, not the full fee), and asserts a plan-change split of a fee across a 10:20 day boundary sums back to the whole period fee exactly (`compareTo == 0`).

Reference: [Wikipedia — Day count convention (actual/actual vs 30/360 — the declared basis)](https://en.wikipedia.org/wiki/Day_count_convention)

Reference: [Wikipedia — Accrual (an amount earned/incurred over a period)](https://en.wikipedia.org/wiki/Accrual)

Reference: [Wikipedia — Matching principle (the amount belongs to the period it is earned in)](https://en.wikipedia.org/wiki/Matching_principle)

Reference (sibling — conserve the split, do not conflate): [practices/rules/rounded-split-conserves-total-largest-remainder.md](rounded-split-conserves-total-largest-remainder.md)

Reference (sibling — scale/round the amount once, do not conflate): [practices/rules/lang-bigdecimal-for-measured-decimals.md](lang-bigdecimal-for-measured-decimals.md)

Reference (sibling — read the clock + period bounds from an injected Clock): [practices/rules/time-gated-decisions-read-injected-clock.md](time-gated-decisions-read-injected-clock.md)
