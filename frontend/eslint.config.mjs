// frontend/eslint.config.mjs — ESLint v9 flat config.
//
// Wires @ax/eslint-plugin-ax (the practices-react/eslint-plugin-ax local plugin)
// into the frontend. The plugin's rules pair 1:1 with practices-react/rules/
// entries whose `verification.rule_id` points here.
//
// Surfaces linted:
//   - src/**, tests/**       — the root web-shell app (predates the catalog).
//   - packages/**            — the shared catalog (@ax/ui / @ax/blocks / @ax/core).
//   - apps/**                — per-persona apps. These additionally get
//                              ax/no-app-local-ui-primitives at ERROR: they MUST
//                              reuse the shared catalog, never fork primitives.

import globals from 'globals'
import tsParser from '@typescript-eslint/parser'
import axPlugin from '@ax/eslint-plugin-ax'

const sharedRules = {
  'ax/react-async-parallel': 'warn',
  'ax/no-broad-barrel-imports': 'warn',
  'ax/no-falsy-numeric-render': 'error',
  'ax/no-array-includes-in-loop': 'warn',
  'ax/no-array-mutate-on-state': 'error',
  'ax/prefer-functional-setstate': 'warn',
  'ax/no-inline-component-definition': 'error',
  // Frontend decomposition (TIER-0, spec 2026-06-08-frontend-decomposition): feature-slice
  // isolation + single-direction layer imports + published-API-via-barrel.
  'ax/no-cross-feature-deep-import': 'error',
  'ax/no-upward-layer-import': 'error',
  'ax/no-feature-internal-import': 'error',
  // TIER-1 (Phase 2): route-thin core at error.
  // no-god-route + no-server-state-in-local-state shipped advisory (warn) per the
  // ralplan codex critic (line-count is a gameable proxy; state-boundary is a heuristic).
  // BACKLOG P2-2 promotion (2026-06-24): the measurement gate is now met — after the
  // P2-12 decomposition wave all 6 reference apps lint at 0 violations under
  // `eslint . --max-warnings 0`, proving both rules are satisfiable with real
  // decomposition rather than gaming. Promoted to error so a regression (a new god
  // route / server-state-in-useState) HARD-FAILS rather than silently accruing.
  'ax/no-route-client-data-fetching': 'error',
  'ax/no-server-state-in-local-state': 'error',
  'ax/no-god-route': 'error',
}

// Per-persona apps must reuse the shared catalog — enforced only under apps/**.
// (The rule is also internally scoped to apps/ paths, so this is belt-and-braces.)
const appRules = {
  ...sharedRules,
  'ax/no-app-local-ui-primitives': 'error',
}

export default [
  {
    ignores: [
      'dist',
      'node_modules',
      'playwright-report',
      'test-results',
      'coverage',
      '.next',
      // Per-persona app build artifacts (apps/*/.next) are generated output, not
      // source — never lint them. Bare '.next' only matches the repo-root dir.
      '**/.next/**',
    ],
  },
  {
    files: [
      'src/**/*.{js,jsx}',
      'tests/**/*.{js,jsx}',
      'packages/**/*.{js,jsx}',
    ],
    languageOptions: {
      ecmaVersion: 2024,
      sourceType: 'module',
      globals: { ...globals.browser, ...globals.es2022, ...globals.node },
      parserOptions: { ecmaFeatures: { jsx: true } },
    },
    plugins: { ax: axPlugin },
    rules: sharedRules,
  },
  {
    files: [
      'src/**/*.{ts,tsx}',
      'middleware.ts',
      'tests/**/*.{ts,tsx}',
      'packages/**/*.{ts,tsx}',
    ],
    languageOptions: {
      parser: tsParser,
      ecmaVersion: 2024,
      sourceType: 'module',
      globals: { ...globals.browser, ...globals.es2022, ...globals.node },
      parserOptions: { ecmaFeatures: { jsx: true } },
    },
    plugins: { ax: axPlugin },
    rules: sharedRules,
  },
  // Per-persona apps — shared rules PLUS the enforced-reuse rule at error.
  {
    files: ['apps/**/*.{js,jsx}'],
    languageOptions: {
      ecmaVersion: 2024,
      sourceType: 'module',
      globals: { ...globals.browser, ...globals.es2022, ...globals.node },
      parserOptions: { ecmaFeatures: { jsx: true } },
    },
    plugins: { ax: axPlugin },
    rules: appRules,
  },
  {
    files: ['apps/**/*.{ts,tsx}'],
    languageOptions: {
      parser: tsParser,
      ecmaVersion: 2024,
      sourceType: 'module',
      globals: { ...globals.browser, ...globals.es2022, ...globals.node },
      parserOptions: { ecmaFeatures: { jsx: true } },
    },
    plugins: { ax: axPlugin },
    rules: appRules,
  },
]
