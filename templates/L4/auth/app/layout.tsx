/*
---
template_id: L4/auth/app/layout
layer: L4
domain: auth
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 auth vertical — root layout: html/body shell, Providers wrapper, design token application via ui-tokens manifest."
  - source_type: external
    citation: "Next.js 15 App Router — root layout requirements (html + body tags)"
    url: "https://nextjs.org/docs/app/building-your-application/routing/layouts-and-templates"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [other L4 domains]
---
*/
import React from 'react'
import type { Metadata } from 'next'
import { Providers } from './providers'

export const metadata: Metadata = {
  title: 'App',
  description: 'Built with ax-template',
}

/**
 * RootLayout — L4 auth vertical root layout.
 *
 * Applies:
 *   - HTML lang attribute (update to your locale)
 *   - Tailwind body base styles (bg-background, text-foreground)
 *   - Providers wrapper (QueryClient + MSW)
 *
 * Fork instructions:
 *   1. Update metadata.title / description
 *   2. Add your font imports (next/font/google)
 *   3. Import your global CSS (design tokens, base styles)
 *   4. Add any analytics, monitoring, or feature-flag providers inside Providers
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
