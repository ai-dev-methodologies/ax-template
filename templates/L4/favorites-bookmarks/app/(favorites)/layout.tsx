/*
---
template_id: L4/favorites-bookmarks/app/(favorites)/layout
layer: L4
domain: favorites-bookmarks
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 favorites-bookmarks vertical — route group layout: AppShell + Sidebar (My favorites)."
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

interface FavoritesLayoutProps {
  children: React.ReactNode
}

const NAV_ITEMS = [
  { href: '/favorites', label: 'My favorites', icon: 'star' as const },
]

export default function FavoritesLayout({ children }: FavoritesLayoutProps) {
  return (
    <AppShell
      sidebar={<Sidebar navItems={NAV_ITEMS} />}
      header={<AppHeader title="Favorites" />}
    >
      {children}
    </AppShell>
  )
}
