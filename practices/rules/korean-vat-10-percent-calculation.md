---
title: "Backend services computing Korean VAT must use BigDecimal with rate 0.10 and HALF_UP rounding; float, double, and inline rate literals (0.10d / 0.10f) are prohibited"
rule_id: korean-vat-10-percent-calculation
impact: HIGH
impactDescription: "Korean VAT (부가가치세) is fixed at 10% by statute. Computing VAT with float / double silently introduces sub-부 rounding errors that compound across invoices; declaring the rate as 0.10d (double literal) inside a BigDecimal constructor (`new BigDecimal(0.10d)`) materializes the float-noise value 0.1000000000000000055511151231257827021181583404541015625 into the audit trail. The HALF_UP requirement matches the Korean invoice rounding convention — different rounding modes systematically over- or under-collect across invoice volume."
tags:
  - billing
  - tax
  - vat
  - korean-compliance
  - currency
provenance_class: external
spec_ref: "specs/billing-l0.yaml#BILLING-CUR-001"
verification:
  type: review
  status: manual
  notes: "Static analysis (1): grep -rE '\\b0\\.10[dfDF]?\\b' against any billing / payment / invoice / tax module must return zero matches OUTSIDE a BigDecimal(\"0.10\") string-constructor call. Static analysis (2): every method computing a VAT amount must use BigDecimal.setScale(0, RoundingMode.HALF_UP) (or the equivalent 2-arg .multiply + .setScale chain) — never .doubleValue() / .floatValue() intermediates. The 3 representative fixtures asserted by review: (i) supply 1000 → vat 100; (ii) supply 1001 → vat 100; (iii) supply 1005 → vat 101 (HALF_UP on the .5 boundary). Cross-links: lang-bigdecimal-for-money.md + currency-amount-precision-explicit.md (long minor-units transport) — the VAT rate is the lone decimal-rate exception that BigDecimal exists to handle."
evidence:
  - source_type: external
    citation: "Wikipedia (Korean) 부가가치세 — verbatim '대한민국 10% VAT = 부가세(附加稅) 또는 부가가치세(附加價値稅)'"
    url: "https://ko.wikipedia.org/wiki/부가가치세"
    quoted_at: "2026-05-24"
  - source_type: external
    citation: "Wikipedia (Korean) 부가가치세 — verbatim '대한민국에서는 1977년 7월 1일부터 시행하였다.'"
    url: "https://ko.wikipedia.org/wiki/부가가치세"
    quoted_at: "2026-05-24"
  - source_type: external
    citation: "국세청 (NTS) 부가가치세 기장의무 — verbatim '직전연도(2024년) 업종별 수입금액 기준으로 판단'"
    url: "https://www.nts.go.kr/nts/cm/cntnts/cntntsView.do?mi=2272&cntntsId=7669"
    quoted_at: "2026-05-24"
  - source_type: external
    citation: "PwC Tax Summaries (Korea) — verbatim 'VAT is generally levied at a rate of 10% on the supply of goods and services in Korea.'"
    url: "https://taxsummaries.pwc.com/republic-of-korea/corporate/other-taxes"
    quoted_at: "2026-05-24"
decided_at: "2026-05-24"
---

## Korean VAT must be computed with BigDecimal("0.10") and HALF_UP rounding

**Impact: HIGH — Korean 부가가치세 is fixed at 10% by statute. Float / double arithmetic silently accumulates rounding noise; inline `0.10d` literals materialize float-noise values into the audit trail; non-HALF_UP rounding systematically biases collection across invoice volume.**

The Korean 부가가치세 (Value-Added Tax) rate is 10% on the supply of goods and services and has been in force since 1977-07-01. The PwC Tax Summaries restate the rule in English: *"VAT is generally levied at a rate of 10% on the supply of goods and services in Korea."* The 국세청 (NTS) `부가가치세 기장의무` page restates the bookkeeping threshold tied to the prior-year revenue ('직전연도(2024년) 업종별 수입금액 기준으로 판단'). The rate itself is single-valued and statutory — there is no business case for computing it with floating-point arithmetic, and every Korean-domain backend that bills, invoices, or settles must use the BigDecimal-with-HALF_UP path.

