/*
---
template_id: L4/approval-workflow/app/page
layer: L4
domain: approval-workflow
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 approval-workflow vertical — root page redirects to inbox (most action-driven surface)."
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
 * ApprovalsRootPage — redirects to the caller's pending inbox.
 *
 * Default entry is /approvals/inbox because the action-driven surface
 * (steps you need to approve right now) is the most common reason a
 * user opens this app. The "my filed requests" view is a secondary tab.
 */
export default function ApprovalsRootPage() {
  redirect('/approvals/inbox')
}
