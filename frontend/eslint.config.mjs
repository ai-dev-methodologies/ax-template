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
