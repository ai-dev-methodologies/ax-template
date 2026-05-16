# Snapshot: Next.js 16 — Lazy Loading guide

- **source**: https://nextjs.org/docs/app/guides/lazy-loading
- **role**: canonical-nextjs
- **fetched_at**: 2026-05-13T00:00:00Z
- **doc_last_updated_at**: 2026-05-13
- **version_observed**: 16.2.6
- **via**: WebFetch

## Two patterns (verbatim)

> "There are two ways you can implement lazy loading in Next.js:
> 1. Using Dynamic Imports with `next/dynamic`
> 2. Using `React.lazy()` with Suspense"

## next/dynamic is composite of React.lazy + Suspense (verbatim)

> "`next/dynamic` is a composite of `React.lazy()` and Suspense. It behaves the same way in the app and pages directories to allow for incremental migration."

## Server vs Client Component lazy semantics (verbatim)

> "By default, Server Components are automatically code split, and you can use streaming to progressively send pieces of UI from the server to the client. Lazy loading applies to Client Components."

> "When a Server Component dynamically imports a Client Component, automatic code splitting is currently NOT supported."

## ssr:false constraint (verbatim)

> "`ssr: false` option will only work for Client Components, move it into Client Components ensure the client code-splitting working properly."

> "`ssr: false` is not allowed with `next/dynamic` in Server Components. Please move it into a Client Component."

## External library on-demand (verbatim)

> "External libraries can be loaded on demand using the import() function."

Example: `const Fuse = (await import('fuse.js')).default` inside an event handler.

## Magic comments

- `webpackIgnore` / `turbopackIgnore` — skip bundling, runtime-only modules.
- `turbopackOptional` — suppress build errors when a module might not exist.
- Magic comments do NOT work with static `import` statements.

## Audit implication

Bundle-family rules should:
1. Distinguish Client Component lazy (the target) from Server Component (already auto-split).
2. Use React.lazy + Suspense as the portable default; next/dynamic when Next-specific features are needed.
3. Recognize that external SDK module loading is via bare `import()`, not via `next/dynamic` (which is for components).
