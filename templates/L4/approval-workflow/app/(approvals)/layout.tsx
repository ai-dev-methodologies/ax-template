/*
---
template_id: L4/approval-workflow/app/(approvals)/layout
layer: L4
domain: approval-workflow
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 approval-workflow vertical — route group layout: AppShell + Sidebar (Inbox / My requests / New request)."
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

interface ApprovalsLayoutProps {
  children: React.ReactNode
}

const NAV_ITEMS = [
  // R43 iter1 (P1-F10): labels disambiguated. "Inbox" / "My" were too
  // ambiguous for Korean-enterprise users where "결재함" splits into
  // received-for-approval vs filed-by-me.
  { href: '/approvals/inbox', label: 'Pending my approval', icon: 'inbox' as const },
  { href: '/approvals/my', label: 'My filed requests', icon: 'list' as const },
  { href: '/approvals/new', label: 'New request', icon: 'plus' as const },
]

/**
 * ApprovalsLayout — route group layout for approval-workflow pages.
 *
 * Information architecture:
 *   - Inbox    : steps that are currently PENDING and assigned to caller
 *                (the action-driven surface, the default landing)
 *   - My       : requests the caller filed (regardless of status)
 *   - New      : draft a new request
 *
 * Fork instructions:
 *   1. If your forks split requester / approver into distinct roles, you
 *      may want to hide "New request" for approver-only users.
 *   2. Add a count badge on Inbox showing pending step count — that is
 *      the single most useful affordance for approvers.
 */
export default function ApprovalsLayout({ children }: ApprovalsLayoutProps) {
  return (
    <AppShell
      sidebar={<Sidebar navItems={NAV_ITEMS} />}
      header={<AppHeader title="Approvals" />}
    >
      {children}
    </AppShell>
  )
}
