---
title: "All monetary amounts in billing domain must be stored as long integer minor units; float, double, and BigDecimal representations are prohibited"
rule_id: currency-amount-precision-explicit
impact: CRITICAL
impactDescription: "float/double representation of monetary amounts causes silent rounding errors (e.g., 10.1 KRW stored as 10.099999...). BigDecimal is verbose and mutation-prone. Stripe and Toss both use integer minor units as their canonical wire format."
tags:
  - billing
  - currency
  - precision
  - integer-minor-units
provenance_class: internal_design
protects_template_id: templates/backend/billing/Plan.java
failing_fixture_path: practices/evals/fixtures/currency-amount-precision/fail_float_amount/
spec_ref: "specs/billing-l0.yaml#BILLING-CUR-001"
verification:
  gradle_task: testBilling
  tag: BILLING-CUR-001
  notes: |
    Enforced by BillingArchitectureTest.billingAmountFieldsMustBeIntegerMinorUnits
    (@Tag BILLING-CUR-001): fields named *amount*/*price*/*fee*/*cost* in ..billing..
    MUST NOT have raw type double/float/BigDecimal (the float family -> silent
    rounding). long AND boxed Long are both integer minor units and allowed — request
    DTOs box to Long for @NotNull validation, so mandating the long primitive would
    wrongly reject them (this corrects the prior note's literal haveRawType(long)).
    Failing fixture: practices/evals/fixtures/currency-amount-precision/fail_float_amount/.
evidence:
  - source_type: upstream_id
    upstream_id: stripe-billing-2026-05
    section: "Amounts and currencies"
    quote: "All amounts are stored in the smallest currency unit (e.g., 100 cents to charge $1.00). For zero-decimal currencies such as JPY or KRW, use the amount directly (e.g., 150 to charge ¥150)."
  - source_type: upstream_id
    upstream_id: toss-billing-2026-05
    section: "금액 단위"
    quote: "amount 필드는 항상 정수(원 단위)로 전달합니다. 소수점 금액은 허용하지 않습니다."
  - source_type: external
    citation: "Martin Fowler — Money pattern: store amounts as integer minor units to avoid floating-point rounding; pair with a Currency object for formatting."
    url: "https://martinfowler.com/eaaCatalog/money.html"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## All monetary amounts must be long integer minor units

**Impact: CRITICAL — float/double amounts silently accumulate rounding errors. A 0.1 KRW float error compounded over 1,000 invoices is 100 KRW gone. Stripe and Toss both define integer minor units as canonical. This template enforces the same.**

> **Layered boundary (#39 reconcile, 2026-05-31).** This rule governs the canonical STORAGE/domain representation (long minor-units). The payment/PG-edge intentionally uses `BigDecimal` major-units (`lang-bigdecimal-for-money` + `payment-iso-4217-currency`); the two layers are NOT a contradiction but a documented seam bridged ONLY by `common/Money.toMajorUnits` / `toMinorUnits` and enforced by `money_boundary_seam_guard.sh`. Never hand-convert with `BigDecimal.valueOf(<minor getter>)` — that over-charges 100x on 2-decimal currencies. See `DECISIONS.md` → "Money representation — layered boundary".

Both Stripe and Toss Payments use integer minor-unit amounts as their canonical wire format:
- KRW (South Korean Won): no subdivisions — 1,000 KRW = `1000` (long)
- USD (US Dollar): cents — $10.00 = `1000` (long, cents)
- JPY: no subdivisions — ¥150 = `150` (long)

### What "minor units" means

| Currency | Decimal | Minor units (long) |
|---|---|---|
| KRW ₩10,000 | 10000.00 | `10000L` |
| USD $9.99 | 9.99 | `999L` |
| EUR €4.50 | 4.50 | `450L` |

**Incorrect — float or BigDecimal storage for monetary amounts:**

```java
// VIOLATION: float causes rounding loss on any non-exact binary fraction
private float amount;  // 10.1 stored as 10.09999942779541 (IEE 754)
// VIOLATION: BigDecimal is verbose and mutation-prone
private BigDecimal amountDue;
// VIOLATION: double — same rounding problem as float
private double price;
```

**Correct — long integer minor units for all monetary amounts:**

```java
// CORRECT: long, minor units — KRW 10,000원 stored as 10000L
private long amount;
// CORRECT: Invoice.java — both fields as long
private long amountDue;
private long amountPaid;
```

Reference: https://martinfowler.com/eaaCatalog/money.html

### Correct — rejecting float inputs at the HTTP boundary

```java
// BillingController.java — CreateSubscriptionRequest record
// amount is declared as long; if client sends 9.99, Jackson throws 400
record CreateSubscriptionRequest(
    @NotNull UUID planId,
    @NotBlank String provider
) {}

// PlanController.java — CreatePlanRequest record
record CreatePlanRequest(
    @NotBlank String name,
    @Positive long amount,  // rejects float JSON with 400 ProblemDetail
    @NotBlank String currency,
    @NotBlank String billingCycle
) {}
```

### Display formatting

For display, convert minor units to decimal in the frontend using `CurrencyFormatter` (L1):

```typescript
import { formatCurrencyAmount } from '@/templates/L1/components/currency-input'

// KRW: no decimal places
formatCurrencyAmount(10000, 'KRW', 'ko-KR') // → "₩10,000"

// USD: two decimal places
formatCurrencyAmount(999, 'USD', 'en-US') // → "$9.99"
```

**Never convert back to float/double for storage or computation.** All arithmetic (discounts, proration) stays in long arithmetic.

## ArchUnit enforcement

```java
// BillingArchitectureTest.java (@Tag BILLING-CUR-001) — matches the live testBilling rule.
// EXCLUSION form, not haveRawType(long): boxed Long is also integer minor units and MUST stay
// allowed (request DTOs box to Long for @NotNull). Only the float family + BigDecimal are banned.
@ArchTest
static final ArchRule billingAmountFieldsMustBeIntegerMinorUnits = fields()
    .that().areDeclaredInClassesThat().resideInAPackage("..billing..")
    .and().haveNameMatching(".*[Aa]mount.*|.*[Pp]rice.*|.*[Ff]ee.*|.*[Cc]ost.*")
    .should().notHaveRawType(double.class)
    .andShould().notHaveRawType(Double.class)
    .andShould().notHaveRawType(float.class)
    .andShould().notHaveRawType(Float.class)
    .andShould().notHaveRawType(java.math.BigDecimal.class)
    .because("monetary amounts in billing must be integer minor units (long/Long), never float/double/BigDecimal");
```

## Failing fixture

See: `practices/evals/fixtures/currency-amount-precision/fail_float_amount/BillingPlanFloatAmount.java` — a Plan entity with `private double amount`.

See: `practices/evals/fixtures/currency-amount-precision/pass_integer_amount/BillingPlanLongAmount.java` — correct `private long amount`.
