---
title: Don't useMemo a primitive-result expression — the memo overhead exceeds the cost
impact: LOW-MEDIUM
impactDescription: "useMemo allocates a closure and a dependency array, runs shallow-equality comparison, and only THEN runs your function. For `a || b` or `a + b`, this is more expensive than just re-running the expression."
tags: [rerender, useMemo, optimization, react-compiler]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-009"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Compiler-first: manual useMemo usually redundant in React 19 + Compiler"
      - "Exception: measured-expensive computation OR referential stability required by external API"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-simple-expression-in-memo"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-simple-expression-in-memo.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-simple-expression-in-memo"
    quote: "Calling useMemo and comparing hook dependencies may consume more resources than the expression itself."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_AS_IS }
sibling_rules: [rerender-memo]
---

## Don't useMemo a primitive-result expression — the memo costs more than the work

**Impact: LOW-MEDIUM — useMemo has overhead; cheap expressions pay it for no gain.**

### Incorrect

```tsx
function Header({ user, notifications }: Props) {
  const isLoading = useMemo(
    () => user.isLoading || notifications.isLoading,
    [user.isLoading, notifications.isLoading],
  )
  return isLoading ? <Skeleton /> : <Real />
}
```

`useMemo` allocates: a function closure, a dependency array, a hook slot. On every render it shallow-compares the deps. The expression `a || b` is one CPU cycle.

### Correct

```tsx
function Header({ user, notifications }: Props) {
  const isLoading = user.isLoading || notifications.isLoading
  return isLoading ? <Skeleton /> : <Real />
}
```

### Compiler-era framing

React Compiler (GA in React 19) auto-memoizes intermediate values. Manual `useMemo` for primitives is doubly redundant when compiler is on — it adds noise AND a duplicate cache layer.

### Exceptions — when useMemo IS justified

1. **Measured-expensive computation** — sort a million items, build a graph, run a parser. Profile first.
2. **Referential stability required by external API** — passing a value to a memoized child where prop equality is the optimization, OR using it as a `useEffect` dependency that should NOT re-run.

If neither applies, skip the memo.

Sources:
- [Vercel: rerender-simple-expression-in-memo](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-simple-expression-in-memo.md)
