---
title: Do not seed useState with a query/SWR result's .data — the query cache is the source of truth
impact: MEDIUM
impactDescription: "useState(useSWR(...).data) / useState(useQuery(...).data) copies a snapshot of server state into local component state at mount/render time. The two then drift: revalidation, background refetch, and cache invalidation update the query cache, but the copied useState value does not follow unless the developer wires an extra useEffect to resync it — at which point the local state was never needed. The component can render stale data even while the cache holds the fresh value."
tags: [architecture, client, server-state, state-boundary, eslint]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-STATE-001"
verification:
  type: lint
  rule_id: "ax/no-server-state-in-local-state"
  status: shipped
  notes: "Shipped + enabled (advisory/warn by design): ax/no-server-state-in-local-state flags the direct, unambiguous shape useState(<queryCall>(...).data) where <queryCall> is useSWR/useSWRInfinite/useQuery/useInfiniteQuery/useSuspenseQuery. Honest limit (documented in the rule source): copying a query result through an intermediate variable first (const r = useSWR('/x'); const [d, setD] = useState(r.data)) is NOT caught — this is a heuristic that catches the most common direct-seed mistake, not a data-flow analysis. Registered in the plugin and enforcing as warn."
provenance: { pilot: false, pipeline_version: "2026-06-08", pipeline_steps: [phaseA_frontend_decomposition_design, phaseB_rule_authoring, phaseC_teeth_proof] }
audit:
  accuracy: { status: verified, last_verified: "2026-08-13" }
  freshness: { status: current, last_verified: "2026-08-13", next_review_by: "2026-11-11" }
  completeness: { status: complete, amendments: ["Catalog doc authored post-ship — rule and tests predate this file; see practices-react/eslint-plugin-ax/rules/no-server-state-in-local-state.js and tests/no-server-state-in-local-state.test.js.", "P2-87: cross-referenced client-swr-dedup.md (the review-tier sibling covering the broader server-state-caching practice) so the two do not read as unrelated."] }
  gap_check: { status: complete }
upstream:
  - id: tanstack-query-v5
    title: "TanStack Query v5 — React Overview (server state vs client state)"
    url: "https://tanstack.com/query/latest/docs/framework/react/overview"
    role: seed
evidence:
  - upstream_id: tanstack-query-v5
    section: "Overview"
    quote: "TanStack Query makes fetching, caching, synchronizing and updating async state trivial."
    anchors: generic_principle_only
  - upstream_id: tanstack-query-v5
    section: "When to Use vs Zustand"
    quote: "Do NOT duplicate server state into Zustand stores."
    anchors: generic_principle_only
sibling_rules: [client-swr-dedup, no-god-route, no-route-client-data-fetching]
---

## Do not seed useState with a query/SWR result's .data

**Impact: MEDIUM (advisory) — A heuristic that catches the most common direct-seed mistake. The query cache (SWR / TanStack Query) is the source of truth for server state; copying its `.data` into `useState` creates a second, independently-stale copy.**

The upstream evidence generalizes past any one library: **server state is not client state**,
and a caching library's entire reason to exist is to own synchronization, revalidation, and
staleness for it — so much so that TanStack Query's own guidance is explicit that server
state must not be duplicated into a separate client store. `useState(useSWR(...).data)` /
`useState(useQuery(...).data)` commits exactly that mistake at the component level: it takes
a value the cache already owns and re-owns it in local state, which the cache's revalidation
cycle then has no way to keep in sync.

### Incorrect — SWR result seeded into useState

```tsx
function Profile() {
  const [data, setData] = useState(useSWR('/api/me').data)
  // `data` is a snapshot taken once. When SWR revalidates in the background,
  // the cache updates but `data` does not — this component can render stale.
  return <div>{data?.name}</div>
}
```

### Incorrect — TanStack Query result seeded into useState

```tsx
function Profile() {
  const [data, setData] = useState(useQuery({ queryKey: ['me'], queryFn: fetchMe }).data)
  return <div>{data?.name}</div>
}
```

### Correct — read from the query cache directly

```tsx
function Profile() {
  const { data } = useSWR('/api/me')
  // No useState — every render reads the current cache value. Revalidation,
  // background refetch, and mutation-driven cache updates are all reflected
  // automatically.
  return <div>{data?.name}</div>
}
```

```tsx
function Profile() {
  const { data } = useQuery({ queryKey: ['me'], queryFn: fetchMe })
  return <div>{data?.name}</div>
}
```

### When useState with an initial prop value is fine

```tsx
// FINE: seeding local UI state from a prop (not a query result) is unrelated to this rule.
function Editable({ initial }: { initial: string }) {
  const [value, setValue] = useState(initial)
  return <input value={value} onChange={(e) => setValue(e.target.value)} />
}
```

### Honest limit

Only the **direct, unambiguous shape** `useState(<queryCall>(...).data)` is flagged. Copying
a query result through an intermediate variable first is not caught:

```tsx
// NOT caught — the query result is bound to `r` before being read, and the rule does not
// perform data-flow analysis to trace `r.data` back to a query call.
const r = useSWR('/api/me')
const [data, setData] = useState(r.data)
```

This keeps the rule a precise, low-false-positive heuristic rather than a general "never
mirror any derived value into state" checker, at the cost of missing the indirected form of
the same mistake.

Reference: [TanStack Query v5 — React Overview](https://tanstack.com/query/latest/docs/framework/react/overview)

Reference: [practices-react/rules/client-swr-dedup.md](client-swr-dedup.md) — the review-tier sibling covering the broader practice (dedupe client-side server-state reads with a cache layer) that this rule's narrow ESLint shape enforces one slice of.
