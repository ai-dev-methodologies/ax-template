---
title: Never define a component inside another component — it remounts on every parent render and destroys state
impact: HIGH
impactDescription: "A new function literal is a new component type. React unmounts the old instance and mounts a fresh one — input focus is lost, animations restart, effects re-run, scroll position resets. Define child components at module scope; pass data as props."
tags: [rerender, components, remount, performance, correctness]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-015"
verification:
  type: lint
  rule_id: "ax/no-inline-component-definition"
  status: planned
  notes: "Custom ESLint rule planned: flag function declarations inside other function components whose return type is JSX, except small inline render helpers explicitly returning array-of-JSX nodes."
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness: { status: complete, amendments: ["Lead with remount/lost state, not perf", "Allow tiny render helpers that are not component types"] }
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel: rerender-no-inline-components"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-no-inline-components.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-no-inline-components"
    quote: "Defining a component inside another component creates a new component type on every render. React sees a different component each time and fully remounts it, destroying all state and DOM."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_AS_IS }
sibling_rules: []
---

## Never define a component inside another component

**Impact: HIGH — This is a correctness bug, not just perf. New component type each render → remount → lost state.**

### Incorrect — Avatar/Stats remount on every UserProfile render

```tsx
function UserProfile({ user, theme }) {
  const Avatar = () => <img src={user.avatarUrl} className={theme === 'dark' ? 'avatar-dark' : 'avatar-light'} />
  const Stats = () => <div><span>{user.followers}</span><span>{user.posts}</span></div>
  return <div><Avatar /><Stats /></div>
}
```

Every time `UserProfile` renders, `Avatar` and `Stats` are new component types. React's reconciler treats them as completely different components → unmounts old instance → mounts new instance.

### Symptoms of this bug

- Input fields **lose focus** on every keystroke (parent state update → input child remounts → DOM input element replaced).
- Animations **restart unexpectedly** (mount → animation start; remount → animation start again).
- `useEffect` cleanup + setup run **on every parent render**.
- Scroll position **resets** inside the inner component.
- State accumulated in the inner component (e.g. accordion open/closed) is wiped.

### Correct — define at module scope; pass data as props

```tsx
function Avatar({ src, theme }: { src: string; theme: 'light' | 'dark' }) {
  return <img src={src} className={theme === 'dark' ? 'avatar-dark' : 'avatar-light'} />
}

function Stats({ followers, posts }: { followers: number; posts: number }) {
  return <div><span>{followers}</span><span>{posts}</span></div>
}

function UserProfile({ user, theme }) {
  return (
    <div>
      <Avatar src={user.avatarUrl} theme={theme} />
      <Stats followers={user.followers} posts={user.posts} />
    </div>
  )
}
```

### Inline render helpers (not component types) are OK

```tsx
// FINE: this is a render helper, not a component type. React doesn't treat it as a component
// because we call it directly, not via JSX.
function ItemList({ items }: { items: Item[] }) {
  const renderItem = (item: Item) => <li key={item.id}>{item.name}</li>
  return <ul>{items.map(renderItem)}</ul>
}
```

The boundary: anything returned via `<Foo />` (JSX with capital-letter element) becomes a component type and triggers this bug. Anything called as `renderFoo(x)` is just a function call — no remount.

### Why "access parent variables" isn't a reason

The temptation is "I want this child to see `user` without passing it as a prop." Pass it as a prop. The cost (clearer interface) is negligible; the cost of inline definition (remount-on-everything) is huge.

Sources:
- [Vercel: rerender-no-inline-components](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-no-inline-components.md)
