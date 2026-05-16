---
title: Split useMemo/useEffect when independent tasks have different dependencies; don't split tightly coupled logic
impact: MEDIUM
impactDescription: "A combined hook reruns the entire body when any dependency changes. Splitting independent tasks means each runs only when its own deps change. Don't split coupled logic into noise."
tags: [rerender, useMemo, useEffect, dependencies, optimization]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-010"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Split only when tasks are TRULY independent"
      - "Avoid splitting tightly coupled logic into noise"
      - "Compiler may handle some of these automatically"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-split-combined-hooks"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-split-combined-hooks.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-split-combined-hooks"
    quote: "A combined hook reruns all tasks when any dependency changes, even if some tasks don't use the changed value."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: []
---

## Split useMemo/useEffect when independent tasks have different deps

**Impact: MEDIUM — Independent tasks get independent invalidation.**

### Incorrect — combined useMemo

```tsx
const sorted = useMemo(() => {
  const filtered = products.filter((p) => p.category === category)
  return filtered.toSorted((a, b) =>
    sortOrder === 'asc' ? a.price - b.price : b.price - a.price,
  )
}, [products, category, sortOrder])   // changing sortOrder re-runs filter
```

### Correct — split by dependency boundary

```tsx
const filtered = useMemo(
  () => products.filter((p) => p.category === category),
  [products, category],
)

const sorted = useMemo(
  () => filtered.toSorted((a, b) =>
    sortOrder === 'asc' ? a.price - b.price : b.price - a.price,
  ),
  [filtered, sortOrder],
)
```

Now changing `sortOrder` only re-sorts; filter result is reused.

### Same pattern for useEffect

```tsx
// BAD: both side effects re-run when either dep changes
useEffect(() => {
  analytics.trackPageView(pathname)
  document.title = `${pageTitle} | My App`
}, [pathname, pageTitle])

// GOOD: independent invalidation
useEffect(() => analytics.trackPageView(pathname), [pathname])
useEffect(() => { document.title = `${pageTitle} | My App` }, [pageTitle])
```

### Don't split for the sake of it

If logic is **tightly coupled** — second step needs the result of the first, computed mid-render — splitting becomes noise. Profile-driven decision.

### Compiler note

React Compiler may auto-track finer dependency boundaries in compiled code. Apply this rule consciously in compiler-off projects; with compiler on, profile before adding splits.

Sources:
- [Vercel: rerender-split-combined-hooks](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-split-combined-hooks.md)
