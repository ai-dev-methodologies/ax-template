---
title: Don't break RSC prop reference-dedup with sort/filter/map at the Server→Client boundary
impact: LOW
impactDescription: "RSC→Client serialization dedupes payloads by object reference. .toSorted/.filter/.map create new references and force re-serialization. For large duplicated payloads, pass canonical refs and transform in the client."
tags:
  - server
  - rsc
  - serialization
  - props
  - optimization
applicable_to:
  - react
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-SERVER-005"
verification:
  type: review
  status: manual
  notes: "Reviewer flags Client Component props where the same data is passed twice in different shapes (e.g. items + sortedItems). Confirms transformation can move client-side without harming the page."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
  completeness:
    status: complete
    amendments:
      - "Scoped to LARGE duplicated props — small ones aren't worth the rule"
      - "Warned that client transform increases client JS/work — not free"
      - "Reference-sharing primitive depends on RSC implementation details"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: server-dedup-props"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-dedup-props.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "server-dedup-props"
    quote: "RSC→client serialization deduplicates by object reference, not value. Same reference = serialized once; new reference = serialized again."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - server-serialization
---

## Don't break RSC prop reference-dedup with sort/filter/map at the Server→Client boundary

**Impact: LOW — RSC→Client serialization dedupes payloads by object reference. `.toSorted` / `.filter` / `.map` create new references and force re-serialization. For large duplicated payloads, pass canonical refs and transform in the client.**

### When this rule pays off

Only when you're passing the same data in two different shapes to a Client Component. For small arrays (< ~100 primitives, < ~20 small objects), the bytes saved are negligible — skip the rule.

### Operations that BREAK reference dedup

- Arrays: `.toSorted()`, `.filter()`, `.map()`, `.slice()`, spread `[...arr]`
- Objects: spread `{...obj}`, `Object.assign()`, `structuredClone()`, `JSON.parse(JSON.stringify())`

Each of these returns a new reference even if the values are identical.

### Incorrect — sends 6 strings for a 3-string array

```tsx
// RSC
<ClientList usernames={usernames} sorted={usernames.toSorted()} />
```

### Correct — pass once, transform client-side

```tsx
// RSC
<ClientList usernames={usernames} />
```

```tsx
// Client
'use client'
import { useMemo } from 'react'

export default function ClientList({ usernames }: { usernames: string[] }) {
  const sorted = useMemo(() => usernames.slice().sort(), [usernames])
  // render
}
```

### Type matters for impact

| Prop shape | Dedup impact |
|---|---|
| `string[]` / `number[]` / `boolean[]` | HIGH — primitives fully duplicated |
| `object[]` | LOW — array structure duplicates, but nested objects still share refs |
| Single deeply-nested object | LOW — already a single reference at the top |

### Caveat — client transform isn't free

Moving the sort/filter/map to the client costs client JS and client CPU. If the data is small or the transformation is cheap, the savings of fewer bytes don't beat the cost of running the transformation on potentially thousands of devices.

Use this rule when:
- The array is large (hundreds of items).
- The transformation runs frequently on the client anyway (you'd `useMemo` it regardless).
- Network egress / RSC payload weight is the bottleneck.

### Exception — when to derive on the server anyway

- Transformation is **expensive** (sort a million items) — do once on server.
- Client **only needs the derived shape** — original is wasted bytes.
- Server runs the same transformation multiple times per session (cache it on server).

Sources:

- [Vercel: server-dedup-props](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-dedup-props.md)
