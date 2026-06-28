---
title: Monetary arithmetic must be currency-TAGGED and FAIL-CLOSED across currencies — adding or subtracting two amounts whose ISO-4217 currency codes differ, absent an explicit recorded conversion, MUST THROW (never silently coerce, never assume a shared currency, never use one operand's currency for the other); same-currency arithmetic returns a new exact-integer amount in that same currency, and the ONLY sanctioned cross-currency path is an explicit, RECORDED conversion that brings one operand into the other's currency (the exchange RATE itself is out of scope — the converted amount is supplied)
impact: HIGH
impactDescription: "A monetary value carried as a bare amount with no currency tag (or an arithmetic that ignores the tag it has) makes a cross-currency add structurally possible and SILENT: 1000원 + $10 evaluates to 1010 of nothing, conjuring or destroying money on every order — the textbook incorrect-calculation defect (CWE-682). The bug is invisible from the inside because the integer arithmetic is exact; only a currency-aware value type that refuses to add dollars to yen catches it. Guarding only addition lets subtraction become a back-door. Performing an implicit conversion (a hidden rate, a zero-rate assumption, 'use the left operand's currency') silently misstates value and hides the FX decision from audit. The fix is a currency-tagged value object whose plus/minus throw on a currency mismatch and whose only cross-currency seam is an explicit, recorded conversion."
tags:
  - money
  - currency
  - iso-4217
  - fail-closed
  - business-logic
spec_ref: "specs/currency-arithmetic-l0.yaml#CCY-FAILCLOSED-ADD"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/currencyarithmetic/CurrencyMoney.java + backend/src/main/java/com/ax/template/authblueprint/currencyarithmetic/CurrencyArithmeticService.java"
  pattern: "CurrencyMoney is an immutable record { long minorUnits; String currency } whose currency is validated as ISO-4217 alpha-3 (Currency.getInstance) in the compact constructor; plus(addend) and minus(subtrahend) both call a private requireSameCurrency that throws CurrencyArithmeticException (CURRENCY_MISMATCH → 422) when the currencies differ, BEFORE producing any value — there is no branch that coerces a differing-currency operand, so a cross-currency combination is unrepresentable without first going through convertedVia(CurrencyConversion), the sole cross-currency seam, which rejects a from-currency mismatch and otherwise yields a new CurrencyMoney in the conversion's toCurrency (the converted amount is supplied; no rate is computed or looked up). Same-currency plus/minus return a new CurrencyMoney with the exact integer sum/difference in the same currency (Math.addExact/subtractExact, no floating point). CurrencyArithmeticService is the sole mutator of the CurrencyLedger balance: it reads the persisted balance back into a CurrencyMoney through the ledger's immutable (@Column(updatable=false)) currency tag, performs the throwing plus/minus, and only on success applies the new balance and (for a conversion) records the ConversionRecord — so a 422 leaves the balance unmutated (fail-closed, no partial write)"
upstream:
  - "https://www.iso.org/iso-4217-currency-codes.html"
  - "https://martinfowler.com/eaaCatalog/money.html"
  - "https://cwe.mitre.org/data/definitions/682.html"
evidence:
  - upstream_id: iso-4217
    section: "Alpha-3 code structure"
    quote: "Each currency is identified by a three-letter alpha-3 code."
  - source_type: external
    citation: "Martin Fowler, Patterns of Enterprise Application Architecture — Money pattern (martinfowler.com/eaaCatalog)"
    url: "https://martinfowler.com/eaaCatalog/money.html"
    quote: "once you involve multiple currencies you want to avoid adding your dollars to your yen without taking the currency differences into account"
    quoted_at: "2026-06-28"
  - source_type: external
    citation: "CWE-682: Incorrect Calculation — MITRE Common Weakness Enumeration"
    url: "https://cwe.mitre.org/data/definitions/682.html"
    quote: "The product performs a calculation that generates incorrect or unintended results that are later used in security-critical decisions or resource management."
    quoted_at: "2026-06-28"
---

## Rule

A monetary value is **not** just a number — it is an amount **paired with a currency**. ISO 4217 fixes a three-letter alpha-3 code per currency (USD, KRW, JPY, EUR, …). An arithmetic that drops or ignores that tag makes a cross-currency add structurally possible and **silent**: `1000` (KRW) `+ 1099` (USD) `= 2099` of nothing. Integer minor-units arithmetic is exact, so nothing inside the calculation flags the error — exactly the **incorrect-calculation** weakness below.

The discipline is Martin Fowler's **Money** pattern: a **currency-tagged** value type whose arithmetic is **fail-closed across currencies**.

1. **FAIL-CLOSED.** `plus` and `minus` require both operands to carry the **same** ISO-4217 currency; a mismatch **throws** (`CURRENCY_MISMATCH` → 422) before producing any value. There is **no** silent-coercion path — no implicit re-tag, no "use the left operand's currency", no zero-rate assumption — and the guard is **symmetric** (subtraction is never a back-door around addition's check).
2. **SAME-CURRENCY OK.** When both operands share a currency, the result is a new amount with the **exact integer** sum/difference in **that same currency** — `Math.addExact`/`subtractExact`, never binary float.
3. **EXPLICIT RECORDED CONVERSION.** The **only** sanctioned cross-currency path is `convertedVia(CurrencyConversion)` — an explicit conversion carrying `{fromCurrency, toCurrency, convertedMinorUnits}` that re-tags one operand into the other's currency, after which an ordinary same-currency `plus` succeeds. The exchange **RATE is out of scope**: the converted amount is **supplied**, never computed or looked up here. The conversion is **recorded** (persisted to an audit trail) so the FX step is auditable, never implicit.

Money is integer **minor units**. The persisted ledger's currency tag is immutable (`@Column(updatable=false)`), so a cross-currency add can never be retroactively legitimized by mutating the tag.

**Correct — currency-tagged value object, fail-closed plus/minus, explicit conversion as the only seam:**

```java
// backend/.../currencyarithmetic/CurrencyMoney.java
public record CurrencyMoney(long minorUnits, String currency) {
    public CurrencyMoney { currency = requireIso4217(currency); }     // ISO-4217 alpha-3

    public CurrencyMoney plus(CurrencyMoney addend) {
        requireSameCurrency(addend);                                  // THROWS on mismatch — fail-closed
        return new CurrencyMoney(Math.addExact(minorUnits, addend.minorUnits), currency);
    }
    public CurrencyMoney minus(CurrencyMoney subtrahend) {
        requireSameCurrency(subtrahend);                              // symmetric — no back-door
        return new CurrencyMoney(Math.subtractExact(minorUnits, subtrahend.minorUnits), currency);
    }
    public CurrencyMoney convertedVia(CurrencyConversion c) {         // the ONLY cross-currency seam
        if (!c.fromCurrency().equals(currency)) throw CurrencyArithmeticException.conversionMismatch(currency, c.fromCurrency());
        return new CurrencyMoney(c.convertedMinorUnits(), c.toCurrency());   // converted amount supplied; no rate math
    }
    private void requireSameCurrency(CurrencyMoney other) {
        if (!currency.equals(other.currency)) throw CurrencyArithmeticException.currencyMismatch(currency, other.currency);
    }
}
```

**Incorrect — bare amount with no currency tag (or an arithmetic that ignores it): a silent cross-currency add:**

```java
// WRONG: money is a bare long — addition cannot know the currencies differ
long balance = 1000;            // KRW (but the type doesn't say so)
long addend  = 1099;            // USD (but the type doesn't say so)
long total   = balance + addend; // 2099 of NOTHING — conjures money, no error raised

// WRONG: a value object that "helpfully" coerces instead of throwing
CurrencyMoney plus(CurrencyMoney other) {
    return new CurrencyMoney(this.minorUnits + other.minorUnits, this.currency); // silently re-tags other to this.currency
}
```

The incorrect version silently mixes currencies — the dollars-to-yen mistake Fowler's Money pattern exists to prevent, and a CWE-682 incorrect calculation that misstates money on every operation. The correct version makes a cross-currency combination **unrepresentable** without an explicit, recorded conversion.

Reference: [ISO 4217 — Codes for the representation of currencies](https://www.iso.org/iso-4217-currency-codes.html)

Reference: [Martin Fowler — Money pattern (P of EAA)](https://martinfowler.com/eaaCatalog/money.html)

Reference: [CWE-682: Incorrect Calculation](https://cwe.mitre.org/data/definitions/682.html)
