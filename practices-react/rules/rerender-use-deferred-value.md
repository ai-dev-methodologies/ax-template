---
title: useDeferredValue + useMemo for expensive derived renders behind urgent input — fix the algorithm first if hot
impact: MEDIUM
impactDescription: "Keeps the input snappy by letting the derived expensive render lag behind. Must wrap the expensive computation in useMemo with the deferred value as dependency — otherwise the optimization doesn't apply."
tags: [rerender, useDeferredValue, optimization, concurrent]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-013"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Always wrap the expensive computation in useMemo with the DEFERRED value"
      - "Fix algorithmic cost first for very large datasets"
      - "Don't combine with manual memo unless profiler proves it"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-use-deferred-value"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-use-deferred-value.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-use-deferred-value"
    quote: "When user input triggers expensive computations or renders, use `useDeferredValue` to keep the input responsive."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: [rerender-transitions]
---

## useDeferredValue + useMemo for expensive derived renders behind urgent input

**Impact: MEDIUM — Snappy input + expensive derived view, without throttling.**

### Correct

```tsx
import { useDeferredValue, useMemo, useState } from 'react'

function Search({ items }: { items: Item[] }) {
  const [query, setQuery] = useState('')
  const deferredQuery = useDeferredValue(query)

  const filtered = useMemo(
    () => items.filter((it) => fuzzyMatch(it, deferredQuery)),
    [items, deferredQuery],                    // ← key: depends on DEFERRED, not query
  )

  const isStale = query !== deferredQuery

  return (
    <>
      <input value={query} onChange={(e) => setQuery(e.target.value)} />
      <div style={{ opacity: isStale ? 0.7 : 1 }}>
        <ResultsList results={filtered} />
      </div>
    </>
  )
}
```

### Critical — the useMemo dep must be the DEFERRED value

```tsx
// BAD: useMemo deps on the urgent `query` → re-runs fuzzyMatch immediately, defeating the purpose.
const filtered = useMemo(
  () => items.filter((it) => fuzzyMatch(it, query)),
  [items, query],
)
```

Wrap the expensive computation in `useMemo` AND list `deferredQuery` (or the deferred derived form) as the dependency. Otherwise React's deferred-value scheduling doesn't help.

### Show staleness if the gap is visible

`isStale = query !== deferredQuery` lets you dim/blur the result while it catches up. Common UX touch.

### Fix algorithmic cost first for very large datasets

`useDeferredValue` re-schedules work. It doesn't make work faster. For 100K+ items, build an index (Map / search index), virtualize the list, or move the work to a worker. `useDeferredValue` is a smoothing layer over already-reasonable algorithms.

### Sibling rules

- `rerender-transitions` (startTransition): use when you want to *trigger* a non-urgent update from a handler.
- `rerender-use-deferred-value` (this rule): use when a derived render is the expensive thing and you can't easily separate the trigger from the work.

Sources:
- [Vercel: rerender-use-deferred-value](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-use-deferred-value.md)
- [React — useDeferredValue](https://react.dev/reference/react/useDeferredValue)
