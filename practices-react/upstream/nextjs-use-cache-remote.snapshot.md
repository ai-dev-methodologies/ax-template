# Snapshot: Next.js 16 — 'use cache: remote'

- **source**: https://nextjs.org/docs/app/api-reference/directives/use-cache-remote
- **role**: canonical-nextjs
- **fetched_at**: 2026-05-13T00:00:00Z
- **doc_last_updated_at**: 2026-05-13
- **version_observed**: 16.2.6
- **via**: WebFetch

Version history: introduced in v16.0.0 with Cache Components.

## What it solves (verbatim)

> "use cache stores entries in-memory, which has inherent limitations:
> - Cache entries being evicted to make room for new ones
> - Memory constraints in your deployment environment
> - Cache not persisting across requests or server restarts"

> "The 'use cache: remote' directive lets you declaratively specify that a cached output should be stored in a remote cache instead of in-memory, providing durable caching shared across all server instances."

> "This comes with tradeoffs: infrastructure cost and network latency during cache lookups."

## When to use (verbatim — compelling scenarios)

> "- **Rate-limited APIs**: Your upstream service has rate limits or request quotas that you risk hitting
> - **Protecting slow backends**: Your database or API becomes a bottleneck under high traffic
> - **Expensive operations**: Database queries or computations that are costly to run repeatedly
> - **Flaky or unreliable services**: External services that occasionally fail or have availability issues"

## When NOT to use (verbatim)

> "- If you already have a server-side cache key-value store wrapping your data layer, use cache may be sufficient
> - If operations are already fast (< 50ms) due to proximity or local access, the remote cache lookup might not improve performance
> - If cache keys have mostly unique values per request (search filters, price ranges, user-specific parameters), cache utilization will be near-zero
> - If data changes frequently (seconds to minutes), cache hits will quickly go stale"

## Comparison (verbatim table)

| Feature | use cache | 'use cache: remote' | 'use cache: private' |
|---|---|---|---|
| Server-side caching | In-memory or cache handler | Remote cache handler | None |
| Cache scope | Shared across all users | Shared across all users | Per-client (browser) |
| Can access cookies/headers directly | No | No | Yes |
| Server cache utilization | May be low outside static shell | High (shared across instances) | N/A |
| Additional costs | None | Infrastructure (storage, network) | None |
| Latency impact | None | Cache handler lookup | None |

## Nesting rules (verbatim)

> "- Remote caches can be nested inside other remote caches ('use cache: remote')
> - Remote caches can be nested inside regular caches ('use cache')
> - Remote caches **cannot** be nested inside private caches ('use cache: private')
> - Private caches **cannot** be nested inside remote caches"

## Cache-key sizing principle (verbatim)

> "Be thoughtful about which values you include in cache keys. Each unique value creates a separate cache entry, reducing cache utilization."

> "The pattern is the same in both examples: find the dimension with fewer unique values (category vs. price, language vs. user ID), cache on that dimension, and filter or select the rest in memory."

## Platform support (verbatim)

| Deployment | Supported |
|---|---|
| Node.js server | Yes |
| Docker | Yes |
| Static export | No |
| Adapters | Yes |

## Configuration

Configured via `cacheHandlers` in next.config; hosting providers typically provide the handler.

## Audit implication

`'use cache: remote'` is the **shared durable cache** layer for content deferred to request time. It is NOT a default; it should be reached for ONLY when:
- the work is genuinely expensive or rate-limited,
- the cache keys have few unique values (high hit rate),
- the cost of cache misses outweighs the cost of remote storage + network latency.

Manual LRU (sibling rule `server-cache-lru`) becomes the fallback when Cache Components is unavailable or when in-process best-effort caching is the explicit intent.
