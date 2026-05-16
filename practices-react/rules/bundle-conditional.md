---
title: Load feature modules only when the feature is activated
impact: HIGH
impactDescription: "Keeps optional feature code out of the initial bundle entirely. Module loads the moment the feature is genuinely needed (toggle on, settings open, data threshold crossed) — not before."
tags:
  - bundle
  - conditional-loading
  - lazy-loading
  - feature-gates
applicable_to:
  - react
  - nextjs
  - vite
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-BUNDLE-004"
verification:
  type: review
  status: manual
  notes: "Reviewer checks: (a) the feature is genuinely optional (not on the default path), (b) the gating condition reliably triggers when needed, (c) loading-state UI exists, (d) failure path handles import rejection."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
    notes: "Mechanic (dynamic import inside useEffect gated by condition) is correct."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Bare import() is framework-portable. Works in Next, Vite, Webpack, Rollup, esbuild."
  completeness:
    status: complete
    amendments:
      - "Removed `typeof window !== 'undefined'` inside useEffect — effects only run on the client, so the guard is dead code"
      - "Distinguished loading UI components (use React.lazy/next/dynamic) vs non-UI modules (use plain import())"
      - "Scoped to 'activation', distinct from 'intent prefetch' (sibling bundle-preload)"
  gap_check:
    status: complete
    note: "Distinct from bundle-preload: this rule loads ON activation, preload loads BEFORE activation based on intent."
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: bundle-conditional"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/bundle-conditional.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "bundle-conditional"
    quote: "Load large data or modules only when a feature is activated."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Remove typeof window from useEffect"
    - "Distinguish UI vs non-UI module loading"
    - "Scope clearly: activation, not prefetch"
sibling_rules:
  - bundle-dynamic-imports
  - bundle-preload
---

## Load feature modules only when the feature is activated

**Impact: HIGH — Keeps optional feature code out of the initial bundle entirely. Module loads the moment the feature is genuinely needed (toggle on, settings open, data threshold crossed) — not before.**

### Correct — non-UI module gated on activation

```tsx
'use client'
import { useEffect, useState } from 'react'

function AnimationPlayer({
  enabled,
  setEnabled,
}: {
  enabled: boolean
  setEnabled: (b: boolean) => void
}) {
  const [frames, setFrames] = useState<Frame[] | null>(null)

  useEffect(() => {
    if (!enabled || frames) return
    import('./animation-frames.js')
      .then((m) => setFrames(m.frames))
      .catch(() => setEnabled(false))
  }, [enabled, frames, setEnabled])

  if (!enabled) return null
  if (!frames) return <FrameLoadingSkeleton />
  return <Canvas frames={frames} />
}
```

### Correct — UI component gated on activation

```tsx
import { lazy, Suspense } from 'react'

const SettingsDrawer = lazy(() => import('./settings-drawer'))

function App() {
  const [open, setOpen] = useState(false)
  return (
    <>
      <button onClick={() => setOpen(true)}>Settings</button>
      {open && (
        <Suspense fallback={<DrawerSkeleton />}>
          <SettingsDrawer onClose={() => setOpen(false)} />
        </Suspense>
      )}
    </>
  )
}
```

### Why not `typeof window !== 'undefined'`?

`useEffect` runs only on the client. The guard is dead code inside an effect. The guard IS legitimate at module-top-level when you import a browser-only module unconditionally — that's a different case, covered indirectly by next/dynamic's `ssr: false`.

### When this rule applies

- Feature is opt-in (admin tools, advanced editor mode, debug overlays).
- Module is non-trivial (≥10–20 KB minified). Below that, the dynamic-import overhead may exceed the savings.
- Module use is reliably correlated with the activation gate (no flicker between gate states).

### Choosing UI vs non-UI

| Module kind | Pattern |
|---|---|
| React component to render | `React.lazy` + Suspense (portable) or `next/dynamic` |
| Data, processor, utility lib | bare `import('./module')` inside effect/handler |
| Heavy WASM / worker / shader | bare `import()` of the bootstrap module |

Sources:

- [Vercel: bundle-conditional](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/bundle-conditional.md)
