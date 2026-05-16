---
title: Store event handlers in refs as a fallback when useEffectEvent is unavailable
impact: LOW
impactDescription: "Stable subscriptions (window/document/external listeners) that need the latest handler without re-subscribing. On React 19.2+ prefer useEffectEvent; this ref pattern is the fallback for older React or non-Effect subscription APIs."
tags:
  - advanced
  - hooks
  - refs
  - event-handlers
  - optimization
  - react
applicable_to:
  - react
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-ADVANCED-001"
verification:
  type: review
  status: manual
  notes: "Reviewer checks that the ref-backed handler isn't hiding a dependency that should re-trigger the Effect; checks React version to prefer useEffectEvent when ≥19.2."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
    notes: "Ref-backed handler still works for stable subscriptions."
  freshness:
    status: partially-stale
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "React 19.2 stabilizes useEffectEvent — Vercel rule's 'if you're on latest React' framing now means 19.2+. Re-position as fallback."
  completeness:
    status: complete
    amendments:
      - "Prefer useEffectEvent on React 19.2+; ref pattern is fallback"
      - "Add warning: do not hide dependencies that should re-trigger the Effect"
  gap_check:
    status: complete
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: react-best-practices (advanced-event-handler-refs)"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/advanced-event-handler-refs.md"
    role: seed
  - id: react-19-useeffectevent
    title: "React 19 — useEffectEvent (stable in 19.2)"
    url: "https://react.dev/reference/react/useEffectEvent"
    role: canonical-react
evidence:
  - upstream_id: vercel-react-best-practices
    section: "advanced-event-handler-refs"
    quote: "Store callbacks in refs when used in effects that shouldn't re-subscribe on callback changes."
  - source_type: external
    citation: "React 19 docs — useEffectEvent (the non-stable identity acts as a runtime assertion: if your code incorrectly depends on the function identity, you'll see the Effect re-running on every render)"
    url: "https://react.dev/reference/react/useEffectEvent"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Ref pattern still works"
    - "Should reposition as fallback to useEffectEvent on 19.2+"
    - "Warn against hiding deps"
sibling_rules:
  - advanced-use-latest
---

## Store event handlers in refs as a fallback when useEffectEvent is unavailable

**Impact: LOW — Stable subscriptions that need the latest handler without re-subscribing. On React 19.2+ prefer `useEffectEvent`; this ref pattern is the fallback for older React or non-Effect subscription APIs.**

> **Positioning note.** The seed source presents this as the primary pattern with `useEffectEvent` as alternative. As of React 19.2 the priority is reversed: prefer `useEffectEvent` (sibling rule `advanced-use-latest.md`), use this ref pattern only when not on 19.2+ or when subscribing via APIs that need a stable listener identity (e.g. third-party SDKs that compare listener identity).

**Incorrect — re-subscribes every render:**

```tsx
function useWindowEvent(event: string, handler: (e: Event) => void) {
  useEffect(() => {
    window.addEventListener(event, handler)
    return () => window.removeEventListener(event, handler)
  }, [event, handler])  // handler identity changes every render
}
```

**Fallback (pre-19.2) — ref-backed handler, stable subscription:**

```tsx
function useWindowEvent(event: string, handler: (e: Event) => void) {
  const handlerRef = useRef(handler)
  useEffect(() => {
    handlerRef.current = handler
  }, [handler])

  useEffect(() => {
    const listener = (e: Event) => handlerRef.current(e)
    window.addEventListener(event, listener)
    return () => window.removeEventListener(event, listener)
  }, [event])
}
```

**Preferred (React 19.2+) — useEffectEvent:**

```tsx
import { useEffectEvent } from 'react'

function useWindowEvent(event: string, handler: (e: Event) => void) {
  const onEvent = useEffectEvent(handler)
  useEffect(() => {
    window.addEventListener(event, onEvent)
    return () => window.removeEventListener(event, onEvent)
  }, [event])
}
```

### Anti-patterns

- Hiding a real dependency. If a value should cause the Effect to re-run, keep it as a dependency. Don't use refs (or Effect Events) to silence React's reactivity rules.
- Putting the ref-read inside render code instead of inside the listener — defeats the freshness purpose.

Sources:

- [Vercel: advanced-event-handler-refs](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/advanced-event-handler-refs.md)
- [React 19 — useEffectEvent](https://react.dev/reference/react/useEffectEvent)
