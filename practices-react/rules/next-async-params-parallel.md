---
title: "Next.js 16 async params — await params alongside independent server work, not before"
impact: MEDIUM
impactDescription: "In Next.js 16, route params and searchParams are Promises. If next-step work is independent of the param value, include the param promise in the same Promise.all rather than awaiting it sequentially. Only param-dependent work blocks on await."
tags:
  - async
  - parallelization
  - nextjs
  - app-router
  - params
applicable_to:
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-ASYNC-006"
verification:
  type: review
  status: manual
  notes: "Reviewer flags any `const { x } = await params` immediately followed by independent server work that doesn't reference x — those should be aggregated into one Promise.all."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus_inherited]
  split_origin: "async-parallel codex review explicitly endorsed creating this sibling rule (verbatim: 'Next.js async params deserves a separate sibling rule')."
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-17"
    notes: "Next.js 16 App Router signature: `params: Promise<...>`. Evidence in nextjs-fetching-data.snapshot.md."
  freshness:
    status: current
    last_verified: "2026-05-17"
    next_review_by: "2026-08-15"
    notes: "Async params introduced in Next.js 15; default in Next.js 16."
  completeness:
    status: complete
    amendments:
      - "Distinguish param-dependent (sequential needed) vs param-independent (parallelizable) next-step work"
      - "Add type signature reminder: params: Promise<{ id: string }> in Next 16"
      - "Cross-link parent rule async-parallel"
  gap_check:
    status: complete
upstream:
  - id: nextjs-fetching-data
    title: "Next.js 16 — Fetching Data (params are Promise<T>)"
    url: "https://nextjs.org/docs/app/getting-started/fetching-data"
    role: canonical-nextjs
evidence:
  # Re-anchored 2026-08-01 (BACKLOG P2-73): the previous quote was a colon-lead-in digest
  # sentence, not page prose. Quote below is copied verbatim from the 2026-08-01 extractor
  # output appended to the snapshot.
  - upstream_id: nextjs-fetching-data
    section: "Next.js 16-specific: async params"
    quote: "However, within any component, multiple async / await requests can still be sequential if placed after the other."
  - upstream_id: nextjs-fetching-data
    section: "Parallel data fetching"
    quote: "Start multiple requests by calling `fetch`, then await them with `Promise.all`. Requests begin as soon as `fetch` is called."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=high (inherited from async-parallel review)"
  reviewed_at: "2026-05-16"
  verdict: SHIP_AS_PRE_ENDORSED_SIBLING
  agreements:
    - "Endorsed during async-parallel review as a needed split"
    - "Rationale verbatim: 'await params/searchParams alongside independent server work to avoid a needless waterfall'"
    - "'only start param-dependent work after the params resolve'"
sibling_rules:
  - async-parallel
  - async-dependencies
  - async-defer-await
---

## Next.js 16 async params — aggregate, don't sequence

**Impact: MEDIUM — `params` and `searchParams` are Promises in Next.js 16. Treat them as one more independent promise to aggregate via `Promise.all`, not as a precondition to await before anything else can start.**

### The shape that changed

```tsx
// Next.js 16 App Router signatures
export default async function Page({
  params,
  searchParams,
}: {
  params: Promise<{ id: string }>
  searchParams: Promise<{ sort?: string }>
}) {
  // ...
}
```

`params` is now a `Promise<{ id: string }>`, not `{ id: string }`. Same for `searchParams`. Awaiting them costs one tick of the microtask queue per param object — small per call, but it stacks if you also need to do other independent server work.

### Incorrect — params block independent work

```tsx
export default async function Page({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  // ❌ Sequential: config fetch waits for params to resolve, even though
  // config has nothing to do with the route id.
  const { id } = await params
  const config = await fetchConfig()
  const post = await fetchPost(id)
  return <Article config={config} post={post} />
}
```

`fetchConfig()` could have started at the same moment as `params`. Instead it waits for params to resolve, then for `fetchPost` (which legitimately depends on id), then renders.

### Correct — param-dependent work waits; param-independent work parallelizes

```tsx
export default async function Page({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  // Initiate everything that doesn't depend on params alongside params itself.
  const configPromise = fetchConfig()
  const { id } = await params

  // Post fetch depends on id — start it now (after params resolved).
  const postPromise = fetchPost(id)

  // Aggregate at the latest moment.
  const [config, post] = await Promise.all([configPromise, postPromise])

  return <Article config={config} post={post} />
}
```

`fetchConfig` and the params resolution run concurrently. `fetchPost` waits for params (genuinely dependent). The final aggregate joins them.

### When the next-step work is ALL param-independent

If nothing else needs `id`, you can include `params` directly in the `Promise.all`:

```tsx
export default async function Page({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const [{ id }, config, recommendations] = await Promise.all([
    params,
    fetchConfig(),
    fetchRecommendations(),
  ])
  return <Article id={id} config={config} recommendations={recommendations} />
}
```

`Promise.all` destructures the resolved param object inline. One await for three independent values.

### `searchParams` is the same shape

```tsx
export default async function ListPage({
  searchParams,
}: {
  searchParams: Promise<{ q?: string; sort?: string }>
}) {
  const [{ q, sort }, totalCount] = await Promise.all([
    searchParams,
    fetchTotalCount(),
  ])
  // ...
}
```

### Type-safety reminder

TypeScript catches missing `await` on params at compile time — using `params.id` instead of `(await params).id` is a type error. The risk is not a missing await; it's a NEEDLESSLY SEQUENTIAL await blocking independent work.

### Cross-rule scope

- `async-parallel` — the parent rule (init early, await late, aggregate via `Promise.all`).
- This rule — the Next.js-16-specific specialization for `params` / `searchParams`.
- `async-dependencies` — when there's a partial dependency graph (some calls depend, some don't).
- `async-defer-await` — when work might not be needed at all (early-return path).

Sources:

- [Next.js 16 — Fetching Data (async params)](https://nextjs.org/docs/app/getting-started/fetching-data)
