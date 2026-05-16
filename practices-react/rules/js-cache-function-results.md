---
title: Memoize pure deterministic function results in a bounded module-level Map; never store user/tenant-scoped data
impact: LOW-MEDIUM
impactDescription: "Avoids recomputation when the same pure function is called many times with the same inputs (slugify, parseColor, formatDate). Cache MUST be bounded (LRU or explicit max size) and MUST NOT key on user/tenant-scoped data without scoping."
tags: [javascript, cache, memoization, performance]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-JS-005"
verification: { type: review, status: manual, notes: "Reviewer checks: (a) function is pure & deterministic, (b) cache has explicit max size or LRU, (c) cache key doesn't leak across users/tenants, (d) cache doesn't grow unboundedly in long-lived processes." }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Required bounded cache (LRU / max size / TTL)"
      - "Forbid user/tenant-sensitive keys without scoping"
      - "Pure deterministic functions only"
      - "Include locale/options/version in key when relevant"
  gap_check:
    status: complete
    note: "Narrow scope: pure client/shared JS function memoization. Server caches use sibling rules (server-cache-react, nextjs-use-cache, server-cache-lru)."
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: js-cache-function-results"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-cache-function-results.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "js-cache-function-results"
    quote: "Use a module-level Map to cache function results when the same function is called repeatedly with the same inputs during render."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: [server-cache-react, server-cache-lru, nextjs-use-cache]
---

## Memoize pure deterministic function results in a bounded module-level Map

**Impact: LOW-MEDIUM — Useful for pure repeated work; the cache MUST be bounded.**

### When this applies

- Function is **pure** — same inputs → same output, no side effects.
- Inputs are **primitives or stable references**.
- Function is called many times during render or in a hot path.
- Examples: `slugify`, `parseColor`, `formatDate(locale, value)`, `escapeRegex`, hash functions on stable strings.

### Correct — bounded LRU

```typescript
import { LRUCache } from 'lru-cache'

const slugCache = new LRUCache<string, string>({ max: 500 })

export function cachedSlugify(text: string): string {
  const hit = slugCache.get(text)
  if (hit) return hit
  const result = slugify(text)
  slugCache.set(text, result)
  return result
}
```

### Correct — small bounded Map (no library)

```typescript
const MAX = 100
const cache = new Map<string, string>()

export function cachedFormat(input: string): string {
  if (cache.has(input)) return cache.get(input)!
  if (cache.size >= MAX) cache.clear()   // simple bounded behavior
  const result = format(input)
  cache.set(input, result)
  return result
}
```

### Incorrect — unbounded Map (memory leak)

```typescript
// BAD: grows forever, no eviction. Long-lived processes (Next.js servers) leak memory.
const cache = new Map<string, string>()
export function cachedSlugify(text: string): string {
  if (!cache.has(text)) cache.set(text, slugify(text))
  return cache.get(text)!
}
```

### Forbidden cache keys

- User-derived strings without scoping (admin sees other user's data — leak)
- Tenant-derived data without tenant id in key (tenant leak)
- Locale-sensitive output without locale in key (wrong language served)
- Time-sensitive output without TTL (stale data)

### Key composition for parametrized functions

```typescript
const cache = new LRUCache<string, string>({ max: 500 })

export function cachedFormat(value: number, locale: string): string {
  const key = `${locale}|${value}`            // locale in key
  const hit = cache.get(key)
  if (hit !== undefined) return hit
  const result = new Intl.NumberFormat(locale).format(value)
  cache.set(key, result)
  return result
}
```

### Sibling rules

- Server-side per-request dedup → `server-cache-react` (React.cache).
- Server-side cross-request → `nextjs-use-cache` (Next 16) or `server-cache-lru` (fallback).
- This rule: pure JS / client-side function memoization, scoped narrowly.

Sources:
- [Vercel: js-cache-function-results](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-cache-function-results.md)
