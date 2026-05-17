/*
---
template_id: L4/payment/app/page
layer: L4
domain: payment
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 payment vertical — root page; redirects to /(payment)/checkout to enter the payment flow."
  - source_type: external
    citation: "Next.js 15 App Router — redirect() from next/navigation"
    url: "https://nextjs.org/docs/app/api-reference/functions/redirect"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices]
---
*/
import { redirect } from 'next/navigation'

/**
 * RootPage — redirects to /checkout.
 *
 * Fork instructions:
 *   1. Change redirect target to your entry point.
 */
export default function RootPage() {
  redirect('/checkout')
}
