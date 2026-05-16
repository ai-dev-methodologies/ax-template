---
title: Use startTransition for non-urgent state updates that affect rendering — not for imperative bookkeeping
impact: MEDIUM
impactDescription: "startTransition marks a state update as non-urgent, letting React keep urgent updates (typing, clicks) responsive. For imperative work (scroll position storage, mouse tracking), prefer refs / requestAnimationFrame / throttle."
tags: [rerender, transitions, startTransition, performance, concurrent]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-012"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Use for non-urgent state that affects RENDERING"
      - "Don't use for imperative scroll bookkeeping — prefer refs/throttling there"
      - "Not a perf cure-all"
  gap_check: { status: complete, note: "Vercel's scroll-position example is the wrong flagship (codex finding). Reframed example used." }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-transitions"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-transitions.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-transitions"
    quote: "Mark frequent, non-urgent state updates as transitions to maintain UI responsiveness."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  notes: "Vercel's scroll example is weak — typically refs/throttle is the right tool there."
sibling_rules: [rerender-use-deferred-value, rerender-use-ref-transient-values, rendering-usetransition-loading]
---

## Use startTransition for non-urgent state updates that affect rendering

**Impact: MEDIUM — Keeps urgent updates responsive while non-urgent work yields.**

### Use cases

- A filter UI: typing in the search box is urgent; the filtered list update is non-urgent (but it RENDERS something — list/chart updates).
- A tab switch: the active-tab indicator is urgent; the new tab's content render is non-urgent.
- Anything where one state update is interruptible by the next.

### Correct — filter state behind input

```tsx
import { startTransition, useState } from 'react'

function Search({ items }: { items: Item[] }) {
  const [query, setQuery] = useState('')
  const [filtered, setFiltered] = useState(items)

  function onChange(e: ChangeEvent<HTMLInputElement>) {
    setQuery(e.target.value)               // urgent: input must echo
    startTransition(() => {
      setFiltered(items.filter((i) => i.name.includes(e.target.value)))
    })
  }

  return (
    <>
      <input value={query} onChange={onChange} />
      <List items={filtered} />
    </>
  )
}
```

### Wrong tool — imperative bookkeeping

```tsx
// BAD: scroll position tracking. There's no render dependency on scrollY here.
// Use a ref + throttle / rAF instead.
function ScrollTracker() {
  const [scrollY, setScrollY] = useState(0)
  useEffect(() => {
    const handler = () => startTransition(() => setScrollY(window.scrollY))
    window.addEventListener('scroll', handler, { passive: true })
    return () => window.removeEventListener('scroll', handler)
  }, [])
  // ... but nothing here actually rerenders based on scrollY meaningfully
}
```

If `scrollY` doesn't drive a visible render, store it in a ref (see `rerender-use-ref-transient-values`). `startTransition` is for state that DOES drive a render but isn't the urgent one.

### Comparison

| Tool | Use case |
|---|---|
| `startTransition` | non-urgent state update that drives a render |
| `useDeferredValue` | derived render lagging behind urgent input |
| `useRef` | value that should NOT drive a render |
| Throttle / rAF | imperative imperative-throttle scroll/mouse |

### Not a perf cure-all

`startTransition` doesn't make code faster. It changes scheduling priority. If your filter is genuinely slow (sorting a million items), fix the algorithm first.

Sources:
- [Vercel: rerender-transitions](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-transitions.md)
- [React — startTransition](https://react.dev/reference/react/startTransition)
