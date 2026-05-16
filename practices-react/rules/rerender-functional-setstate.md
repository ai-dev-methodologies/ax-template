---
title: Use setState(prev => …) when the new state depends on the current state — primary win is correctness (no stale closure)
impact: MEDIUM
impactDescription: "Functional update form prevents stale-closure bugs and lets useCallback omit the state from its dependency array. The rerender benefit (stable callback identity) is secondary."
tags: [react, hooks, useState, useCallback, callbacks, closures, correctness]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-007"
verification:
  type: lint
  rule_id: "ax/prefer-functional-setstate"
  status: planned
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Lead with stale-closure correctness, not rerender"
      - "Stable callback only helps when callback identity is observable (passed to memoized child)"
      - "Don't imply functional updater itself reduces renders"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-functional-setstate"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-functional-setstate.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-functional-setstate"
    quote: "When updating state based on the current state value, use the functional update form of setState instead of directly referencing the state variable."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: []
---

## Functional setState when the new state depends on current — primary win is correctness

**Impact: MEDIUM — Eliminates the most common React stale-closure bug class.**

### Incorrect — stale closure + recreated callback

```tsx
function TodoList() {
  const [items, setItems] = useState(initialItems)

  const addItems = useCallback((newItems: Item[]) => {
    setItems([...items, ...newItems])       // closes over `items` at callback creation
  }, [items])                                // → must depend on items → recreated each items change

  const removeItem = useCallback((id: string) => {
    setItems(items.filter((x) => x.id !== id))   // stale closure if deps omitted
  }, [])                                          // ❌ missing dep — uses initial items forever

  return <ItemsEditor items={items} onAdd={addItems} onRemove={removeItem} />
}
```

### Correct — functional update, no stale closure, stable callback

```tsx
function TodoList() {
  const [items, setItems] = useState(initialItems)

  const addItems = useCallback((newItems: Item[]) => {
    setItems((curr) => [...curr, ...newItems])  // always uses latest state
  }, [])                                         // no items dep needed

  const removeItem = useCallback((id: string) => {
    setItems((curr) => curr.filter((x) => x.id !== id))
  }, [])

  return <ItemsEditor items={items} onAdd={addItems} onRemove={removeItem} />
}
```

### Why this is primarily correctness, not perf

- **Stale closures** are real bugs: data loss, lost updates, wrong calculations.
- **Stable callback identity** only matters when:
  - The callback is passed to a memoized child (`memo`), or
  - It's a dependency of another `useCallback`/`useMemo`/`useEffect`.

Without those, stable identity is free but invisible. The functional updater pays for itself in bug prevention regardless.

### When direct updates are fine

- `setCount(0)` — static value.
- `setName(newName)` — value comes from props/args only, doesn't depend on prior state.

### Compiler note

React Compiler can stabilize some of these patterns. Functional updates remain the right correctness practice — compiler doesn't fix stale closures, it just adjusts memoization.

Sources:
- [Vercel: rerender-functional-setstate](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-functional-setstate.md)
