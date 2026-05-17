/*
---
template_id: L4/practices/app/layout
layer: L4
domain: practices
domain_mode: frontend_only
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 practices vertical — root layout: html/body shell, minimal Providers wrapper (no QueryClient needed for frontend_only domain)."
  - source_type: external
    citation: "Next.js 15 App Router — root layout requirements (html + body tags)"
    url: "https://nextjs.org/docs/app/building-your-application/routing/layouts-and-templates"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/payment]
---
*/
import React from 'react'
import type { Metadata } from 'next'
import { Providers } from './providers'

export const metadata: Metadata = {
  title: 'Practices Catalog',
  description: 'Java + React best-practice rules — built with ax-template',
}

/**
 * RootLayout — L4 practices vertical root layout.
 *
 * Fork instructions:
 *   1. Update metadata.title / description.
 *   2. Add your font imports (next/font/google).
 *   3. Import your global CSS (design tokens, base styles).
 *   4. Add analytics / monitoring providers inside Providers.
 */
export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className="min-h-svh bg-background font-sans antialiased">
        <Providers>
          {children}
        </Providers>
      </body>
    </html>
  )
}
