---
title: Cache deep stable property paths outside hot loops; length caching is mostly noise on modern engines
impact: LOW
impactDescription: "Reduces deep property lookups in hot loops when the path is stable across iterations. Modern V8/SpiderMonkey/JavaScriptCore already optimize many cases — apply only when profiling shows real cost."
tags: [javascript, loops, optimization, caching]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-JS-004"
verification:
  type: review
  status: manual
  notes: "Reviewer checks the hot loop for a stable property-access path (e.g. `obj.a.b.c`) re-read on every iteration; confirms it is cached into a local, and that profiling (not speculation) showed the lookup was a real cost."
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Modern engines inline-cache property access; the 'cache length' guidance is legacy from older engines."
  completeness: { status: complete, amendments: ["Downplay 'cache length' for modern engines", "Apply only to genuinely hot loops with deep stable paths", "Prefer clarity over micro-opt"] }
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: js-cache-property-access"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-cache-property-access.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "js-cache-property-access"
    quote: "Cache object property lookups in hot paths."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: []
---

## Cache deep stable property paths outside hot loops

**Impact: LOW — Reduces deep property lookups in hot loops when the path is stable.**

### When this rule applies

- Deeply nested access (`obj.a.b.c.d`) inside a loop body.
- The path is **stable** — none of `a`/`b`/`c`/`d` change within the loop.
- The loop is genuinely hot (>~10K iterations or per-frame).

In any other case, prefer clarity. Modern engines (V8 / SpiderMonkey / JavaScriptCore) inline-cache property access well; the optimization may not show up in real benchmarks.

### Correct — deep path hoisted

```typescript
const config = obj.config.settings.value
for (let i = 0; i < arr.length; i++) {
  process(config)
}
```

### Modern caveat — `length` caching

```typescript
// Old advice: cache length for "faster" loops.
// Modern engines: irrelevant. Prefer clarity:
for (const x of arr) { /* ... */ }
// or
for (let i = 0; i < arr.length; i++) { /* ... */ }
```

The classic `const len = arr.length` micro-optimization predates inline caching. On modern engines, it's noise. Reserve for proven hot-loop-mutation-of-length cases.

### Anti-pattern — hoisting unstable paths

```typescript
const items = obj.config.settings.items  // ← if obj.config changes during loop, this is stale
for (let i = 0; i < N; i++) {
  obj.config.settings.someFlag = true
  process(items)
}
```

If the path isn't stable, don't hoist. Stale reads are worse than slow loops.

Sources:
- [Vercel: js-cache-property-access](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-cache-property-access.md)
