---
title: For numeric/falsy-tricky conditions use ternary or explicit boolean cast; `&&` is fine for real booleans
impact: LOW
impactDescription: "`0 && <X />` renders the literal '0'. Use `count > 0 ? <X /> : null` or `Boolean(count) && <X />`. Don't ban `&&` outright."
tags: [rendering, conditional, jsx, falsy-values, correctness]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RENDERING-008"
verification:
  type: lint
  rule_id: "ax/no-falsy-numeric-render"
  status: shipped
  notes: "Shipped + enabled: ax/no-falsy-numeric-render flags `numeric && <JSX>` patterns; safe `boolean && <JSX>` left alone; registered in the plugin and enforcing."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Don't ban && for real booleans — only for numeric/falsy-tricky conditions"
      - "Acceptable: Boolean(x) && <JSX>"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: rendering-conditional-render"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-conditional-render.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rendering-conditional-render"
    quote: "Use explicit ternary operators (? :) instead of && for conditional rendering when the condition can be 0, NaN, or other falsy values that render."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules: []
---

## For numeric/falsy-tricky conditions use ternary or explicit boolean cast; `&&` is fine for real booleans

**Impact: LOW — `0 && <X />` renders the literal "0". `NaN && <X />` renders "NaN". `'' && <X />` renders the empty string (visible nothing, but a stray text node). Use ternary or explicit boolean cast for these.**

### Incorrect — numeric condition

```tsx
function Badge({ count }: { count: number }) {
  return <div>{count && <span className="badge">{count}</span>}</div>
}
// count = 0 → renders <div>0</div>
```

### Correct — ternary

```tsx
function Badge({ count }: { count: number }) {
  return <div>{count > 0 ? <span className="badge">{count}</span> : null}</div>
}
```

### Correct — explicit boolean cast

```tsx
function Badge({ count }: { count: number }) {
  return <div>{Boolean(count) && <span className="badge">{count}</span>}</div>
}
```

### Don't ban `&&` for real booleans

`&&` is perfectly safe (and idiomatic) when the left side is a real boolean:

```tsx
// PERFECTLY FINE
function Toolbar({ canEdit }: { canEdit: boolean }) {
  return <div>{canEdit && <EditButton />}</div>
}

function List({ items }: { items: Item[] }) {
  return <ul>{items.length > 0 && items.map(...)}</ul>
}
```

### Rule of thumb

- Left side is a primitive `boolean` (literal, comparison result, ! coerced) → `&&` is safe.
- Left side might be `number`, `string`, `NaN`, `null`, `undefined`, or a value of unknown type → use ternary or `Boolean(...)`.

### TypeScript helps

A strict-typed boolean prop won't have the numeric trap. The trap shows up most often with:

```tsx
{user.unreadCount && <Dot />}             // number
{search.results.length && <Heading />}     // number
{maybeArray.length && maybeArray.map(...)} // number
```

All three are fixed by either `> 0` comparison or `Boolean()` cast.

Sources:
- [Vercel: rendering-conditional-render](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-conditional-render.md)
