/*
---
template_id: L4/session-management/app/(sessions)/layout
layer: L4
domain: session-management
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 session-management vertical — route group layout: AppShell + Sidebar (My / Admin tabs)."
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

interface SessionsLayoutProps {
  children: React.ReactNode
}

const NAV_ITEMS = [
  { href: '/sessions', label: 'My Sessions', icon: 'list' as const },
  { href: '/sessions/admin', label: 'Admin (force logout)', icon: 'shield' as const },
]

/**
 * SessionsLayout — route group layout for session pages.
 *
 * Applies:
 *   - AppShell (authenticated shell with sidebar + content area)
 *   - AppHeader with current page title
 *   - Sidebar with nav items (My sessions / Admin)
 *
 * Fork instructions:
 *   1. The /sessions/admin route MUST be gated by ROLE_ADMIN; the Spring
 *      AdminSessionController already enforces it server-side, but a UI gate
 *      is defense-in-depth.
 *   2. Replace NAV_ITEMS with your navigation structure.
 *   3. Add breadcrumb component if needed.
 */
export default function SessionsLayout({ children }: SessionsLayoutProps) {
  return (
    <AppShell
      sidebar={<Sidebar navItems={NAV_ITEMS} />}
      header={<AppHeader title="Sessions" />}
    >
      {children}
    </AppShell>
  )
}
