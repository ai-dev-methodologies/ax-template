# `@ax/eslint-plugin-ax`

Custom ESLint rules for the [ax-template `practices-react/`
catalog](https://github.com/ai-dev-methodologies/ax-template/tree/main/practices-react).
Each rule maps 1:1 to a catalog rule whose `verification.rule_id` points here.
All rules are evidence-anchored to React 19 + Next.js 16 official docs and were
codex-reviewed before shipping.

## Status

`v0.1.0` — preview / pre-1.0. Public API may shift while the catalog stabilizes.

## Install

```bash
npm install --save-dev eslint @ax/eslint-plugin-ax
# (Until published: `npm install --save-dev file:./path/to/eslint-plugin-ax`)
```

Peer requirement: ESLint 9+ (flat config).

## Quick wire — `eslint.config.mjs`

```js
import axPlugin from '@ax/eslint-plugin-ax'

export default [
  {
    files: ['app/**/*.{ts,tsx,js,jsx}', 'src/**/*.{ts,tsx,js,jsx}'],
    plugins: { ax: axPlugin },
    rules: axPlugin.configs.recommended.rules,
  },
]
```

Or override individual rule severities:

```js
rules: {
  'ax/react-async-parallel': 'warn',
  'ax/no-broad-barrel-imports': ['warn', {
    expensivePackages: ['my-icon-lib'],
    excludeOptimized: ['lucide-react'],
  }],
  'ax/no-falsy-numeric-render': 'error',
  'ax/no-array-includes-in-loop': 'warn',
  'ax/no-array-mutate-on-state': 'error',
  'ax/prefer-functional-setstate': 'warn',
  'ax/no-inline-component-definition': 'error',
}
```

## Rules

| Rule | Catalog source | Severity |
|---|---|---|
| `ax/react-async-parallel` | `async-parallel.md` | warn |
| `ax/no-broad-barrel-imports` | `bundle-barrel-imports.md` | warn (configurable allowlist) |
| `ax/no-falsy-numeric-render` | `rendering-conditional-render.md` | **error** |
| `ax/no-array-includes-in-loop` | `js-set-map-lookups.md` | warn |
| `ax/no-array-mutate-on-state` | `js-tosorted-immutable.md` | **error** |
| `ax/prefer-functional-setstate` | `rerender-functional-setstate.md` | warn |
| `ax/no-inline-component-definition` | `rerender-no-inline-components.md` | **error** |

`error` is reserved for correctness bugs (visible falsy rendering, prop
mutation, full component remount). `warn` is for style / performance nudges
that may be intentional in some contexts.

## Design choices

These rules are **conservative** by default:

- **No false positives in Playwright tests.** Sibling-receiver and cross-
  receiver detection in `ax/react-async-parallel` treats `page.x() → page.y()`
  and `page.goto() → expect(page.locator(...))` as dependent — they mutate /
  consume shared state by convention.
- **MemberExpression property names are not free references.**
  `setMessage(res.message)` doesn't match the state name `message` because
  `message` is just a property on `res`.
- **`ax/no-broad-barrel-imports`** ships with a small default expensive-
  packages list and opt-in configuration. Most projects (especially Next.js 16,
  which auto-optimizes ~28 packages) won't see warnings unless they explicitly
  list unoptimized packages.

See `practices-react/pilot/pilot-report.md` and
`practices-react/pilot/external-validation.md` in the parent repository for
the full audit + validation trail.

## Test

```bash
npm test
```

7 RuleTester suites covering valid and invalid cases for every rule.

## License

Apache-2.0
