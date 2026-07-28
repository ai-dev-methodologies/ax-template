/*
---
template_id: L4/webhook/app/providers
layer: L4
domain: webhook
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 webhook vertical — client provider tree: QueryClientProvider for server-state."
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
 * Providers — client-side provider tree for the L4 webhook vertical.
 *
 * Webhook QueryClient settings:
 *   - staleTime: 15s for endpoints (mostly static after registration)
 *   - refetchInterval: deliveries page sets its own 10s poll since delivery
 *     status transitions during retry windows. Canonical delivery-status
 *     vocabulary (exhaustively gated by contract_enum_parity_guard):
 *     vocab:delivery-status:start
 *     PENDING / PENDING_RETRY / SUCCEEDED / FAILED_PERMANENT
 *     vocab:delivery-status:end
 *   - retry: 1
 */
export function Providers({ children }: ProvidersProps) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 15 * 1000,
            retry: 1,
          },
        },
      })
  )

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
    <QueryClientProvider client={queryClient}><RateLimitBannerProvider>{children}</RateLimitBannerProvider></QueryClientProvider>
  )
}
