/*
---
template_id: L4/billing/app/layout
layer: L4
domain: billing
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 billing vertical — root app layout with providers (QueryClient, AuthProvider)."
  - source_type: external
    citation: "Next.js 15 App Router — root layout wraps all pages"
    url: "https://nextjs.org/docs/app/building-your-application/routing/layouts-and-templates"
    quoted_at: "2026-05-18"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/payment]
---
*/
import type { Metadata } from 'next'
import React from 'react'

export const metadata: Metadata = {
  title: '청구 — ax-template',
  description: 'Subscription and billing management',
}

export default function BillingRootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body>{children}</body>
    </html>
  )
}
