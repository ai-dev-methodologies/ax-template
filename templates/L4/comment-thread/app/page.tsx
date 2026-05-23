/*
---
template_id: L4/comment-thread/app/page
layer: L4
domain: comment-thread
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 comment-thread vertical — root page redirects to /(comments) demo entity."
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
 * CommentRootPage — redirects to a sample entity's comment thread.
 *
 * Fork instructions:
 *   1. Replace the demo entity with your real (entityType, entityId).
 *      Comments are polymorphic — any (type, id) pair works.
 */
export default function CommentRootPage() {
  redirect('/comments/post/sample')
}
