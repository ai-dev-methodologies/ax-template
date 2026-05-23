/*
---
template_id: L4/tag-categorization/app/layout
layer: L4
domain: tag-categorization
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 tag-categorization vertical — root layout: html/body shell, Providers wrapper."
  - source_type: external
    citation: "Next.js 15 App Router — root layout requirements (html + body tags)"
    url: "https://nextjs.org/docs/app/building-your-application/routing/layouts-and-templates"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
---
*/
import React from 'react'
import type { Metadata } from 'next'
import { Providers } from './providers'

export const metadata: Metadata = {
  title: 'Tags',
  description: 'Built with ax-template',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className="min-h-svh bg-background font-sans antialiased">
        <Providers>{children}</Providers>
      </body>
    </html>
  )
}
