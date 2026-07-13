---
title: useTransition for non-urgent UI updates (search/filter/navigation) — not a replacement for network-lifecycle loading state
impact: LOW
impactDescription: "Marks state updates as transitions so React keeps the prior UI responsive while the new state is computed. Built-in isPending. NOT a substitute for explicit loading state on network fetches, uploads, mutations."
tags: [rendering, transitions, useTransition, loading, state, react-19]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RENDERING-009"
verification:
  type: review
  status: manual
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Reframed: transition is for non-urgent UI updates that should be interruptible"
      - "Network-lifecycle loading state (uploads, mutations, imperative async) still uses explicit useState"
      - "Suspense + framework pending state is the preferred path for data fetching"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: rendering-usetransition-loading"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-usetransition-loading.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rendering-usetransition-loading"
    quote: "Use `useTransition` instead of manual `useState` for loading states."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - async-suspense-boundaries
---

## useTransition for non-urgent UI updates — not a replacement for network-lifecycle loading state

**Impact: LOW — Marks state updates as transitions so React keeps the prior UI responsive while the new state is computed. NOT a substitute for explicit loading state on network fetches, uploads, mutations.**

### When useTransition is the right tool

- A search input where typing updates immediately but results filter lazily
- Tab navigation that triggers a heavy re-render
- A filter/sort change on a long list
- Any UI update that's expensive but interruptible

The key: the update is **interruptible** (a new keystroke supersedes the prior one) and the user should see the OLD state while the NEW state computes.

### Correct — search with interruptible filter

```tsx
import { useState, useTransition } from 'react'

function Search() {
  const [query, setQuery] = useState('')
  const [isPending, startTransition] = useTransition()

  return (
    <>
      <input
        value={query}
        onChange={(e) => {
          setQuery(e.target.value)  // urgent — input must feel instant
          startTransition(() => {
            // Marked as non-urgent; React can interrupt this on next keystroke.
            setFilteredResults(filterBig(e.target.value))
          })
        }}
      />
      {isPending && <Spinner />}
      <FilteredResultsList />
    </>
  )
}
```

### Not the right tool for network lifecycle

useTransition is **not** an async lifecycle manager. For:
- HTTP fetches with error/timeout/cancellation
- File uploads with progress
- Optimistic mutations with rollback

…use explicit `useState` for the loading state, or better, the data-fetch library's pending/error/data shape (TanStack Query, SWR), or React 19 `use(promise)` + Suspense (sibling rule `async-suspense-boundaries`).

### Don't wrap async/await inside startTransition

```tsx
// BAD: useTransition is synchronous-update-oriented. Wrapping an async function
// here can mislead readers and obscure error/cancel semantics.
startTransition(async () => {
  const data = await fetch(...)
  setResults(data)
})
```

If you have an actual async fetch, use `useActionState` (React 19) for forms, or framework data primitives, or your data library's mutation hook.

### Comparison

| Need | Tool |
|---|---|
| Interruptible UI update (filter, search, tab) | `useTransition` |
| Network fetch / mutation / upload | data library (TanStack/SWR) OR `useActionState` OR explicit `useState` |
| Read-only data dependency | `<Suspense>` + `use(promise)` |
| Pending state on Server Action / form | `useActionState` / `useFormStatus` |

Sources:
- [Vercel: rendering-usetransition-loading](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-usetransition-loading.md)
- [React 19 — useTransition](https://react.dev/reference/react/useTransition)
