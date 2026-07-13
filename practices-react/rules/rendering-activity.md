---
title: Use Activity (React 19.2+) for expensive UI that toggles visibility frequently — be aware hidden mode unmounts effects
impact: MEDIUM
impactDescription: "Preserves DOM and state for hidden components while deferring their updates. Effects are unmounted on hide and remounted on show — design subscriptions accordingly."
tags: [rendering, activity, visibility, state-preservation, react-19]
applicable_to: [react, nextjs]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RENDERING-007"
verification:
  type: review
  status: manual
  notes: "Reviewer checks: (a) component is on React 19.2+, (b) hidden mode acceptable for component's effects (they will unmount/remount), (c) used for genuinely-toggling UI (tabs/drawers/panels), (d) hidden updates being deferred is acceptable."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Activity stable in React 19.2 (released Oct 2025)."
  completeness:
    status: complete
    amendments:
      - "Hidden mode unmounts effects (Vercel rule didn't say this)"
      - "Hidden updates are deferred"
      - "Use for tabs/drawers/panels, not for hidden subscriptions that must keep running"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: rendering-activity"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-activity.md"
    role: seed
  - id: react-19-activity
    title: "React 19.2 — <Activity>"
    url: "https://react.dev/reference/react/Activity"
    role: canonical-react
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rendering-activity"
    quote: "Avoids expensive re-renders and state loss."
  - source_type: external
    citation: "React 19.2 — Activity supports 'visible' and 'hidden' modes; hidden hides children, unmounts effects, and defers all updates until React has nothing left to work on"
    url: "https://react.dev/reference/react/Activity"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules: []
---

## Use Activity (React 19.2+) for expensive UI that toggles visibility frequently — hidden mode unmounts effects

**Impact: MEDIUM — Preserves DOM and state. Hidden mode unmounts effects and defers updates.**

### Mode semantics (per React 19.2 docs)

- **`visible`**: children render, effects mount, updates process normally.
- **`hidden`**: children stay in DOM with state preserved, effects **unmount**, updates **defer** until React is idle.

The state-and-DOM preservation is what `display: none` can't give you (which loses focus/scroll position/uncontrolled inputs). Activity is a React-aware version.

### Correct — tabs / drawers / panels that toggle often

```tsx
import { Activity } from 'react'

function Sidebar({ open }: { open: boolean }) {
  return (
    <Activity mode={open ? 'visible' : 'hidden'}>
      <ExpensiveSidebarContent />
    </Activity>
  )
}
```

On every close-and-reopen: state in `ExpensiveSidebarContent` is preserved, DOM stays mounted, effects re-run.

### Incorrect — using Activity for components whose effects MUST keep running

```tsx
// BAD: hidden mode unmounts useEffect — the subscription tears down when sidebar closes.
function NotificationsSidebar({ open }: { open: boolean }) {
  return (
    <Activity mode={open ? 'visible' : 'hidden'}>
      <NotificationsListener />   {/* subscribes to a websocket */}
    </Activity>
  )
}
```

If the subscription must keep running when the panel is hidden, don't use Activity. Lift the subscription to a parent, or use `display: none` (loses some benefits) or keep the component visible behind a CSS hide.

### Use cases

Good fits:
- Tabs with expensive inner state (form drafts, scrolled lists)
- Drawers/panels users toggle 10× per session
- Multi-step wizards where the user can navigate back
- Modals with heavy inner content that should re-open instantly

Bad fits:
- Components whose effects need to run while hidden (subscriptions, timers, periodic polls)
- Components where the hidden update being deferred is unacceptable

### Notes

- Doesn't replace virtualization. Doesn't unload the DOM.
- The React team flags more modes as future work — current API has just `visible`/`hidden`.

Sources:
- [Vercel: rendering-activity](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-activity.md)
- [React 19.2 — Activity](https://react.dev/reference/react/Activity)
