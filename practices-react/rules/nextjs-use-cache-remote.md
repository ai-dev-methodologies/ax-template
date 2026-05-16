---
title: "use cache: remote — shared durable caching across server instances; gate on hit-rate and cost first"
impact: HIGH
impactDescription: "Stores cache entries in a remote handler (Redis/KV) for cross-instance persistence and high cache utilization. Pays infrastructure + lookup latency cost. ONLY use when work is expensive/rate-limited AND cache keys have few unique values."
tags:
  - server
  - cache
  - nextjs
  - cache-components
  - use-cache-remote
applicable_to:
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-SERVER-011"
verification:
  type: review
  status: manual
  notes: "Reviewer enforces the decision gate: (a) work is expensive/rate-limited/flaky, (b) cache keys have low cardinality (high hit rate expected), (c) cache scope is shared (NOT per-user). Reviewer confirms nesting rules and that cacheHandlers is configured if self-hosting."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
    notes: "Durable shared cache, not per-user. Adds infra + network cost."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Stable since v16.0.0 with Cache Components."
  completeness:
    status: complete
    amendments:
      - "Promote 'when NOT to use' to first-class decision gate"
      - "Cache-scope bluntly: shared across all users; never per-user (use 'use cache: private' for that)"
      - "Nesting matrix complete: remote-in-remote OK, remote-in-regular OK, remote+private forbidden in both directions"
      - "Cache-key cardinality principle with examples"
  gap_check:
    status: complete
    note: "Biggest practical failure mode is choosing remote when keys are mostly-unique — paid latency for near-zero hits."
upstream:
  - id: nextjs-use-cache-remote
    title: "Next.js 16 — 'use cache: remote' directive"
    url: "https://nextjs.org/docs/app/api-reference/directives/use-cache-remote"
    role: canonical-nextjs
  - id: nextjs-use-cache-directive
    title: "Next.js 16 — 'use cache' (parent directive)"
    url: "https://nextjs.org/docs/app/api-reference/directives/use-cache"
    role: canonical-nextjs
evidence:
  - upstream_id: nextjs-use-cache-remote
    section: "What it solves"
    quote: "The 'use cache: remote' directive lets you declaratively specify that a cached output should be stored in a remote cache instead of in-memory, providing durable caching shared across all server instances."
  - upstream_id: nextjs-use-cache-remote
    section: "Tradeoffs"
    quote: "This comes with tradeoffs: infrastructure cost and network latency during cache lookups."
  - upstream_id: nextjs-use-cache-remote
    section: "Cache-key principle"
    quote: "Be thoughtful about which values you include in cache keys. Each unique value creates a separate cache entry, reducing cache utilization."
  - upstream_id: nextjs-use-cache-remote
    section: "Nesting rules"
    quote: "Remote caches cannot be nested inside private caches ('use cache: private'). Private caches cannot be nested inside remote caches."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Promote 'when NOT to use' to first-class decision gate"
    - "Cache-key cardinality is the second most important content"
    - "Shared scope must be explicit; never confuse with per-user"
    - "Nesting matrix must be exact"
sibling_rules:
  - nextjs-use-cache
  - nextjs-use-cache-private
  - server-cache-lru
  - server-cache-react
---

## 'use cache: remote' for shared durable caching — gate on hit rate and cost first

**Impact: HIGH (when correctly applied) — Cross-instance durable cache layer for expensive shared work. Wrong gate decision turns this into paid latency for near-zero hits.**

### Decision gate (per Next.js docs)

**Reach for `'use cache: remote'` ONLY when ALL of these hold:**

