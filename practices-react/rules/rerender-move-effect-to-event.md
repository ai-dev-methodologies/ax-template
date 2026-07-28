---
title: Side effects triggered by user actions belong in event handlers, not state + Effect
impact: MEDIUM
impactDescription: "Canonical React docs pattern. Modeling 'user clicked submit' as state→Effect causes the Effect to re-run on unrelated dep changes (theme/context) and may duplicate the action. Run it in the handler."
tags: [rerender, useEffect, events, side-effects, dependencies, you-might-not-need-an-effect]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-011"
verification:
  type: review
  status: manual
  notes: "Reviewer checks that an action triggered by a specific user event (e.g. submit) runs directly in the event handler, not modeled as state that an `Effect` reacts to — the Effect form re-runs on unrelated dependency changes and can duplicate the action."
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness: { status: complete }
  gap_check: { status: complete, note: "Sibling rerender-derived-state-no-effect; both anchored in You Might Not Need an Effect" }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-move-effect-to-event"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-move-effect-to-event.md"
    role: seed
  - id: react-you-might-not-need-effect
    title: "React docs — You Might Not Need an Effect (should this code move to an event handler?)"
    url: "https://react.dev/learn/you-might-not-need-an-effect"
    role: canonical-react
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-move-effect-to-event"
    quote: "If a side effect is triggered by a specific user action (submit, click, drag), run it in that event handler."
  - source_type: external
    citation: "React docs — Should this code move to an event handler? (canonical 'derive from action' guidance)"
    url: "https://react.dev/learn/removing-effect-dependencies#should-this-code-move-to-an-event-handler"
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_AS_IS }
sibling_rules: [rerender-derived-state-no-effect]
---

## Side effects from user actions belong in event handlers, not state + Effect

**Impact: MEDIUM — Canonical React docs guidance.**

### Incorrect — action modeled as state + Effect

```tsx
function Form() {
  const [submitted, setSubmitted] = useState(false)
  const theme = useContext(ThemeContext)

  useEffect(() => {
    if (submitted) {
      post('/api/register')          // re-runs if theme changes after submit!
      showToast('Registered', theme)
    }
  }, [submitted, theme])

  return <button onClick={() => setSubmitted(true)}>Submit</button>
}
```

Bug: if `theme` changes after submit (legitimate UI update), the Effect re-runs and re-submits.

### Correct — handler

```tsx
function Form() {
  const theme = useContext(ThemeContext)

  function handleSubmit() {
    post('/api/register')
    showToast('Registered', theme)
  }

  return <button onClick={handleSubmit}>Submit</button>
}
```

### What Effects ARE for (kept for clarity)

- **Synchronization with external systems**: subscriptions, websockets, browser APIs.
- **Side effects caused by rendering / mounting**, not by user actions.
- **Cleanup on unmount**.

If the side effect's cause is "user did X", it goes in the handler. If the cause is "this thing rendered" or "this state value reached a sync point with the outside world", it goes in an Effect.

### Decision question (per React docs)

> "Did this code happen because the user did something specific?"

- Yes → event handler.
- No, it's part of rendering / sync → Effect.

Sources:
- [Vercel: rerender-move-effect-to-event](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-move-effect-to-event.md)
- [React — Should this code move to an event handler?](https://react.dev/learn/removing-effect-dependencies#should-this-code-move-to-an-event-handler)
