/*
---
template_id: L4/practices/app/page
layer: L4
domain: practices
domain_mode: frontend_only
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 practices vertical — root page redirects to /practices list."
  - source_type: external
    citation: "Next.js 15 App Router — redirect() for server-side redirects"
    url: "https://nextjs.org/docs/app/api-reference/functions/redirect"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/payment]
---
*/
import { redirect } from 'next/navigation'

/**
 * Root page — immediately redirects to the practices list.
 *
 * Fork instructions:
 *   Update the redirect target if you rename the practices route.
 */
export default function RootPage() {
  redirect('/practices')
}
