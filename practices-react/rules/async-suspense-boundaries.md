---
title: Stream wrapper UI fast — fetch in Server Components, pass promises down, resolve with use() inside Suspense
impact: HIGH
impactDescription: "Wrapper layout renders immediately; data-dependent regions stream in via Suspense. Pattern: Server Component creates promise → passes to Client Component as prop → Client Component reads with use() inside a Suspense boundary."
tags:
  - async
  - suspense
  - streaming
  - server-components
  - use-hook
applicable_to:
  - react
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-ASYNC-005"
verification:
  type: review
  status: manual
  notes: "Reviewer checks: (a) async functions are Server Components (not Client), (b) promises are created in Server Components (not in Client Component render), (c) Suspense boundary wraps the data-needing region (not the whole page) when partial streaming is the goal, (d) layout shift caveats applied (skeleton sized close to final content)."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "React 19 use() stable; Next.js 16 canonical pattern for streaming Client Component data is exactly this shape."
  completeness:
    status: complete
    amendments:
      - "Clarified Server vs Client Component boundaries (async = SC only)"
      - "Promise creation must be in SC, not Client Component render (recreated every render)"
      - "Layout-shift caveat tied to skeleton sizing"
  gap_check:
    status: complete
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: async-suspense-boundaries"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/async-suspense-boundaries.md"
    role: seed
  - id: react-19-use
    title: "React 19 — use()"
    url: "https://react.dev/reference/react/use"
    role: canonical-react
  - id: nextjs-fetching-data
    title: "Next.js 16 — Fetching Data (streaming with use())"
    url: "https://nextjs.org/docs/app/getting-started/fetching-data"
    role: canonical-nextjs
evidence:
  - upstream_id: vercel-react-best-practices
    section: "async-suspense-boundaries"
    quote: "Instead of awaiting data in async components before returning JSX, use Suspense boundaries to show the wrapper UI faster while data loads."
  - upstream_id: react-19-use
    section: "Promise creation site"
    quote: "Prefer creating Promises in Server Components and passing them to Client Components over creating Promises in Client Components. Promises created in Client Components are recreated on every render."
  - upstream_id: nextjs-fetching-data
    section: "Client Components — use() API"
    quote: "Start by fetching data in your Server component, and pass the promise to your Client Component as prop. [...] use the use API to read the promise."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - async-parallel
  - server-cache-react
  - nextjs-use-cache
---

## Stream wrapper UI fast — fetch in Server Components, pass promises down, resolve with use() inside Suspense

**Impact: HIGH — Wrapper layout renders immediately; data-dependent regions stream in via Suspense.**

### Server vs Client Component scope

- `async function Page()` and other async components are **Server Components only**. Client Components must not be `async`.
- **Create promises in Server Components.** Promises created in Client Components are recreated every render — they don't share, don't cache, and re-fetch.

### Incorrect — async page awaits data before returning any JSX

```tsx
async function Page() {
  const data = await fetchData() // wrapper blocked
  return (
    <div>
      <Sidebar />
      <Header />
      <div><DataDisplay data={data} /></div>
      <Footer />
    </div>
  )
}
```

Sidebar / Header / Footer have to wait for `fetchData` even though only the middle needs it.

### Correct (single consumer) — Suspense boundary around data-needing region

```tsx
import { Suspense } from 'react'

function Page() {
  return (
    <div>
      <Sidebar />
      <Header />
      <Suspense fallback={<Skeleton />}>
        <DataDisplay />
      </Suspense>
      <Footer />
    </div>
  )
}

async function DataDisplay() {  // Server Component
  const data = await fetchData()
  return <div>{data.content}</div>
}
```

Sidebar / Header / Footer ship immediately; DataDisplay streams in.

### Correct (shared promise, multi consumer) — `use()` in Client Components

```tsx
// Server Component
import Posts from '@/app/ui/posts'

export default function Page() {
  const postsPromise = getPosts()   // not awaited — passed as a promise
  return (
    <Suspense fallback={<Skeleton />}>
      <Posts posts={postsPromise} />
      <PostsSummary posts={postsPromise} />
    </Suspense>
  )
}
```

```tsx
// Client Component
'use client'
import { use } from 'react'

export default function Posts({ posts }: { posts: Promise<Post[]> }) {
  const list = use(posts)
  return <ul>{list.map(p => <li key={p.id}>{p.title}</li>)}</ul>
}
```

`Posts` and `PostsSummary` share one resolution of `getPosts()` because they share the same promise reference. One fetch, two consumers.

### Promise creation site — the critical rule

```tsx
// BAD: promise created in Client Component render — recreated every render → infinite re-suspend.
'use client'
function Bad() {
  const posts = fetch('/api/posts').then(r => r.json())  // ❌ new promise each render
  const data = use(posts)
}

// GOOD: promise created in Server Component, passed down as a prop.
function Page() {
  const posts = fetchPosts()
  return <Bad posts={posts} />
}
```

### When NOT to use Suspense streaming

- **Critical above-the-fold content** that the user must see before interactivity. SEO content too.
- **Tiny fast queries** — the Suspense overhead may exceed the latency saved.
- **Layout-shift-sensitive surfaces** — the skeleton-to-content swap can shift the page. Size the skeleton close to the final content if you do stream.

### Layout-shift mitigation

If you stream, the fallback should occupy the same approximate bounding box as the resolved content. Otherwise you trade waiting-then-pop for layout-shift-then-jump — different UX, often not better.

Sources:

- [Vercel: async-suspense-boundaries](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/async-suspense-boundaries.md)
- [React 19 — use()](https://react.dev/reference/react/use)
- [Next.js 16 — Fetching Data (Client Component use() pattern)](https://nextjs.org/docs/app/getting-started/fetching-data)
