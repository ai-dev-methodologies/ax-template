---
title: Deduplicate client-side server-state requests with a server-state cache (SWR / TanStack Query / RTK Query / framework primitive)
impact: MEDIUM-HIGH
impactDescription: "Multiple component instances asking for the same data share one in-flight request and one cache entry. Avoids fan-out of identical fetches and per-instance state machines. Library choice is implementation detail; the catalog encodes the practice."
tags:
  - client
  - server-state
  - data-fetching
  - deduplication
  - swr
  - tanstack-query
applicable_to:
  - react
  - nextjs
  - vite
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-CLIENT-001"
verification:
  type: review
  status: manual
  notes: "Reviewer flags any new useEffect+fetch+setState pattern in code that has access to a server-state cache library. New direct fetches need justification (one-off, init-only, or out-of-cache scope)."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified-with-vendor-decouple
    last_verified: "2026-05-16"
    notes: "Mechanic correct; Vercel framing overcouples to SWR. Catalog must encode the practice, not the library."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "React 19 use() + Suspense covers some read patterns but does not replace mutations, revalidation, retries, cache invalidation."
  completeness:
    status: complete
    amendments:
      - "Decoupled from SWR — present SWR / TanStack Query / RTK Query / use() as implementation options"
      - "Clarified use() is read-only; doesn't replace mutation/revalidation surface"
      - "Added 'one-off / init-only' acceptable exception"
  gap_check:
    status: complete
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: client-swr-dedup"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/client-swr-dedup.md"
    role: seed
  - id: nextjs-fetching-data
    title: "Next.js 16 — Fetching Data (Client Components: SWR or TanStack Query)"
    url: "https://nextjs.org/docs/app/getting-started/fetching-data"
    role: canonical-nextjs
evidence:
  - upstream_id: vercel-react-best-practices
    section: "client-swr-dedup"
    quote: "SWR enables request deduplication, caching, and revalidation across component instances."
  - upstream_id: nextjs-fetching-data
    section: "On Server Component data fetching"
    quote: "Identical `fetch` requests in a React component tree are memoized by default, so you can fetch data in the component that needs it instead of drilling props."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Vendor-decouple"
    - "Retitle generically"
    - "use() doesn't replace mutations"
sibling_rules:
  - client-event-listeners
---

## Deduplicate client-side server-state requests with a server-state cache (SWR / TanStack Query / RTK Query / framework primitive)

**Impact: MEDIUM-HIGH — Multiple component instances asking for the same data share one in-flight request and one cache entry. Avoids fan-out of identical fetches and per-instance state machines. Library choice is implementation detail; the catalog encodes the practice.**

### The practice

For any **server-state** read in a Client Component that may appear in multiple components or routes during one session, use a deduplicating cache layer. Plain `useEffect + fetch + setState` should be reserved for genuinely one-off init reads or fetches that are out of the cache's natural scope.

### Implementation choices

| Library / primitive | Strengths |
|---|---|
| **SWR** | Tight Next.js integration (Vercel), simple API, stale-while-revalidate by default |
| **TanStack Query** (`@tanstack/react-query`) | Framework-agnostic, rich query/mutation/infinite/optimistic surface |
| **RTK Query** | Redux Toolkit-native, codegen options |
| **React 19 `use(promise)` + `<Suspense>`** | Built-in; no extra dep — **reads only**; does NOT cover mutations, retries, optimistic updates, cache invalidation |

### Correct — SWR

```tsx
import useSWR from 'swr'

function UserList() {
  const { data: users } = useSWR('/api/users', fetcher)
  // every UserList renders shares the same request and cache entry
}
```

### Correct — TanStack Query

```tsx
import { useQuery } from '@tanstack/react-query'

function UserList() {
  const { data: users } = useQuery({
    queryKey: ['users'],
    queryFn: () => fetch('/api/users').then((r) => r.json()),
  })
}
```

### Correct — React 19 `use()` + Suspense (read-only)

```tsx
// Server Component creates the promise; Client Component consumes it.
// Multiple Client Component consumers of the same promise share the resolve.
'use client'
import { use } from 'react'

function UserList({ usersPromise }: { usersPromise: Promise<User[]> }) {
  const users = use(usersPromise)
  // ...
}
```

This is genuinely deduplicating because the promise is the cache key — same promise reference = same await.

### Incorrect — direct fetch in useEffect, per-instance state

```tsx
function UserList() {
  const [users, setUsers] = useState<User[]>([])
  useEffect(() => {
    fetch('/api/users').then((r) => r.json()).then(setUsers)
  }, [])
  // Five <UserList>s on one page = five identical /api/users requests + five private caches.
}
```

### When direct fetch is acceptable

- One-off boot-time init (read once, store in module-level state — covered by `advanced-init-once`).
- Endpoint is outside the server-state cache scope (e.g. a one-shot debug ping).
- Library not yet adopted in the project — even then, plan the migration.

Sources:

- [Vercel: client-swr-dedup](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/client-swr-dedup.md)
- [Next.js 16 — Client Component data fetching (mentions both SWR and React Query)](https://nextjs.org/docs/app/getting-started/fetching-data)
- [React 19 — use()](https://react.dev/reference/react/use)
