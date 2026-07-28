---
title: Derive values during render, not in state synced via Effect
impact: MEDIUM
impactDescription: "If a value can be computed from current props/state, compute it during render. Storing it in state and syncing via useEffect adds an extra render, causes drift bugs, and is the canonical 'You Might Not Need an Effect' anti-pattern."
tags: [rerender, derived-state, useEffect, state, you-might-not-need-an-effect]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-006"
verification:
  type: review
  status: manual
  notes: "Reviewer checks that a value computable from current props/state is computed during render, not stored in state and synced via `useEffect` — the canonical 'You Might Not Need an Effect' anti-pattern that adds an extra render and risks drift bugs."
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness: { status: complete, amendments: ["Lead with React docs framing", "useMemo only for genuinely expensive derivation with profiler evidence"] }
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-derived-state-no-effect"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-derived-state-no-effect.md"
    role: seed
  - id: react-you-might-not-need-effect
    title: "React docs — You Might Not Need an Effect"
    url: "https://react.dev/learn/you-might-not-need-an-effect"
    role: canonical-react
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-derived-state-no-effect"
    quote: "If a value can be computed from current props/state, do not store it in state or update it in an effect."
  - source_type: external
    citation: "React docs — Updating state based on props or state (the You Might Not Need an Effect canonical 'derive during render' guidance)"
    url: "https://react.dev/learn/you-might-not-need-an-effect#updating-state-based-on-props-or-state"
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_AS_IS }
sibling_rules: [rerender-move-effect-to-event]
---

## Derive during render, not via state + Effect

**Impact: MEDIUM — Canonical React docs guidance ("You Might Not Need an Effect"). Extra render + drift risk.**

### Incorrect — redundant state, drift-prone

```tsx
function Form() {
  const [first, setFirst] = useState('First')
  const [last, setLast] = useState('Last')
  const [full, setFull] = useState('')

  useEffect(() => {
    setFull(`${first} ${last}`)        // extra render after each change; can drift if any path forgets to update
  }, [first, last])

  return <p>{full}</p>
}
```

### Correct — derive during render

```tsx
function Form() {
  const [first, setFirst] = useState('First')
  const [last, setLast] = useState('Last')
  const full = `${first} ${last}`      // no extra render, no drift possible
  return <p>{full}</p>
}
```

### When `useMemo` is justified

Only when the derivation is **genuinely expensive** AND profiler evidence shows the cost matters:

```tsx
const sortedItems = useMemo(
  () => items.toSorted(expensiveComparator),
  [items],
)
```

For cheap derivations (string concat, simple arithmetic, boolean), don't reach for `useMemo` — the memo overhead exceeds the derivation cost (see sibling rule `rerender-simple-expression-in-memo`).

### Related rule

`rerender-move-effect-to-event` covers the sibling case: "if it's not derivation but a user-action side effect, the side effect belongs in an event handler, not in state + Effect".

Sources:
- [Vercel: rerender-derived-state-no-effect](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-derived-state-no-effect.md)
- [React — You Might Not Need an Effect](https://react.dev/learn/you-might-not-need-an-effect)
