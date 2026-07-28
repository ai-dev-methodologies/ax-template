---
title: When a memoized component has a non-primitive default prop value, extract the default to a module constant
impact: MEDIUM
impactDescription: "Default value expressions like `onClick = () => {}` or `items = []` create a new reference every render, defeating React.memo's shallow-equality check. Extract to module constant so the reference is stable."
tags: [rerender, memo, optimization]
applicable_to: [react, nextjs]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-003"
verification:
  type: review
  status: manual
  notes: "Reviewer checks that default prop values like `() => {}` or `[]` are hoisted to a module-level constant rather than written inline, since an inline default creates a new reference every render and defeats `React.memo`'s shallow-equality check."
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Frame as 'when a memoized boundary already exists'"
      - "Don't make this a memo-promotion rule (see rerender-memo)"
      - "Compiler may already handle this in React 19 + Compiler projects"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-memo-with-default-value"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-memo-with-default-value.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-memo-with-default-value"
    quote: "When memoized component has a default value for some non-primitive optional parameter, such as an array, function, or object, calling the component without that parameter results in broken memoization."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: [rerender-memo]
---

## When a memoized component already exists, extract non-primitive default prop values to module constants

**Impact: MEDIUM — Scope: only applies to components that already have a memoization boundary. This rule does NOT recommend adding `memo()` — see `rerender-memo` for that policy.**

### Incorrect — default `() => {}` defeats memo

```tsx
const UserAvatar = memo(function UserAvatar({
  onClick = () => {},        // ❌ new function each render
}: { onClick?: () => void }) {
  // ...
})

<UserAvatar />   // memo compares props: onClick !== prevOnClick → re-renders
```

### Correct — module-level constant default

```tsx
const NOOP = () => {}

const UserAvatar = memo(function UserAvatar({
  onClick = NOOP,            // ✅ stable reference
}: { onClick?: () => void }) {
  return <button onClick={onClick}>Avatar</button>
})
```

Same logic for `arr = []`, `obj = {}` defaults. Hoist them.

### Don't reach for `memo` to "fix" this

If the component isn't already memoized, you may not need memo at all. Read sibling rule `rerender-memo` — React Compiler usually makes manual memo unnecessary. Only apply this rule when a memo boundary already exists and is justified.

### Compiler-era nuance

React Compiler (GA in React 19) may auto-stabilize the default expression as well. With compiler on, manual constant extraction is redundant. Verify by profiling.

Sources:
- [Vercel: rerender-memo-with-default-value](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-memo-with-default-value.md)
