---
title: Use react-dom resource-hint APIs in Server Components/layouts for critical resources; avoid hinting everything
impact: HIGH
impactDescription: "Server-side resource hints arrive in the HTML before the client even gets the document. preconnect/preload for critical resources; prefetchDNS for speculative; preinit for stylesheets/scripts that must execute early. Overuse harms the very metric you're trying to optimize."
tags: [rendering, preload, preconnect, prefetch, resource-hints, react-19]
applicable_to: [react, nextjs]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RENDERING-010"
verification:
  type: review
  status: manual
  notes: "Reviewer checks: (a) hints are in Server Components or layout/root context, (b) preload/preinit limited to critical above-the-fold resources, (c) prefetchDNS/preconnect limited to origins actually needed soon, (d) framework primitives (Next.js metadata) used where stronger."
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
      - "Required Server Component / layout / top-level intent"
      - "Warned against hinting every route/asset (overuse penalty)"
      - "Distinguished preload/preinit (active) from prefetchDNS/preconnect (speculative)"
      - "Preferred framework primitives where stronger"
  gap_check:
    status: complete
    note: "Cross-link to bundle-preload (which is component-level lazy preload, not resource-hint API)."
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: rendering-resource-hints"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-resource-hints.md"
    role: seed
  - id: react-dom-resource-hints
    title: "React DOM — Resource Preloading APIs"
    url: "https://react.dev/reference/react-dom"
    role: canonical-react
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rendering-resource-hints"
    quote: "These are especially useful in server components to start loading resources before the client even receives the HTML."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - bundle-preload
---

## Use react-dom resource-hint APIs in Server Components/layouts for critical resources

**Impact: HIGH — Server-rendered hints arrive in the HTML before the client receives the document. But overuse causes the very latency penalty you're trying to avoid.**

### API surface (React 19 stable)

| API | Side effect | Use case |
|---|---|---|
| `prefetchDNS(href)` | DNS resolve | speculative origin you'll connect to later |
| `preconnect(href)` | DNS + TCP + TLS | API/CDN you'll fetch from soon |
| `preload(href, { as, type, crossOrigin })` | fetch resource | critical font/style/script needed on current page |
| `preloadModule(href, { as })` | fetch ESM module | likely-next route's JS module |
| `preinit(href, { as })` | fetch + execute stylesheet/script | critical CSS that must apply before paint |
| `preinitModule(href)` | fetch + execute ESM | ESM that must run before render |

### Correct — Server Component / root layout

```tsx
import { preconnect, prefetchDNS, preload, preinit } from 'react-dom'

export default function RootLayout({ children }: { children: ReactNode }) {
  // Speculative origins — DNS only
  prefetchDNS('https://analytics.example.com')

  // Origins we'll hit on this page — full handshake
  preconnect('https://api.example.com')

  // Critical above-the-fold font
  preload('/fonts/inter.woff2', {
    as: 'font',
    type: 'font/woff2',
    crossOrigin: 'anonymous',
  })

  // Critical CSS that must apply before paint
  preinit('/styles/critical.css', { as: 'style' })

  return (
    <html>
      <body>{children}</body>
    </html>
  )
}
```

### Correct — speculative preload on user intent

```tsx
'use client'
import { preloadModule } from 'react-dom'

function Nav() {
  return (
    <a
      href="/dashboard"
      onMouseEnter={() => preloadModule('/dashboard.js', { as: 'script' })}
    >
      Dashboard
    </a>
  )
}
```

### Overuse penalty

Hints compete for bandwidth on a constrained connection. Excessive `preload` / `preinit` can:
- Delay the critical path resources you actually need.
- Make Lighthouse complain about "wasted bytes".
- On mobile, exhaust the connection pool.

Rules of thumb:
- `prefetchDNS` is cheapest — still don't hint every origin in the codebase.
- `preconnect` for ≤ 5 origins per page.
- `preload` / `preinit` for the ABOVE-the-fold critical path only.
- `preloadModule` only on strong intent signals.

### Framework primitives

Next.js has stronger primitives in some cases:
- `next/font` handles font preloading + display swap natively.
- `Metadata.preconnect` / metadata API may emit hints from a route's metadata export.
- `<Link prefetch>` covers route-level prefetch.

Use these where applicable; reach for the react-dom API for cases the framework doesn't cover.

### Sibling rule

`bundle-preload` is component-level lazy preload (dynamic `import()` on hover). `rendering-resource-hints` (this rule) is the lower-level HTML resource-hint API. Different abstraction layer, different audit.

Sources:
- [Vercel: rendering-resource-hints](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-resource-hints.md)
- [React DOM — Resource Preloading APIs](https://react.dev/reference/react-dom)
