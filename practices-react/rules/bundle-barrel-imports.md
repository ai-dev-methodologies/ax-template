---
title: Avoid expensive package barrel imports when the bundler does not already optimize them
impact: HIGH
impactDescription: "Reduces dev startup/prebundle time, build time, and cold-start latency by avoiding load of thousands of unused re-exports. Frameworks may already auto-optimize the most common offenders — measure before broad rewrites."
tags:
  - bundle
  - imports
  - tree-shaking
  - barrel-files
  - performance
  - react
  - nextjs
  - vite
applicable_to:
  - react
  - nextjs
  - vite
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-BUNDLE-001"
verification:
  type: eslint
  rule_id: "ax/no-broad-barrel-imports"
  status: shipped
  notes: "Custom ESLint rule planned: flag `import { ... } from 'X'` for X in a configurable allowlist of known-expensive packages, with an escape hatch for packages already auto-optimized by the project's bundler. Until shipped: peer-review checkpoint."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
    notes: "Vercel-measured 200-800ms cost is real but specific to their benchmarks; Codex flagged universalization of the numbers as misleading. Vercel's `dist/esm/...` 'Correct' example is private to the package and can break across versions."
  freshness:
    status: partially-stale
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Next.js 16.2.6 auto-optimizes ~28 packages by default — many libraries listed in the Vercel rule are no-ops in current Next.js. optimizePackageImports remains experimental in 16.2.6."
  completeness:
    status: complete
    amendments:
      - "Distinguish dev startup/prebundle cost from production bundle size"
      - "List Next.js 16.2.6 default-optimized packages so no-op config is avoided"
      - "Flag experimental status of optimizePackageImports"
      - "Add Vite/Rollup/esbuild guidance"
      - "Warn against private dist/... deep imports; prefer documented subpath exports"
      - "Add 'measure first' advice before broad rewrites"
      - "Document modularizeImports as legacy/custom-bundler fallback"
  gap_check:
    status: complete
    note: "Vite-specific handling folded in as conditional section; private-deep-import caveat surfaced as a first-class concern. No sibling rule needed."
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: react-best-practices (rule: bundle-barrel-imports)"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/bundle-barrel-imports.md"
    role: "seed"
  - id: nextjs-optimize-package-imports
    title: "Next.js 16 — experimental.optimizePackageImports"
    url: "https://nextjs.org/docs/app/api-reference/config/next-config-js/optimizePackageImports"
    version: "16.2.6"
    fetched: "2026-05-13"
    role: "canonical-nextjs"
  - id: vercel-blog-barrel-imports
    title: "How we optimized package imports in Next.js (Vercel engineering blog)"
    url: "https://vercel.com/blog/how-we-optimized-package-imports-in-next-js"
    role: "benchmark-evidence"
evidence:
  - upstream_id: vercel-react-best-practices
    section: "bundle-barrel-imports"
    quote: "Popular icon and component libraries can have up to 10,000 re-exports in their entry file."
  - upstream_id: nextjs-optimize-package-imports
    section: "optimizePackageImports"
    quote: "Adding a package to experimental.optimizePackageImports will only load the modules you are actually using, while still giving you the convenience of writing import statements with many named exports."
  - upstream_id: nextjs-optimize-package-imports
    section: "Experimental warning"
    quote: "This feature is currently experimental and subject to change, it's not recommended for production."
  - source_type: external
    citation: "Vercel engineering blog — How we optimized package imports in Next.js (200-800ms import cost measurements for React packages)"
    url: "https://vercel.com/blog/how-we-optimized-package-imports-in-next-js"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=high"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Core barrel-cost issue is real; numbers are bundler/package/version specific"
    - "Next 16 default-optimization list invalidates blanket CRITICAL impact"
    - "Distinguish prod bundle size vs dev prebundle/startup"
    - "One rule body sufficient if conditional guidance is included"
  amendments_required:
    - "Replace absolute benchmark numbers with 'measured for some packages; varies'"
    - "List Next.js 16 default-optimized packages explicitly"
    - "Note optimizePackageImports experimental status"
    - "Add Vite/Rollup/esbuild conditional"
    - "Warn against private dist/* imports; prefer documented exports"
    - "Add 'measure first' counsel"
    - "Downgrade impact CRITICAL → HIGH"
sibling_rules:
  - bundle-dynamic-imports
---

## Avoid expensive package barrel imports when the bundler does not already optimize them

**Impact: HIGH — Reduces dev startup/prebundle time, build time, and cold-start latency by avoiding load of thousands of unused re-exports. Modern frameworks may already auto-optimize the most common offenders — measure before broad rewrites.**

Barrel files (`index.js` files that do `export * from './...'`) are an ergonomic
authoring pattern. Their cost depends on the bundler. Some bundlers analyze the
entire export graph at import time even when only a few symbols are used,
inflating dev startup and (occasionally) production bundles. Vercel measured
**200–800 ms** of import overhead for some React packages with up to 10,000
re-exports per barrel; results vary by bundler, package, and version. Treat the
numbers as evidence the cost exists, not as a universal multiplier.

