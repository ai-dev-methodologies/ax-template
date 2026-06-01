---
title: A value derived from a time-varying input MUST pin that input for re-derivability
impact: HIGH
impactDescription: "Recomputing a stored derived value against the CURRENT rate silently rewrites history and breaks reconciliation; the only safe form pins the rate value + as-of + source at write time"
tags:
  - provenance
  - money
  - reproducibility
  - fx
  - tax
  - re-derivability
spec_ref: "specs/value-provenance-l0.yaml#PROVENANCE-PINNED-INPUT-001"
verification:
  type: review
  source: "specs/value-provenance-l0.yaml#PROVENANCE-PINNED-INPUT-001"
  pattern: "Derived monetary field stores appliedRate + rateAsOf + rateSource at write time (all @Column(updatable=false)); re-derivation over pinned inputs reproduces the stored value; no path recomputes it with a current rate"
evidence:
  - source_type: external
    citation: "IAS 21 — The Effects of Changes in Foreign Exchange Rates, paragraph 21 (initial recognition of a foreign currency transaction)"
    url: "https://www.readyratios.com/reference/accounting/ias_21_the_effects_of_changes_in_foreign_exchange_rates.html"
    quote: "A foreign currency transaction shall be recorded, on initial recognition in the functional currency, by applying to the foreign currency amount the spot exchange rate between the functional currency and the foreign currency at the date of the transaction."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "Reproducibility — Wikipedia (definition of a reproducible result)"
    url: "https://en.wikipedia.org/wiki/Reproducibility"
    quote: "For the findings of a study to be reproducible means that results obtained by an experiment or an observational study or in a statistical analysis of a data set should be achieved again with a high degree of reliability when the study is replicated."
    quoted_at: "2026-06-01"
---

## A value derived from a time-varying input MUST pin that input for re-derivability

**Impact: HIGH — recomputing a stored derived value against the CURRENT rate silently rewrites history and breaks reconciliation; the only safe form pins the rate value + as-of timestamp + source at the moment of the write.**

A derived value is any amount you compute from a *volatile, point-in-time* input: a converted amount from an FX rate, a tax amount from a VAT/sales-tax rate, a line total from a unit price on a rate-card, accrued interest from an interest rate, a duty from a tariff schedule. The input is not a constant — it moves. The trap is to store only the derived result (the converted total) and, later, when you need to display, export, or reconcile that row, recompute it from the *current* rate. That silently rewrites history: a January invoice that converted at 1,300 KRW/USD now re-renders at June's 1,380, the order's stored total no longer equals its line items, and the reconciliation job reports a phantom drift that did not exist when the row was written.

Accounting has codified the correct posture for a century: the rate is pinned to the transaction at the date of the transaction and never re-applied later. IAS 21 ¶21 states a foreign currency transaction is recorded on initial recognition *by applying the spot exchange rate at the date of the transaction* — not the rate that prevails when you later look at it. The software invariant is the same and it is generic across FX, tax, price, and interest: at write time, persist the three facts that make the derived value re-derivable — the **rate VALUE actually applied**, an **as-of timestamp**, and the **rate SOURCE/provider** — and treat the derived value as a reproducible function of those pinned facts. Reproducibility (Wikipedia) requires that a result "should be achieved again with a high degree of reliability when the study is replicated"; re-running the same conversion over the same pinned inputs MUST reproduce the same stored total, on any day, forever.

**Incorrect — stores only the derived total; recomputes against the live rate on read, silently rewriting history:**

```java
public class InvoiceLine {
    private BigDecimal foreignAmount;   // 100.00 USD
    private BigDecimal convertedTotal;  // 130000 KRW at write time
    // no appliedRate, no rateAsOf, no rateSource persisted
}

// later — export / re-render / reconcile:
BigDecimal shown = line.getForeignAmount()
        .multiply(fxService.currentRate("USD", "KRW")); // ❌ today's rate
// January's invoice now re-prices at June's rate; convertedTotal no longer
// matches; recon job reports a drift that never happened.
```

**Correct — pins rate value + as-of + source at write time; derived value stays exactly re-derivable from the pinned facts:**

```java
public class InvoiceLine {
    private BigDecimal foreignAmount;                         // 100.00 USD

    @Column(name = "applied_rate", updatable = false, nullable = false)
    private BigDecimal appliedRate;        // 1300.00 — the rate USED, pinned
    @Column(name = "rate_as_of", updatable = false, nullable = false)
    private Instant rateAsOf;              // 2026-01-14T09:00:00Z — when it was effective
    @Column(name = "rate_source", updatable = false, nullable = false)
    private String rateSource;             // "ecb-daily-2026-01-14" — provider/version

    @Column(name = "converted_total", updatable = false, nullable = false)
    private BigDecimal convertedTotal;     // foreignAmount × appliedRate, pinned

    public static InvoiceLine pin(BigDecimal foreignAmount, FxQuote q) {
        BigDecimal total = foreignAmount.multiply(q.rate());
        return new InvoiceLine(foreignAmount, q.rate(), q.asOf(), q.source(), total);
    }

    /** Re-derivation MUST reproduce the stored total — never uses a current rate. */
    public BigDecimal reDerive() {
        return foreignAmount.multiply(appliedRate); // == convertedTotal, always
    }
}
```

The `updatable = false` columns make the pinned inputs immutable after the write, so no later code path can quietly swap in a fresh rate. This rule is **distinct** from `value-provenance` (`PROVENANCE-SOURCE-001` tracks whether a field's value came from the machine or a human, and guards against clobbering a human override) and from `PRACTICES-TIME-001` (server clock as the authority for a *decision* instant). Here the concern is value-CONSERVATION across time: pinning a *time-varying external factor* so a *monetary* result remains re-derivable. The same shape applies to tax (`appliedTaxRate` + `taxTableVersion` + `taxAsOf`), unit price (`appliedUnitPrice` + `priceListId`), interest (`appliedApr` + `rateAsOf`), and tariff (`appliedDutyRate` + `tariffScheduleRev`).

Verification (review-tier): assert each derived monetary field carries its `appliedRate`/`rateAsOf`/`rateSource` triple as non-null `@Column(updatable=false)`; write a pin-then-shift test that pins a row at rate r0, moves the live rate to r1, then re-opens/re-exports the row and asserts the stored derived value still equals `operand × r0` (NOT `operand × r1`) and the three pinned columns are byte-identical to write time. Grep the read/export/reconcile paths for any call to a `currentRate(...)` / live-rate lookup on a row that already has a pinned rate — that is the canonical violation.

Reference: [IAS 21 — The Effects of Changes in Foreign Exchange Rates, ¶21](https://www.readyratios.com/reference/accounting/ias_21_the_effects_of_changes_in_foreign_exchange_rates.html)

Reference: [Reproducibility — Wikipedia](https://en.wikipedia.org/wiki/Reproducibility)
