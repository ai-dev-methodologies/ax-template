/*
---
template_id: L4/audit-log/app/page
layer: L4
domain: audit-log
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 audit-log vertical — root page redirects to /(audit-log) list."
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
 * AuditLogRootPage — redirects to the audit log list.
 *
 * Fork instructions:
 *   1. Change redirect target if your entry point differs.
 */
export default function AuditLogRootPage() {
  redirect('/audit-log')
}
