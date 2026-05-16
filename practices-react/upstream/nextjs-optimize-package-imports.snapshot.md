# Snapshot: Next.js 16 — experimental.optimizePackageImports

- **source**: https://nextjs.org/docs/app/api-reference/config/next-config-js/optimizePackageImports
- **role**: canonical-nextjs
- **fetched_at**: 2026-05-13T00:00:00Z
- **doc_last_updated_at**: 2026-05-13 (per source frontmatter)
- **version_observed**: 16.2.6
- **via**: WebFetch

## Status notice (verbatim)

> "This feature is currently experimental and subject to change, it's not recommended for production. Try it out and share your feedback on GitHub."

## What it does (verbatim)

> "Some packages can export hundreds or thousands of modules, which can cause performance issues in development and production."

> "Adding a package to `experimental.optimizePackageImports` will only load the modules you are actually using, while still giving you the convenience of writing import statements with many named exports."

## Configuration example (verbatim)

```js
// next.config.js
module.exports = {
  experimental: {
    optimizePackageImports: ['package-name'],
  },
}
```

## Default-optimized package list (verbatim, as of 16.2.6)

The following libraries are optimized by default and do **not** need to be added:

- `lucide-react`
- `date-fns`
- `lodash-es`
- `ramda`
- `antd`
- `react-bootstrap`
- `ahooks`
- `@ant-design/icons`
- `@headlessui/react`
- `@headlessui-float/react`
- `@heroicons/react/20/solid`
- `@heroicons/react/24/solid`
- `@heroicons/react/24/outline`
- `@visx/visx`
- `@tremor/react`
- `rxjs`
- `@mui/material`
- `@mui/icons-material`
- `recharts`
- `react-use`
- `@material-ui/core`
- `@material-ui/icons`
- `@tabler/icons-react`
- `mui-core`
- `react-icons/*`
- `effect`
- `@effect/*`

## Audit implication

- Many libraries the Vercel react-best-practices `bundle-barrel-imports` rule lists as "affected" are already optimized by default in Next.js 16. The rule's "add to optimizePackageImports" instruction would be a **no-op** for these.
- The feature being experimental in a current stable Next release means production-grade catalogs cannot present it as the canonical fix without a caveat.
