# Snapshot: Next.js 16 — 'use cache: private'

- **source**: https://nextjs.org/docs/app/api-reference/directives/use-cache-private
- **role**: canonical-nextjs
- **fetched_at**: 2026-05-13T00:00:00Z
- **doc_last_updated_at**: 2026-05-13
- **version_observed**: 16.2.6
- **via**: WebFetch

## Status (verbatim)

> "This feature is currently experimental and subject to change, it's not recommended for production."

> "This directive is marked as experimental because it depends on runtime prefetching, which is not yet stable."

Version history: introduced in v16.0.0 with Cache Components.

## What it does (verbatim)

> "The 'use cache: private' directive allows functions to access runtime request APIs like cookies(), headers(), and searchParams within a cached scope. However, results are never stored on the server, they're cached only in the browser's memory and do not persist across page reloads."

## When to use (verbatim)

> "Reach for 'use cache: private' when:
> - You want to cache a function that already accesses runtime data, and refactoring to move the runtime access outside and pass values as arguments is not practical.
> - Compliance requirements prevent storing certain data on the server, even temporarily"

## API allowed inside (verbatim table)

| API | use cache | use cache: private |
|---|---|---|
| cookies() | No | **Yes** |
| headers() | No | **Yes** |
| searchParams | No | **Yes** |
| connection() | No | No |

## Critical constraints (verbatim)

- "Because this directive accesses runtime data, the function executes on every server render and is excluded from running during static shell generation."
- "It is **not** possible to configure custom cache handlers for 'use cache: private'."
- "This directive is not available in Route Handlers."
- "The stale time must be at least 30 seconds for runtime prefetching to work."

## Cache scope

Per-client (browser memory only). Does not persist across page reloads. Never written server-side.

## Audit implication

`'use cache: private'` is the **escape hatch** when you legitimately cannot refactor runtime data out of a cached scope. It is:
- experimental in 16.2.6 (not production-recommended),
- per-client (no cross-user sharing),
- non-persistent (lost on reload),
- not allowed in Route Handlers,
- not custom-handler-configurable.

A catalog rule must lead with: "first try to refactor; this is the fallback."
