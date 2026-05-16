# Snapshot: React DOM — Resource Preloading APIs (React 19 stable)

- **source**: https://react.dev/reference/react-dom
- **role**: canonical-react
- **fetched_at**: 2026-05-16T00:00:00Z
- **react_version_observed**: 19 (stable)
- **via**: web-derived from React docs index

## APIs (verbatim names)

- `prefetchDNS(href)` — DNS resolution only
- `preconnect(href)` — DNS + TCP + TLS
- `preload(href, options)` — fetch a resource (font/style/script/image)
- `preloadModule(href, options)` — fetch an ES module
- `preinit(href, options)` — fetch AND execute a stylesheet/script
- `preinitModule(href, options)` — fetch AND execute an ES module

## When to use which (per React DOM table)

| API | Use case |
|---|---|
| prefetchDNS | Third-party domains you'll connect to later |
| preconnect | APIs/CDNs you'll fetch from immediately |
| preload | Critical resources needed for current page |
| preloadModule | JS modules for likely next navigation |
| preinit | Stylesheets/scripts that must execute early |
| preinitModule | ES modules that must execute early |

## Usage shape

Callable from Server Components, Client Components, layouts. Especially powerful from Server Components — the hint can be in the HTML before the client even receives the document.

## Audit implication

These are stable React 19 APIs. Catalog rule should require Server Component / layout use site for max benefit. Overuse penalty: hinting too many origins or preloading too many resources competes for the very bandwidth being optimized.
