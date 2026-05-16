# Snapshot: React 19 — useEffectEvent

- **source**: https://react.dev/reference/react/useEffectEvent
- **role**: canonical-react
- **fetched_at**: 2026-05-16T00:00:00Z
- **react_version_observed**: 19.2 (stable)
- **via**: WebFetch

## Stability status

- Stable in React 19.2 (Oct 2025).
- React 19.0–19.1: `experimental_useEffectEvent`.
- React 18 and earlier: not available.

## What it solves (verbatim)

> "Effect Events 'always see the latest values from render (like props and state) without re-synchronizing your Effect.'"

> "They're excluded from Effect dependencies."

## Three misuse warnings (verbatim)

1. **Don't use to hide dependencies.** "If a value should cause your Effect to re-run, keep it as a dependency. Only use Effect Events for logic that genuinely should not re-trigger your Effect."

2. **Don't call during render.** Effect Events are only legitimate inside Effects.

3. **Don't pass to other components or include in dependencies.** "ESLint will warn."

## Identity contract

> "The non-stable identity acts as a runtime assertion: if your code incorrectly depends on the function identity, you'll see the Effect re-running on every render, making the bug obvious."

This is the opposite of `useCallback`'s contract (stable identity). Choose Effect Event for Effect-internal usage; choose `useCallback` for passing to children.

## Audit implication

Two of the Vercel "advanced" rules (handler-refs + use-latest) overlap. Catalog
should keep both with explicit framing: ref pattern as legacy/fallback,
useEffectEvent as preferred 19.2+ primitive.
