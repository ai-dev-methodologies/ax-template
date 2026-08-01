---
title: Initiate independent promises early, then await with Promise.all (or allSettled)
impact: HIGH
impactDescription: "Eliminates avoidable sequential-await waterfalls in async data flows; preserves correctness around partial failure"
tags:
  - async
  - parallelization
  - promises
  - waterfalls
  - react
  - nextjs
applicable_to:
  - react
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-ASYNC-001"
verification:
  type: eslint
  rule_id: "ax/react-async-parallel"
  status: shipped
  notes: "Shipped: custom ESLint rule ax/react-async-parallel is registered in the plugin and enabled (error in own-blocks/recommended, warn in frontend)"
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
    notes: "Core pattern confirmed by both Next.js 16.2.6 docs and MDN; Vercel's 'fewer round trips' wording corrected — Promise.all does not reduce round trips, it removes per-await sequential blocking."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Anchored to Next.js 16.2.6 and React 19. Re-review when Next.js 17 or React 20 publishes a parallel-fetch guidance change."
  completeness:
    status: complete
    amendments:
      - "Added Promise.allSettled fallback for partial-failure tolerance (per Next.js docs explicit callout)"
      - "Made 'initiate early, await late' the primary mechanic (per Next.js docs canonical example)"
      - "Clarified that Promise.all waits for the slowest required result (per MDN)"
      - "Cross-referenced React 19 promise-as-prop + use() pattern (related rule async-suspense-boundaries)"
  gap_check:
    status: split
    note: "Next.js 16 async params (params: Promise<T>) interaction with Promise.all is a separate concern; tracked as sibling rule next-async-params-parallel."
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: react-best-practices (rule: async-parallel)"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/async-parallel.md"
    role: "seed"
  - id: nextjs-fetching-data
    title: "Next.js 16 docs — Fetching Data"
    url: "https://nextjs.org/docs/app/getting-started/fetching-data"
    version: "16.2.6"
    fetched: "2026-05-13"
    role: "canonical-example"
  - id: react-19-use
    title: "React 19 — use() hook reference"
    url: "https://react.dev/reference/react/use"
    role: "boundary-pattern"
  - id: mdn-promise-all
    title: "MDN — Promise.all"
    url: "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/all"
    role: "primitive-semantics"
  - id: mdn-promise-allsettled
    title: "MDN — Promise.allSettled"
    url: "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/allSettled"
    role: "partial-failure-fallback"
evidence:
  - upstream_id: vercel-react-best-practices
    section: "async-parallel"
    quote: "When async operations have no interdependencies, execute them concurrently using `Promise.all()`."
  - upstream_id: nextjs-fetching-data
    section: "Parallel data fetching"
    quote: "Start multiple requests by calling `fetch`, then await them with `Promise.all`. Requests begin as soon as `fetch` is called."
  - upstream_id: nextjs-fetching-data
    section: "Parallel data fetching — Good to know"
    quote: "If one request fails when using `Promise.all`, the entire operation will fail. To handle this, you can use the `Promise.allSettled` method instead."
  # Re-anchored 2026-08-01 (BACKLOG P2-73): react.dev/reference/react/use was rewritten and
  # no longer contains the previous sentence. Quote below is copied verbatim from the
  # 2026-08-01 extractor output appended to the snapshot.
  - upstream_id: react-19-use
    section: "Server Component data fetching"
    quote: "Ideally, Promises are created before rendering, such as in an event handler, a route loader, or a Server Component, and passed to the component that calls use"
  - upstream_id: mdn-promise-all
    section: "Description"
    quote: "It rejects when any of the input's promises rejects, with this first rejection reason."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=high"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "accuracy/freshness/completeness/gap_check verdicts confirmed"
    - "Promise.all is an aggregation primitive, not a parallelization trigger"
    - "Next.js async params deserves a separate sibling rule"
    - "'init early, await late' stays folded into this rule (not over-extracted)"
  amendments_required:
    - "Replace 'fewer round trips' framing — it is sequential-wait elimination, not network round trip reduction"
    - "Add Promise.allSettled / partial failure caveat"
    - "Note that Promise.all waits for the slowest required result"
    - "Caveat the unverified '2-10×' impact claim — benchmark not anchored"
