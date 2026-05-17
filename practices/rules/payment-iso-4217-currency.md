---
title: Currency codes must be ISO 4217 alpha-3 and the amount scale must match the currency's minor-unit count
impact: HIGH
impactDescription: "A KRW amount with two decimal places, or a BHD amount with two decimals, silently misrepresents value by orders of magnitude"
tags:
  - payment
  - validation
  - iso-4217
  - currency
spec_ref: "specs/payment-l0.yaml#PAYMENT-MONEY-003"
verification:
  gradle_task: testPayment
  tag: PAYMENT-MONEY-003
upstream:
  - "https://www.iso.org/iso-4217-currency-codes.html"
  - "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Currency.html"
evidence:
  - upstream_id: iso-4217
    section: "Minor unit (scale) per currency"
    quote: "minor unit"
  - source_type: external
    citation: "ISO 4217 — Codes for the representation of currencies (ISO)"
    url: "https://www.iso.org/iso-4217-currency-codes.html"
  - source_type: external
    citation: "java.util.Currency.getDefaultFractionDigits() — Java SE 21 API"
    url: "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Currency.html#getDefaultFractionDigits()"
---

## Currency codes must be ISO 4217 alpha-3 and the amount scale must match the currency's minor-unit count

**Impact: HIGH — A KRW amount with two decimal places, or a BHD amount with two decimals, silently misrepresents value by orders of magnitude**

ISO 4217 fixes a three-letter alpha-3 code per currency (KRW, USD, JPY, EUR, BHD, ...) and the **minor-unit count** — the number of digits after the decimal separator that the currency uses canonically. KRW and JPY are 0-decimal currencies (한국 원 and 円 do not subdivide); USD, EUR, GBP and most others are 2-decimal; BHD, KWD, OMR are 3-decimal; UYW and CLF are 4-decimal. Treating one as another silently scales the value: `100` interpreted as KRW means 100원, but interpreted as USD-with-implicit-cents means $1.00 — a factor-of-100 discrepancy. The bug is hard to detect from the inside because the integer arithmetic is exact; only a per-currency validator that consults `Currency.getInstance(code).getDefaultFractionDigits()` catches it. Mandating both the code lookup (well-formed alpha-3) **and** the scale check (`BigDecimal.scale() == Currency.getDefaultFractionDigits()`) closes the gap.

This rule sits in the `payment-*` namespace because — at the time of writing — Payment is the only multi-currency domain in the catalog. A future Invoice / FX / Billing blueprint with a second multi-currency surface would justify promoting this rule to `validation-currency-code.md` under the generic `validation-*` namespace; the promotion trigger is documented in `practices/DECISIONS.md`.

**Incorrect — currency is an arbitrary string; amount scale is whatever the deserializer happened to produce:**

```java
public record CreatePaymentRequest(
        BigDecimal amount,
        String currency,           // accepts "krw", "krwon", "XYZ", anything
        String orderId
) {
    // no validation: amount=10.99, currency="KRW" → stored as 10.99원
    // (KRW has no sub-units; the .99 is silently meaningless)
}
```

**Correct — Currency.getInstance + per-currency scale assertion:**

```java
public record CreatePaymentRequest(
        @NotNull BigDecimal amount,
        @NotBlank String currency,
        @NotBlank String orderId
) {}

@Service
public class CurrencyValidator {

    public void validate(BigDecimal amount, String currency) {
        Currency iso;
        try {
            iso = Currency.getInstance(currency);          // throws IllegalArgumentException if not ISO 4217
        } catch (IllegalArgumentException e) {
            throw new InvalidCurrencyException(currency);   // → 400 RFC 7807, type=urn:ax:payment:invalid-currency
        }
        int allowedScale = iso.getDefaultFractionDigits(); // KRW=0, USD=2, BHD=3
        if (amount.scale() > allowedScale) {
            throw new ScaleMismatchException(currency, allowedScale, amount.scale());
        }
    }
}
```

Pair this rule with `lang-bigdecimal-for-money.md` (which forbids `double`/`float` for monetary fields) and with a Jackson deserializer that rejects JSON number tokens with a decimal point. The wire-side accepted shapes are **integer minor units** (KRW `1000`, USD `1099`, BHD `10250`) or **explicit decimal strings** with exactly `getDefaultFractionDigits()` digits after the point (KRW `"1000"`, USD `"10.99"`, BHD `"10.250"`). JSON floats are never accepted.

Verification: `./gradlew testPayment --tests "*Currency*"` exercises: (a) `{"currency": "XYZ"}` → 400; (b) `{"currency": "KRW", "amount": "10.99"}` → 400 (scale violation); (c) `{"currency": "USD", "amount": "10.999"}` → 400 (3 digits > USD's 2); (d) `{"currency": "KRW", "amount": 1000}` → 201; (e) `{"currency": "BHD", "amount": "10.250"}` → 201 (3 digits matches BHD scale).

Reference: [ISO 4217 — Codes for the representation of currencies](https://www.iso.org/iso-4217-currency-codes.html)

Reference: [java.util.Currency — Java SE 21 API documentation](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Currency.html)