> **Note on the impact downgrade.** The seed source labels this CRITICAL. Codex
> review surfaced that Next.js 16.2.6 already default-optimizes ~28 of the
> commonly-cited offenders, so the cost a real project sees today is workload-
> and toolchain-specific. Downgraded to HIGH; escalate back to CRITICAL only
> when measurement shows large regression.

### Decision tree (measure first, then act)

1. **Run the bundler's import-cost diagnostic** (Next.js `analyze`, Vite
   `build --report`, webpack-bundle-analyzer, `import-cost` plugin). If the
   library is not on a hot path, stop.
2. **Check if your bundler already optimizes it.** Next.js 16.2.6 default-
   optimizes these without config:
   `lucide-react`, `date-fns`, `lodash-es`, `ramda`, `antd`, `react-bootstrap`,
   `ahooks`, `@ant-design/icons`, `@headlessui/react`, `@headlessui-float/react`,
   `@heroicons/react/{20/solid,24/solid,24/outline}`, `@visx/visx`,
   `@tremor/react`, `rxjs`, `@mui/material`, `@mui/icons-material`, `recharts`,
   `react-use`, `@material-ui/core`, `@material-ui/icons`, `@tabler/icons-react`,
   `mui-core`, `react-icons/*`, `effect`, `@effect/*`. **For these, no action
   needed.**
3. **Otherwise, prefer a documented public subpath import** if the package
   exposes one via its `exports` field. Do **not** reach into `dist/...` private
   paths — those break on minor version bumps and may stop working when the
   package adds an `exports` map.
4. **If no documented subpath exists**, add the package to
   `experimental.optimizePackageImports` (Next.js) or rely on Vite/Rollup
   tree-shaking. Note that `optimizePackageImports` remains **experimental** in
   Next.js 16.2.6 — not recommended for production without validation.

### Correct patterns

**Default-optimized in Next.js — keep the ergonomic barrel import:**

```tsx
// No action needed. Next 16 default-optimizes lucide-react.
import { Check, X, Menu } from 'lucide-react'
```

**Library exposes a documented subpath via `exports`:**

```tsx
// Public, version-stable subpath as documented by the package.
import Button from '@mui/material/Button'
import TextField from '@mui/material/TextField'
```

**Add to optimizePackageImports for uncommon offenders (Next.js):**

```js
// next.config.js
module.exports = {
  experimental: {
    optimizePackageImports: ['some-niche-icon-pack'],
  },
}
```

### Incorrect patterns

**Reaching into private internal paths:**

```tsx
// BAD: dist/esm/* is not part of the package's public API.
// Will break on package upgrades; may stop resolving when the package adds
// an "exports" field that hides internals.
import Check from 'lucide-react/dist/esm/icons/check'
```

**Adding a no-op config entry:**

```js
// BAD: lucide-react and @mui/material are already in Next 16's default list.
// This entry does nothing and creates the false impression of intervention.
module.exports = {
  experimental: {
    optimizePackageImports: ['lucide-react', '@mui/material'],
  },
}
```

### Vite / Rollup / esbuild

- Vite uses Rollup for production builds — tree-shaking is generally effective
  for ESM packages. The cost barrel imports impose on a Vite app is mostly in
  **dev pre-bundling** (esbuild dependency optimization), not production bundle
  size.
- If dev startup is slow, profile with `vite dev --debug=resolve` and consider
  documented subpath imports for the heaviest offenders.
- For CommonJS packages, tree-shaking is less reliable; lean toward documented
  subpath imports or `modularizeImports`-style transforms (custom plugin).
- Do not import deep private paths solely to "help" Vite — the cost is small in
  production and you trade for upgrade fragility.

### Legacy / custom bundlers

`modularizeImports` is the pre-Next-13.5 transform that remaps named imports to
per-module imports at build time. Use it when:

- the project uses a non-Next bundler that does not auto-optimize, and
- the package does not expose documented subpaths, and
- profiling shows the cost matters.

### Verification

- Static check (planned): custom ESLint rule `ax/no-broad-barrel-imports`. Maintains
  an allowlist of packages already optimized by the project's bundler (read from
  `eslint.config.js` plugin options). Flags `import { ... } from 'X'` only when X
  is on the project's "known expensive, not auto-optimized" list and the import
  is not from a documented subpath.
- Manual: bundle-analyzer reports for every commit that adds a new top-level
  dependency.

Sources for this rule:

- [Vercel agent-skills: bundle-barrel-imports](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/bundle-barrel-imports.md)
- [Next.js 16 — optimizePackageImports](https://nextjs.org/docs/app/api-reference/config/next-config-js/optimizePackageImports)
- [Vercel engineering blog — How we optimized package imports in Next.js](https://vercel.com/blog/how-we-optimized-package-imports-in-next-js)
