---
title: useRef for transient values that don't drive rendering; useState only for values the UI must reflect
impact: MEDIUM
impactDescription: "Updating a ref does not trigger a re-render. For mouse positions, intervals, transient flags, and any value the UI doesn't render based on, use a ref. Use state ONLY for values that should cause UI updates."
tags: [rerender, useref, state, performance]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-014"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness: { status: complete }
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-use-ref-transient-values"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-use-ref-transient-values.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-use-ref-transient-values"
    quote: "When a value changes frequently and you don't want a re-render on every update [...] store it in useRef instead of useState."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_AS_IS }
sibling_rules: [rerender-transitions]
---

## useRef for transient values that don't drive rendering

**Impact: MEDIUM — State is for the UI; refs are for everything else.**

### Incorrect — re-renders on every mouse move

```tsx
function Tracker() {
  const [lastX, setLastX] = useState(0)
  useEffect(() => {
    const onMove = (e: MouseEvent) => setLastX(e.clientX)
    window.addEventListener('mousemove', onMove)
    return () => window.removeEventListener('mousemove', onMove)
  }, [])
  return <div style={{ position: 'fixed', left: lastX, top: 0, width: 8, height: 8, background: 'black' }} />
}
```

Re-renders 60+ times per second whenever the mouse moves.

### Correct — ref + imperative DOM update

```tsx
function Tracker() {
  const dotRef = useRef<HTMLDivElement>(null)
  useEffect(() => {
    const onMove = (e: MouseEvent) => {
      if (dotRef.current) {
        dotRef.current.style.transform = `translateX(${e.clientX}px)`
      }
    }
    window.addEventListener('mousemove', onMove)
    return () => window.removeEventListener('mousemove', onMove)
  }, [])
  return (
    <div
      ref={dotRef}
      style={{ position: 'fixed', left: 0, top: 0, width: 8, height: 8, background: 'black', transform: 'translateX(0)' }}
    />
  )
}
```

Zero re-renders. The DOM is mutated directly; React doesn't see this and doesn't care.

### Decision: state vs ref

| Question | Answer → use |
|---|---|
| Does the UI render based on this value? | `useState` |
| Does it just need to be remembered? | `useRef` |
| Does it change very frequently? | usually `useRef` |
| Should it appear in dependency arrays? | usually `useState` |

### Pair with requestAnimationFrame for surfacing to UI

If the value eventually needs to appear in the UI (e.g. a debounced "last position" display), accumulate in a ref and flush to state on a rAF tick:

```tsx
useEffect(() => {
  let pending = false
  const onMove = (e: MouseEvent) => {
    lastXRef.current = e.clientX
    if (pending) return
    pending = true
    requestAnimationFrame(() => {
      pending = false
      setDisplayX(lastXRef.current)   // re-render at most once per frame
    })
  }
  // ...
}, [])
```

Sources:
- [Vercel: rerender-use-ref-transient-values](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-use-ref-transient-values.md)
