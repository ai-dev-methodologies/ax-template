---
title: A "use client" route file should stay thin — a route that grows past a line threshold likely belongs in a feature container
impact: MEDIUM
impactDescription: "A route file (app/**/page.tsx or layout.tsx) that accumulates form state, business logic, and inline UI instead of delegating to a @/features/<f> container becomes the thing every other change touches — it cannot be reused outside the route, cannot be unit-tested without the full routing/rendering harness, and every unrelated feature change risks touching the same file. Line count is a gameable proxy (a rule can be satisfied by splitting into equally-tangled helper files in the same directory), so this ships advisory (warn), not a hard block — it is a visible remediation signal, not a guarantee of decomposition quality."
tags: [architecture, routing, feature-layout, size-heuristic, eslint]
applicable_to: [react, nextjs]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-ROUTE-001"
verification:
  type: lint
  rule_id: "ax/no-god-route"
  status: shipped
  notes: "Shipped + enabled (advisory/warn by design): ax/no-god-route flags a \"use client\" file under app/**/page|layout whose physical line count exceeds a configurable threshold (default 100, via the rule's maxLines option). Only CLIENT route files are checked — a server route file (no \"use client\" directive) is not flagged, since server-side data-layer code is a different concern than client UI/business-logic bloat. Registered in the plugin and enforcing as warn; promotion to error is tracked separately (see practices-react/DECISIONS.md, BACKLOG P2-2) because line count alone cannot distinguish a genuinely tangled route from one that is merely verbose (e.g. a long but flat list of typed form fields)."
provenance: { pilot: false, pipeline_version: "2026-06-08", pipeline_steps: [phaseA_frontend_decomposition_design, phaseB_rule_authoring, phaseC_teeth_proof] }
audit:
  accuracy: { status: verified, last_verified: "2026-08-13" }
  freshness: { status: current, last_verified: "2026-08-13", next_review_by: "2026-11-11" }
  completeness: { status: complete, amendments: ["Catalog doc authored post-ship — rule and tests predate this file; see practices-react/eslint-plugin-ax/rules/no-god-route.js and tests/no-god-route.test.js.", "P2-87: documented the honest advisory/gameable-proxy limit explicitly rather than presenting the line threshold as a correctness guarantee."] }
  gap_check: { status: complete }
upstream:
  - id: nextjs-fetching-data
    title: "Next.js 16 — Fetching Data (Server Components vs Client Components)"
    url: "https://nextjs.org/docs/app/getting-started/fetching-data"
    role: seed
evidence:
  - upstream_id: nextjs-fetching-data
    section: "Fetching Data — Server Components / With the fetch API"
    quote: "To fetch data with the `fetch` API, turn your component into an asynchronous function, and await the `fetch` call."
    anchors: generic_principle_only
  - upstream_id: nextjs-fetching-data
    section: "Fetching Data — Client Components / Community libraries"
    quote: "You can use a community library like SWR or React Query to fetch data in Client Components. These libraries have their own semantics for caching, streaming, and other features."
    anchors: generic_principle_only
sibling_rules: [no-route-client-data-fetching, no-server-state-in-local-state]
---

## A "use client" route file should stay thin

**Impact: MEDIUM (advisory) — Line count is a gameable proxy for "this route is doing a feature container's job," not a correctness guarantee. It surfaces the smell visibly (warn) without breaking the build on a false positive.**

Next.js's own documented split is that **Server Components** are where `fetch`/database/ORM
calls belong (`await fetch(...)` inside an async component), while **Client Components**
that need data reach for a dedicated library (`use()`, SWR, React Query) rather than
hand-rolling the fetch. A `"use client"` route file (`app/**/page.tsx` or `layout.tsx`) that
instead grows a large body of inline form state, branching business logic, and JSX is doing
a feature container's job in the routing layer — the anchored generic principle is *fetching
and business logic have a documented home that is not the route file itself*; the specific
line threshold and "extract to `@/features/<f>`" convention below are this catalog's own
decomposition heuristic, not a Next.js requirement.

`ax/no-god-route` only checks **client** route files — a server route (no `"use client"`)
is not flagged, because Server Component data-layer code is a different concern from client
UI/business-logic bloat.

### Incorrect — a fat client route holding form state + business logic + inline UI

```tsx
// src/app/(authenticated)/dashboard/page.tsx
'use client'

export default function Page() {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  // ...100+ more lines of validation, submit handling, and inline JSX...
  return (
    <form onSubmit={handleSubmit}>
      {/* a whole feature's worth of markup lives here */}
    </form>
  )
}
```

Past the line threshold (default 100), `ax/no-god-route` warns: this route is doing the job
of a `@/features/<f>` container instead of delegating to one.

### Correct — the route delegates to a feature container

```tsx
// src/app/(authenticated)/dashboard/page.tsx
'use client'
import { DashboardForm } from '@/features/dashboard'

export default function Page() {
  return <DashboardForm />
}
```

The form state, validation, and business logic live in `@/features/dashboard`, where they
are unit-testable in isolation and reusable outside the route.

### What this rule does NOT flag

- **Server route files** (no `"use client"` directive) — their size is typically data-layer
  composition, not UI/business logic, and is out of scope for this rule.
- **Non-route files** — anything outside `app/**/page.tsx` / `layout.tsx` is unaffected.
- **A short client route that legitimately needs the space** — the threshold is
  configurable (`maxLines` option) per project.

### Honest limit

Line count cannot distinguish a genuinely tangled route (mixed concerns, hard to test) from
one that is merely verbose (e.g. a long but flat list of typed props or copy strings). A
route can dodge this warning by moving code into equally-tangled sibling files in the same
`app/` directory without ever touching `@/features/<f>` — the rule surfaces a signal for
human/TIER-2 review, it does not verify the decomposition actually happened. This is why the
rule ships `warn`, not `error`.

Reference: [Next.js 16 — Fetching Data](https://nextjs.org/docs/app/getting-started/fetching-data)

Reference: [practices-react/rules/no-route-client-data-fetching.md](no-route-client-data-fetching.md) — the sibling TIER-1 rule that blocks the most common source of route bloat (inline client data-fetching) outright.
