---
title: Profile before manual memoization — prefer React Compiler, pure rendering, and local state
impact: MEDIUM
impactDescription: "Manual memo()/useMemo() is a fallback optimization, not a default design choice. In React 19 + Compiler era, most manual memoization is unnecessary or harmful. Reach for memo() only after profiling shows perceptible lag and the props are stable."
tags:
  - rerender
  - memo
  - useMemo
  - react-compiler
  - optimization
  - performance
applicable_to:
  - react
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-001"
verification:
  type: review
  status: manual
  notes: "Static detection of 'unnecessary memo' is unreliable (false positives common). Verification combines: (a) React DevTools Profiler evidence in the PR description, (b) check whether React Compiler is enabled — if so, prefer compiler-managed memoization and only ship manual memo for custom comparator cases or explicit cleanup migration."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
  contested: true
  contested_axis: "(Claude + Codex) vs Vercel seed — both reviewers independently judged Vercel's framing inverts React 19 official guidance"
audit:
  accuracy:
    status: verified-with-framing-flip
    last_verified: "2026-05-16"
    notes: "Vercel's 'extract to memoized child for early return' example is technically valid, but the rule's title and premise present manual memoization as a default move when React 19 docs make it a fallback. Verdict: the underlying mechanic is correct; the framing inverts the priority."
  freshness:
    status: stale
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Vercel rule treats React Compiler as a one-line footnote. As of May 2026 React Compiler is GA in React 19; this is the dominant fact for the topic. Catalog rule must lead with compiler-first guidance."
  completeness:
    status: complete
    amendments:
      - "Inverted premise: profile-first, compiler-first, manual memo as last resort"
      - "Added 'always new prop' trap (object/function/array literals defeat memoization)"
      - "Added deep equality custom-comparator warning (slow, brittle)"
      - "Added React Compiler migration guidance (leave existing memos during migration, remove in cleanup)"
      - "Listed React 19 alternatives: pure rendering, children composition, local state, fewer Effects"
      - "Verification conditional on whether React Compiler is enabled"
  gap_check:
    status: complete
    note: "Custom comparator case folded in as a warning section rather than spawning a sibling rule (per codex)."
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: react-best-practices (rule: rerender-memo)"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-memo.md"
    role: "seed"
  - id: react-19-memo
    title: "React 19 — memo() API reference"
    url: "https://react.dev/reference/react/memo"
    role: "canonical-react"
  - id: react-compiler
    title: "React 19 — React Compiler"
    url: "https://react.dev/learn/react-compiler"
    role: "canonical-react"
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rerender-memo"
    quote: "Extract expensive work into memoized components to enable early returns before computation."
  - source_type: external
    citation: "React 19 — memo() reference, 'When to use memo' / 'When not to use memo' (verbatim guidance: 'You should only rely on memo as a performance optimization. If your code doesn't work without it, find the underlying problem and fix it first.')"
    url: "https://react.dev/reference/react/memo"
  - source_type: external
    citation: "React 19 — memo() reference, 'always different props' trap ('memo is completely useless if the props passed to your component are always different, such as if you pass an object or a plain function defined during rendering')"
    url: "https://react.dev/reference/react/memo"
  - source_type: external
    citation: "React 19 — React Compiler GA documentation (compiler applies automatic memoization equivalent to memo + useMemo; manual memo can be safely removed)"
    url: "https://react.dev/learn/react-compiler"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=high"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Vercel rule is lossy with counter-recommendation pressure, not flatly false"
    - "Premise should be inverted"
    - "Custom comparator folded in as warning, not separate rule"
    - "Verification conditional on React Compiler presence"
  framing_flip:
    from: "Extract to Memoized Components (memo as default move)"
    to: "Profile Before Manual Memoization (memo as last resort)"
sibling_rules: []
---

## Profile before manual memoization — prefer React Compiler, pure rendering, and local state

**Impact: MEDIUM — Manual `memo()`/`useMemo()` is a fallback optimization, not a default design choice. In React 19 + Compiler era, most manual memoization is unnecessary or harmful. Reach for `memo()` only after profiling shows perceptible lag and the props are stable.**

> **Note on framing.** The seed source frames this rule as "extract expensive
> work into memoized components." Both reviewers (Claude and Codex)
> independently judged that framing as an inversion of React 19's official
> guidance. React docs say: "**You should only rely on memo as a performance
> optimization. If your code doesn't work without it, find the underlying
> problem and fix it first.**" The ax catalog flips the framing accordingly.

### Decision order — try these first

