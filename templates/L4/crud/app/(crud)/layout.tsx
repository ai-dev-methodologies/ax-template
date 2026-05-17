/*
---
template_id: L4/crud/app/(crud)/layout
layer: L4
domain: crud
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 crud vertical — crud route group layout with AppShell (sidebar + header) and breadcrumb slot."
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

interface CrudLayoutProps {
  children: React.ReactNode
}

const NAV_ITEMS = [
  { href: '/items', label: 'Items', icon: 'list' as const },
]

/**
 * CrudLayout — route group layout for all crud pages.
 *
 * Applies:
 *   - AppShell (authenticated shell with sidebar + content area)
 *   - AppHeader with the current page title
 *   - Sidebar with nav items for the crud domain
 *
 * Fork instructions:
 *   1. Update NAV_ITEMS to match your domain's resource routes.
 *   2. Add user profile / logout action to AppHeader.
 *   3. Replace Sidebar with your navigation component if needed.
 */
export default function CrudLayout({ children }: CrudLayoutProps) {
  return (
    <AppShell
      sidebarSlot={
        <Sidebar
          navItems={NAV_ITEMS}
          title="Items App"
        />
      }
      headerSlot={
        <AppHeader title="Items" />
      }
    >
      {children}
    </AppShell>
  )
}
