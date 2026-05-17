// frontend/eslint.config.mjs — ESLint v9 flat config.
//
// Wires @ax/eslint-plugin-ax (the practices-react/eslint-plugin-ax local plugin)
// into the frontend. The plugin's 7 rules pair 1:1 with practices-react/rules/
// entries whose `verification.rule_id` points here.

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

export default [
  {
    ignores: [
      'dist',
      'node_modules',
      'playwright-report',
      'test-results',
      'coverage',
      '.next',
    ],
  },
  {
    files: ['src/**/*.{js,jsx}', 'tests/**/*.{js,jsx}'],
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
    files: ['src/**/*.{ts,tsx}', 'middleware.ts', 'tests/**/*.{ts,tsx}'],
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
]
