# Snapshot: Next.js 16 — 'use cache' directive

- **source**: https://nextjs.org/docs/app/api-reference/directives/use-cache
- **role**: canonical-nextjs
- **fetched_at**: 2026-05-13T00:00:00Z
- **doc_last_updated_at**: 2026-05-13 (per source frontmatter)
- **version_observed**: 16.2.6
- **via**: WebFetch

## Status / version history (verbatim)

| Version | Changes |
|---------|---------|
| v16.0.0 | "use cache" is enabled with the Cache Components feature. |
| v15.0.0 | "use cache" is introduced as an experimental feature. |

`use cache` is a Cache Components feature; enable it via `cacheComponents: true` in `next.config.ts`.

## Apply level (verbatim)

> "The use cache directive allows you to mark a route, React component, or a function as cacheable. It can be used at the top of a file to indicate that all exports in the file should be cached, or inline at the top of function or component to cache the return value."

Three application sites:
- File level (top of file → all exported async functions cached)
- Component level (top of async component body)
- Function level (top of async function body)

## Cache key composition (verbatim)

A cache entry's key is generated using a serialized version of its inputs, which includes:

1. **Build ID** — Unique per build, changing this invalidates all cache entries
2. **Function ID** — A secure hash of the function's location and signature in the codebase
3. **Serializable arguments** — Props (for components) or function arguments
4. **HMR refresh hash** (development only) — Invalidates cache on hot module replacement

> "When a cached function references variables from outer scopes, those variables are automatically captured and bound as arguments, making them part of the cache key."

## Serialization constraints

- Arguments use **React Server Component** serialization (more restrictive)
- Return values use **React Client Component** serialization
- Supported: primitives, plain objects, arrays, Dates, Maps, Sets, TypedArrays, ArrayBuffers, React elements as pass-through
- Unsupported: class instances, functions (except pass-through), Symbols, WeakMaps, WeakSets, URL instances

## Runtime APIs forbidden inside 'use cache' (verbatim)

> "Cached functions and components cannot directly access runtime APIs like cookies(), headers(), or searchParams. Instead, read these values outside the cached scope and pass them as arguments."

## React.cache isolation (verbatim, CRITICAL for catalog rule split)

> "React.cache operates in an isolated scope inside use cache boundaries. Values stored via React.cache outside a use cache function are not visible inside it."

> "This isolation ensures cached functions have predictable, self-contained behavior. To pass data into a use cache scope, use function arguments instead."

## Default cache profile (verbatim)

| Profile field | Default |
|---|---|
| stale (client-side) | 5 minutes |
| revalidate (server-side) | 15 minutes |
| expire | Never expires by time |

Customizable via `cacheLife('hours' | 'minutes' | …)`.

## On-demand invalidation surface

- `cacheTag('tag-name')` inside the cached function
- `updateTag('tag-name')` from a Server Action / Route Handler
- `revalidateTag('tag-name')` for revalidation

## Runtime caching considerations

| Environment | Runtime caching behavior |
|---|---|
| Serverless | Cache entries typically don't persist across requests |
| Self-hosted | Cache entries persist across requests; control with `cacheMaxMemorySize` |

For durable shared caching across instances/requests: `use cache: remote` (platform-provided cache handler — Redis/KV/etc.) — incurs network roundtrip + platform fees.

For compliance / can't refactor runtime values to args: `use cache: private`.

## Platform support (verbatim)

| Deployment | Supported |
|---|---|
| Node.js server | Yes |
| Docker container | Yes |
| Static export | No |
| Adapters | Platform-specific |

## Build-hang anti-pattern (verbatim)

> "If your build hangs, you're accessing Promises that resolve to uncached or runtime data, created outside a use cache boundary. The cached function waits for data that can't resolve during the build, causing a timeout after 50 seconds."

## Audit implication

`'use cache'` is the **primary** Next.js 16 caching primitive. It is distinct from React.cache() and they isolate from each other inside cached scopes. A catalog rule for `React.cache()` alone misses the framework-recommended primitive.