1. The work is genuinely expensive (slow DB query, rate-limited API, flaky service, costly computation).
2. Cache keys have **low cardinality** (few unique values → high hit rate).
3. The cache scope is **shared across users** (never per-user — that's `'use cache: private'`).
4. Data doesn't change so frequently that hits go stale immediately.

**Do NOT reach for `'use cache: remote'` when:**

- You already have a server-side key-value store wrapping your data layer (regular `'use cache'` may suffice).
- Operations are already fast (< 50ms) due to proximity or local access — the lookup might be slower than the original work.
- Cache keys are mostly-unique per request (search filters, price ranges, user IDs) — utilization will be near-zero.
- Data changes faster than the cache TTL (seconds/minutes) — most lookups are stale.

### Cache-key cardinality is the implementation gate

The single most common failure mode is caching on a high-cardinality dimension. Fix by **caching on the low-cardinality dimension and filtering in memory**.

```tsx
// BAD: cache per (category, minPrice). minPrice has thousands of values → near-zero hit rate.
async function getProducts(category: string, minPrice: number) {
  'use cache: remote'
  return db.products.findByCategoryAndPrice(category, minPrice)
}
```

```tsx
// GOOD: cache per category (few values), filter price in memory.
async function getProductsByCategory(category: string) {
  'use cache: remote'
  cacheTag(`products-${category}`)
  return db.products.findByCategory(category)
}

async function ProductList({ category, minPrice }: Props) {
  const products = await getProductsByCategory(category)
  return <List items={minPrice ? products.filter((p) => p.price >= minPrice) : products} />
}
```

Same principle for user-specific data: extract the low-cardinality preference (language, currency) and cache on that, not on `sessionId`.

### Configuration

```ts
// next.config.ts
import type { NextConfig } from 'next'
const nextConfig: NextConfig = { cacheComponents: true }
export default nextConfig
```

Cache handler is configured via [`cacheHandlers`](https://nextjs.org/docs/app/api-reference/config/next-config-js/cacheHandlers). Hosting providers (Vercel etc.) typically configure this automatically; self-hosters wire their own (Redis / KV / etc.).

### Comparison with siblings

| Feature | `'use cache'` | `'use cache: remote'` | `'use cache: private'` |
|---|---|---|---|
| Server-side caching | in-memory or custom handler | remote handler (Redis/KV) | none |
| Cache scope | shared across users | shared across users | per-client (browser) |
| `cookies()`/`headers()` inside | No | No | Yes |
| Server cache utilization | may be low outside static shell | high (shared across instances) | n/a |
| Extra cost | none | infrastructure + lookup latency | none |
| Production-ready | yes | yes (since 16.0.0) | **no** (experimental) |

### Nesting rules (build-time enforced)

| Outer / Inner | `'use cache'` | `'use cache: remote'` | `'use cache: private'` |
|---|---|---|---|
| `'use cache'` | OK | OK | OK |
| `'use cache: remote'` | OK | OK | **FORBIDDEN** |
| `'use cache: private'` | OK | **FORBIDDEN** | OK |

Private and remote cannot nest in either direction.

### Correct example — rate-limited CMS

```tsx
import { cookies } from 'next/headers'
import { cacheLife } from 'next/cache'

async function WelcomeMessage() {
  // Language is a small-cardinality preference (~10-50 values)
  const language = (await cookies()).get('language')?.value || 'en'
  const content = await getCMSContent(language)
  return <div>{content.welcomeMessage}</div>
}

async function getCMSContent(language: string) {
  'use cache: remote'
  cacheLife({ expire: 3600 })
  // ~10 cache entries for ~10 languages, shared across ALL users
  return cms.getHomeContent(language)
}
```

### Platform support (verbatim)

| Deployment | Supported |
|---|---|
| Node.js server | Yes |
| Docker | Yes |
| Static export | **No** |
| Adapters | Yes |

### Invalidation

Use `cacheTag('...')` inside the cached function and `revalidateTag('...')` from a Server Action / Route Handler.

Sources:

- [Next.js — 'use cache: remote'](https://nextjs.org/docs/app/api-reference/directives/use-cache-remote)
- [Next.js — cacheHandlers](https://nextjs.org/docs/app/api-reference/config/next-config-js/cacheHandlers)
