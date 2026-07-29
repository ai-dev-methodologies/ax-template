---
title: Index-by-id Map for joining two collections — O(n²) .find loops become O(n)
impact: LOW-MEDIUM
impactDescription: "Build a Map keyed by id once (O(n)); each lookup is then O(1). For 1000 rows × 1000 lookups: 1M ops → 2K ops. Sibling of js-set-map-lookups (membership) — this one is for joins."
tags: [javascript, map, indexing, optimization, performance]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-JS-003"
verification:
  type: review
  status: manual
  notes: "Reviewer checks that a join/lookup against another array by id builds a `Map` once (O(n)) rather than calling `.find()`/`.indexOf()` inside a loop (O(n²)) — confirms the Map is built outside the loop that queries it."
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness: { status: complete, amendments: ["Cross-link to js-set-map-lookups (membership-vs-join distinction)", "Map build cost noted", "Key identity matters (primitive keys preferred)"] }
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: js-index-maps"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-index-maps.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "js-index-maps"
    quote: "Multiple `.find()` calls by the same key should use a Map."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: [js-set-map-lookups]
---

## Index-by-id Map for joining two collections

**Impact: LOW-MEDIUM — Build Map once (O(n)), then all lookups are O(1).**

### Incorrect (O(n) per `.find`, called N times)

```typescript
const enriched = orders.map((o) => ({
  ...o,
  user: users.find((u) => u.id === o.userId),
}))
```

### Correct (O(n) build + O(1) lookup × N)

```typescript
const userById = new Map(users.map((u) => [u.id, u]))
const enriched = orders.map((o) => ({
  ...o,
  user: userById.get(o.userId),
}))
```

### Same fix, spread-free (non-mutating copy)

```typescript
const userById = new Map()
for (const u of users) userById.set(u.id, u)
const enriched = orders.map((o) => Object.assign({}, o, { user: userById.get(o.userId) }))
```

Note the copy: enriching by assigning onto `o` would mutate the caller's `orders`
rows. `Object.assign({}, o, …)` produces a new object, same as the spread form above.

### When it pays off

Build cost is O(n). Only worth it when:
- You're doing **multiple lookups** against the same collection.
- The collection is non-trivial (≥ ~50 items).
- Either / both are in a hot path.

For a single lookup, `find` is fine.

### Sibling distinction

- `js-set-map-lookups` covers **membership** (`Set.has`).
- This rule covers **join** (`Map.get(id)` returns the row).

### Key identity caveats

- Primitive keys (`string`, `number`) use SameValueZero — `1 === 1`.
- Object keys use reference identity — `{ id: 1 } !== { id: 1 }`. Index by a primitive (the id), not the whole object.

Sources:
- [Vercel: js-index-maps](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-index-maps.md)
