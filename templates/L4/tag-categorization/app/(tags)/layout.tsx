/*
---
template_id: L4/tag-categorization/app/(tags)/layout
layer: L4
domain: tag-categorization
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 tag-categorization vertical — route group layout: AppShell + Sidebar (Library / Attach to entity)."
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

interface TagsLayoutProps {
  children: React.ReactNode
}

const NAV_ITEMS = [
  { href: '/tags', label: 'Tag library', icon: 'list' as const },
  // R45: the attach surface lives at /tags/by-entity/{type}/{id}. We
  // do NOT put it in the sidebar because there is no single canonical
  // entity to land on — attach is reached from the host entity's UI.
]

export default function TagsLayout({ children }: TagsLayoutProps) {
  return (
    <AppShell
      sidebar={<Sidebar navItems={NAV_ITEMS} />}
      header={<AppHeader title="Tags" />}
    >
      {children}
    </AppShell>
  )
}
