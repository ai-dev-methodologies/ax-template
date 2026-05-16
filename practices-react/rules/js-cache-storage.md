---
title: Cache repeated synchronous browser-storage reads in memory; invalidate on local writes and cross-tab storage events
impact: LOW-MEDIUM
impactDescription: "localStorage / document.cookie reads are synchronous and not free. Cache in memory for hot paths, but invalidate on local writes (storage event doesn't fire on the writing tab) and best-effort revalidate on focus/visibility."
tags: [javascript, localStorage, storage, caching, performance]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-JS-006"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "SSR guard required (window undefined on server)"
      - "Same-tab storage events do NOT fire on the writing tab — invalidate on local writes manually"
      - "Cookies can change via server Set-Cookie — visibility revalidation is best-effort"
      - "Keep localStorage and cookie examples separate (different invalidation paths)"
  gap_check:
    status: complete
    note: "Sibling client-localstorage-schema covers correctness/versioning/SSR; this rule is the performance dimension."
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: js-cache-storage"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-cache-storage.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "js-cache-storage"
    quote: "localStorage, sessionStorage, and document.cookie are synchronous and expensive. Cache reads in memory."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: [client-localstorage-schema]
---

## Cache repeated synchronous browser-storage reads in memory; invalidate on local writes and cross-tab events

**Impact: LOW-MEDIUM — Storage reads are synchronous and not free. Cache them, but invalidate correctly.**

### localStorage — cache + write-through

```typescript
const cache = new Map<string, string | null>()

export function getLocalItem(key: string): string | null {
  if (typeof window === 'undefined') return null   // SSR guard
  if (cache.has(key)) return cache.get(key) ?? null
  const value = window.localStorage.getItem(key)
  cache.set(key, value)
  return value
}

export function setLocalItem(key: string, value: string): void {
  if (typeof window === 'undefined') return
  try {
    window.localStorage.setItem(key, value)
    cache.set(key, value)   // keep cache in sync — storage event won't fire here
  } catch {}
}

// Invalidate on cross-tab change (the storage event fires ONLY on other tabs)
if (typeof window !== 'undefined') {
  window.addEventListener('storage', (e) => {
    if (e.key) cache.delete(e.key)
  })
}
```

### Critical invalidation rule

- `storage` event fires on OTHER tabs, NOT on the tab that called `setItem`. You must update the cache manually on every write in the same tab.
- `visibilitychange` invalidation is best-effort — useful for cookie data that the server might set, but doesn't guarantee freshness.

### Cookie — different invalidation path

```typescript
let cookieCache: Record<string, string> | null = null

export function getCookie(name: string): string | undefined {
  if (typeof document === 'undefined') return undefined   // SSR guard
  if (cookieCache === null) {
    cookieCache = Object.fromEntries(
      document.cookie.split('; ').filter(Boolean).map((c) => {
        const i = c.indexOf('=')
        return i < 0 ? [c, ''] : [c.slice(0, i), decodeURIComponent(c.slice(i + 1))]
      }),
    )
  }
  return cookieCache[name]
}

// Cookies can be set server-side (Set-Cookie header on AJAX/navigation responses)
// → best-effort revalidation on focus/visibility
if (typeof document !== 'undefined') {
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') cookieCache = null
  })
}
```

### Incorrect — every call re-reads

```typescript
function getTheme() {
  return localStorage.getItem('theme') ?? 'light'   // 10× = 10 storage reads
}
```

### Sibling rule

`client-localstorage-schema` covers correctness (versioning, try-catch, minimal fields, SSR safety). This rule is the **performance** dimension — combine both for production code.

Sources:
- [Vercel: js-cache-storage](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-cache-storage.md)
