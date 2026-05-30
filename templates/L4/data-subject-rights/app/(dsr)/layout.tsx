/*
---
template_id: L4/data-subject-rights/app/(dsr)/layout
layer: L4
domain: data-subject-rights
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 data-subject-rights vertical — dsr route group layout with AppShell (sidebar + header) and nav for each data-subject right."
  - source_type: external
    citation: "Next.js 15 App Router — route groups and layouts"
    url: "https://nextjs.org/docs/app/building-your-application/routing/route-groups"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [other L4 domains]
---
*/
import React from 'react'
import AppShell from 'templates/L2/blocks/app-shell'
import AppHeader from 'templates/L2/blocks/app-header'
import Sidebar from 'templates/L2/blocks/sidebar'

interface DsrLayoutProps {
  children: React.ReactNode
}

const NAV_ITEMS = [
  { href: '/privacy', label: 'My requests', icon: 'list' as const },
  { href: '/privacy/access', label: 'Access', icon: 'list' as const },
  { href: '/privacy/rectify', label: 'Rectify', icon: 'list' as const },
  { href: '/privacy/portability', label: 'Export', icon: 'list' as const },
  { href: '/privacy/restrict', label: 'Restrict', icon: 'list' as const },
  { href: '/privacy/erasure', label: 'Erase', icon: 'list' as const },
]

/**
 * DsrLayout — route group layout for all privacy-console pages.
 *
 * Applies:
 *   - AppShell (authenticated shell with sidebar + content area)
 *   - AppHeader with the console title
 *   - Sidebar with one nav item per data-subject right
 *
 * Fork instructions:
 *   1. Update NAV_ITEMS to match the rights your service exposes.
 *   2. Add user profile / logout action to AppHeader.
 *   3. Replace Sidebar with your navigation component if needed.
 */
export default function DsrLayout({ children }: DsrLayoutProps) {
  return (
    <AppShell
      sidebarSlot={
        <Sidebar
          navItems={NAV_ITEMS}
          title="Privacy & Data Rights"
        />
      }
      headerSlot={
        <AppHeader title="Privacy & Data Rights" />
      }
    >
      {children}
    </AppShell>
  )
}
