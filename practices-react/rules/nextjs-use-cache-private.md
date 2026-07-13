---
title: "use cache: private — experimental escape hatch; refactor runtime APIs out of cached scopes first; not production-recommended in 16.2.6"
impact: LOW
impactDescription: "Per-client browser-memory cache that allows cookies()/headers()/searchParams inside a cached scope. Lost on reload, never stored server-side, not a durable per-user cache. Use ONLY when refactoring the runtime read out is impractical or compliance forbids server storage."
tags:
  - server
  - cache
  - nextjs
  - cache-components
  - use-cache-private
  - experimental
applicable_to:
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-SERVER-010"
verification:
  type: review
  status: manual
  notes: "Reviewer rejects 'use cache: private' unless (a) the function genuinely cannot have its runtime read refactored out, OR (b) compliance forbids server-side storage. Explicit ADR justifying the choice required. Confirms stale >= 30s and use is not in a Route Handler."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
    notes: "Escape-hatch framing aligns with docs explicit guidance. Not durable, not per-user server cache."
  freshness:
    status: experimental-not-production
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Feature is experimental in 16.2.6, depends on runtime prefetching which is not yet stable. Re-review when stabilized."
  completeness:
    status: complete
    amendments:
      - "Lead with experimental status + not-production warning"
      - "Refactor-first hierarchy: try moving runtime read out before reaching for this"
      - "Hard constraints: no Route Handlers, no custom cache handlers, connection() forbidden, stale >= 30s"
      - "Per-client browser-memory only, lost on reload"
  gap_check:
    status: complete
    note: "Main misuse risk: 'private' easily misread as 'durable per-user server cache'. It is NOT that."
upstream:
  - id: nextjs-use-cache-private
    title: "Next.js 16 — 'use cache: private' directive"
    url: "https://nextjs.org/docs/app/api-reference/directives/use-cache-private"
    role: canonical-nextjs
  - id: nextjs-use-cache-directive
    title: "Next.js 16 — 'use cache' (parent directive)"
    url: "https://nextjs.org/docs/app/api-reference/directives/use-cache"
    role: canonical-nextjs
evidence:
  - upstream_id: nextjs-use-cache-private
    section: "Experimental status"
    quote: "This feature is currently experimental and subject to change, it's not recommended for production."
  - upstream_id: nextjs-use-cache-private
    section: "Storage model"
    quote: "However, results are never stored on the server, they're cached only in the browser's memory and do not persist across page reloads."
  - upstream_id: nextjs-use-cache-private
    section: "When to use"
    quote: "You want to cache a function that already accesses runtime data, and refactoring to move the runtime access outside and pass values as arguments is not practical."
  - upstream_id: nextjs-use-cache-private
    section: "Constraints"
    quote: "It is **not** possible to configure custom cache handlers for 'use cache: private'."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Frame as escape hatch, not a recommended cache strategy"
    - "Lead with experimental + not-production warning"
    - "Strong 'refactor first' hierarchy"
    - "Cache scope bluntly stated: browser-memory only, per-client, reload-lost"
sibling_rules:
  - nextjs-use-cache
  - nextjs-use-cache-remote
  - server-cache-react
---

## 'use cache: private' is an experimental escape hatch — refactor first

**Impact: LOW — Narrow tool, not production-recommended in 16.2.6.**

