---
title: Use useEffectEvent (React 19.2+) for non-reactive callbacks inside Effects
impact: LOW
impactDescription: "Prevents Effect re-runs when a callback identity changes but should NOT trigger reconnection. Replaces the older useRef-backed pattern with a clean, stable API."
tags:
  - advanced
  - hooks
  - useEffectEvent
  - effects
  - optimization
applicable_to:
  - react
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-ADVANCED-003"
verification:
  type: review
  status: manual
  notes: "Reviewer checks: (a) React 19.2+ available, (b) useEffectEvent is only called inside Effects (never during render, never as a prop, never in dependency arrays), (c) Effect Event isn't hiding a dependency that should re-trigger the Effect."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
    notes: "Pattern is correct when onSearch is non-reactive and only `query` should re-trigger."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "useEffectEvent is STABLE in React 19.2 (released Oct 2025). Earlier 19.0-19.1 needed experimental_useEffectEvent."
  completeness:
    status: complete
    amendments:
      - "Stated version requirement: 19.2+ stable, 19.0-19.1 experimental_"
      - "Added 3 misuse warnings from React docs (hide deps / call during render / pass to components)"
  gap_check:
    status: complete
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: react-best-practices (advanced-use-latest)"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/advanced-use-latest.md"
    role: seed
  - id: react-19-useeffectevent
    title: "React 19 — useEffectEvent (stable in 19.2)"
    url: "https://react.dev/reference/react/useEffectEvent"
    role: canonical-react
evidence:
  - upstream_id: vercel-react-best-practices
    section: "advanced-use-latest"
    quote: "Access latest values in callbacks without adding them to dependency arrays. Prevents effect re-runs while avoiding stale closures."
  - source_type: external
    citation: "React docs — useEffectEvent: 'If a value should cause your Effect to re-run, keep it as a dependency. Only use Effect Events for logic that genuinely should not re-trigger your Effect.'"
    url: "https://react.dev/reference/react/useEffectEvent"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Accurate when onSearch is non-reactive"
    - "Add version requirement"
    - "Add 3 misuse warnings"
sibling_rules:
  - advanced-event-handler-refs
---

## Use useEffectEvent (React 19.2+) for non-reactive callbacks inside Effects

**Impact: LOW — Prevents Effect re-runs when a callback identity changes but should NOT trigger reconnection. Replaces the older useRef-backed pattern with a clean, stable API.**

### Version requirement

- **React 19.2+**: `useEffectEvent` is stable.
- **React 19.0–19.1**: use `experimental_useEffectEvent`.
- **React 18 and earlier**: not available; use the ref-backed fallback (sibling rule `advanced-event-handler-refs.md`).

### Incorrect — Effect re-runs on every callback change

```tsx
function SearchInput({ onSearch }: { onSearch: (q: string) => void }) {
  const [query, setQuery] = useState('')
  useEffect(() => {
    const timeout = setTimeout(() => onSearch(query), 300)
    return () => clearTimeout(timeout)
  }, [query, onSearch])   // onSearch identity may change every parent render
}
```

### Correct — Effect Event reads latest onSearch without re-running on its identity

```tsx
import { useEffectEvent } from 'react'

function SearchInput({ onSearch }: { onSearch: (q: string) => void }) {
  const [query, setQuery] = useState('')
  const onSearchEvent = useEffectEvent(onSearch)
  useEffect(() => {
    const timeout = setTimeout(() => onSearchEvent(query), 300)
    return () => clearTimeout(timeout)
  }, [query])   // onSearchEvent intentionally excluded
}
```

### Three misuse warnings (React docs verbatim)

1. **Don't use Effect Events to hide dependencies.** If a value should cause the Effect to re-run, keep it in the dep array. Effect Events are for logic that genuinely should not re-trigger.

2. **Don't call during render.** Effect Events are only legitimate inside Effects.

3. **Don't pass Effect Events to other components, and don't put them in dependency arrays.** Effect Events have intentionally unstable identity per render — this is a runtime assertion to catch misuse.

### Comparison to the ref-backed pattern (sibling rule)

| | Ref pattern | useEffectEvent |
|---|---|---|
| React version | any | 19.2+ stable |
| Mechanic | manual ref sync + read inside listener | built-in |
| Latest-value guarantee | yes | yes |
| Stable identity | yes (the ref is stable) | **intentionally NOT stable** (runtime assertion) |
| Failure mode | silent stale closures if ref sync forgotten | loud — Effect re-runs every render if misused |

Prefer `useEffectEvent` on 19.2+ — the unstable identity is a feature, not a bug.

Sources:

- [Vercel: advanced-use-latest](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/advanced-use-latest.md)
- [React 19 — useEffectEvent](https://react.dev/reference/react/useEffectEvent)
