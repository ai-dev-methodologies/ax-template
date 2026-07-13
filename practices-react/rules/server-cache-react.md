---
title: Use React.cache() for per-request, in-process deduplication of non-fetch server work
impact: MEDIUM
impactDescription: "Eliminates duplicate execution of expensive non-fetch async work (DB queries, auth lookups, file I/O, pure computations) across components in a single request. Narrow scope: server-only, request-scoped, in-process; not a substitute for Next.js Cache Components."
tags:
  - server
  - cache
  - react-cache
  - deduplication
  - rsc
applicable_to:
  - react
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-SERVER-001"
verification:
  type: review
  status: manual
  notes: "Static detection is unreliable: any module-scope `cache(fn)` wrapping looks correct from AST alone. Verification requires reviewer confirming (a) function defined at module level, (b) primitive args or stable references, (c) Server-Component-only usage, (d) not used inside a 'use cache' boundary (where React.cache is isolated)."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
  split_origin: "Vercel server-cache-react seed rule — split into this rule + nextjs-use-cache.md"
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
    notes: "Mechanic (shallow equality / Object.is / per-request invalidation / module-level definition) matches React 19 docs verbatim."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "React.cache() unchanged in React 19; narrowed scope reflects Next.js 16 caching-model evolution rather than React-side change."
  completeness:
    status: complete
    amendments:
      - "Added React 19 specifics: server-only, errors cached, separate cache per cache() call, primitives or same reference"
      - "Added boundary: do not use inside 'use cache' (Next.js) — React.cache isolated there"
      - "Removed implicit claim that React.cache() is Next.js's recommended caching primitive (it is not in Next 16)"
      - "Repositioned: per-request in-process dedup ONLY; for cross-request caching use 'use cache' directive (sibling rule)"
  gap_check:
    status: split
    note: "Cross-request/durable caching is the sibling rule nextjs-use-cache.md. Fetch memoization (Next.js built-in) is documented as a related no-op pattern but does not need its own catalog rule."
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: react-best-practices (rule: server-cache-react)"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-cache-react.md"
    role: "seed"
  - id: react-19-cache
    title: "React 19 — cache() API reference"
    url: "https://react.dev/reference/react/cache"
    role: "canonical-react"
  - id: nextjs-use-cache-directive
    title: "Next.js 16 — 'use cache' directive (for boundary/isolation context)"
    url: "https://nextjs.org/docs/app/api-reference/directives/use-cache"
    role: "canonical-nextjs"
evidence:
  - upstream_id: react-19-cache
    section: "Scope"
    quote: "cache is only for use with React Server Components."
  - upstream_id: react-19-cache
    section: "Lifetime"
    quote: "React will invalidate the cache for all memoized functions for each server request."
  - upstream_id: react-19-cache
    section: "Per-call cache"
    quote: "Each call to cache creates a new function. This means that calling cache with the same function multiple times will return different memoized functions that do not share the same cache."
  - upstream_id: react-19-cache
    section: "Arguments"
    quote: "If your arguments are not primitives (ex. objects, functions, arrays), ensure you're passing the same object reference."
  - upstream_id: nextjs-use-cache-directive
    section: "React.cache isolation"
    quote: "React.cache operates in an isolated scope inside use cache boundaries. Values stored via React.cache outside a use cache function are not visible inside it."
  - upstream_id: vercel-react-best-practices
    section: "server-cache-react"
    quote: "Use `React.cache()` for server-side request deduplication."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=high"
  reviewed_at: "2026-05-16"
  verdict: SPLIT
  agreements:
    - "Vercel rule mechanic is correct"
    - "Severely stale on Next.js positioning"
    - "Should split, not amend"
    - "Do not mark React.cache() deprecated"
  boundary:
    react_cache: "RSC per-request in-process dedup of non-fetch server work; module-scope definition; isolated inside 'use cache' boundaries"
    use_cache_directive: "Next.js 16 Cache Components persistent caching with cacheLife / cacheTag / updateTag"
sibling_rules:
  - nextjs-use-cache
---

## Use React.cache() for per-request, in-process deduplication of non-fetch server work

**Impact: MEDIUM — Eliminates duplicate execution of expensive non-fetch async work (DB queries, auth lookups, file I/O, pure computations) across components in a single request. Narrow scope: server-only, request-scoped, in-process; not a substitute for Next.js Cache Components.**

### Scope discipline