> **Experimental in Next.js 16.2.6** (per official docs: "This feature is currently
> experimental and subject to change, it's not recommended for production"). The
> feature depends on runtime prefetching, which is not yet stable. Treat as
> opt-in for compliance / unrefactorable corners only.

### Refactor-first hierarchy

Before reaching for `'use cache: private'`, try in order:

1. **Move runtime read out of the cached scope.** Read `cookies()` / `headers()` / `searchParams` in the calling component and pass the resolved value as an argument to a regular `'use cache'` function. See sibling rule `nextjs-use-cache` and the Next.js "Working with runtime APIs" guide.
2. **Use a `<Suspense>` boundary** to defer the runtime read to request time while keeping cached children separate.
3. **Only after the above fail or are impractical**, consider `'use cache: private'`. Document the choice in an ADR.

### What this directive actually is

- **Per-client cache** — lives in the user's browser memory only.
- **Not stored on the server** — useful for compliance ("we can't cache this data server-side") but means cache utilization is zero across users.
- **Lost on page reload** — not durable.
- **Server still re-executes the function on every render** — the cache only helps client-side navigation, not server work.

### First try the simpler form — `cookies()` outside any cached scope

Most cookie reads need NO caching directive at all. Read in the Server
Component / Server Action / Route Handler and use the value directly:

```tsx
// app/page.tsx — read cookie in an UNCACHED Server Component, pass value
// to other layers as a primitive argument.
import { cookies } from 'next/headers'

export default async function Page() {
  const theme = (await cookies()).get('theme')?.value ?? 'light'
  return <Hero theme={theme} />
}
```

```tsx
// app/actions.ts — read cookie inside a Server Action, the simplest
// "cookie-scoped data" pattern. No caching involved.
'use server'
import { cookies } from 'next/headers'
import { saveFor } from '@/lib/store'

export async function save(formData: FormData) {
  const owner = (await cookies()).get('owner')?.value
  if (!owner) return
  saveFor(owner, formData.get('value') as string)
}
```

If you only need to consume the cookie value (no caching required), these are
the right forms. `'use cache: private'` enters the picture only when ALL of
the following hold: (a) the function genuinely benefits from caching, (b)
refactoring the runtime read out (per the parent `'use cache'` rule) is
impractical, and (c) you accept the experimental status documented above.

External-validation evidence: the ax-validation-todo app (a Server-Action
based Todo) issues and reads a cookie on every action call without ever
reaching for `'use cache: private'` — the simpler form was sufficient.

### Correct usage (when justified)

```tsx
// app/product/[id]/page.tsx
import { Suspense } from 'react'
import { cookies } from 'next/headers'
import { cacheLife, cacheTag } from 'next/cache'

async function getRecommendations(productId: string) {
  'use cache: private'
  cacheTag(`recommendations-${productId}`)
  cacheLife({ stale: 60 }) // >= 30s required for runtime prefetching

  const sessionId = (await cookies()).get('session-id')?.value || 'guest'
  return getPersonalizedRecommendations(productId, sessionId)
}

export default async function ProductPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params
  return (
    <>
      <ProductDetails id={id} />
      <Suspense fallback={<div>Loading recommendations...</div>}>
        <Recommendations productId={id} />
      </Suspense>
    </>
  )
}
```

### Hard constraints (build-time errors)

- **Not allowed in Route Handlers.**
- **Cannot configure a custom cache handler.**
- **`connection()` is forbidden inside** (provides connection-specific data that cannot be safely cached).
- **`stale` time must be >= 30 seconds** for runtime prefetching to work.

### Allowed inside this scope (verbatim from docs)

| API | `use cache` | `'use cache: private'` |
|---|---|---|
| `cookies()` | No | **Yes** |
| `headers()` | No | **Yes** |
| `searchParams` | No | **Yes** |
| `connection()` | No | No |

### What 'use cache: private' is NOT

- NOT a per-user server cache (that's `'use cache: remote'` keyed by user — but see sibling rule's warning about high-cardinality keys).
- NOT durable across page reloads.
- NOT a shared cache across users.
- NOT a replacement for proper data refactor.

### Anti-pattern — reaching for private when refactor is feasible

```tsx
// BAD: trivial refactor available — read theme outside, pass as arg
async function getThemedContent() {
  'use cache: private'
  const theme = (await cookies()).get('theme')?.value
  return cms.getThemedHero(theme)
}
```

Refactor:

```tsx
// GOOD: regular 'use cache' with the runtime value passed as an arg
async function getThemedContent(theme: string) {
  'use cache'
  cacheLife({ expire: 3600 })
  return cms.getThemedHero(theme)
}

// In the consuming component:
async function Hero() {
  const theme = (await cookies()).get('theme')?.value ?? 'light'
  const content = await getThemedContent(theme)
  return <Banner data={content} />
}
```

The regular `'use cache'` form now caches **per theme** — far better hit rate than per-client.

Sources:

- [Next.js — 'use cache: private'](https://nextjs.org/docs/app/api-reference/directives/use-cache-private)
- [Next.js — Working with runtime APIs](https://nextjs.org/docs/app/getting-started/caching#working-with-runtime-apis)
