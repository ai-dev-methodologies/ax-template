# Snapshot: React 19 — lazy()

- **source**: https://react.dev/reference/react/lazy
- **role**: canonical-react
- **fetched_at**: 2026-05-16T00:00:00Z
- **react_version_observed**: 19.2
- **via**: WebFetch

## What it does (verbatim)

> "Call lazy outside your components to declare a lazy-loaded React component"

```js
import { lazy } from 'react';
const MarkdownPreview = lazy(() => import('./MarkdownPreview.js'));
```

## Suspense required (verbatim)

> "Now that your component's code loads on demand, you also need to specify what should be displayed while it is loading. You can do this by wrapping the lazy component or any of its parents into a `<Suspense>` boundary"

## Critical caveat — module top-level only (verbatim)

> "Do NOT declare lazy components inside other components"

The doc shows the bad pattern explicitly with comments: "This will cause all state to be reset on re-renders".

## Module loading mechanic (verbatim)

> "This code relies on dynamic import(), which might require support from your bundler or framework."

## Audit implication

Vercel rules in the bundle-* family that present `next/dynamic` as the singular pattern are too Next-specific for a stack-agnostic catalog. React.lazy + Suspense is the portable equivalent and is GA across all bundlers since React 18.