sibling_rules:
  - next-async-params-parallel
  - async-suspense-boundaries
  - async-dependencies
---

## Initiate independent promises early, then await with Promise.all (or allSettled)

**Impact: HIGH — Eliminates avoidable sequential-await waterfalls in async data flows; preserves correctness around partial failure**

The mechanic is "initiate early, await late". `Promise.all` does not start work — it
aggregates. Work begins the moment a promise-returning call is made. A sequential
chain of `await` statements blocks each call until the previous one resolves, even
when the calls are independent. Calling them all first (without `await`) lets them
run concurrently; aggregating with `Promise.all` then waits for the **slowest required
result**. Use `Promise.allSettled` when partial success is acceptable — `Promise.all`
rejects the entire aggregate on the first rejection.

> **Note on impact claim:** The seed source labels this CRITICAL with a "2–10×"
> improvement claim. The improvement is real but the multiplier is workload-dependent
> (it equals N for N independent equal-latency calls). The rule is downgraded to
> HIGH here because the multiplier is unverified per-call-site and depends on the
> distribution of latencies, not on this rule alone.

**Incorrect — sequential awaits block each call (waterfall):**

```typescript
// Each await blocks the next request from starting.
// Total latency ≈ sum of all latencies.
export default async function Page() {
  const user = await fetchUser()
  const posts = await fetchPosts()
  const comments = await fetchComments()
  return <Dashboard user={user} posts={posts} comments={comments} />
}
```

**Correct — initiate early, await late:**

```typescript
// All three requests fire as soon as the calls are made.
// Total latency ≈ MAX of the three latencies, not the sum.
export default async function Page() {
  const userPromise = fetchUser()
  const postsPromise = fetchPosts()
  const commentsPromise = fetchComments()

  const [user, posts, comments] = await Promise.all([
    userPromise,
    postsPromise,
    commentsPromise,
  ])

  return <Dashboard user={user} posts={posts} comments={comments} />
}
```

**Partial-failure tolerance — use Promise.allSettled when one failure should not
collapse the whole render:**

```typescript
const settled = await Promise.allSettled([
  fetchUser(),
  fetchOptionalRecommendations(),
  fetchNotices(),
])

const user = settled[0].status === 'fulfilled' ? settled[0].value : null
const recs = settled[1].status === 'fulfilled' ? settled[1].value : []
const notices = settled[2].status === 'fulfilled' ? settled[2].value : []
```

### React 19 / Next.js 16 nuances

- In a Server Component, prefer `async`/`await` over `use()` for direct fetching.
  Create promises in the Server Component and pass them to Client Components as
  props when you want streaming via Suspense — see sibling rule
  `async-suspense-boundaries`.
- In Next.js 16, route `params` and `searchParams` are promises themselves. If a
  call depends on a param value, await the param first; if the call does not depend
  on it, include the param promise in the same `Promise.all` to avoid an extra
  sequential await — see sibling rule `next-async-params-parallel`.
- By default the Next.js App Router renders sibling layouts and pages in parallel,
  so this rule is about avoiding waterfalls **within** a single component body, not
  across the route tree.

### When the pattern does not apply

- Truly dependent operations: `const user = await fetchUser(); const posts = await
  fetchPostsForUser(user.id)` — parallelization here would be wrong. For partial
  dependency graphs, `Promise.all` can leave easy wins on the table; the
  `async-dependencies` rule covers the partial-dependency case.
- Side-effecting writes with ordering requirements: aggregation discards the
  ordering signal of sequential awaits.

### Verification

- Static check (shipped + enabled): custom ESLint rule `ax/react-async-parallel` flags two
  or more consecutive top-level `await` statements that share no `await`-bound
  identifiers, inside the same async function body, where each awaited expression
  is a call (i.e., independent network or DB I/O).
- Manual: code review complements the shipped ESLint rule for cases static analysis cannot see.
