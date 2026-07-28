---
title: Pass a function to useState when the initial value requires heavy computation
impact: MEDIUM
impactDescription: "useState(expensive()) runs expensive() on every render even though only the first call's result is used. useState(() => expensive()) runs it only on mount. StrictMode dev double-mount may call the initializer twice — make it pure."
tags: [react, hooks, useState, performance, initialization]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-008"
verification:
  type: review
  status: manual
  notes: "Reviewer checks that an expensive initializer passed to `useState` is wrapped in a function (`useState(() => expensive())`) rather than called eagerly (`useState(expensive())`), and that the initializer is pure (StrictMode dev double-mounts may invoke it twice)."
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Scope to heavy sync initializers"
      - "Lazy init for cheap literals is unnecessary noise"
      - "StrictMode dev double-mount may call initializer twice — make pure"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-lazy-state-init"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-lazy-state-init.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-lazy-state-init"
    quote: "Without the function form, the initializer runs on every render even though the value is only used once."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_AS_IS }
sibling_rules: []
---

## Pass a function to useState when the initial value requires heavy computation

**Impact: MEDIUM — Compiler-irrelevant; avoids repeating expensive sync work each render.**

### Incorrect — runs every render

```tsx
function FilteredList({ items }: { items: Item[] }) {
  const [index, setIndex] = useState(buildSearchIndex(items))  // runs on every render
  // ...
}

function UserProfile() {
  const [settings] = useState(
    JSON.parse(localStorage.getItem('settings') ?? '{}'),       // runs on every render
  )
  // ...
}
```

### Correct — runs on mount only

```tsx
function FilteredList({ items }: { items: Item[] }) {
  const [index, setIndex] = useState(() => buildSearchIndex(items))
  return <SearchResults index={index} onRebuild={() => setIndex(buildSearchIndex(items))} />
}

function UserProfile() {
  const [settings] = useState(() => {
    const stored = localStorage.getItem('settings')
    return stored ? JSON.parse(stored) : {}
  })
  return <SettingsPanel value={settings} />
}
```

### When lazy init is unnecessary

- Cheap literals: `useState(0)`, `useState('')`, `useState({})`.
- Direct prop references: `useState(props.initial)`.
- Simple defaults: `useState({ open: false })`.

The function form here is noise, not a win.

### StrictMode caveat

In React StrictMode (dev only), the initializer may run twice as part of intentional double-invocation. Make initializers pure — no side effects, no logging, no DOM mutations. Reading from `localStorage` is acceptable (idempotent read).

### Use cases

- Building search indexes / Map / Set from props.
- Reading and parsing from `localStorage` / `sessionStorage`.
- Reading from the DOM (rare; only when SSR-safe).
- Heavy sync transformations of large input.

Sources:
- [Vercel: rerender-lazy-state-init](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-lazy-state-init.md)
