/*
---
template_id: L4/session-management/app/page
layer: L4
domain: session-management
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 session-management vertical — root page redirects to /(session) list."
  - source_type: external
    citation: "Next.js 15 App Router — redirect() from next/navigation"
    url: "https://nextjs.org/docs/app/api-reference/functions/redirect"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
---
*/
import { redirect } from 'next/navigation'

/**
 * SessionRootPage — redirects to the session list.
 *
 * Fork instructions:
 *   1. Change redirect target if your entry point differs (e.g., /admin/sessions).
 */
export default function SessionRootPage() {
  redirect('/sessions')
}
