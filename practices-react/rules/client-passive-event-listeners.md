---
title: "Use passive listeners on touch/wheel events that do not need preventDefault"
impact: MEDIUM
impactDescription: "Tells the browser the listener will not call preventDefault, allowing it to scroll immediately instead of waiting for the listener to run. Modern Chrome already defaults document-level touchstart/touchmove to passive; the rule still matters for explicit listeners on custom scroll containers and for cross-browser clarity."
tags:
  - client
  - event-listeners
  - scrolling
  - performance
  - touch
  - wheel
applicable_to:
  - react
  - nextjs
  - vite
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-CLIENT-003"
verification:
  type: review
  status: manual
  notes: "Reviewer flags addEventListener('touchstart'|'touchmove'|'wheel', ...) without an options argument. If the listener calls preventDefault, explicit { passive: false } is required and reviewer confirms the use-case (custom swipe/zoom)."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
    notes: "passive:true means preventDefault is a no-op (the browser ignores it)."
  freshness:
    status: current-with-nuance
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Chrome 56+ (2017) made touchstart/touchmove on document/window default to passive. Custom scroll containers and other engines still need explicit options."
  completeness:
    status: complete
    amendments:
      - "Added Chrome default-passive nuance"
      - "Added explicit { passive: false } pattern for legitimate preventDefault use"
      - "Scoped to cancelable scroll/touch/wheel listeners"
  gap_check:
    status: complete
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: client-passive-event-listeners"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/client-passive-event-listeners.md"
    role: seed
  - id: mdn-addeventlistener-passive
    title: "MDN — EventTarget.addEventListener passive option"
    url: "https://developer.mozilla.org/en-US/docs/Web/API/EventTarget/addEventListener#passive"
    role: primitive-semantics
evidence:
  - upstream_id: vercel-react-best-practices
    section: "client-passive-event-listeners"
    quote: "Add { passive: true } to touch and wheel event listeners to enable immediate scrolling."
  - source_type: external
    citation: "MDN — EventTarget.addEventListener (passive option indicates the listener will never call preventDefault; calling it has no effect)"
    url: "https://developer.mozilla.org/en-US/docs/Web/API/EventTarget/addEventListener#passive"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Add Chrome default-passive nuance"
    - "Don't make passive sound universally required"
    - "Explicit passive:false when cancellation is intentional"
sibling_rules:
  - client-event-listeners
---

## Use `{ passive: true }` on touch/wheel listeners that do not need preventDefault

**Impact: MEDIUM — Tells the browser the listener will not call `preventDefault`, allowing it to scroll immediately instead of waiting for the listener to run. Modern Chrome already defaults document-level `touchstart`/`touchmove` to passive; the rule still matters for explicit listeners on custom scroll containers and for cross-browser clarity.**

### When passive helps

The browser must determine whether your listener will cancel the scroll. Without `passive`, it waits for the listener to run. With `passive: true`, the browser proceeds immediately.

Use passive when the listener is for:

- analytics / metrics
- logging
- updating UI state that doesn't cancel scroll
- any handler that genuinely doesn't call `preventDefault`

### Correct — passive listeners

```tsx
useEffect(() => {
  const onTouch = (e: TouchEvent) => trackTouch(e.touches[0].clientX)
  const onWheel = (e: WheelEvent) => trackScroll(e.deltaY)

  document.addEventListener('touchstart', onTouch, { passive: true })
  document.addEventListener('wheel', onWheel, { passive: true })

  return () => {
    document.removeEventListener('touchstart', onTouch)
    document.removeEventListener('wheel', onWheel)
  }
}, [])
```

### Correct — explicit `{ passive: false }` when you genuinely need preventDefault

```tsx
useEffect(() => {
  const onWheel = (e: WheelEvent) => {
    if (!shouldHandleZoom(e)) return
    e.preventDefault()  // would be a no-op under passive
    customZoom(e.deltaY)
  }
  document.addEventListener('wheel', onWheel, { passive: false })
  return () => document.removeEventListener('wheel', onWheel)
}, [])
```

`{ passive: false }` is required for custom swipe/zoom/pull-to-refresh gestures or any handler that may cancel the scroll/gesture.

### Default-passive caveat (Chrome 56+, since 2017)

Chrome made `touchstart` / `touchmove` listeners on `document` and `window` default to `passive: true`. Firefox and Safari followed. **For these targets, an unset option already implies passive.** The rule still has teeth for:

- Custom scroll containers (`element.addEventListener('touchstart', ...)`).
- `wheel` listeners (not auto-passive on document in most engines).
- Cross-engine consistency — older browsers and embedded webviews may differ.

Explicit options remove ambiguity at no cost.

### Anti-pattern

```tsx
useEffect(() => {
  const onTouch = (e: TouchEvent) => {
    if (someCondition(e)) e.preventDefault()   // silently ignored if browser made this passive
    track(e)
  }
  document.addEventListener('touchstart', onTouch)
}, [])
```

Either you genuinely want to cancel and need `{ passive: false }`, or you don't and should declare `{ passive: true }` explicitly.

Sources:

- [Vercel: client-passive-event-listeners](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/client-passive-event-listeners.md)
- [MDN — addEventListener passive option](https://developer.mozilla.org/en-US/docs/Web/API/EventTarget/addEventListener#passive)
