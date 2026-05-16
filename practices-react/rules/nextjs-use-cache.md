---
title: Use the 'use cache' directive for Next.js 16 Cache Components persistent caching
impact: HIGH
impactDescription: "Provides persistent, cross-request, tag-revalidatable caching with compiler-generated cache keys. The Next.js 16 framework-recommended caching primitive for App Router. Replaces most use cases previously served by React.cache() in Next.js apps."
tags:
  - server
  - cache
  - nextjs
  - cache-components
  - use-cache
  - rsc
applicable_to:
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-SERVER-002"
verification:
  type: review
  status: manual
  notes: "Static rule could detect: 'use cache' inside file/function/component, configured `cacheComponents: true` in next.config, no cookies()/headers()/searchParams direct access inside 'use cache' scope, non-serializable arg patterns. None in pilot scope. Manual review until ESLint rule ships."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
  split_origin: "Created from Vercel server-cache-react split. Vercel's seed catalog has no rule for 'use cache' directive — this is a gap_check finding shipped as a new sibling rule."
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
    notes: "Mechanic, cache-key composition, serialization constraints, runtime-API forbidden list, React.cache isolation — all verified verbatim against Next.js 16.2.6 use-cache directive page."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "'use cache' was introduced experimental in v15.0.0 and enabled with Cache Components in v16.0.0. Anchored to next@16.2.6."
  completeness:
    status: complete
    amendments:
      - "Documented cache key composition (build ID + function ID + args + closures)"
      - "Documented serialization constraints (different systems for args vs return)"
      - "Forbidden inside-cache APIs (cookies / headers / searchParams)"
      - "React.cache isolation rule"
      - "Runtime caching by deployment environment (serverless vs self-hosted)"
      - "Build-hang anti-pattern (passing runtime Promises into cached scope)"
      - "On-demand invalidation surface (cacheTag / updateTag / revalidateTag)"
  gap_check:
    status: complete
    note: "Sibling rule for React.cache (server-cache-react.md) covers the non-Next case. 'use cache: private' and 'use cache: remote' are referenced but not catalog rules of their own (yet) — flag in pilot-report for future."
upstream:
  - id: nextjs-use-cache-directive
    title: "Next.js 16 — 'use cache' directive API reference"
    url: "https://nextjs.org/docs/app/api-reference/directives/use-cache"
    role: "canonical-nextjs"
  - id: react-19-cache
    title: "React 19 — cache() API (for isolation context)"
    url: "https://react.dev/reference/react/cache"
    role: "canonical-react"
evidence:
  - upstream_id: nextjs-use-cache-directive
    section: "Apply level"
    quote: "The use cache directive allows you to mark a route, React component, or a function as cacheable."
  - upstream_id: nextjs-use-cache-directive
    section: "Cache keys"
    quote: "When a cached function references variables from outer scopes, those variables are automatically captured and bound as arguments, making them part of the cache key."
  - upstream_id: nextjs-use-cache-directive
    section: "Runtime APIs forbidden"
    quote: "Cached functions and components cannot directly access runtime APIs like cookies(), headers(), or searchParams. Instead, read these values outside the cached scope and pass them as arguments."
  - upstream_id: nextjs-use-cache-directive
    section: "React.cache isolation"
    quote: "React.cache operates in an isolated scope inside use cache boundaries. Values stored via React.cache outside a use cache function are not visible inside it."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=high"
  reviewed_at: "2026-05-16"
  verdict: SPLIT (this rule is one half of the split — see server-cache-react.md for the other)
  agreements:
    - "Need a dedicated Next 16 'use cache' rule"
    - "Define boundary between this and React.cache() crisply"
    - "Vercel fetch-dedup claim still holds for dedup but caching semantics changed under Cache Components"
sibling_rules:
  - server-cache-react
---

## Use the 'use cache' directive for Next.js 16 Cache Components persistent caching

**Impact: HIGH — Provides persistent, cross-request, tag-revalidatable caching with compiler-generated cache keys. The Next.js 16 framework-recommended caching primitive for App Router. Replaces most use cases previously served by `React.cache()` in Next.js apps.**

### Enable

Cache Components is opt-in. Add to `next.config.ts`:

```ts
import type { NextConfig } from 'next'
const nextConfig: NextConfig = { cacheComponents: true }
export default nextConfig
```

`use cache` does not work without this flag set. (Version history: introduced experimental in v15.0.0; enabled with Cache Components in v16.0.0.)

### Apply at three levels

