/*
---
template_id: L4/crud/app/(crud)/page
layer: L4
domain: crud
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 crud vertical — crud route group root redirects to /items list."
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
 * Crud route group root — redirects to the items list.
 *
 * Fork instructions:
 *   Update the redirect target if your list route differs.
 */
export default function CrudRootPage() {
  redirect('/items')
}
