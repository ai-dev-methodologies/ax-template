/*
---
template_id: L4/payment/app/(payment)/page
layer: L4
domain: payment
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 payment vertical — payment route group root; redirects to /checkout to start the payment flow."
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
 * PaymentRootPage — redirects to /checkout.
 *
 * Fork instructions:
 *   1. Change redirect target to your entry point (e.g. /methods if you want
 *      to show saved methods first rather than going straight to checkout).
 */
export default function PaymentRootPage() {
  redirect('/checkout')
}
