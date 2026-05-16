---
title: Defer non-critical third-party SDK init or script loading until after hydration
impact: MEDIUM
impactDescription: "Removes analytics/logging/error-tracking code from the initial render-blocking path. Prefer official integration APIs (next/script, vendor-recommended loader) when available; dynamic import() of SDK modules for general libraries."
tags:
  - bundle
  - third-party
  - analytics
  - defer
  - scripts
applicable_to:
  - react
  - nextjs
  - vite
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-BUNDLE-003"
verification:
  type: review
  status: manual
  notes: "Reviewer checks: (a) the third-party isn't needed for initial render, (b) preferred path is the framework's official integration API (next/script, etc.) if one exists, (c) component-level dynamic() only used when actually rendering a component."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified-with-nuance
    last_verified: "2026-05-16"
    notes: "Directionally right. Vercel rule conflates 'deferring SDK initialization' with 'wrapping a provider component in dynamic()' — the right pattern depends on whether you're rendering a component or just loading a library."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Next.js has dedicated script-loading primitives (next/script with strategy options). Most vendors also have framework-specific integration packages."
  completeness:
    status: complete
    amendments:
      - "Distinguish script loading vs SDK module import vs provider component"
      - "Prefer official integration APIs (next/script, vendor packages)"
      - "Use dynamic import() for SDK modules — not next/dynamic, which is for components"
      - "Removed 'loads after hydration' overclaim — timing depends on implementation"
  gap_check:
    status: complete
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: bundle-defer-third-party"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/bundle-defer-third-party.md"
    role: seed
  - id: nextjs-lazy-loading
    title: "Next.js 16 — Lazy Loading guide"
    url: "https://nextjs.org/docs/app/guides/lazy-loading"
    role: canonical-nextjs
evidence:
  - upstream_id: vercel-react-best-practices
    section: "bundle-defer-third-party"
    quote: "Analytics, logging, and error tracking don't block user interaction. Load them after hydration."
  - source_type: external
    citation: "Next.js 16 — Lazy Loading: external libraries can be loaded on demand using import() function; pattern of dynamic import inside event handlers / effects"
    url: "https://nextjs.org/docs/app/guides/lazy-loading"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Distinguish script vs module vs provider"
    - "Prefer official integration APIs"
    - "Use dynamic import() for SDK modules"
sibling_rules:
  - bundle-dynamic-imports
---

## Defer non-critical third-party SDK init or script loading until after hydration

**Impact: MEDIUM — Removes analytics/logging/error-tracking code from the initial render-blocking path. Prefer official integration APIs (`next/script`, vendor-recommended loader) when available; dynamic `import()` of SDK modules for general libraries.**

### Three distinct shapes — pick the right one

1. **Loading a `<script>` tag** (Google Analytics, Tag Manager, vendor pixels) → use the framework's script primitive.
2. **Importing an SDK module** (Sentry, PostHog, Mixpanel client library) → dynamic `import()` inside an effect or event handler.
3. **Rendering a vendor's React provider component** (`<Analytics />`) → `next/dynamic` (or `React.lazy` + Suspense).

### Pattern 1 — script tag with framework primitive

```tsx
// app/layout.tsx (Next.js)
import Script from 'next/script'

export default function RootLayout({ children }) {
  return (
    <html>
      <body>
        {children}
        <Script
          src="https://example.com/analytics.js"
          strategy="afterInteractive"
        />
      </body>
    </html>
  )
}
```

### Pattern 2 — dynamic import() of SDK module

```tsx
'use client'
import { useEffect } from 'react'

function AnalyticsInit() {
  useEffect(() => {
    void (async () => {
      const { init, trackPageview } = await import('@vendor/analytics-sdk')
      init({ token: process.env.NEXT_PUBLIC_VENDOR_TOKEN })
      trackPageview()
    })()
  }, [])
  return null
}
```

### Pattern 3 — vendor provider component, deferred render

```tsx
import dynamic from 'next/dynamic'

const Analytics = dynamic(
  () => import('@vercel/analytics/react').then((m) => m.Analytics),
  { ssr: false },
)

export default function RootLayout({ children }) {
  return (
    <html>
      <body>
        {children}
        <Analytics />
      </body>
    </html>
  )
}
```

### Incorrect — static import in root layout for non-critical third-party

```tsx
import { Analytics } from '@vercel/analytics/react'

export default function RootLayout({ children }) {
  return (
    <html>
      <body>
        {children}
        <Analytics />   {/* In the initial bundle, regardless of need */}
      </body>
    </html>
  )
}
```

### Anti-patterns

- Wrapping a non-component import in `next/dynamic`. Use plain `import()` for SDK modules.
- Using `ssr: false` to hide a real SSR bug in the third-party — fix the root cause first.
- Loading 5+ analytics scripts on one page — each adds connection cost; prefer one server-side analytics gateway if possible.

Sources:

- [Vercel: bundle-defer-third-party](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/bundle-defer-third-party.md)
- [Next.js 16 — Lazy Loading](https://nextjs.org/docs/app/guides/lazy-loading)
