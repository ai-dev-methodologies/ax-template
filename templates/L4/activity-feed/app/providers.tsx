/*
---
template_id: L4/activity-feed/app/providers
layer: L4
domain: activity-feed
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 activity-feed vertical — client provider tree: QueryClientProvider for server-state."
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

interface ProvidersProps {
  children: React.ReactNode
}

/**
 * Providers — client-side provider tree for the L4 activity-feed vertical.
 *
 * Activity QueryClient settings:
 *   - staleTime: 10s (feed needs to refresh quickly — a newly-published
 *                     activity should appear in the inbox within a tick;
 *                     mark-read state must propagate to the unread badge)
 *   - refetchInterval: 30s (background polling so the inbox does not
 *                           need a manual refresh while the tab is open)
 *   - retry: 1
 *
 * Fork instructions:
 *   1. Replace the 30s poll with SSE / WebSocket push if your stack
 *      supports it — polling is a baseline for the catalog template.
 *   2. Add session provider so useCallerId can read the live caller id.
 *   3. ToastQueue provider recommended — mark-all-read should surface
 *      "N marked" feedback briefly.
 */
export function Providers({ children }: ProvidersProps) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 10 * 1000,
            refetchInterval: 30 * 1000,
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
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  )
}
