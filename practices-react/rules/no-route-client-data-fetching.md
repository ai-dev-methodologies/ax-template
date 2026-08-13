---
title: A "use client" route file must not call client data-fetching hooks or raw fetch directly — delegate to a feature hook
impact: HIGH
impactDescription: "A route (app/**/page.tsx or layout.tsx) marked \"use client\" that calls useSWR/useQuery/useMutation/axios or a raw fetch() directly hard-couples routing to a specific data-fetching implementation and a specific endpoint shape. The route becomes untestable without mocking the network, unreusable if the same data is needed from a different route, and the data logic cannot be unit-tested independently of the App Router rendering harness. Unlike the route-thinness size heuristic (ax/no-god-route), this pattern is a precise AST shape (a known hook/library call or fetch()), so it ships as a hard ERROR, not advisory."
tags: [architecture, routing, feature-layout, data-fetching, client-server-boundary, eslint]
applicable_to: [react, nextjs]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-ROUTE-002"
verification:
  type: lint
  rule_id: "ax/no-route-client-data-fetching"
  status: shipped
  notes: "Shipped + enabled (ERROR): ax/no-route-client-data-fetching flags, inside a \"use client\" file under app/**/page|layout, any call to useSWR/useSWRInfinite/useSWRMutation/useQuery/useMutation/useInfiniteQuery/useQueryClient/useSuspenseQuery, any raw fetch(...), and any call through a local binding imported from swr/@tanstack/react-query/axios (name-agnostic — `import { useSWR as useFetch } from 'swr'` and `import http from 'axios'; http.get(...)` are both still caught, closing a bypass an audit found for naive name matching). A SERVER route (no \"use client\") is not checked — `await fetch()` there is the idiomatic App Router server data layer. Honest limit: a route calling a LOCAL wrapper hook that internally uses useSWR (e.g. useDashboardData() defined in @/features/dashboard) is not caught, because seeing through the wrapper needs data-flow analysis — this is the intended escape hatch (the wrapper IS the feature hook this rule wants), not a loophole. Registered in the plugin and enforcing."
provenance: { pilot: false, pipeline_version: "2026-06-08", pipeline_steps: [phaseA_frontend_decomposition_design, phaseB_rule_authoring, phaseC_teeth_proof] }
audit:
  accuracy: { status: verified, last_verified: "2026-08-13" }
  freshness: { status: current, last_verified: "2026-08-13", next_review_by: "2026-11-11" }
  completeness: { status: complete, amendments: ["Catalog doc authored post-ship — rule and tests predate this file; see practices-react/eslint-plugin-ax/rules/no-route-client-data-fetching.js and tests/no-route-client-data-fetching.test.js.", "P2-87: documented the renamed-import / aliased-axios bypass the 2026-06-08 audit found and confirmed the rule closes it."] }
  gap_check: { status: complete }
upstream:
  - id: nextjs-fetching-data
    title: "Next.js 16 — Fetching Data (Client Components: use() vs community libraries)"
    url: "https://nextjs.org/docs/app/getting-started/fetching-data"
    role: seed
evidence:
  - upstream_id: nextjs-fetching-data
    section: "Fetching Data — Client Components / Streaming data with the use API"
    quote: "You can use React's `use` API to stream data from the server to client. Start by fetching data in your Server component, and pass the promise to your Client Component as prop."
    anchors: generic_principle_only
  - upstream_id: nextjs-fetching-data
    section: "Fetching Data — Client Components / Community libraries"
    quote: "You can use a community library like SWR or React Query to fetch data in Client Components. These libraries have their own semantics for caching, streaming, and other features."
    anchors: generic_principle_only
sibling_rules: [no-god-route, no-server-state-in-local-state]
---

## A "use client" route file must not fetch client data directly

**Impact: HIGH — This is a precise AST shape (a known data-fetching hook/library call or raw fetch), not a heuristic, so it ships as a hard ERROR. A route that fetches its own data cannot be reused, cannot be tested without mocking the network, and couples routing to one implementation.**

Next.js's own documented data-fetching model draws a clear line: a **Server Component**
fetches with `await fetch(...)` directly; a **Client Component** that needs data either
receives a promise from its Server Component parent (`use()`), or reaches for a
**community library** (SWR / React Query) — either way, data-fetching in a Client Component
is meant to go through a dedicated mechanism, not be inlined ad hoc. `ax/no-route-client-data-fetching`
takes that generic principle and applies it specifically to **route files**: the route is the
routing layer, so even the "dedicated mechanism" call (`useSWR`, `useQuery`, ...) belongs in
a `@/features/<f>` hook that the route renders, not in the route file itself. That last
step — routes delegate to a feature hook rather than calling the library directly — is this
catalog's own layering decision, not a Next.js requirement.

### Incorrect — client route calling useSWR directly

```tsx
// src/app/(authenticated)/dashboard/page.tsx
'use client'
import useSWR from 'swr'

export default function Page() {
  const { data } = useSWR('/api/me')
  return <div>{data?.name}</div>
}
```

### Incorrect — client route calling raw fetch

```tsx
// src/app/(authenticated)/dashboard/page.tsx
'use client'

export default function Page() {
  fetch('/api/x')
  return null
}
```

### Incorrect — renamed import / aliased axios still caught

```tsx
// VIOLATION: renaming the binding does not bypass the rule — bindings imported from
// swr / @tanstack/react-query / axios are tracked name-agnostically.
'use client'
import { useSWR as useFetch } from 'swr'

export default function Page() {
  const { data } = useFetch('/api/me')
  return <div>{data?.name}</div>
}
```

### Correct — server route awaits fetch directly (idiomatic)

```tsx
// src/app/showcase/page.tsx — no "use client": Server Component, unaffected
export default async function Page() {
  const res = await fetch('/api/x')
  const data = await res.json()
  return <div>{data.name}</div>
}
```

### Correct — client route delegates to a feature hook

```tsx
// src/app/(authenticated)/dashboard/page.tsx
'use client'
import { useDashboardData } from '@/features/dashboard'

export default function Page() {
  const { data } = useDashboardData()
  return <div>{data?.name}</div>
}
```

```ts
// src/features/dashboard/useDashboardData.ts — the feature hook, NOT a route file
import useSWR from 'swr'

export function useDashboardData() {
  return useSWR('/api/me')
}
```

### Honest limit

A route calling a **local wrapper hook** that internally uses `useSWR` (`useDashboardData()`
defined in `@/features/dashboard`, as above) is not caught — seeing through the wrapper to
detect the underlying `useSWR` call needs data-flow analysis this rule does not do. This is
not a loophole to close: the wrapper hook **is** the feature-hook delegation this rule wants
routes to have. Genuine in-route data orchestration (the hook/library call written directly
in the route file) is what gets blocked.

Reference: [Next.js 16 — Fetching Data](https://nextjs.org/docs/app/getting-started/fetching-data)

Reference: [practices-react/rules/no-god-route.md](no-god-route.md) — the sibling advisory size heuristic covering the broader "route doing too much" smell this rule's precise shape does not fully capture.