This rule sits beside `currency-amount-precision-explicit.md` (which mandates long integer minor units for transport storage) and `lang-bigdecimal-for-money.md` (which forbids float / double for money). The VAT rate is the **only** decimal-rate exception in the Korean billing pipeline — long arithmetic still applies to the resulting amounts.

**Incorrect — double rate; float intermediate; banker's-rounding default; inline 0.10d:**

```java
public long computeVatAmount(long supplyAmount) {
    // VIOLATION (1): double accumulates IEEE 754 noise.
    double rate = 0.10d;
    double vat = supplyAmount * rate;          // 1005 * 0.10 = 100.50000000000001
    return Math.round(vat);                    // banker's-rounding NOT HALF_UP — drifts on .5
}

// VIOLATION (2): inline double literal inside BigDecimal materializes float noise.
public BigDecimal vat(BigDecimal supply) {
    return supply.multiply(new BigDecimal(0.10d));   // rate becomes 0.1000000000000000055511...
}
```

**Correct — BigDecimal("0.10") string constructor; HALF_UP rounding to scale 0; cross-linked to long minor-units transport:**

```java
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class KoreanVat {

    // Statutory rate per 위키백과 부가가치세 + PwC Tax Summaries (Korea).
    // String constructor is mandatory: new BigDecimal(0.10d) materializes IEEE 754 noise.
    private static final BigDecimal RATE = new BigDecimal("0.10");

    private KoreanVat() {}

    /**
     * Compute the VAT amount (in 원, integer scale 0) for a supply amount.
     * HALF_UP matches the Korean tax-invoice rounding convention.
     */
    public static long computeVatAmount(long supplyAmount) {
        // supply * 0.10, rounded HALF_UP to the nearest 원.
        return BigDecimal.valueOf(supplyAmount)
                .multiply(RATE)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }
}
```

The three boundary cases anchored by review:

| `supplyAmount` (원) | `RATE` | unrounded product | `HALF_UP` (scale 0) | result |
|---|---|---|---|---|
| `1000`  | `BigDecimal("0.10")` | `100.00`     | `100`           | `100`  |
| `1001`  | `BigDecimal("0.10")` | `100.10`     | `100`           | `100`  |
| `1005`  | `BigDecimal("0.10")` | `100.50`     | `101` (HALF_UP) | `101`  |

These three cases pin the rule unambiguously: the exact-zero-cents case (`1000 → 100`), the round-down case (`1001 → 100`), and the half-boundary case (`1005 → 101`) where HALF_UP differs from HALF_EVEN (banker's rounding) — `HALF_EVEN` would round `1005 → 100`, and the silent drift across invoice volume is what the rule prevents.

### Cross-links

- Transport storage: `currency-amount-precision-explicit.md` — supply / VAT amounts are stored and wired as long integer minor units (원). The BigDecimal path exists only inside the VAT computation method.
- Money type rule: `lang-bigdecimal-for-money.md` — float / double prohibited for all monetary fields. The VAT rate constant is the only decimal value in the chain.
- Statutory rate: 위키백과 부가가치세 establishes the 10% rate and the 1977-07-01 enactment; PwC Tax Summaries cross-anchors in English; NTS 부가가치세 기장의무 provides the surrounding bookkeeping context.

### Why HALF_UP and not HALF_EVEN

Korean tax-invoice convention rounds the 0.5 boundary **up**, not to the nearest even integer (banker's rounding). `setScale(0, RoundingMode.HALF_UP)` matches `세금계산서` rounding; `RoundingMode.HALF_EVEN` (Java's default for `BigDecimal.divide` without a mode argument) does not. The rule is enforced by explicit `HALF_UP` at every VAT site — never by reliance on the BigDecimal default.

Reference: https://ko.wikipedia.org/wiki/부가가치세

Reference: https://www.nts.go.kr/nts/cm/cntnts/cntntsView.do?mi=2272&cntntsId=7669

Reference: https://taxsummaries.pwc.com/republic-of-korea/corporate/other-taxes
