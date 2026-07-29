// FIXTURE (P3-81) — must FAIL: a rule that exists in the plugin is wired only
// inside a COMMENT, so it never lints anything, yet the pre-P3-81 whole-file
// `grep -qF "'ax/no-god-route'"` reported it as wired.
//
// Expected: exit 1, MISSING ax/no-god-route.
// Under the old substring check this fixture exited 0.

import axPlugin from '@ax/eslint-plugin-ax'

export default [
  {
    files: ['**/*.{ts,tsx}'],
    plugins: { ax: axPlugin },
    rules: {
      'ax/react-async-parallel': 'error',
      // temporarily disabled while the L4 verticals are migrated:
      // 'ax/no-god-route': 'warn',
    },
  },
]
