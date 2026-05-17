/*
---
template_id: L4/file-storage/app/(file-storage)/page
layer: L4
domain: file-storage
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 file-storage domain index page — redirects to upload entry point."
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
 * File-storage domain index — redirects to the upload page.
 *
 * Fork instructions:
 *   1. Change redirect target to your preferred landing page (e.g., /files for list-first UX).
 */
export default function FileStorageIndexPage() {
  redirect('/upload')
}
