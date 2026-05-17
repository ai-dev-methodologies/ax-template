/*
---
template_id: L4/auth/app/page
layer: L4
domain: auth
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 auth vertical — root page redirects to /login (unauthenticated default entry point)."
  - source_type: external
    citation: "Next.js App Router — redirect() from next/navigation"
    url: "https://nextjs.org/docs/app/api-reference/functions/redirect"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [other L4 domains]
---
*/
import { redirect } from 'next/navigation'

/**
 * RootPage — redirect root URL to /login.
 *
 * Fork instructions:
 *   - Change target to '/dashboard' if your app defaults to the authenticated view
 *   - Or implement a smart redirect that checks auth state server-side
 */
export default function RootPage() {
  redirect('/login')
}
