/*
---
template_id: L4/tag-categorization/app/page
layer: L4
domain: tag-categorization
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 tag-categorization vertical — root page redirects to /tags library."
  - source_type: external
    citation: "Next.js 15 App Router — redirect() from next/navigation"
    url: "https://nextjs.org/docs/app/api-reference/functions/redirect"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
---
*/
import { redirect } from 'next/navigation'

export default function TagRootPage() {
  redirect('/tags')
}