`React.cache()` is a request-scoped, in-process memoization primitive for **Server Components only**. It is NOT:

- a substitute for Next.js 16's `'use cache'` directive (sibling rule `nextjs-use-cache.md`),
- a persistent cache (lives one request),
- usable in Client Components (no Server context, no cache access),
- usable inside a `'use cache'` boundary (React.cache is isolated there per Next docs),
- a replacement for built-in `fetch` request memoization (Next.js dedupes identical `fetch` calls automatically).

Use it for: database client calls, auth/session lookups, expensive synchronous computations turned async, file-system reads, any non-fetch async work that may be invoked from multiple components in one request.

### Correct usage

**Module-level definition (REQUIRED):**

```tsx
// app/lib/user.ts
import { cache } from 'react'
import { db } from '@/lib/db'
import { auth } from '@/lib/auth'

// Defined ONCE at module level. Every Server Component import gets
// the SAME memoized function and therefore shares the cache.
export const getCurrentUser = cache(async () => {
  const session = await auth()
  if (!session?.user?.id) return null
  return db.user.findUnique({ where: { id: session.user.id } })
})
```

**Calling from multiple Server Components in one request:**

```tsx
// app/header.tsx
import { getCurrentUser } from '@/lib/user'

export async function Header() {
  const user = await getCurrentUser()    // First call: query runs
  return <header>Welcome, {user?.name}</header>
}

// app/sidebar.tsx
import { getCurrentUser } from '@/lib/user'

export async function Sidebar() {
  const user = await getCurrentUser()    // Same request: cache hit, no query
  return <aside>{user?.name}</aside>
}
```

### Incorrect patterns

**Defining inside a component — defeats sharing:**

```tsx
// BAD: every render creates a new cache() — siblings do not share.
export async function Profile() {
  const getUser = cache(async () => db.user.findUnique({ where: { id: 1 } }))
  return <div>{(await getUser()).name}</div>
}
```

**Inline object arguments — every call is a cache miss:**

```tsx
// BAD: { id: 1 } is a new reference every call.
const getUser = cache(async (params: { id: number }) =>
  db.user.findUnique({ where: { id: params.id } }),
)

await getUser({ id: 1 })   // miss
await getUser({ id: 1 })   // miss again — new object reference
```

```tsx
// GOOD: primitive arg uses value equality.
const getUser = cache(async (id: number) =>
  db.user.findUnique({ where: { id } }),
)

await getUser(1)   // miss
await getUser(1)   // hit
```

**Using React.cache inside a 'use cache' boundary — values do not cross:**

```tsx
// BAD: shared.current will read as null inside Child because
// React.cache is isolated inside 'use cache' scopes.
import { cache } from 'react'

const store = cache(() => ({ current: null as string | null }))

function Parent() {
  const shared = store()
  shared.current = 'set by parent'
  return <Child />
}

async function Child() {
  'use cache'
  const shared = store()
  return <div>{shared.current}</div>  // null
}
```

Pass data into a `'use cache'` scope via function arguments — see sibling rule `nextjs-use-cache.md`.

### Errors are cached too

Per React 19 docs: if the cached function throws for given arguments, the error is memoized and re-thrown on next call with same arguments — for the duration of the request. If you call `getCurrentUser()` and the DB is down, every subsequent call within that request will re-throw without re-hitting the DB. This is usually what you want; be aware of it.

### When NOT to use React.cache()

- For `fetch()` calls in Next.js — `fetch` is already request-memoized.
- For cross-request caching — use `'use cache'` (Next.js) or an external cache.
- For Client Component work — `React.cache()` does nothing client-side.
- Inside a `'use cache'` boundary — the cache is isolated there.
- For "expensive computation across renders in a Client Component" — that is `useMemo()`.

### Verification

- Static detection is unreliable. Reviewers must confirm: (a) module-scope `cache(fn)` definition, (b) primitive arguments or stable references, (c) Server-Component-only call sites, (d) not nested inside a `'use cache'` boundary.
- A targeted lint rule could check (a) (module-scope-ness of `cache(...)` calls). Not in current pilot scope.

Sources for this rule:

- [Vercel agent-skills: server-cache-react](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-cache-react.md)
- [React 19 — cache() API](https://react.dev/reference/react/cache)
- [Next.js 16 — 'use cache' (React.cache isolation note)](https://nextjs.org/docs/app/api-reference/directives/use-cache)
