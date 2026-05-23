/*
---
template_id: L4/api-key/app/(api-key)/layout
layer: L4
domain: api-key
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 api-key vertical — route group layout with AppShell (sidebar + header) and ROLE_ADMIN guard slot."
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

interface ApiKeyLayoutProps {
  children: React.ReactNode
}

const NAV_ITEMS = [
  { href: '/api-key', label: 'API Keys', icon: 'list' as const },
  { href: '/api-key/new', label: 'Create Key', icon: 'plus' as const },
]

/**
 * ApiKeyLayout — route group layout for all api-key admin pages.
 *
 * Applies:
 *   - AppShell (authenticated shell with sidebar + content area)
 *   - AppHeader with the current page title
 *   - Sidebar with nav items for the api-key domain
 *
 * Fork instructions:
 *   1. Replace NAV_ITEMS with your navigation structure.
 *   2. Add a ROLE_ADMIN gate here — api-key management surface is admin-only
 *      (matches the @PreAuthorize gate on the Spring controller).
 *   3. Add breadcrumb component if needed.
 */
export default function ApiKeyLayout({ children }: ApiKeyLayoutProps) {
  return (
    <AppShell
      sidebar={<Sidebar navItems={NAV_ITEMS} />}
      header={<AppHeader title="API Keys" />}
    >
      {children}
    </AppShell>
  )
}
