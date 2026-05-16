---
title: Depend on the primitive value your Effect actually reads — not the parent object — and don't use this to hide real deps
impact: LOW
impactDescription: "useEffect([user.id]) re-runs only when id changes; useEffect([user]) re-runs on any field change. Honest narrowing reduces churn. Dishonest narrowing (skip deps your Effect reads) is a bug."
tags: [rerender, useEffect, dependencies, optimization]
applicable_to: [react, nextjs]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-004"
verification: { type: review, status: manual, notes: "Reviewer enforces react-hooks/exhaustive-deps; checks that narrowed deps reflect what the Effect actually reads." }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Narrow only when Effect actually reads only that field"
      - "For non-reactive callback dependencies on 19.2+, use useEffectEvent"
      - "Cross-link with rerender-derived-state (threshold case)"
  gap_check: { status: complete, note: "Overlaps rerender-derived-state on threshold case — kept separate as 'narrow deps' vs 'subscribe to derived signal'." }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-dependencies"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-dependencies.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-dependencies"
    quote: "Specify primitive dependencies instead of objects to minimize effect re-runs."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: [rerender-derived-state, advanced-use-latest]
---

## Depend on the primitive value the Effect reads — and never use this to hide real deps

**Impact: LOW — Narrowing deps is honest only when the Effect truly reads just that field.**

### Correct

```tsx
useEffect(() => {
  log(user.id)               // only reads user.id
}, [user.id])                // only that primitive is in the deps
```

### Incorrect — wasteful

```tsx
useEffect(() => {
  log(user.id)               // still only reads user.id
}, [user])                   // but re-runs on every user-field change
```

### Forbidden — dishonest narrowing

```tsx
// BAD: Effect actually reads user.role too. Listing only user.id silences the
// exhaustive-deps rule but creates a stale-closure bug.
useEffect(() => {
  if (user.role === 'admin') doAdminThing(user.id)
}, [user.id])
```

Keep `react-hooks/exhaustive-deps` ESLint rule on. If you find yourself adding eslint-disable comments to silence it, you're either:
- Hiding a real dep (bug), or
- Working around a need for `useEffectEvent` (React 19.2+, see sibling rule `advanced-use-latest`).

### Threshold case — see sibling rule

If the Effect uses a continuous value via a threshold comparison (`width < 768`), prefer subscribing to the derived signal directly (`useMediaQuery`). See `rerender-derived-state`.

Sources:
- [Vercel: rerender-dependencies](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-dependencies.md)
