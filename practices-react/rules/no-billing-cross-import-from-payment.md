---
title: "billing UI components must not import from payment UI components and vice versa; the L4/billing ↔ L4/payment boundary is enforced by ESLint"
rule_id: no-billing-cross-import-from-payment
impact: HIGH
impactDescription: "Cross-importing between billing and payment UI components couples two separate checkout flows. A payment UI change (e.g., PaymentMethodSelector) should never force billing UI changes (e.g., PricingTable). Subscription UI (billing) and one-shot checkout UI (payment) are independent user flows."
tags:
  - billing
  - payment
  - boundary
  - cross-import
  - l4
applicable_to:
  - react
  - nextjs
provenance_class: internal_design
protects_template_id: templates/L4/billing/app/(billing)/subscriptions/page.tsx
spec_ref: "specs/billing-frontend-l0.yaml#BILLING-FE-004"
verification:
  type: review
  notes: |
    Review-tier / project ESLint config (no shipped ax rule): wire eslint import/no-restricted-paths so
    L4/billing/** must not import from L4/payment/**
    L4/payment/** must not import from L4/billing/**
    L2/billing/** must not import from L2/payment/**
    Shared L1 and L2 neutral blocks are allowed from both.
    Failing fixture: a billing page importing PaymentMethodSelector from payment UI.
evidence:
  - source_type: external
    citation: "Domain-Driven Design (Evans): Bounded contexts have explicit boundaries. UI components are part of the presentation layer of a bounded context; cross-importing presentation components couples contexts at the view layer."
    url: "https://martinfowler.com/bliki/BoundedContext.html"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "Next.js App Router documentation: Route groups allow separate domain-specific layouts. L4/billing/(billing)/** and L4/payment/(payment)/** are intentionally separate route groups with separate layouts."
    url: "https://nextjs.org/docs/app/building-your-application/routing/route-groups"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## billing UI ↔ payment UI cross-import is prohibited

**Impact: HIGH — billing and payment are separate UI flows. `L4/billing` handles subscription lifecycle (pricing plans, subscription management, invoices). `L4/payment` handles one-shot checkout (payment method entry, single charge confirmation). Cross-importing couples these flows at the component level.**

### Allowed import directions

```
L1 components (currency-input, number-input, range-picker)
    ↑ allowed from both L4/billing and L4/payment
L2 neutral blocks (pagination, data-table, form elements)
    ↑ allowed from both

L4/billing/* → L2/billing blocks only → L1
L4/payment/* → L2/payment blocks only → L1

L4/billing/* ↛ L4/payment/*  (FORBIDDEN)
L4/payment/* ↛ L4/billing/*  (FORBIDDEN)
```

### Incorrect — billing page imports payment component

```tsx
// ❌ WRONG: billing subscription page importing from payment domain
// app/(billing)/subscriptions/new/page.tsx

import { PaymentMethodSelector } from '@/templates/L4/payment/components/PaymentMethodSelector'  // ← VIOLATION
import { CheckoutButton } from '@/app/(payment)/checkout/CheckoutButton'  // ← VIOLATION

export default function NewSubscriptionPage() {
  return (
    <div>
      <PlanSelector />
      <PaymentMethodSelector />  {/* billing should not embed payment UI */}
    </div>
  )
}
```

### Incorrect — payment checkout imports billing plan data

```tsx
// ❌ WRONG: payment checkout embedding billing plan display
// app/(payment)/checkout/page.tsx

import PricingTable from '@/templates/L2/blocks/pricing-table'  // ← VIOLATION if billing-specific
import { SubscriptionSummary } from '@/templates/L4/billing/components/SubscriptionSummary'  // ← VIOLATION

export default function CheckoutPage() {
  return (
    <div>
      <SubscriptionSummary />  {/* payment checkout should not know billing internals */}
    </div>
  )
}
```

### Correct — separate flows, independent components

```tsx
// ✅ CORRECT: billing subscription page only uses billing/L1/L2 imports
// templates/L4/billing/app/(billing)/subscriptions/new/page.tsx

import { PricingTable } from '@/templates/L2/blocks/pricing-table'
import { CurrencyInput } from '@/templates/L1/components/currency-input'
// No payment imports

export default function NewSubscriptionPage() {
  return (
    <div>
      <h1>구독 신청</h1>
      <PricingTable plans={[]} />
      {/* User selects plan → POST /api/subscriptions */}
      {/* Payment method handled separately by payment domain */}
    </div>
  )
}
```

```tsx
// ✅ CORRECT: payment checkout only uses payment/L1/L2 imports
// app/(payment)/checkout/page.tsx

import { CardNumberInput } from '@/templates/L2/blocks/card-number-input'
// No billing imports

export default function CheckoutPage() {
  return (
    <div>
      <h1>결제</h1>
      <CardNumberInput />
    </div>
  )
}
```

### Coordination via URLs, not imports

If a user flow moves from billing (select plan) → payment (enter card), use **navigation** not imports:

```tsx
// ✅ CORRECT: billing page navigates to payment flow via URL
import { useRouter } from 'next/navigation'

function PlanSelectButton({ planId }: { planId: string }) {
  const router = useRouter()
  const handleSelect = () => {
    // Navigate to payment flow — no payment component import needed
    router.push(`/payment/checkout?planId=${planId}&flow=subscription`)
  }
  return <button onClick={handleSelect}>구독 시작</button>
}
```

## ESLint enforcement (import/no-restricted-paths)

```js
// eslint.config.js — add to billing context
{
  rules: {
    'import/no-restricted-paths': ['error', {
      zones: [
        {
          target: './templates/L4/billing',
          from: './templates/L4/payment',
          message: 'billing UI must not import from payment UI (§5.2.6 boundary)'
        },
        {
          target: './app/(billing)',
          from: './app/(payment)',
          message: 'billing route group must not import from payment route group'
        }
      ]
    }]
  }
}
```

## Failing fixture

Illustrative FAIL shape: a billing page importing `PaymentMethodSelector` from the payment domain (no dedicated frontend fixture shipped — the Java boundary fixture lives at practices/evals/fixtures/no-billing-cross-import-from-payment/).

Illustrative PASS shape: a billing page with no payment-domain imports (verified at review).
