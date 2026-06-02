---
title: React hooks MUST be called before any conditional early return — Rules of Hooks
impact: HIGH
impactDescription: "Hooks placed after early returns mount into different slots between renders and crash the component with 'Rendered more hooks than during the previous render'"
tags:
  - react
  - hooks
  - rules-of-hooks
  - render-correctness
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-001"
verification:
  type: review
  source: "templates/L4/approval-workflow/app/(approvals)/[id]/page.tsx"
  pattern: "all useQuery / useMutation / useState / useMemo calls above the `if (isLoading) return …` / `if (error) return …` / `if (!data) return …` block"
upstream:
  - "https://react.dev/reference/rules/rules-of-hooks"
  - "https://react.dev/learn/state-as-a-snapshot"
evidence:
  - source_type: external
    citation: "React — Rules of Hooks (Only call Hooks at the top level)"
    url: "https://react.dev/reference/rules/rules-of-hooks"
    quote: "Don't call Hooks inside loops, conditions, or nested functions. Instead, always use Hooks at the top level of your React function, before any early returns."
    quoted_at: "2026-05-25"
  - source_type: external
    citation: "React — State as a Snapshot"
    url: "https://react.dev/learn/state-as-a-snapshot"
    quote: "A state variable's value never changes within a render, even if its event handler's code is asynchronous."
    quoted_at: "2026-05-25"
---

## React hooks MUST be called before any conditional early return — Rules of Hooks

**Impact: HIGH — every render must call hooks in the same order**

React tracks hook state by call order. A hook placed after a conditional early return is sometimes called and sometimes not, depending on the early-return condition. On the first render where the early-return fires, the hook is skipped; on the next render where data arrives and execution continues past the early returns, the hook is called for the first time. React's internal slot counter sees a different shape than the prior render and throws `'Rendered more hooks than during the previous render'`. In production builds the failure mode is silent state corruption between slots (the "second hook" gets state belonging to the "first hook").

This is the most common AI-generated React bug. The pattern looks correct — guard against null data, then use it. But the guard must come AFTER all hooks, not before.

Apply this rule to: `useState`, `useEffect`, `useMemo`, `useCallback`, `useRef`, `useQuery`, `useMutation`, custom hooks (`useFoo`). Anything starting with `use*` follows the same rule.

**Incorrect — hook placed after the data guard:**

```tsx
export default function DetailPage() {
  const { data, isLoading, error } = useQuery(...)
  const [comment, setComment] = useState('')

  if (isLoading) return <Spinner />
  if (error) return <ErrorPanel />
  if (!data) return <NotFound />

  // ❌ This memo is sometimes called, sometimes not.
  // First render: data === undefined → early return at line above → memo NEVER runs.
  // Second render after data arrives → memo runs for the first time.
  // React: 'Rendered more hooks than during the previous render'.
  const summary = React.useMemo(() => buildSummary(data), [data])

  return <Page summary={summary} />
}
```

**Correct — every hook lives above the early returns; guard nullable inputs INSIDE the memo body:**

```tsx
export default function DetailPage() {
  const { data, isLoading, error } = useQuery(...)
  const [comment, setComment] = useState('')

  // ✅ Hook called unconditionally on every render.
  // The nullable input is handled inside the memo, not by a structural guard around it.
  const summary = React.useMemo(
    () => (data ? buildSummary(data) : null),
    [data],
  )

  if (isLoading) return <Spinner />
  if (error) return <ErrorPanel />
  if (!data) return <NotFound />

  return <Page summary={summary!} />
}
```

The `data!` non-null assertion at the use-site is safe because the `!data` early return already established `data` is non-null at that point. TypeScript's narrowing tracks that.

**A note on `chainPreview` / `chain` patterns**: when a derived value depends on the not-yet-loaded data, do **not** double-derive (`chainPreview = data ? compute() : null; … chain = chainPreview ?? compute()`) — the second derivation is provably unreachable after the `!data` guard, and the duplication invites drift. Compute once inside a memo whose deps include the data, then assert non-null at the use-site.

Reference: [React — Rules of Hooks](https://react.dev/reference/rules/rules-of-hooks)

Reference: [React — State as a Snapshot](https://react.dev/learn/state-as-a-snapshot)
