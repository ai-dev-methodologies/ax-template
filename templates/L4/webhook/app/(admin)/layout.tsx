/*
---
template_id: L4/webhook/app/(admin)/layout
layer: L4
domain: webhook
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 webhook vertical — admin route group layout: AppShell + Sidebar (Endpoints / Deliveries tabs). Role gate is enforced via useCallerRole — non-admin viewers see a 'Admin access required' empty state in the child pages."
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

interface WebhookAdminLayoutProps {
  children: React.ReactNode
}

const NAV_ITEMS = [
  { href: '/admin/webhooks', label: 'Endpoints', icon: 'link' as const },
  { href: '/admin/webhooks/deliveries', label: 'Deliveries', icon: 'send' as const },
]

export default function WebhookAdminLayout({ children }: WebhookAdminLayoutProps) {
  return (
    <AppShell
      sidebar={<Sidebar navItems={NAV_ITEMS} />}
      header={<AppHeader title="Webhooks" />}
    >
      {children}
    </AppShell>
  )
}
