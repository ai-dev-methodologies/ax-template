---
title: Hoist static RegExp to module scope; for prop-dependent regex use useMemo; beware /g lastIndex
impact: LOW-MEDIUM
impactDescription: "new RegExp() inside render allocates and recompiles. Hoist if static; memoize if pattern depends on props. Global /g and sticky /y regexes have mutable lastIndex state — don't share across calls."
tags: [javascript, regexp, optimization, memoization]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-JS-010"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Static patterns → hoist; dynamic patterns from props → useMemo"
      - "Reset lastIndex on /g and /y if shared across calls (or avoid sharing)"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: js-hoist-regexp"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-hoist-regexp.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "js-hoist-regexp"
    quote: "Don't create RegExp inside render."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: []
---

## Hoist static RegExp; for dynamic patterns use useMemo; beware /g lastIndex

**Impact: LOW-MEDIUM — Don't allocate a new RegExp per render.**

### Correct — static pattern hoisted

```tsx
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function isEmail(s: string) {
  return EMAIL_RE.test(s)
}
```

### Correct — dynamic pattern memoized

```tsx
function Highlighter({ text, query }: Props) {
  const re = useMemo(
    () => new RegExp(`(${escapeRegex(query)})`, 'gi'),
    [query],
  )
  // ...
}
```

### Incorrect — new RegExp on every render

```tsx
function Highlighter({ text, query }: Props) {
  const re = new RegExp(`(${query})`, 'gi')   // recompiled every render + injection-prone
  // ...
}
```

Bonus bug: building a regex from untrusted `query` without escaping makes it user-controlled. Always `escapeRegex` the parts.

### The `/g` / `/y` lastIndex footgun

Global and sticky regex instances carry mutable `lastIndex` state:

```typescript
const RE = /foo/g
RE.test('foo')   // true, lastIndex = 3
RE.test('foo')   // false, lastIndex was 3, no match from there → reset to 0
RE.test('foo')   // true again, lastIndex = 3
```

Either:
- Don't share a `/g` regex across calls.
- Use `String.prototype.matchAll` (returns iterator, doesn't mutate the regex).
- Reset before use: `RE.lastIndex = 0`.

Sources:
- [Vercel: js-hoist-regexp](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-hoist-regexp.md)
