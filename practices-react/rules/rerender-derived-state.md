---
title: Subscribe to the semantic signal you actually need, not the continuous value behind it
impact: MEDIUM
impactDescription: "When the UI only cares about a threshold (mobile vs desktop), subscribe to the derived boolean (useMediaQuery), not the raw value (useWindowWidth). Re-render only on transitions, not on every pixel."
tags: [rerender, derived-state, media-query, optimization]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-005"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Don't replace width when exact pixel value is needed for layout math"
      - "Mention overlap with rerender-dependencies (narrowing); this rule is 'choose the right hook'"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-derived-state"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-derived-state.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-derived-state"
    quote: "Subscribe to derived boolean state instead of continuous values to reduce re-render frequency."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: [rerender-dependencies]
---

## Subscribe to the semantic signal, not the continuous value

**Impact: MEDIUM — Re-render on transitions, not on every micro-change.**

### Incorrect

```tsx
function Sidebar() {
  const width = useWindowWidth()    // re-renders on every resize pixel
  const isMobile = width < 768
  return <nav className={isMobile ? 'mobile' : 'desktop'} />
}
```

### Correct

```tsx
function Sidebar() {
  const isMobile = useMediaQuery('(max-width: 767px)')   // re-renders only on transition
  return <nav className={isMobile ? 'mobile' : 'desktop'} />
}
```

### When you DO need the continuous value

If the UI renders the exact pixel value, or uses it for layout math (e.g. `style={{ width: width / 2 }}`), keep `useWindowWidth`. The rule applies when the UI uses ONLY the derived boolean.

### Related rule

`rerender-dependencies` (narrow deps to primitives) targets the same goal from a different angle: narrowing what an Effect reads. This rule is "pick the right subscription primitive in the first place".

Sources:
- [Vercel: rerender-derived-state](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-derived-state.md)
