/*
---
template_id: L4/data-subject-rights/app/page
layer: L4
domain: data-subject-rights
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 data-subject-rights vertical — root page redirects to /(dsr)/privacy console."
  - source_type: external
    citation: "Next.js 15 App Router — redirect() for server-side redirects"
    url: "https://nextjs.org/docs/app/api-reference/functions/redirect"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [other L4 domains]
---
*/
import { redirect } from 'next/navigation'

/**
 * Root page — immediately redirects to the privacy console.
 *
 * Fork instructions:
 *   Update the redirect target if your console route differs.
 */
export default function RootPage() {
  redirect('/privacy')
}
