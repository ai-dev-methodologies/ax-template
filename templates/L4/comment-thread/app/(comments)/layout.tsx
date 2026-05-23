/*
---
template_id: L4/comment-thread/app/(comments)/layout
layer: L4
domain: comment-thread
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 comment-thread vertical — route group layout: AppShell + Sidebar (entity navigator)."
  - source_type: external
    citation: "Next.js 15 App Router — route groups and layouts"
    url: "https://nextjs.org/docs/app/building-your-application/routing/route-groups"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
---
*/
import React from 'react'
import AppShell from 'templates/L2/blocks/app-shell'
import AppHeader from 'templates/L2/blocks/app-header'
import Sidebar from 'templates/L2/blocks/sidebar'

interface CommentsLayoutProps {
  children: React.ReactNode
}

const NAV_ITEMS = [
  { href: '/comments/post/sample', label: 'Sample post', icon: 'message-square' as const },
]

/**
 * CommentsLayout — route group layout for comment-thread pages.
 *
 * Applies:
 *   - AppShell (authenticated shell with sidebar + content area)
 *   - AppHeader with current page title
 *   - Sidebar with entity navigator (sample shown — replace with real entities)
 *
 * Fork instructions:
 *   1. NAV_ITEMS should be derived from your real (entityType, entityId)
 *      list — e.g. recent posts, open tickets, in-review documents.
 *   2. Add a "your comments" view route if needed (filtered list across
 *      all entities authored by the caller).
 *   3. Comments are polymorphic — there is no "comments root" entity.
 *      The route always lives under /comments/{entityType}/{entityId}.
 */
export default function CommentsLayout({ children }: CommentsLayoutProps) {
  return (
    <AppShell
      sidebar={<Sidebar navItems={NAV_ITEMS} />}
      header={<AppHeader title="Comments" />}
    >
      {children}
    </AppShell>
  )
}
