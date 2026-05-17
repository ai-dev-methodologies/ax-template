/*
---
template_id: L4/practices/app/(practices)/layout
layer: L4
domain: practices
domain_mode: frontend_only
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 practices vertical — practices route group layout with AppShell (sidebar + header) for the practices catalog viewer."
  - source_type: external
    citation: "Next.js 15 App Router — route groups and layouts"
    url: "https://nextjs.org/docs/app/building-your-application/routing/route-groups"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/payment]
---
*/
import React from 'react'
import AppShell from 'templates/L2/blocks/app-shell'
import AppHeader from 'templates/L2/blocks/app-header'
import Sidebar from 'templates/L2/blocks/sidebar'

interface PracticesLayoutProps {
  children: React.ReactNode
}

const NAV_ITEMS = [
  { href: '/practices', label: 'All Rules', icon: 'list' as const },
  { href: '/practices/category/async', label: 'Async', icon: 'list' as const },
  { href: '/practices/category/cache', label: 'Caching', icon: 'list' as const },
  { href: '/practices/category/security', label: 'Security', icon: 'list' as const },
  { href: '/practices/category/bundle', label: 'Bundle', icon: 'list' as const },
]

/**
 * PracticesLayout — route group layout for all practices pages.
 *
 * Applies:
 *   - AppShell (shell with sidebar + content area)
 *   - AppHeader with domain title
 *   - Sidebar with nav items for the practices catalog
 *
 * Fork instructions:
 *   1. Update NAV_ITEMS to expose the category prefixes relevant to your fork.
 *   2. Add user profile / logout action to AppHeader if needed.
 *   3. loadAllPrefixes() can be called server-side to auto-generate nav items.
 */
export default function PracticesLayout({ children }: PracticesLayoutProps) {
  return (
    <AppShell
      sidebarSlot={
        <Sidebar
          navItems={NAV_ITEMS}
          title="Practices"
        />
      }
      headerSlot={
        <AppHeader title="Practices Catalog" />
      }
    >
      {children}
    </AppShell>
  )
}
