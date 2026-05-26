/*
---
template_id: L4/audit-log/app/providers
layer: L4
domain: audit-log
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 audit-log vertical — client provider tree: QueryClientProvider for server-state."
  - source_type: external
    citation: "TanStack Query v5 — QueryClient and QueryClientProvider setup"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/QueryClientProvider"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
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
 * Providers — client-side provider tree for the L4 audit-log vertical.
 *
 * Wraps the app with:
 *   - QueryClientProvider (TanStack Query v5 for server state)
 *   - MSW (Mock Service Worker) in development only
 *
 * Audit-log QueryClient settings:
 *   - staleTime: 60s (audit data is mostly static; re-read is acceptable delay)
 *   - retry: 2 (audit data is important; worth retrying API failures)
 *
 * Fork instructions:
 *   1. Add your global state providers here.
 *   2. For MSW: create src/mocks/browser.ts + src/mocks/handlers.ts
 *      (mock /api/audit-logs endpoints).
 *   3. Add session provider (next-auth, clerk, etc.) for RBAC role checks on export page.
 *   4. Add ToastQueue provider to surface export submission feedback.
 */
export function Providers({ children }: ProvidersProps) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            // Audit logs are append-only — moderate stale time acceptable
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
