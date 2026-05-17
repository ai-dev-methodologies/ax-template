/*
---
template_id: L4/payment/app/(payment)/layout
layer: L4
domain: payment
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 payment vertical — payment route group layout with AppShell (sidebar + header) and breadcrumb slot."
  - source_type: external
    citation: "Next.js 15 App Router — route groups and layouts"
    url: "https://nextjs.org/docs/app/building-your-application/routing/route-groups"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices]
---
*/
import React from 'react'
import AppShell from 'templates/L2/blocks/app-shell'
import AppHeader from 'templates/L2/blocks/app-header'
import Sidebar from 'templates/L2/blocks/sidebar'

interface PaymentLayoutProps {
  children: React.ReactNode
}

const NAV_ITEMS = [
  { href: '/checkout', label: 'Checkout', icon: 'credit-card' as const },
  { href: '/methods', label: 'Payment History', icon: 'list' as const },
]

/**
 * PaymentLayout — route group layout for all payment pages.
 *
 * Applies:
 *   - AppShell (authenticated shell with sidebar + content area)
 *   - AppHeader with the current page title
 *   - Sidebar with nav items for the payment domain
 *
 * Fork instructions:
 *   1. Update NAV_ITEMS to match your payment flow's navigation requirements.
 *   2. Add user profile / logout action to AppHeader.
 *   3. Add trust signals (SSL badge, PCI DSS logo) to AppHeader or Sidebar footer.
 */
export default function PaymentLayout({ children }: PaymentLayoutProps) {
  return (
    <AppShell
      sidebarSlot={
        <Sidebar
          navItems={NAV_ITEMS}
          title="Payments"
        />
      }
      headerSlot={
        <AppHeader title="Payments" />
      }
    >
      {children}
    </AppShell>
  )
}
