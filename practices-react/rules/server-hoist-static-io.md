---
title: Hoist truly static asset I/O to module scope; never hoist request-, user-, or tenant-scoped data
impact: HIGH
impactDescription: "Module-scope I/O runs once per process instance, not per request. Reduces latency for fonts/templates/bundled config. Hoisting request-scoped data is a correctness bug."
tags:
  - server
  - io
  - performance
  - static-assets
  - module-scope
applicable_to:
  - react
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-SERVER-006"
verification:
  type: review
  status: manual
  notes: "Reviewer checks: (a) hoisted asset is genuinely static across all requests, all users, all tenants; (b) Edge runtime constraints respected (no fs); (c) module-level fetch doesn't bypass intended Next cache/revalidation."
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
      - "Forbid hoisting request/user/tenant/secret-rotating data — correctness bug"
      - "Edge runtime constraints (no fs)"
      - "Module-level fetch can bypass Next cache/revalidation semantics"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: server-hoist-static-io"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-hoist-static-io.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "server-hoist-static-io"
    quote: "Module-level code runs once when the module is first imported, not on every request."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules: []
---

## Hoist truly static asset I/O to module scope; never hoist request-, user-, or tenant-scoped data

**Impact: HIGH — Module-scope I/O runs once per process instance, not per request. Reduces latency for fonts/templates/bundled config. Hoisting request-scoped data is a correctness bug.**

### When to hoist

Hoist I/O at module top level when the asset is:

- **Immutable** across all requests / users / tenants for the lifetime of the process.
- Examples: fonts for `ImageResponse`, logo bytes, bundled `template.html`, build-time generated constants, static config files.

### Correct — module-level promise; awaited per request

```typescript
// app/api/og/route.tsx
import { ImageResponse } from 'next/og'

// Module-level — runs once per process instance.
const fontDataPromise = fetch(
  new URL('./fonts/Inter.ttf', import.meta.url),
).then((r) => r.arrayBuffer())

const logoDataPromise = fetch(
  new URL('./images/logo.png', import.meta.url),
).then((r) => r.arrayBuffer())

export async function GET() {
  const [font, logo] = await Promise.all([fontDataPromise, logoDataPromise])
  return new ImageResponse(
    <div style={{ fontFamily: 'Inter' }}>
      <img src={logo as unknown as string} />
      Hello World
    </div>,
    { fonts: [{ name: 'Inter', data: new Uint8Array(font) }] },
  )
}
```

### Correct — sync `readFileSync` for tiny config

```typescript
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

const config = JSON.parse(
  readFileSync(join(process.cwd(), 'config/runtime.json'), 'utf-8'),
)

export async function processRequest(data: Data) {
  return render(data, config)
}
```

### Incorrect — fetches on every request

```typescript
export async function GET() {
  const fontData = await fetch(new URL('./fonts/Inter.ttf', import.meta.url))
    .then((r) => r.arrayBuffer())
  // ...
}
```

### Forbidden hoist — these are NOT static

| Data | Why not |
|---|---|
| Current user / session | Request-scoped |
| Tenant config | Per-tenant, mutates per workspace |
| Rotating secrets (JWT keys, API tokens) | Must re-read on rotation |
| Feature flags that change live | Hoisting freezes them at process boot |
| Request cookies / headers | Request-scoped |
| Database queries against user data | Request-scoped |

Hoisting any of these creates one of: tenant data leak, stale secrets, can't roll out flags, security incident.

### Edge runtime caveat

`node:fs` doesn't exist on Edge. For Edge-targeted routes, use `import.meta.url` + `fetch` patterns or import the asset directly via bundler (e.g. `import font from './Inter.ttf'`).

### Module-level fetch and Next cache

A module-level `fetch(...)` is NOT wrapped by Next's per-request cache/revalidation pipeline. If you intended `next: { revalidate: 60 }` semantics, module hoisting bypasses it. Either:
- Hoist only truly immutable assets that don't need revalidation.
- Keep request-scoped fetch in the route and use Next's caching options.
- Use Cache Components (`'use cache'`) for managed cached fetches.

### Deployment

| Runtime | Effect of module-level hoist |
|---|---|
| Long-running Node (incl. Vercel Fluid Compute) | Loaded once at boot; shared across requests. Max gain. |
| Traditional serverless cold starts | Re-loaded per cold start; reused per warm invocation. Partial gain. |

Sources:

- [Vercel: server-hoist-static-io](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-hoist-static-io.md)
