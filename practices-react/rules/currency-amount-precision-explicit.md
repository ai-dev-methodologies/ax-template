---
title: "All monetary amounts in billing UI must be displayed via CurrencyFormatter using integer minor-unit values; raw number display and float arithmetic are prohibited"
rule_id: currency-amount-precision-explicit
impact: CRITICAL
impactDescription: "Displaying monetary amounts as raw numbers (e.g., 1000 instead of ₩1,000) or converting minor units to float before display silently misrepresents prices. Users may see ₩100,000 displayed as 100000 or $9.99 displayed as $10.00 due to float rounding."
tags:
  - billing
  - currency
  - precision
  - integer-minor-units
  - display
applicable_to:
  - react
  - nextjs
provenance_class: internal_design
protects_template_id: templates/L1/components/currency-input.tsx
failing_fixture_path: practices-react/evals/fixtures/currency-amount-precision-explicit/
spec_ref: "specs/billing-frontend-l0.yaml#BILLING-FE-001"
verification:
  type: script
  notes: |
    ESLint rule (custom): no-raw-billing-amount
    Detects: numeric billing amount literals rendered directly in JSX without CurrencyFormatter.
    Detects: amount / 100, amount * 0.01, parseFloat(amount), Number(amount).toFixed(2)
    in billing component files.
    Failing fixture: a PricingCard that renders {plan.amount} directly in JSX.
evidence:
  - source_type: upstream_id
    upstream_id: stripe-billing-2026-05
    section: "Amounts and currencies"
    quote: "All amounts are stored in the smallest currency unit (e.g., 100 cents to charge $1.00). For zero-decimal currencies such as JPY or KRW, use the amount directly."
  - source_type: upstream_id
    upstream_id: toss-billing-2026-05
    section: "금액 단위"
    quote: "amount 필드는 항상 정수(원 단위)로 전달합니다. 소수점 금액은 허용하지 않습니다."
  - source_type: external
    citation: "WCAG 2.2 SC 1.3.3 Sensory Characteristics: Instructions do not rely solely on sensory characteristics. Formatted currency labels (₩10,000) are more accessible than raw numbers (10000) because screen readers announce the currency symbol."
    url: "https://www.w3.org/WAI/WCAG22/Understanding/sensory-characteristics.html"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## Monetary amounts must use CurrencyFormatter — never raw display

**Impact: CRITICAL — Raw number display of minor-unit amounts misrepresents prices to users. `10000` KRW displayed as `10000` looks like 10,000 but with no currency symbol; `999` USD cents displayed as `999` looks like $999 instead of $9.99. All billing UI must use `formatCurrencyAmount()` from `@/templates/L1/components/currency-input`.**

### What CurrencyFormatter handles

| Amount (long, minor units) | Currency | Locale | Displayed |
|---|---|---|---|
| `10000` | `KRW` | `ko-KR` | `₩10,000` |
| `999` | `USD` | `en-US` | `$9.99` |
| `450` | `EUR` | `de-DE` | `4,50 €` |
| `0` | `KRW` | `ko-KR` | `₩0` |

### Incorrect — raw number display

```tsx
// ❌ WRONG: raw integer, no currency symbol, wrong scale for multi-decimal currencies
function PricingCard({ plan }: { plan: Plan }) {
  return (
    <div>
      <span>{plan.amount}</span>  {/* → "10000" — looks like ₩10,000 but no symbol */}
      <span>{plan.amount / 100}</span>  {/* ← VIOLATION: float arithmetic */}
      <span>{(plan.amount / 100).toFixed(2)}</span>  {/* ← VIOLATION: float */}
    </div>
  )
}
```

### Incorrect — float arithmetic before display

```tsx
// ❌ WRONG: parseFloat / Number conversion bypasses integer guarantees
const displayAmount = parseFloat(plan.amount.toString())  // ← VIOLATION
const displayAmount = Number(plan.amount) / 100  // ← VIOLATION
```

### Correct — CurrencyFormatter

```tsx
// ✅ CORRECT: always via formatCurrencyAmount
import { formatCurrencyAmount } from '@/templates/L1/components/currency-input'

interface Plan {
  amount: number  // long integer minor units from API
  currency: string
}

function PricingCard({ plan }: { plan: Plan }) {
  const displayPrice = formatCurrencyAmount(plan.amount, plan.currency, 'ko-KR')

  return (
    <div>
      <span aria-label={`월 ${displayPrice}`}>{displayPrice}</span>
    </div>
  )
}
```

### Correct — currency-input component (interactive)

```tsx
// ✅ CORRECT: use CurrencyInput for editable amount fields
import CurrencyInput from '@/templates/L1/components/currency-input'

function PlanForm() {
  const [amount, setAmount] = useState<number>(0)  // minor units

  return (
    <CurrencyInput
      value={amount}
      currency="KRW"
      locale="ko-KR"
      onChange={(val) => setAmount(val)}  // val is always long integer
    />
  )
}
```

## No-raw-billing-amount ESLint rule (custom)

Detects the following patterns in billing component files:

| Pattern | Violation |
|---|---|
| `{plan.amount}` in JSX | ✅ raw render |
| `{invoice.amountDue}` in JSX | ✅ raw render |
| `plan.amount / 100` | ✅ float arithmetic |
| `parseFloat(amount)` | ✅ float conversion |
| `Number(amount).toFixed(2)` | ✅ float formatting |
| `formatCurrencyAmount(amount, ...)` | ✅ correct — no violation |

## Failing fixture

See: `practices-react/evals/fixtures/currency-amount-precision-explicit/fail_raw_amount/PricingCardRawAmount.tsx` — a PricingCard that renders `{plan.amount}` directly.

See: `practices-react/evals/fixtures/currency-amount-precision-explicit/pass_formatted_amount/PricingCardFormatted.tsx` — correct usage via `formatCurrencyAmount`.
