// frontend/eslint.own-blocks.config.mjs — ESLint v9 flat config for the
// CATALOG'S OWN shipped components (templates/L2 + templates/L0).
//
// FDW1 (frontend dogfood) found that frontend/eslint.config.mjs globs only
// src/** and tests/**, so the catalog NEVER lints its own blocks — and
// column-picker.tsx shipped an ax/no-array-includes-in-loop violation that
// stayed invisible until a fork copied it into src/. "The catalog must eat
// its own dogfood." This config lints the template trees with EVERY ax rule
// at error level; lint_own_blocks_guard.sh runs it with --max-warnings 0 so
// even the warn-level rules block.
//
// Lives in frontend/ (not practices-react/evals/) so its plugin/parser
// imports resolve from frontend/node_modules. The guard passes the template
// file paths explicitly; the single catch-all config object below applies to
// whatever is passed.

import globals from 'globals'
import tsParser from '@typescript-eslint/parser'
import axPlugin from '@ax/eslint-plugin-ax'

export default [
  {
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      parser: tsParser,
      ecmaVersion: 2024,
      sourceType: 'module',
      globals: { ...globals.browser, ...globals.es2022, ...globals.node },
      parserOptions: { ecmaFeatures: { jsx: true } },
    },
    plugins: { ax: axPlugin },
    rules: {
      'ax/react-async-parallel': 'error',
      'ax/no-broad-barrel-imports': 'error',
      'ax/no-falsy-numeric-render': 'error',
      'ax/no-array-includes-in-loop': 'error',
      'ax/no-array-mutate-on-state': 'error',
      'ax/prefer-functional-setstate': 'error',
      'ax/no-inline-component-definition': 'error',
      'ax/no-cross-feature-deep-import': 'error',
      'ax/no-upward-layer-import': 'error',
      'ax/no-feature-internal-import': 'error',
      'ax/no-route-client-data-fetching': 'error',
      'ax/no-server-state-in-local-state': 'warn',
      'ax/no-god-route': 'warn',
      'ax/no-caller-identity-from-props': 'error',
      // ax/no-app-local-ui-primitives is INTENTIONALLY excluded (P3-55) — it is
      // scoped to files under an apps/ segment (flags an app re-implementing a
      // catalog primitive locally). templates/L0-L4, what this config lints,
      // ARE the shared catalog itself, not a consumer of it — the rule would
      // flag the canonical primitives (Button/Input/Card/...) as if they were
      // local reimplementations of themselves. Category mismatch, not a gap.
      // lint_own_blocks_guard.sh mechanizes this exclusion as an explicit
      // allowlist entry (diffed against practices-react/eslint-plugin-ax/rules/).
    },
  },
]
