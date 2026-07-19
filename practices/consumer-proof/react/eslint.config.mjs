// Consumer-proof flat config (ESLint 9+).
//
// This is the ENTIRE integration a consumer needs: install @ax/eslint-plugin-ax,
// register it, turn the rules on. ZERO path coupling — the rules fire on arbitrary
// React/TSX anywhere. We enable exactly the 4 rules under test as `error` so that a
// clean fixture is silent unless one of THESE rules genuinely fires (isolating the
// proof from unrelated catalog rules).
//
// Parser mirrors the repo's own frontend/eslint.config.mjs: @typescript-eslint/parser
// with the JSX language feature, so realistic AI-generated TSX (type annotations,
// generics) parses.

import tsParser from '@typescript-eslint/parser'
import globals from 'globals'
import axPlugin from '@ax/eslint-plugin-ax'

export default [
  {
    ignores: ['node_modules', '**/node_modules/**'],
  },
  {
    files: ['**/*.{ts,tsx,js,jsx}'],
    languageOptions: {
      parser: tsParser,
      ecmaVersion: 2024,
      sourceType: 'module',
      globals: { ...globals.browser, ...globals.es2022, ...globals.node },
      parserOptions: { ecmaFeatures: { jsx: true } },
    },
    plugins: { ax: axPlugin },
    rules: {
      'ax/no-array-mutate-on-state': 'error',
      'ax/prefer-functional-setstate': 'error',
      'ax/no-god-route': 'error',
      'ax/no-server-state-in-local-state': 'error',
    },
  },
]
