---
title: Manual LRU cache for cross-request sharing — fallback when Cache Components is unavailable
impact: MEDIUM
impactDescription: "Caches data across sequential requests within a warm process instance. In Next.js 16+ prefer 'use cache' / 'use cache: remote'. LRU remains useful when Cache Components is not available, or when you deliberately want best-effort in-process caching scoped to a warm instance."
tags:
  - server
  - cache
  - lru
  - cross-request
applicable_to:
  - react
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-SERVER-004"
verification:
  type: review
  status: manual
  notes: "Reviewer checks: (a) Cache Components not available or explicitly inappropriate, (b) LRU is module-level with TTL, (c) cache keys don't leak across tenants/users, (d) no expectation of cross-instance coherence."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness:
    status: repositioned
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Next 16 Cache Components ('use cache') is the framework path for durable caching. LRU is now a fallback, not the primary."
  completeness:
    status: complete
    amendments:
      - "Repositioned as fallback to 'use cache' / 'use cache: remote'"
      - "Added deployment matrix (Fluid Compute / serverless / multi-instance)"
      - "Warned about cross-instance non-coherence"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: server-cache-lru"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-cache-lru.md"
    role: seed
  - id: nextjs-use-cache-directive
    title: "Next.js 16 — 'use cache' (the preferred primary in Next 16+)"
    url: "https://nextjs.org/docs/app/api-reference/directives/use-cache"
    role: canonical-nextjs
evidence:
  - upstream_id: vercel-react-best-practices
    section: "server-cache-lru"
    quote: "React.cache() only works within one request. For data shared across sequential requests, use an LRU cache."
  - upstream_id: nextjs-use-cache-directive
    section: "Runtime caching considerations"
    quote: "If the default in-memory cache isn't enough, consider 'use cache: remote' which allows platforms to provide a dedicated cache handler"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - server-cache-react
  - nextjs-use-cache
---

## Manual LRU cache for cross-request sharing — fallback when Cache Components is unavailable

**Impact: MEDIUM — In Next.js 16+ prefer `'use cache'` (sibling rule). LRU remains useful when Cache Components is unavailable, or when you want best-effort in-process caching scoped to a warm instance.**

### Decision order

1. **Next.js 16+ with Cache Components**: use `'use cache'` (sibling rule `nextjs-use-cache.md`).
2. **Need durable cross-instance cache**: use `'use cache: remote'` (Redis/KV via platform handler).
3. **No Cache Components available** OR **deliberate best-effort in-process cache**: manual LRU.
4. **Need per-request dedup only**: `React.cache()` (sibling rule `server-cache-react.md`).

### Manual LRU pattern

```typescript
import { LRUCache } from 'lru-cache'

const userCache = new LRUCache<string, User>({
  max: 1000,
  ttl: 5 * 60 * 1000,  // 5 minutes
})

export async function getUser(id: string) {
  const cached = userCache.get(id)
  if (cached) return cached

  const user = await db.user.findUnique({ where: { id } })
  if (user) userCache.set(id, user)
  return user
}
```

### Deployment matrix

| Environment | LRU effectiveness |
|---|---|
| Vercel Fluid Compute / warm Node server | High — multiple concurrent requests share the same module instance |
| Traditional serverless | Low — cold starts re-execute module code; cache is empty per invocation |
| Multi-instance (e.g. multiple containers) | Low — caches are not coherent across instances |

For cross-instance coherence: Redis/Memcached (or `'use cache: remote'` in Next 16+).

### Non-coherence warning

LRU is **not coherent** across processes. If you write data in one instance and read in another, you'll get stale results. This is acceptable for:
- Reference data (rarely changes — e.g. country list, feature flags).
- "Cache miss is cheap" data (DB read with index).

It is NOT acceptable for:
- Counters / rate limits (each instance has its own count → quota bypass).
- Recently-mutated state (different instances see different states).
- Anything you need a single source of truth for.

### Cache-key safety

Tenant / user / role must be part of the cache key when caching scoped data. `cache.get('user:123')` is fine; `cache.get('settings')` for a multi-tenant app leaks settings between tenants.

Sources:

- [Vercel: server-cache-lru](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-cache-lru.md)
- [Next.js 16 — 'use cache' (preferred primary)](https://nextjs.org/docs/app/api-reference/directives/use-cache)
- [lru-cache (npm)](https://github.com/isaacs/node-lru-cache)
