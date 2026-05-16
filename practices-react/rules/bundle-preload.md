---
title: Prefetch heavy modules on strong user-intent signals (hover, focus, viewport, likely next step)
impact: MEDIUM
impactDescription: "Reduces perceived latency by spending bandwidth/CPU EARLY based on confident-intent signals, before the user clicks. Latency tradeoff, not a bundle-size win."
tags:
  - bundle
  - prefetch
  - preload
  - user-intent
  - hover
applicable_to:
  - react
  - nextjs
  - vite
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-BUNDLE-005"
verification:
  type: review
  status: manual
  notes: "Reviewer checks: (a) the intent signal is strong (hover, focus, click likely-next-page) — not 'every user'; (b) no preload on initial page load for all users (that's a static import); (c) cleanup not required (browser/bundler dedupes preloaded modules)."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
    notes: "Event-driven import() warms the bundle. Bundlers/browsers dedupe so subsequent import() returns the same promise."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Works with all modern bundlers. Modern alternative: <link rel='modulepreload'> from server response — not in this rule's scope."
  completeness:
    status: complete
    amendments:
      - "Removed unnecessary `typeof window` guard from client-only event handlers"
      - "Added 'preload only on strong intent' caution against bandwidth waste"
      - "Framed as latency tradeoff, not bundle-size reduction"
  gap_check:
    status: complete
    note: "Distinct from bundle-conditional (which loads ON activation, not before)."
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: bundle-preload"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/bundle-preload.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "bundle-preload"
    quote: "Preload heavy bundles before they're needed to reduce perceived latency."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Strong-intent only"
    - "Latency tradeoff framing"
    - "Remove redundant typeof window"
sibling_rules:
  - bundle-conditional
  - bundle-dynamic-imports
---

## Prefetch heavy modules on strong user-intent signals (hover, focus, viewport, likely next step)

**Impact: MEDIUM — Reduces perceived latency by spending bandwidth/CPU EARLY based on confident-intent signals, before the user clicks. Latency tradeoff, not a bundle-size win.**

### Correct — hover/focus prefetch on a button

```tsx
'use client'

function EditorButton({ onClick }: { onClick: () => void }) {
  const preload = () => {
    void import('./monaco-editor')
  }
  return (
    <button onMouseEnter={preload} onFocus={preload} onClick={onClick}>
      Open Editor
    </button>
  )
}
```

### Correct — preload behind a feature flag, scoped to a provider

```tsx
'use client'
import { useEffect } from 'react'

function FlagsProvider({ children, flags }: Props) {
  useEffect(() => {
    if (!flags.editorEnabled) return
    void import('./monaco-editor').then((m) => m.init())
  }, [flags.editorEnabled])

  return <FlagsContext.Provider value={flags}>{children}</FlagsContext.Provider>
}
```

### Incorrect — preload for all users on every page load

```tsx
function EveryPage() {
  useEffect(() => {
    // BAD: spends bandwidth even for users who will never open the editor.
    void import('./monaco-editor')
  }, [])
  return null
}
```

If a module is going to load for every user on every page, it should be a static import (i.e. it's not really a lazy-load candidate).

### Strong intent signals — pick from

- `onMouseEnter` / `onFocus` on a button or link
- IntersectionObserver triggering when an "Open X" CTA enters the viewport
- Routing hints: user is on `/dashboard`, the only meaningful next click is `/dashboard/editor` → prefetch its bundle when dashboard renders
- A feature flag that is provably correlated with feature use within the session

### Weak signals — avoid

- Page load itself (not intent — that's a static import in disguise)
- Hover on a generic page area without a specific CTA
- "User is logged in" (not a feature-use signal)

### Why no `typeof window` guard in client handlers

`onMouseEnter` / `onFocus` event handlers only execute on the client. Likewise `useEffect` runs only on the client. The guard is dead code in these positions. Keep guards only at module top level when importing a browser-only module unconditionally.

### Latency tradeoff, not bundle savings

Preload doesn't reduce the bundle. It changes WHEN the bytes load. If your hit-rate (preload → actual use) is low, you've paid for unused work. Profile in real traffic before preloading aggressively.

Sources:

- [Vercel: bundle-preload](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/bundle-preload.md)
