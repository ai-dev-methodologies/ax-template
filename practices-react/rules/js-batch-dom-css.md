---
title: Group DOM writes before reads; prefer className over imperative inline style
impact: MEDIUM
impactDescription: "Interleaving style writes with layout reads (offsetWidth, getBoundingClientRect, getComputedStyle) forces synchronous reflows. In React, prefer state-driven className over imperative ref.style mutations entirely."
tags: [javascript, dom, css, performance, reflow, layout-thrashing]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-JS-002"
verification:
  type: review
  status: manual
  notes: "Reviewer checks: (a) no style write is directly followed by a layout-forcing read (offsetWidth/offsetHeight/getBoundingClientRect/getComputedStyle) before the next write, (b) React components prefer state-driven `className` over imperative `ref.style` mutation outside genuinely imperative cases (raw rAF animation, native DOM integration, focus/scroll positioning)."
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness: { status: complete, amendments: ["Prefer React state/className over ref.style", "Mention requestAnimationFrame for frame-level coordination"] }
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: js-batch-dom-css"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-batch-dom-css.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "js-batch-dom-css"
    quote: "Avoid interleaving style writes with layout reads."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: []
---

## Group DOM writes before reads; prefer className over imperative inline style

**Impact: MEDIUM — Interleaving style writes with layout reads forces synchronous reflows. In React, the deeper fix is to drive style with state + `className`, not imperative `ref.style` mutations.**

### Incorrect — interleaved write/read forces multiple reflows

```typescript
element.style.width = '100px'
const w = element.offsetWidth           // forces reflow
element.style.height = '200px'
const h = element.offsetHeight          // forces reflow
```

### Correct (read-then-write, or write-then-read)

```typescript
// read phase
const r = element.getBoundingClientRect()
// write phase
element.style.width = '100px'
element.style.height = '200px'
```

### Best — declarative className in React

```tsx
function Box({ highlighted }: { highlighted: boolean }) {
  return <div className={highlighted ? 'highlighted-box' : ''}>Content</div>
}
```

Direct `ref.style` writes belong only in genuinely imperative cases (animations driven by raw `requestAnimationFrame`, native DOM API integration, focus/scroll positioning).

### When refs + raw style is justified

- Coordinating frame-level work with `requestAnimationFrame`.
- Pre-measuring DOM for portal/popover placement (read-only in an effect).
- Imperative third-party libraries that own a DOM region.

Even then: keep all writes batched, all reads batched, transitions between phases at frame boundaries.

Sources:
- [Vercel: js-batch-dom-css](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-batch-dom-css.md)
- [Layout thrashing reference (paul irish gist)](https://gist.github.com/paulirish/5d52fb081b3570c81e3a)
