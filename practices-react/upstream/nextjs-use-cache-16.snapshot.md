---
snapshot_id: nextjs-use-cache-16
source: "https://nextjs.org/docs/app/api-reference/directives/use-cache"
fetched_at: "2026-05-17T13:00:00Z"
version_observed: "next@16.2.6"
via: WebFetch
sha: "c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
---

# Next.js 16 — `use cache` Directive

Source: https://nextjs.org/docs/app/api-reference/directives/use-cache  
Version: next@16.2.6 · lastUpdated: 2026-05-13

## What is `use cache`?

A caching directive that marks a route, React component, or function as cacheable. Enabled via
the `cacheComponents` flag in `next.config.ts`.

```ts
// next.config.ts
const nextConfig: NextConfig = { cacheComponents: true }
```

## Usage Levels

```tsx
// File level — all exports cached
'use cache'
export default async function Page() { /* ... */ }

// Component level
export async function MyComponent() {
  'use cache'
  return <></>
}

// Function level
export async function getData() {
  'use cache'
  const data = await fetch('/api/data')
  return data
}
```

## Cache Keys

Generated from: Build ID + Function ID + Serializable arguments + HMR refresh hash.
Variables captured from outer scopes are automatically included in the cache key.

## Supported Serializable Types

**Arguments:** primitives, plain objects, arrays, Dates, Maps, Sets, TypedArrays.
**Return values:** same as arguments + JSX elements.
**Unsupported:** class instances, functions (except pass-through), Symbols, WeakMaps.

## Cache Lifetime & Revalidation

Default profile: stale=5min (client), revalidate=15min (server), expire=never.

```tsx
import { cacheLife } from 'next/cache'
export async function getData() {
  'use cache'
  cacheLife('hours')     // built-in profile
  return fetch('/api/data')
}
```

On-demand invalidation via `cacheTag` + `updateTag`:

```tsx
import { cacheTag } from 'next/cache'
async function getProducts() {
  'use cache'
  cacheTag('products')
  return fetch('/api/products')
}
// In a server action:
import { updateTag } from 'next/cache'
updateTag('products')   // invalidates all 'products' caches
```

## Key Constraints

- Cannot directly access `cookies()`, `headers()`, or `searchParams` inside `use cache` scope.
- Read them outside, pass as arguments.
- Static export: NOT supported.
- `React.cache` isolation: values from outside scope are not visible inside `use cache`.

## Version History

| Version | Changes |
|---------|---------|
| v16.0.0 | `"use cache"` enabled with Cache Components feature |
| v15.0.0 | `"use cache"` introduced as experimental |
