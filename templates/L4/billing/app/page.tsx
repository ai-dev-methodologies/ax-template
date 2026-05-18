/*
---
template_id: L4/billing/app/page
layer: L4
domain: billing
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 billing root — redirects to /billing/pricing."
  - source_type: external
    citation: "Next.js 15 App Router — redirect() from next/navigation"
    url: "https://nextjs.org/docs/app/api-reference/functions/redirect"
    quoted_at: "2026-05-18"
provenance_class: internal_design
imports_from: []
imports_forbidden: [L4/auth, L4/payment]
---
*/
import { redirect } from 'next/navigation'

/**
 * BillingRootPage — redirects to /billing/pricing.
 *
 * Fork: change redirect to /billing/subscriptions if users go to subscription
 * management directly rather than the public pricing page.
 */
export default function BillingRootPage() {
  redirect('/billing/pricing')
}
