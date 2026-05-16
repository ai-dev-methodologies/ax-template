# Snapshot: React 19 — cache() API

- **source**: https://react.dev/reference/react/cache
- **role**: canonical-react
- **fetched_at**: 2026-05-16T00:00:00Z
- **react_version_observed**: 19 (current stable per react.dev)
- **via**: WebFetch

## Scope — Server Components only (verbatim)

> "cache is only for use with React Server Components."
> "cache is for use in Server Components only."

Calling a memoized function outside of a component does not use the cache. "React only provides cache access to the memoized function in a component" because "cache access is provided through a context which is only accessible from a component."

## Lifetime — per request (verbatim)

> "React will invalidate the cache for all memoized functions for each server request."

The cache does not persist across multiple server requests — it is request-scoped.

## Behavior

### Argument-keyed memoization (verbatim)

> "When getMetrics is first called with data, getMetrics will call calculateMetrics(data) and store the result in cache. If getMetrics is called again with the same data, it will return the cached result instead of calling calculateMetrics(data) again."

### Errors are cached too (verbatim)

> "cachedFn will also cache errors. If fn throws an error for certain arguments, it will be cached, and the same error is re-thrown when cachedFn is called with those same arguments."

### Each cache() call creates a separate cache (verbatim)

> "Each call to cache creates a new function. This means that calling cache with the same function multiple times will return different memoized functions that do not share the same cache."

→ Define cached functions at module level, once.

### Non-primitive arguments require same reference (verbatim)

> "If your arguments are not primitives (ex. objects, functions, arrays), ensure you're passing the same object reference."

React uses `Object.is()` for cache-hit comparison.

## Recommended use (verbatim)

> "In general, you should use cache in Server Components to memoize work that can be shared across components."

## Comparison to siblings (verbatim)

- `cache()` — memoize work that can be shared across Server Components in a single request
- `useMemo()` — Client Component component-instance memoization across renders
- `memo()` — prevent component re-render when props are unchanged

## Audit implication

`React.cache()` remains a valid React 19 primitive but it has a **narrow** scope:
- Server-only
- Request-scoped (not persistent)
- Function-keyed by argument value (`Object.is`)
- One cache per `cache()` call (so put it at module scope)

This is fundamentally different from Next.js 16's `'use cache'` directive, which targets persistent (cross-request) caching with serialized cache keys, `cacheLife`, and tag-based invalidation. The two should be presented as **separate primitives with overlapping but non-identical use cases**.
