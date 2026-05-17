/*
---
template_id: L4/file-storage/app/page
layer: L4
domain: file-storage
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 file-storage vertical — root page redirects to /(file-storage)/upload."
  - source_type: external
    citation: "Next.js 15 App Router — redirect() function"
    url: "https://nextjs.org/docs/app/api-reference/functions/redirect"
provenance_class: internal_design
imports_from: []
imports_forbidden: [L4/auth, L4/crud, L4/payment, L4/practices]
---
*/
import { redirect } from 'next/navigation'

/**
 * Root page — redirects to the upload entry point.
 *
 * Fork instructions:
 *   1. Change the redirect target to your preferred entry page.
 *   2. Add a loading spinner if you prefer not to use redirect().
 */
export default function RootPage() {
  redirect('/(file-storage)/upload')
}
