/*
---
template_id: L4/audit-log/app/(audit-log)/layout
layer: L4
domain: audit-log
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 audit-log vertical — route group layout with AppShell (sidebar + header) and breadcrumb slot."
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

interface AuditLogLayoutProps {
  children: React.ReactNode
}

const NAV_ITEMS = [
  { href: '/audit-log', label: 'Audit Log', icon: 'list' as const },
  { href: '/audit-log/export', label: 'Export', icon: 'download' as const },
]

/**
 * AuditLogLayout — route group layout for all audit log pages.
 *
 * Applies:
 *   - AppShell (authenticated shell with sidebar + content area)
 *   - AppHeader with the current page title
 *   - Sidebar with nav items for the audit-log domain
 *
 * Fork instructions:
 *   1. Replace NAV_ITEMS with your navigation structure.
 *   2. Add breadcrumb component if needed.
 *   3. Add session guard (redirect to /login if not authenticated).
 */
export default function AuditLogLayout({ children }: AuditLogLayoutProps) {
  return (
    <AppShell
      sidebar={<Sidebar navItems={NAV_ITEMS} />}
      header={<AppHeader title="Audit Log" />}
    >
      {children}
    </AppShell>
  )
}
