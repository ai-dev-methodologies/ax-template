# L3 / pricing-page

Billing plan selection page composing `PricingTable` + `PlanComparison`.

## Files

| File | Purpose |
|---|---|
| `page.tsx` | Main page component — accepts `plans`, `onSelectPlan`, optional `comparisonFeatures`. |
| `loading.tsx` | Next.js Suspense loading skeleton. |
| `error.tsx` | Next.js error boundary for the route segment. |

## L4 Usage

```tsx
// app/(billing)/pricing/page.tsx
import PricingPage from '@/templates/L3/pages/pricing-page/page'

export default async function PricingRoute() {
  const plans = await fetchPlans()        // GET /api/admin/billing/plans
  const currentPlanId = await getSubscriptionPlanId()

  return (
    <PricingPage
      plans={plans}
      currentPlanId={currentPlanId}
      onSelectPlan={async (planId) => {
        'use server'
        // POST /api/subscriptions with Idempotency-Key
      }}
    />
  )
}
```

## Evidence

- Audit C.2 P1: pricing-page is required for billing SaaS fork day-1.
- `specs/billing-frontend-l0.yaml#BILLING-FE-001`: amounts via CurrencyFormatter (integer minor units).
- `specs/billing-frontend-l0.yaml#BILLING-FE-003`: accessible table headers (scope=col), CTA aria-label.

## Boundary

- Imports: L1, L2 only.
- Must NOT import from L4 or `lib/payment/`.
- Rule: `no-billing-cross-import-from-payment`.
