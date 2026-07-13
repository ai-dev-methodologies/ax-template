---
title: Initialize app-wide state once at module scope, not inside a component's useEffect
impact: LOW-MEDIUM
impactDescription: "Prevents duplicate initialization under React StrictMode dev double-mount and remount scenarios. App-wide state belongs at module scope, not in component lifecycle."
tags:
  - initialization
  - useEffect
  - app-startup
  - side-effects
applicable_to:
  - react
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-ADVANCED-002"
verification:
  type: review
  status: manual
  notes: "Reviewer checks: (a) app-wide init (load storage / check auth) is at module scope or guarded by `didInit`, NOT in component useEffect; (b) the guard is module-level not component-level."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
    notes: "useEffect([]) is unreliable under StrictMode dev double-mount; the didInit pattern is React-docs-canonical."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Pattern is stable across React 18/19; SSR/RSC contexts add the typeof-window guard but the principle holds."
  completeness:
    status: complete
    amendments:
      - "Added preferred module-init option (typeof window check outside any component)"
      - "Clarified scope: this is for app-wide one-time init, not per-component subscriptions / effects needing cleanup"
  gap_check:
    status: complete
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: react-best-practices (advanced-init-once)"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/advanced-init-once.md"
    role: seed
  - id: react-you-might-not-need-effect
    title: "React docs — You Might Not Need an Effect (initializing the application)"
    url: "https://react.dev/learn/you-might-not-need-an-effect"
    role: canonical-react
evidence:
  - upstream_id: vercel-react-best-practices
    section: "advanced-init-once"
    quote: "Do not put app-wide initialization that must run once per app load inside `useEffect([])` of a component."
  - source_type: external
    citation: "React docs — Initializing the application (canonical didInit pattern + module-init alternative)"
    url: "https://react.dev/learn/you-might-not-need-an-effect#initializing-the-application"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Directionally right"
    - "Should mention module-init option in addition to didInit guard"
    - "Scope clarification needed (app-wide init, not component lifecycle)"
sibling_rules: []
---

## Initialize app-wide state once at module scope, not inside a component's useEffect

**Impact: LOW-MEDIUM — Prevents duplicate initialization under React StrictMode dev double-mount and remount scenarios. App-wide state belongs at module scope, not component lifecycle.**

### Scope

This rule applies to **app-wide one-time init** that should run exactly once per app load:

- Loading state from `localStorage` / `sessionStorage`
- Reading & validating an auth token
- Connecting analytics SDKs
- Pre-warming a singleton client (only if it has no per-render config)

It does **not** apply to per-component subscriptions, cleanup-bearing effects, or per-mount setup — those legitimately belong in `useEffect`.

### Incorrect — runs twice in dev, re-runs on remount

```tsx
function App() {
  useEffect(() => {
    loadFromStorage()
    checkAuthToken()
  }, [])
}
```

React docs verbatim: "However, you'll quickly discover that it runs twice in development. This can cause issues — for example, maybe it invalidates the authentication token because the function wasn't designed to be called twice."

### Correct (preferred) — module-level init, browser-guarded

```tsx
if (typeof window !== 'undefined') {
  loadFromStorage()
  checkAuthToken()
}

function App() {
  // ...
}
```

Run once per app load, naturally. The `typeof window` guard prevents the init from executing during SSR.

> **Caveat from React docs:** "Code at the top level runs once when your component is imported — even if it doesn't end up being rendered. To avoid slowdown or surprising behavior when importing arbitrary components, don't overuse this pattern. Keep app-wide initialization logic to root component modules like `App.js` or in your application's entry point."

### Correct (acceptable) — didInit guard inside useEffect

If you must keep the init inside a component (e.g. it depends on a context value), use a module-level `didInit` flag:

```tsx
let didInit = false

function App() {
  useEffect(() => {
    if (didInit) return
    didInit = true
    loadFromStorage()
    checkAuthToken()
  }, [])
}
```

This survives StrictMode's intentional dev-only double-mount because `didInit` lives at module scope, not component scope.

Sources:

- [Vercel: advanced-init-once](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/advanced-init-once.md)
- [React docs — Initializing the application](https://react.dev/learn/you-might-not-need-an-effect#initializing-the-application)
