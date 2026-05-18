/*
---
template_id: L4/billing/app/(billing)/layout
layer: L4
domain: billing
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 billing vertical — billing route group layout with AppShell and navigation."
  - source_type: external
    citation: "Next.js 15 App Router — route groups and layouts"
    url: "https://nextjs.org/docs/app/building-your-application/routing/route-groups"
    quoted_at: "2026-05-18"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/payment]
---
*/
import React from 'react'
import AppShell from '@/templates/L2/blocks/app-shell'
import AppHeader from '@/templates/L2/blocks/app-header'
import Sidebar from '@/templates/L2/blocks/sidebar'

const BILLING_NAV = [
  { href: '/billing/pricing', label: '요금제', icon: 'tag' as const },
  { href: '/billing/subscriptions', label: '구독 관리', icon: 'credit-card' as const },
  { href: '/billing/invoices', label: '청구서', icon: 'file-text' as const },
  { href: '/billing/events', label: '결제 내역', icon: 'list' as const },
]

/**
 * BillingLayout — route group layout for all billing pages.
 *
 * Fork instructions:
 * 1. Update BILLING_NAV to match your billing flow navigation.
 * 2. Add subscription status indicator to AppHeader (e.g., "ACTIVE" badge).
 * 3. Wrap with your auth guard if billing pages require authentication.
 */
export default function BillingLayout({ children }: { children: React.ReactNode }) {
  return (
    <AppShell
      sidebarSlot={<Sidebar navItems={BILLING_NAV} title="청구 관리" />}
      headerSlot={<AppHeader title="청구" />}
    >
      {children}
    </AppShell>
  )
}
