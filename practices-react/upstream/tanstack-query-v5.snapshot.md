---
snapshot_id: tanstack-query-v5
source: "https://tanstack.com/query/latest/docs/framework/react/overview"
fetched_at: "2026-05-17T13:00:00Z"
version_observed: "tanstack-query@5.x"
via: WebFetch
sha: "f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9"
---

# TanStack Query v5 — React Overview

Source: https://tanstack.com/query/latest/docs/framework/react/overview  
Fetched: 2026-05-17

## Overview

> "TanStack Query makes fetching, caching, synchronizing and updating async state trivial."

TanStack Query (previously React Query) addresses managing **server state** — which exists
remotely, requires async operations, can change without your knowledge, and risks becoming outdated.

## Problems it solves

Without TanStack Query, developers manually handle:
- Caching complexity
- Deduplicating redundant requests
- Background data refreshing
- Memory management and garbage collection
- Pagination, infinite scrolling
- Optimistic updates and rollbacks

## Key Benefits

- **Code Reduction**: Eliminates complex boilerplate — replace multiple `useState` + `useEffect` patterns with `useQuery`.
- **Enhanced Maintainability**: Adding new server state sources requires minimal wiring.
- **User Experience**: Makes interfaces "faster and more responsive than ever before."
- **Resource Efficiency**: Intelligent caching reduces bandwidth consumption.

## Basic Pattern (v5)

```tsx
import { QueryClient, QueryClientProvider, useQuery } from '@tanstack/react-query'

const queryClient = new QueryClient()

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <Posts />
    </QueryClientProvider>
  )
}

function Posts() {
  const { data, isPending, isError } = useQuery({
    queryKey: ['posts'],
    queryFn: () => fetch('/api/posts').then(r => r.json()),
  })

  if (isPending) return <div>Loading...</div>
  if (isError) return <div>Error</div>
  return <ul>{data.map(p => <li key={p.id}>{p.title}</li>)}</ul>
}
```

## v5 Key Changes from v4

- Single unified `useQuery` signature (no overloads).
- First-class React 19 Suspense support via `useSuspenseQuery`.
- `status: 'pending'` replaces `status: 'loading'`.
- `fetchStatus` added to distinguish between background fetching and initial load.

## When to Use vs Zustand

| Concern | Tool |
|---------|------|
| Server state (API data) | TanStack Query |
| Client state (UI, auth token, theme) | Zustand |
| Form state | React Hook Form |
| URL state | search params / route segments |

Do NOT duplicate server state into Zustand stores.
