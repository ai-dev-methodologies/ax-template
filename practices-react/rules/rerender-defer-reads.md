---
title: Don't subscribe to dynamic state (useSearchParams, etc.) when you only read it inside a callback
impact: MEDIUM
impactDescription: "Reactive hooks (useSearchParams, useSyncExternalStore-derived) re-render their consumers on every change. If the value is only consumed inside an event handler, read it on-demand from window.location.search / document.cookie instead."
tags: [rerender, searchParams, localStorage, optimization]
applicable_to: [react, nextjs]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-002"
verification: { type: review, status: manual }
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness: { status: complete, amendments: ["Client-only — window.location.search only inside browser callbacks", "If UI must react to URL changes, keep useSearchParams()"] }
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-defer-reads"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-defer-reads.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-defer-reads"
    quote: "Don't subscribe to dynamic state (searchParams, localStorage) if you only read it inside callbacks."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: []
---

## Don't subscribe to dynamic state when you only read it inside a callback

**Impact: MEDIUM — Reactive subscriptions re-render the consumer on every change. If the value is only used at click time, read on-demand.**

### Incorrect — subscribes to every URL change

```tsx
'use client'
import { useSearchParams } from 'next/navigation'

function ShareButton({ chatId }: { chatId: string }) {
  const searchParams = useSearchParams()   // subscribes to every URL change
  return (
    <button onClick={() => shareChat(chatId, { ref: searchParams.get('ref') })}>
      Share
    </button>
  )
}
```

### Correct — read on demand inside the handler

```tsx
'use client'
function ShareButton({ chatId }: { chatId: string }) {
  return (
    <button
      onClick={() => {
        const params = new URLSearchParams(window.location.search)
        shareChat(chatId, { ref: params.get('ref') })
      }}
    >
      Share
    </button>
  )
}
```

### When you DO need the subscription

If the **UI** (rendered output) must reflect the value — e.g. the button label changes when the URL changes — keep `useSearchParams()`. The rule applies only when the value is consumed strictly inside a handler.

Same logic applies to `localStorage` (no React subscription anyway; the rule is "don't bake a sync-hook around it for a callback-only read") and any cookie-watcher hook.

Sources:
- [Vercel: rerender-defer-reads](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-defer-reads.md)
