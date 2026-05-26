/*
---
template_id: L4/file-storage/app/providers
layer: L4
domain: file-storage
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 file-storage vertical — client provider tree: QueryClientProvider for server-state + MSW conditional for dev mock server."
  - source_type: external
    citation: "TanStack Query v5 — QueryClient and QueryClientProvider setup"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/QueryClientProvider"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/payment, L4/practices]
---
*/
'use client'

import React, { useState, useEffect } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { RateLimitBannerProvider } from 'templates/L2/blocks/rate-limit-banner'

interface ProvidersProps {
  children: React.ReactNode
}

/**
 * Providers — client-side provider tree for the L4 file-storage vertical.
 *
 * Wraps the app with:
 *   - QueryClientProvider (TanStack Query v5 for server state)
 *   - MSW (Mock Service Worker) in development only
 *
 * File-storage QueryClient settings (from blueprints/file-storage-ui-manifest.yaml#query):
 *   - staleTime: 60s (files change less frequently than payments)
 *   - retry: 2
 *
 * Fork instructions:
 *   1. Add global state providers here.
 *   2. For MSW: create src/mocks/browser.ts + handlers for /api/files endpoints.
 *   3. Add error boundary and toast provider as needed.
 *   4. Add session provider (next-auth, clerk, etc.) if not handled upstream.
 */
export function Providers({ children }: ProvidersProps) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            // File metadata changes less often than payment data
            staleTime: 60 * 1000, // 60 seconds
            retry: 2,
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
      <RateLimitBannerProvider>{children}</RateLimitBannerProvider>
    </QueryClientProvider>
  )
}
