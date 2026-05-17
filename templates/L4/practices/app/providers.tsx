/*
---
template_id: L4/practices/app/providers
layer: L4
domain: practices
domain_mode: frontend_only
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 practices vertical — minimal client provider tree. No QueryClient or MSW needed: all data is read from static files via Server Components."
  - source_type: external
    citation: "Next.js 15 App Router — Client Components and provider trees"
    url: "https://nextjs.org/docs/app/building-your-application/rendering/client-components"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/payment]
---
*/
'use client'

import React from 'react'

interface ProvidersProps {
  children: React.ReactNode
}

/**
 * Providers — minimal client-side provider tree for the L4 practices vertical.
 *
 * The practices domain is frontend_only: all rule data is read from the
 * filesystem via Server Components, so no QueryClient is needed here.
 *
 * Fork instructions:
 *   1. Add ThemeProvider if you need dark-mode support.
 *   2. Add Toast / notification provider if needed.
 *   3. Add error boundary if needed.
 */
export function Providers({ children }: ProvidersProps) {
  return <>{children}</>
}
