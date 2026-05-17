/*
---
template_id: L4/crud/app/page
layer: L4
domain: crud
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 crud vertical — root page redirects to /(crud)/items list."
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
 * Root page — immediately redirects to the items list.
 *
 * Fork instructions:
 *   Update the redirect target if your resource is not named "items".
 */
export default function RootPage() {
  redirect('/items')
}
