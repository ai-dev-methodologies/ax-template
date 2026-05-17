/*
---
template_id: L4/payment/app/providers
layer: L4
domain: payment
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 payment vertical — client provider tree: QueryClientProvider for server-state + MSW conditional for dev mock server."
  - source_type: external
    citation: "TanStack Query v5 — QueryClient and QueryClientProvider setup"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/QueryClientProvider"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices]
---
*/
'use client'

import React, { useState, useEffect } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

interface ProvidersProps {
  children: React.ReactNode
}

/**
 * Providers — client-side provider tree for the L4 payment vertical.
 *
 * Wraps the app with:
 *   - QueryClientProvider (TanStack Query v5 for server state)
 *   - MSW (Mock Service Worker) in development only
 *
 * Payment-specific QueryClient settings:
 *   - staleTime: 30s (payments data changes frequently — less caching than CRUD)
 *   - retry: 1 (single retry; payment state can change between retries)
 *
 * Fork instructions:
 *   1. Add your global state providers here.
 *   2. For MSW: create src/mocks/browser.ts + src/mocks/handlers.ts
 *      (mock /api/payments endpoints).
 *   3. Add error boundary and toast provider as needed.
 *   4. Add session provider (next-auth, clerk, etc.) if not handled upstream.
 */
export function Providers({ children }: ProvidersProps) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            // Payment data changes frequently — use shorter stale time than CRUD
            staleTime: 30 * 1000, // 30 seconds
            retry: 1,
          },
        },
      })
  )

  // MSW: start mock server in development only
  useEffect(() => {
    if (process.env.NODE_ENV === 'development') {
      import('../mocks/browser')
        .then(({ worker }) => worker.start({ onUnhandledRequest: 'bypass' }))
        .catch(() => {
          // MSW not configured — safe to ignore in production fork
        })
    }
  }, [])

  return (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  )
}
