---
title: Use ES2023 immutable array methods (.toSorted/.toReversed/.toSpliced/.with) for React state and props
impact: MEDIUM-HIGH
impactDescription: ".sort/.reverse/.splice mutate in place — they corrupt props/state arrays and cause stale-closure bugs in React. The ES2023 immutable variants return a new array. Fallback for older targets: spread + sort."
tags: [javascript, arrays, immutability, react, state, mutation]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-JS-012"
verification:
  type: lint
  rule_id: "ax/no-array-mutate-on-state"
  status: shipped
  notes: "Custom ESLint rule planned: flag arr.sort()/arr.reverse()/arr.splice() where arr is a prop or state-derived value."
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Chrome 110+, Safari 16+, Firefox 115+, Node 20+."
  completeness:
    status: complete
    amendments:
      - "State ES2023 support requirement"
      - "Fallback: [...arr].sort() / [...arr].reverse() / slice for older targets"
      - "Remind that .toSorted needs a comparator for numeric sort"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: js-tosorted-immutable"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-tosorted-immutable.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "js-tosorted-immutable"
    quote: ".sort() mutates the array in place, which can cause bugs with React state and props."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: []
---

## Use ES2023 immutable array methods for React state and props

**Impact: MEDIUM-HIGH — `.sort` / `.reverse` / `.splice` mutate. Mutating props is a React contract violation.**

### Incorrect — mutates the prop

```tsx
function UserList({ users }: { users: User[] }) {
  // ❌ .sort() mutates the parent's users array
  const sorted = useMemo(
    () => users.sort((a, b) => a.name.localeCompare(b.name)),
    [users],
  )
  return <ul>{sorted.map(renderUser)}</ul>
}
```

### Correct — immutable variant (ES2023)

```tsx
function UserList({ users }: { users: User[] }) {
  const sorted = useMemo(
    () => users.toSorted((a, b) => a.name.localeCompare(b.name)),
    [users],
  )
  return <ul>{sorted.map(renderUser)}</ul>
}
```

### The full ES2023 set

| Mutating | Immutable |
|---|---|
| `arr.sort(cmp)` | `arr.toSorted(cmp)` |
| `arr.reverse()` | `arr.toReversed()` |
| `arr.splice(start, count, ...items)` | `arr.toSpliced(start, count, ...items)` |
| `arr[i] = v` | `arr.with(i, v)` |

### Fallback for older targets (or polyfill)

```typescript
// Pre-ES2023
const sortedCopy = [...arr].sort((a, b) => a.value - b.value)
const reversedCopy = [...arr].reverse()
```

`[...arr]` makes a shallow copy, then mutating the copy is safe.

### Numeric sort needs a comparator

```typescript
const nums = [10, 2, 1]
nums.toSorted()              // ['1', '10', '2'] — string sort by default
nums.toSorted((a, b) => a - b)  // [1, 2, 10]
```

### Browser support

- Chrome 110+ (2023)
- Safari 16.4+ (2023)
- Firefox 115+ (2023)
- Node 20+ (2023)

Below those targets: use the spread fallback.

Sources:
- [Vercel: js-tosorted-immutable](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-tosorted-immutable.md)
