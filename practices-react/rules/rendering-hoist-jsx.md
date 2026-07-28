---
title: Compiler hoists static JSX automatically — manual hoist only for compiler-off projects or generated blobs
impact: LOW
impactDescription: "Manual hoist of static JSX out of components avoids re-creation per render. React Compiler does this automatically in React 19+ projects."
tags: [rendering, jsx, static, optimization, react-compiler]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RENDERING-003"
verification:
  type: review
  status: manual
  notes: "Reviewer checks that static JSX with no per-render-dependent props is hoisted to module scope, or left to the React Compiler on React 19+ projects where automatic memoization already covers it (avoiding redundant manual hoisting)."
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
    notes: "React Compiler GA makes manual hoist unnecessary in compiler-enabled projects."
  completeness:
    status: complete
    amendments:
      - "Lead with compiler-first framing"
      - "Forbid hoisting JSX that depends on props/state/context/hooks/locale/auth/theme"
      - "Narrowed to compiler-off projects or genuinely static generated blobs"
  gap_check:
    status: complete
    note: "Strong overlap with rerender-memo. Cross-link."
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: rendering-hoist-jsx"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-hoist-jsx.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rendering-hoist-jsx"
    quote: "Extract static JSX outside components to avoid re-creation."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - rerender-memo
---

## Compiler hoists static JSX automatically — manual hoist only for compiler-off projects or generated blobs

**Impact: LOW — React Compiler (GA in React 19) auto-hoists. Manual hoist is a fallback.**

### Decision order

1. **React Compiler enabled?** Stop. The compiler handles this. Manual hoist adds noise.
2. **Compiler off + JSX is genuinely static** (no props, state, context, hooks, locale, auth, theme dependencies)? Manual hoist is acceptable.

### Forbidden hoists (always wrong)

JSX that closes over any of these MUST stay inside the component:

- Props
- State
- Context values
- Hook results
- Locale / i18n
- Theme / dark-mode
- Auth state
- Anything else that changes between renders

Hoisting such JSX freezes the value at module-load time → wrong content forever.

### Correct (compiler off + static blob)

```tsx
const STATIC_SKELETON = (
  <div className="animate-pulse h-20 bg-gray-200" />
)

function Container({ loading }: { loading: boolean }) {
  return <div>{loading && STATIC_SKELETON}</div>
}
```

### Incorrect (compiler off + depends on theme)

```tsx
// BAD: theme changes; hoisted JSX is frozen to whatever theme was at module load.
const FROZEN = (
  <div className={isDarkMode() ? 'dark' : 'light'}>Hello</div>
)
```

### Incorrect (compiler on + manual hoist)

```tsx
// Redundant noise. The compiler already does this.
const SKELETON = <div className="..." />
function X() { return loading ? SKELETON : null }
```

### Especially useful (compiler off) for large inline SVGs

Inline `<svg>` with many children can be expensive to recreate. If the SVG is static, hoisting is meaningful.

Sources:
- [Vercel: rendering-hoist-jsx](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-hoist-jsx.md)
- [React Compiler](https://react.dev/learn/react-compiler)
