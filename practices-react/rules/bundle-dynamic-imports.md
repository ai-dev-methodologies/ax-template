---
title: Lazy-load heavy client-only components via React.lazy/Suspense or next/dynamic
impact: HIGH
impactDescription: "Reduces initial JS payload and improves TTI. May improve LCP only when the deferred code is not on the LCP critical path. Use for below-the-fold or interaction-gated client components."
tags:
  - bundle
  - dynamic-import
  - code-splitting
  - react-lazy
  - next-dynamic
applicable_to:
  - react
  - nextjs
  - vite
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-BUNDLE-002"
verification:
  type: review
  status: manual
  notes: "Reviewer checks: (a) the component is genuinely heavy and off the initial render path, (b) lazy declaration at module level (not inside another component), (c) Suspense fallback present, (d) ssr:false used only for Client Components."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified-with-framing
    last_verified: "2026-05-16"
    notes: "Mechanic correct; Vercel rule too Next-specific. React.lazy + Suspense works for vanilla React/Vite."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Both next/dynamic and React.lazy work in Next.js 16 App Router. Server Components are already auto-code-split — lazy applies to Client Components."
  completeness:
    status: complete
    amendments:
      - "Reframed: 'lazy-load heavy client-only or below-the-fold UI'"
      - "Added React.lazy + Suspense as the generic React option"
      - "Soft impact wording: TTI gains reliable, LCP gains only when deferred code isn't on LCP path"
      - "Clarified ssr:false is Client Component only"
      - "Noted Server Components are auto-code-split — lazy is for Client Components"
  gap_check:
    status: complete
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: bundle-dynamic-imports"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/bundle-dynamic-imports.md"
    role: seed
  - id: nextjs-lazy-loading
    title: "Next.js 16 — Lazy Loading guide"
    url: "https://nextjs.org/docs/app/guides/lazy-loading"
    role: canonical-nextjs
  - id: react-19-lazy
    title: "React 19 — lazy() reference"
    url: "https://react.dev/reference/react/lazy"
    role: canonical-react
evidence:
  - upstream_id: vercel-react-best-practices
    section: "bundle-dynamic-imports"
    quote: "Use next/dynamic to lazy-load large components not needed on initial render."
  - source_type: external
    citation: "Next.js 16 docs — next/dynamic is a composite of React.lazy() and Suspense"
    url: "https://nextjs.org/docs/app/guides/lazy-loading"
  - source_type: external
    citation: "React 19 docs — Do NOT declare lazy components inside other components (state reset on re-renders); declare at module top level"
    url: "https://react.dev/reference/react/lazy"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Reframe as portable (React.lazy generic, next/dynamic Next-specific)"
    - "Soften LCP impact claim"
    - "Note Server Components auto-code-split"
sibling_rules:
  - bundle-defer-third-party
  - bundle-conditional
  - bundle-preload
---

## Lazy-load heavy client-only components via React.lazy/Suspense or next/dynamic

**Impact: HIGH — Reduces initial JS payload and improves TTI. May improve LCP only when the deferred code is not on the LCP critical path. Use for below-the-fold or interaction-gated client components.**

### Scope

This rule applies to **Client Components** that are heavy AND not needed on the initial render. Server Components are already auto-code-split by Next.js — lazy patterns don't apply there.

### Correct — React.lazy + Suspense (portable across React/Vite/Next)

```tsx
import { lazy, Suspense } from 'react'

// Module-top-level. Never inside another component (resets state on re-renders).
const MonacoEditor = lazy(() =>
  import('./monaco-editor').then((m) => ({ default: m.MonacoEditor })),
)

function CodePanel({ code }: { code: string }) {
  return (
    <Suspense fallback={<EditorSkeleton />}>
      <MonacoEditor value={code} />
    </Suspense>
  )
}
```

### Correct — next/dynamic (Next.js specific, composite of React.lazy + Suspense)

```tsx
import dynamic from 'next/dynamic'

const MonacoEditor = dynamic(
  () => import('./monaco-editor').then((m) => m.MonacoEditor),
  { ssr: false, loading: () => <EditorSkeleton /> },
)
// ssr:false only works for Client Components.
```

### Incorrect — static import in initial bundle

```tsx
import { MonacoEditor } from './monaco-editor' // ~300KB ships in main chunk

function CodePanel({ code }: { code: string }) {
  return <MonacoEditor value={code} />
}
```

### Anti-patterns

- Declaring `lazy()` inside a component body — React docs warn this causes state reset on re-renders. Always module top-level.
- Using `ssr: false` in a Server Component — Next will error.
- Lazy-loading a component that's actually on the LCP critical path — defers the visible content. Profile first.

### Choosing between React.lazy and next/dynamic

| | React.lazy | next/dynamic |
|---|---|---|
| Works in | React 18+ (any framework) | Next.js only |
| Loading state | Suspense fallback | `loading:` option (or Suspense) |
| SSR control | none (always prerendered) | `ssr: false` for Client Components |
| Named exports | needs `.then(m => ({ default: m.X }))` | `.then(m => m.X)` directly |
| Server Components | client-only | client-only (`ssr: false` only) |

Sources:

- [Vercel: bundle-dynamic-imports](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/bundle-dynamic-imports.md)
- [Next.js 16 — Lazy Loading](https://nextjs.org/docs/app/guides/lazy-loading)
- [React 19 — lazy()](https://react.dev/reference/react/lazy)
