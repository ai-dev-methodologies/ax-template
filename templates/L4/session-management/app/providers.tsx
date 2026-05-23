/*
---
template_id: L4/session-management/app/providers
layer: L4
domain: session-management
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 session-management vertical — client provider tree: QueryClientProvider for server-state."
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
 * Providers — client-side provider tree for the L4 session-management vertical.
 *
 * Wraps the app with:
 *   - QueryClientProvider (TanStack Query v5 for server state)
 *   - MSW (Mock Service Worker) in development only
 *
 * Session QueryClient settings:
 *   - staleTime: 10s (sessions need fresh status — force-logout must show
 *                     promptly; an admin clicking revoke must see the row
 *                     flip to REVOKED within a heartbeat tick)
 *   - retry: 1 (admin operations are user-driven; surface failures quickly)
 *
 * Fork instructions:
 *   1. For MSW: create src/mocks/browser.ts + src/mocks/handlers.ts
 *      (mock /api/sessions and /api/admin/sessions endpoints).
 *   2. Add session provider (next-auth, clerk, etc.) for ROLE_ADMIN gate
 *      on the admin route group.
 *   3. Add ToastQueue provider for force-logout confirmation feedback.
 */
export function Providers({ children }: ProvidersProps) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 10 * 1000,
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
