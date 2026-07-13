---
title: "Prefer flatMap over map().filter(Boolean) — semantic clarity + single pass"
impact: LOW-MEDIUM
impactDescription: "One pass instead of two, no intermediate array. Also avoids the .filter(Boolean) semantic trap (filters out falsy primitives that may be legitimate values: 0, '', false). NOT always faster — flatMap allocates small wrapper arrays."
tags: [javascript, arrays, flatMap, filter, performance]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-JS-013"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Don't claim 'universally faster' — flatMap allocates small arrays"
      - "Main win is semantic: avoids filter(Boolean) dropping legitimate 0/''/false"
      - "If output should remain nested, wrap as [[y]]"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: js-flatmap-filter"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-flatmap-filter.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "js-flatmap-filter"
    quote: "Use `.flatMap()` to transform and filter in a single pass."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: [js-combine-iterations]
---

## Prefer flatMap over map().filter(Boolean) — semantic clarity + single pass

**Impact: LOW-MEDIUM — One iteration instead of two; correctly handles falsy-but-valid values.**

### The `filter(Boolean)` trap

```typescript
// ❌ Drops legitimate values: 0, '', false, NaN
const userIds = users
  .map((u) => u.isActive ? u.id : null)
  .filter(Boolean)

// If a user.id is 0 (yes, this happens), it's dropped.
```

`filter(Boolean)` removes all falsy values, not just `null`. For ID arrays containing `0`, empty-string keys, or boolean-valued payloads, this is a silent data-loss bug.

### Correct — flatMap

```typescript
const userIds = users.flatMap((u) =>
  u.isActive ? [u.id] : [],
)
```

`[u.id]` is wrapped in an array even when `u.id` is `0` / `''` / `false` — no semantic loss.

### When NOT to claim "faster"

`flatMap` allocates a small wrapper array (`[]` or `[y]`) per element. For very large hot arrays where allocation matters, a plain for-loop may beat both `.map().filter()` and `.flatMap()`. Benchmark before claiming wins.

The main win of this rule is **semantic clarity**, not raw speed.

### If output should remain nested

```typescript
// flatMap flattens ONE level. If your value is itself an array and should
// remain nested:
nodes.flatMap((n) => n.tags.length > 0 ? [[n.id, n.tags]] : [])
//                                       ^^             ^^
//                                       wrap as nested array
```

### More examples

```typescript
// Parse, keeping only valid numbers
const numbers = strings.flatMap((s) => {
  const n = parseInt(s, 10)
  return Number.isNaN(n) ? [] : [n]
})

// Extract from success responses
const emails = responses.flatMap((r) =>
  r.success ? [r.data.email] : [],
)
```

Sources:
- [Vercel: js-flatmap-filter](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-flatmap-filter.md)