1. **Check whether React Compiler is enabled** (`@babel/plugin-react-compiler` / `babel-plugin-react-compiler` configured). If yes:
    - Do not add manual `memo()` / `useMemo()` / `useCallback()` for ordinary re-render optimization. The compiler does it.
    - Exception: custom comparator (`memo(C, areEqual)`) — compiler does not replicate this. Keep / add manual only when you actually need custom comparison.
    - Migration policy: leave existing manual memos in place during compiler adoption to avoid regressions; remove them in a dedicated cleanup pass with profiler verification.

2. **If no compiler**, prefer code shapes that make memoization unnecessary:
    - Accept JSX as `children` so wrappers do not re-render with the children's identity.
    - Keep state local; don't lift state higher than needed.
    - Maintain pure rendering — fix bugs that show up as visible artifacts on re-render, don't mask them with `memo`.
    - Avoid unnecessary Effects and Effect dependencies.

3. **Only then**: profile with React DevTools. If a specific interaction shows perceptible lag AND the offending component's props are stable, consider manual `memo()`.

### When manual memo() is justified

- Component re-renders **often** with the **same exact props**.
- Re-render cost is **measurable** (profiler evidence).
- Props are primitives or stable references (not literals constructed in the parent every render).
- Parent has already been examined for "always-new" prop sources.

### Common traps that defeat or harm memo()

**"Always new" props (one is enough to defeat the whole component):**

```tsx
// BAD: { theme: 'dark' } is a new object every parent render — memo never hits.
<Child options={{ theme: 'dark' }} />

// BAD: inline arrow recreated every render.
<Child onClick={() => doThing()} />

// GOOD: stable reference.
const OPTIONS = { theme: 'dark' }
<Child options={OPTIONS} />
```

> React docs: "a single value that's 'always new' is enough to break memoization for an entire component."

**Custom comparators are dangerous:**

```tsx
// DANGEROUS: deep equality runs on every parent render. May freeze the app
// when data shapes grow. Profile before shipping; never use this for nested
// data structures.
export default memo(MyComponent, (prev, next) =>
  deepEqual(prev.data, next.data),
)
```

> React docs: "Deep equality checks can become incredibly slow and can freeze your app for many seconds if someone changes the data structure later."

If you must use a custom comparator: compare **every** prop explicitly (omitting one is a correctness bug), benchmark against just letting React re-render, and document the choice next to the call.

### The seed rule's extracted-child pattern — narrow application

Vercel's "Correct" example (extracting an expensive child into a `memo()` wrapper so the parent can do an early-return before the work) is valid in a narrow case:

- React Compiler is **off**, AND
- Profiling shows the child's render is expensive, AND
- The parent legitimately does an early return that should skip the child entirely, AND
- The child's props are stable.

In that case, the extraction is sound. But in the React Compiler era this case shrinks toward zero — the compiler already memoizes the child if its props are stable, so the early return in the parent already avoids the work without an explicit `memo()` wrapper.

**Incorrect (seed's "Correct" applied indiscriminately):**

```tsx
// In a Compiler-enabled project, this manual memo is redundant and adds
// per-render overhead.
const UserAvatar = memo(function UserAvatar({ user }: { user: User }) {
  return <Avatar id={computeAvatarId(user)} />
})
```

**Correct (in a no-compiler project, with profile evidence the avatar render is expensive):**

```tsx
const UserAvatar = memo(function UserAvatar({ user }: { user: User }) {
  return <Avatar id={computeAvatarId(user)} />
})

function Profile({ user, loading }: Props) {
  if (loading) return <Skeleton />
  return <UserAvatar user={user} />
}
```

### What this rule does NOT recommend

- "Memoize as much as possible" — explicitly counter-recommended by React docs (readability cost, fragility, may interact poorly with Compiler).
- Using `useMemo` for cheap primitives (`useMemo(() => a + b, [a, b])` is more expensive than the addition).
- Adding `memo()` to fix a re-render-visible bug — that's masking; fix the bug.

### Verification

Conditional on whether React Compiler is enabled:

- **Compiler ON**: PR review checks for manual `memo` / `useMemo` / `useCallback` additions; rejects unless (a) custom comparator, or (b) explicit migration-leave-in-place comment.
- **Compiler OFF**: PR review checks for profiler evidence in the description before accepting new memo. Stable-prop audit on the parent's call site.

Both modes: deep-equality custom comparators require explicit justification + benchmark.

Sources for this rule:

- [Vercel agent-skills: rerender-memo (seed, framing flipped per audit)](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rerender-memo.md)
- [React 19 — memo() API reference](https://react.dev/reference/react/memo)
- [React 19 — React Compiler](https://react.dev/learn/react-compiler)