**File level — all exports cached:**

```tsx
'use cache'
export default async function Page() { /* ... */ }
```
> When used at file level, all function exports must be `async`.

**Component level:**

```tsx
export async function BlogPosts() {
  'use cache'
  const posts = await fetch('/api/posts').then((r) => r.json())
  return <ul>{posts.map((p) => <li key={p.id}>{p.title}</li>)}</ul>
}
```

**Function level:**

```tsx
export async function getProducts() {
  'use cache'
  return db.query('SELECT * FROM products')
}
```

### Cache key composition

The compiler-generated cache key includes:

1. **Build ID** — invalidates everything on build
2. **Function ID** — secure hash of function location + signature
3. **Serializable arguments** — props (component) or call arguments (function)
4. **HMR refresh hash** (dev only)
5. **Closed-over variables** are auto-captured and bound as arguments — they enter the key

```tsx
async function Component({ userId }: { userId: string }) {
  const getData = async (filter: string) => {
    'use cache'
    // userId (closure) + filter (arg) both in the key.
    return fetch(`/api/users/${userId}/data?filter=${filter}`)
  }
  return getData('active')
}
```

### Serialization constraints

| | Argument | Return value |
|---|---|---|
| Serialization system | RSC (more restrictive) | RCC (less restrictive) |
| Primitives | ✓ | ✓ |
| Plain objects, arrays, Dates, Maps, Sets, TypedArrays, ArrayBuffers | ✓ | ✓ |
| React elements | pass-through only | ✓ |
| Class instances | ✗ | ✗ |
| Functions | pass-through only | pass-through only |
| Symbols, WeakMaps, WeakSets, URL | ✗ | ✗ |

**Pass-through pattern**: accept `children` / Server Actions as props without reading them inside the cached body. Their value cannot affect the cache entry.

### Forbidden inside cached scopes

- `cookies()`
- `headers()`
- `searchParams`

Read these **outside** the cached scope and pass values as arguments. (If absolutely required, see `'use cache: private'` directive — out of pilot scope.)

### React.cache is isolated inside 'use cache'

```tsx
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
  return <div>{shared.current}</div>  // null — isolated scope
}
```

Use function arguments to pass data into `use cache`, not closed-over React.cache stores. See sibling rule `server-cache-react.md`.

### Revalidation

Default profile:

| stale (client) | revalidate (server) | expire |
|---|---|---|
| 5 min | 15 min | never |

Override with `cacheLife()` and tag with `cacheTag()`:

```tsx
import { cacheLife, cacheTag } from 'next/cache'

export async function getProducts() {
  'use cache'
  cacheLife('hours')
  cacheTag('products')
  return db.query('SELECT * FROM products')
}
```

Invalidate from a Server Action:

```tsx
'use server'
import { updateTag } from 'next/cache'

export async function publishProduct(input: FormData) {
  await db.products.insert(input)
  updateTag('products')   // invalidates all 'products'-tagged caches
}
```

### Runtime caching by deployment

| Environment | Behavior |
|---|---|
| Serverless | Cache entries typically don't persist across requests; each request may be a fresh instance |
| Self-hosted | Cache entries persist; control with `cacheMaxMemorySize` |
| `'use cache: remote'` | Platform-provided durable handler (Redis/KV) — extra latency, platform fees |

### Build hang anti-pattern

If your build hangs ~50s, you are passing a runtime-data Promise (cookies / headers / dynamic fetch) into a cached scope. Awaiting it during prerender deadlocks. Fix: await runtime data **outside** the cached function, then pass the resolved value as an argument.

### Platform support

| Target | Supported |
|---|---|
| Node.js server | Yes |
| Docker | Yes |
| Static export | **No** |
| Adapters | Platform-specific |

### Verification

- Manual review until ESLint rule ships. Detectable patterns: `'use cache'` directive presence, `cacheComponents: true` in config, no `cookies()`/`headers()`/`searchParams` direct access inside cached scope.
- Integration verification: build the project with `next build` and observe the "static shell" output; `use cache` boundaries should produce prerendered HTML chunks. Cache-Components-aware framework already enforces several of these constraints with a build-time error.

Sources for this rule:

- [Next.js 16 — 'use cache' directive](https://nextjs.org/docs/app/api-reference/directives/use-cache)
- [Next.js 16 — Caching guide](https://nextjs.org/docs/app/getting-started/caching)
- [React 19 — cache() (for isolation context)](https://react.dev/reference/react/cache)
