/*
---
template_id: L4/notification/app/providers
layer: L4
domain: notification
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 notification vertical — client provider tree: TanStack Query + optional MSW dev interceptor."
  - source_type: external
    citation: "TanStack Query v5 — QueryClientProvider setup"
    url: "https://tanstack.com/query/latest/docs/framework/react/quick-start"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/payment, L4/practices]
---
*/
'use client'

import React from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

/**
 * Providers — client-side provider tree for the notification vertical.
 *
 * Wraps:
 *   - QueryClientProvider (TanStack Query v5) for all server-state fetching
 *
 * Fork instructions:
 *   1. Add your auth provider (next-auth SessionProvider, Zustand store, etc.).
 *   2. Add MSW dev interceptor for local development mocking (see MSW docs).
 *   3. Add analytics provider (Segment, PostHog, etc.) for production.
 *   4. Add ToastQueue from templates/L2/blocks/toast-queue for notifications.
 */
function makeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 15_000,      // 15s stale window — avoids flicker on navigation
        refetchOnWindowFocus: false,
      },
    },
  })
}

let browserQueryClient: QueryClient | undefined

function getQueryClient() {
  if (typeof window === 'undefined') {
    // Server: always make a new client
    return makeQueryClient()
  }
  // Browser: reuse existing client
  if (!browserQueryClient) {
    browserQueryClient = makeQueryClient()
  }
  return browserQueryClient
}

export function Providers({ children }: { children: React.ReactNode }) {
  const queryClient = getQueryClient()
  return (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  )
}
