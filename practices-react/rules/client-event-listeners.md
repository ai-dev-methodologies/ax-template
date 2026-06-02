---
title: One global event listener per (target, event, options); many subscribers
impact: LOW
impactDescription: "Avoids N listeners for N hook instances. Single listener fan-outs to a Set of callbacks. Implementation can be plain useEffect + module registry, useSyncExternalStore, or a subscription library primitive — SWR is not required."
tags:
  - client
  - event-listeners
  - subscription
  - performance
applicable_to:
  - react
  - nextjs
  - vite
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-CLIENT-002"
verification:
  type: review
  status: manual
  notes: "Reviewer flags useEffect that addEventListener on a global target (window/document) from a hook used in multiple components. Confirms the hook routes through a singleton subscribe registry."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified-with-vendor-decouple
    last_verified: "2026-05-16"
    notes: "Pattern is correct; useSWRSubscription is one implementation, not the requirement."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
  completeness:
    status: complete
    amendments:
      - "Decoupled from SWR; mentioned plain useEffect, useSyncExternalStore, library subscription as options"
      - "Named the primitive: 'singleton listener + subscriber registry'"
      - "Added cleanup-on-zero-subscribers detail"
      - "Keyed by (target, event, options) — passive listeners need a separate registry from non-passive"
  gap_check:
    status: complete
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: client-event-listeners"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/client-event-listeners.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "client-event-listeners"
    quote: "Use useSWRSubscription() to share global event listeners across component instances."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Pattern right, SWR requirement unnecessary"
    - "Name singleton-listener-plus-registry primitive"
    - "Add zero-subscriber cleanup"
sibling_rules:
  - client-swr-dedup
  - advanced-event-handler-refs
---

## One global event listener per (target, event, options); many subscribers

**Impact: LOW — Avoids N listeners for N hook instances. Single listener fan-outs to a Set of callbacks. Implementation can be plain useEffect + module registry, useSyncExternalStore, or a subscription library primitive.**

### The mechanic

A hook used in many components shouldn't register a new global listener per instance. Instead:

1. Keep a **module-level registry** keyed by event identity (e.g. `'window:keydown'`).
2. The hook just **adds itself** to the registry on mount, **removes** on unmount.
3. The **first** subscriber installs the underlying `addEventListener`.
4. The **last** unsubscriber removes it (cleanup on zero-count).

### Correct — plain useEffect + module-level registry

```tsx
type Cb = (e: KeyboardEvent) => void
const cbs = new Set<Cb>()
let attached = false

function attachIfNeeded() {
  if (attached) return
  attached = true
  window.addEventListener('keydown', dispatch)
}
function detachIfNeeded() {
  if (cbs.size > 0) return
  attached = false
  window.removeEventListener('keydown', dispatch)
}
function dispatch(e: KeyboardEvent) {
  cbs.forEach((cb) => cb(e))
}

export function useGlobalKeydown(callback: Cb) {
  useEffect(() => {
    cbs.add(callback)
    attachIfNeeded()
    return () => {
      cbs.delete(callback)
      detachIfNeeded()
    }
  }, [callback])
}
```

Now `useGlobalKeydown` can be used in any number of components without N listeners.

### Correct — useSyncExternalStore (when you want the value, not just a callback)

```tsx
import { useSyncExternalStore } from 'react'

const listeners = new Set<() => void>()
let lastKey: string | null = null

function getSnapshot() { return lastKey }
// Named handler → removable (an inline closure could never be removeEventListener'd).
function handleKeydown(e: KeyboardEvent) {
  lastKey = e.key
  listeners.forEach((fn) => fn())
}
function subscribe(notify: () => void) {
  if (listeners.size === 0) {
    window.addEventListener('keydown', handleKeydown)   // first subscriber installs
  }
  listeners.add(notify)
  return () => {
    listeners.delete(notify)
    if (listeners.size === 0) {
      window.removeEventListener('keydown', handleKeydown)   // last unsubscriber removes (no leak)
    }
  }
}

export function useLastPressedKey() {
  return useSyncExternalStore(subscribe, getSnapshot, () => null)
}
```

### Correct — library primitive (SWR's useSWRSubscription, observables, etc.)

Acceptable if the project already uses such a library. Don't add a new dependency just for this pattern.

### Incorrect — N instances = N listeners

```tsx
function useKeyboardShortcut(key: string, cb: () => void) {
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.metaKey && e.key === key) cb()
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [key, cb])
  // 10 components using this = 10 keydown listeners.
}
```

### Keying the registry

Use separate registries when the event subscription options differ (passive vs not, capture phase vs bubble). `(target, eventName, options)` is the natural key.

Sources:

- [Vercel: client-event-listeners](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/client-event-listeners.md)
