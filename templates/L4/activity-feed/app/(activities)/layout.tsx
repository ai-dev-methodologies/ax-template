/*
---
template_id: L4/activity-feed/app/(activities)/layout
layer: L4
domain: activity-feed
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 activity-feed vertical — route group layout: AppShell + Sidebar (Inbox / Unread filter)."
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

interface ActivitiesLayoutProps {
  children: React.ReactNode
}

const NAV_ITEMS = [
  { href: '/activities', label: 'All activity', icon: 'list' as const },
  { href: '/activities?unread=1', label: 'Unread', icon: 'bell' as const },
]

/**
 * ActivitiesLayout — route group layout for the activity feed.
 *
 * Fork instructions:
 *   1. NAV_ITEMS shape — keep "All" and "Unread" as the two top-level
 *      tabs; add a "Mentions" filter if your verb taxonomy includes
 *      explicit mentions.
 *   2. Surface the unread count as a sidebar badge next to "Unread"
 *      (read it from useQuery(['activity-unread-count']) or piggyback
 *      on the same feed query).
 */
export default function ActivitiesLayout({ children }: ActivitiesLayoutProps) {
  return (
    <AppShell
      sidebar={<Sidebar navItems={NAV_ITEMS} />}
      header={<AppHeader title="Activity" />}
    >
      {children}
    </AppShell>
  )
}
