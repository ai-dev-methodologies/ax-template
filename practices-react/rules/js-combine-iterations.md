---
title: Combine multiple .filter/.map passes over the same array into one loop when the array is large or hot
impact: LOW-MEDIUM
impactDescription: "Three .filter() calls iterate three times and allocate three arrays. One for-of loop with three branches iterates once and allocates only the needed result arrays. Apply when the array is large or the path is hot — not as a blanket rule."
tags: [javascript, arrays, loops, performance]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-JS-007"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Don't replace readable pipelines by default — apply to large/hot arrays"
      - "Preserve exact semantics: order, short-circuiting, holes, side-effects"
      - "Consider reduce only when it improves clarity over named for-of"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: js-combine-iterations"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-combine-iterations.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "js-combine-iterations"
    quote: "Multiple `.filter()` or `.map()` calls iterate the array multiple times."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: [js-flatmap-filter]
---

## Combine multiple .filter/.map passes when the array is large or hot

**Impact: LOW-MEDIUM — Three `.filter` calls = three iterations + three allocations. One `for-of` = one iteration. Apply when the array is large or the path is hot, not as a blanket rule.**

### Incorrect — 3 iterations + 3 allocations

```typescript
const admins = users.filter((u) => u.isAdmin)
const testers = users.filter((u) => u.isTester)
const inactive = users.filter((u) => !u.isActive)
```

### Correct — 1 iteration, 3 small result arrays

```typescript
const admins: User[] = []
const testers: User[] = []
const inactive: User[] = []

for (const user of users) {
  if (user.isAdmin) admins.push(user)
  if (user.isTester) testers.push(user)
  if (!user.isActive) inactive.push(user)
}
```

### Don't apply blanket

For small arrays (`< ~100` items) or non-hot paths, the readability of `.filter().filter().filter()` outweighs the perf cost. Keep the rule for:

- Lists ≥ ~1K items.
- Render-frequent hot paths.
- Stream-style processing where allocations matter.

### Preserve semantics

Be careful when consolidating:
- **Short-circuiting**: if `.filter` was followed by `.find` or `.some`, the consolidated loop must `break` on the right condition.
- **Order**: `.filter` preserves input order; if your loop puts items into different result arrays, those individual arrays also preserve order — fine.
- **Holes**: sparse arrays (`new Array(5)`) skip with `.filter` but show as `undefined` with `for-of`. Match the semantics you actually need.
- **Side-effects**: chained methods make each pass's side effects visible separately; a single loop interleaves them.

Sources:
- [Vercel: js-combine-iterations](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-combine-iterations.md)
