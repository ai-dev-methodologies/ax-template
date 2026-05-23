/*
---
template_id: L4/approval-workflow/app/providers
layer: L4
domain: approval-workflow
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 approval-workflow vertical — client provider tree: QueryClientProvider for server-state."
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
 * Providers — client-side provider tree for the L4 approval-workflow vertical.
 *
 * Approval QueryClient settings:
 *   - staleTime: 5s (inbox MUST refresh quickly — an approver acting on a
 *                    step should see the next step's status update almost
 *                    immediately; a requester whose request was just
 *                    approved should see the new state on next view)
 *   - retry: 1 (admin/business operations are user-driven; surface
 *               failures quickly rather than appearing to spin)
 *
 * Fork instructions:
 *   1. Add session provider — caller id is required to differentiate
 *      "requester view" vs "approver view" of the same request, and to
 *      enforce the structural duplicate-approver / self-approve guards
 *      from the iter1+2 dogfood closure.
 *   2. For MSW: create src/mocks/browser.ts + src/mocks/handlers.ts.
 *   3. ToastQueue provider is recommended — submit / approve / reject
 *      should each surface a transient confirmation.
 */
export function Providers({ children }: ProvidersProps) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 5 * 1000,
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
