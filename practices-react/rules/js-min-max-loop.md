---
title: Single-pass loop for min/max — O(n) instead of O(n log n) sort; Math.min/max spread only for small arrays
impact: LOW
impactDescription: "Sorting an entire array to find one element is O(n log n) plus O(n) allocation. A single pass is O(n) with no allocation. Math.min(...arr) / Math.max(...arr) hits engine argument-count limits at ~125K-640K elements depending on the engine."
tags: [javascript, arrays, performance, sorting, algorithms]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-JS-011"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Handle empty array and NaN explicitly"
      - "Math.min/max spread only for small bounded arrays"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: js-min-max-loop"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-min-max-loop.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "js-min-max-loop"
    quote: "Single pass through the array, no copying, no sorting."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: []
---

## Single-pass loop for min/max — O(n) instead of O(n log n) sort

**Impact: LOW — Sort to find one element is wasteful; one pass suffices.**

### Incorrect — sort just to find latest

```typescript
function latest(items: Item[]) {
  const sorted = [...items].sort((a, b) => b.updatedAt - a.updatedAt)
  return sorted[0]
}
```

### Correct — single pass

```typescript
function latest(items: Item[]): Item | null {
  if (items.length === 0) return null
  let best = items[0]
  for (let i = 1; i < items.length; i++) {
    if (items[i].updatedAt > best.updatedAt) best = items[i]
  }
  return best
}
```

### Correct — both extrema in one pass

```typescript
function range(items: Item[]) {
  if (items.length === 0) return { oldest: null, newest: null }
  let oldest = items[0]
  let newest = items[0]
  for (let i = 1; i < items.length; i++) {
    if (items[i].updatedAt < oldest.updatedAt) oldest = items[i]
    if (items[i].updatedAt > newest.updatedAt) newest = items[i]
  }
  return { oldest, newest }
}
```

### Math.min/max with spread — small arrays only

```typescript
const numbers = [5, 2, 8, 1, 9]
const min = Math.min(...numbers)
const max = Math.max(...numbers)
```

Spread call-argument limit is engine-dependent (Chrome ~125K, Safari ~640K, Node varies). For potentially-large arrays, use the loop form. Throwing `RangeError: Maximum call stack size exceeded` is the failure mode.

### NaN handling

`Math.min` and `>` comparisons propagate `NaN` — a single `NaN` poisons the result. If your data may have `NaN`s, filter first:

```typescript
const cleaned = data.filter((x) => !Number.isNaN(x))
```

### Empty arrays

`Math.min()` returns `Infinity`, `Math.max()` returns `-Infinity`. Handle empty input explicitly to avoid surprise.

Sources:
- [Vercel: js-min-max-loop](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-min-max-loop.md)
