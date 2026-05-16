# Snapshot: React 19 — use() hook reference

- **source**: https://react.dev/reference/react/use
- **role**: canonical-react
- **fetched_at**: 2026-05-16T00:00:00Z
- **react_version_observed**: 19 (current stable per react.dev)
- **via**: WebFetch

## Relevant quotes (verbatim)

### Where to await — Server vs Client

> "When fetching data in a Server Component, prefer `async` and `await` over `use`. `async` and `await` pick up rendering from the point where `await` was invoked, whereas `use` re-renders the component after the data is resolved."

### Promise creation site

> "Prefer creating Promises in Server Components and passing them to Client Components over creating Promises in Client Components. Promises created in Client Components are recreated on every render. Promises passed from a Server Component to a Client Component are stable across re-renders."

### Suspense boundary contract

> "The fallback will be shown while the promise is being resolved."

(Example shows `<Suspense fallback={<p>Loading settings…</p>}>` wrapping the `use(promise)` consumer component.)

## What the page does NOT say

- The `use()` reference page does **not** mention `Promise.all()` at all. Promise.all is left for the data-fetching guide on Next.js side (see `nextjs-fetching-data.snapshot.md`).
- No explicit guidance on fetching multiple independent resources from within a single Client Component. The implicit recommendation is: initiate in Server Component, pass promises down, resolve with `use()` inside `<Suspense>`.

## Audit implication

For React 19, the parallel-fetch story is **diffused across two pages**:
1. React's `use()` page tells you where to create promises (Server).
2. Next.js docs tell you how to aggregate them (`Promise.all`).

Any catalog rule about parallel fetching must reference **both** to be complete.
