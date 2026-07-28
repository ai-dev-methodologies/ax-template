---
title: Cheap length compare before expensive array equality (sort, serialize, deep compare)
impact: MEDIUM-HIGH
impactDescription: "O(1) length check filters out the vast majority of inequal cases before the O(n log n) sort or O(n) deep compare runs. Big wins in change-detection hot paths."
tags: [javascript, arrays, performance, comparison]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-JS-008"
verification:
  type: review
  status: manual
  notes: "Reviewer checks that an O(1) `.length` (or size) inequality check runs before the more expensive sort/deep-compare, so the common unequal case short-circuits without paying for the full comparison."
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Clarify ordered vs unordered comparison — sorted-join trick assumes unordered"
      - "Avoid JSON.stringify equality for objects with unstable key order or non-JSON values"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: js-length-check-first"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-length-check-first.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "js-length-check-first"
    quote: "When comparing arrays with expensive operations (sorting, deep equality, serialization), check lengths first."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: []
---

## Cheap length compare before expensive array equality

**Impact: MEDIUM-HIGH — O(1) check filters out most inequal cases before O(n log n) or O(n) work.**

### Incorrect — always runs the expensive path

```typescript
function hasChanges(current: string[], original: string[]) {
  return current.toSorted().join() !== original.toSorted().join()
}
```

### Correct — length first

```typescript
function hasChanges(current: string[], original: string[]) {
  if (current.length !== original.length) return true
  return current.toSorted().join() !== original.toSorted().join()
}
```

### Ordered vs unordered comparison

The `toSorted().join()` trick assumes **unordered** equality (the arrays are equal as sets). For **ordered** equality:

```typescript
function arraysEqualOrdered<T>(a: T[], b: T[]) {
  if (a.length !== b.length) return false
  for (let i = 0; i < a.length; i++) {
    if (!Object.is(a[i], b[i])) return false
  }
  return true
}
```

### `JSON.stringify` equality is a trap

```typescript
// BAD: key order is engine-dependent, NaN/undefined/functions don't round-trip,
// Date/Map/Set serialize losslessly only sometimes.
JSON.stringify(a) === JSON.stringify(b)
```

Use a real deep-equal library (`fast-deep-equal`) or write a typed comparator.

Sources:
- [Vercel: js-length-check-first](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-length-check-first.md)
