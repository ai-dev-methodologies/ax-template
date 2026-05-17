/*
---
template_id: L4/notification/app/page
layer: L4
domain: notification
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 notification vertical — root redirect to /(notification)/inbox."
  - source_type: external
    citation: "Next.js 15 App Router — redirect() from next/navigation"
    url: "https://nextjs.org/docs/app/api-reference/functions/redirect"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/payment, L4/practices]
---
*/
import { redirect } from 'next/navigation'

/**
 * Root page — immediately redirects to the notification inbox.
 *
 * Fork instructions:
 *   Update the redirect target if your inbox is mounted at a different path.
 */
export default function RootPage() {
  redirect('/inbox')
}
