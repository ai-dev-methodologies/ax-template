// FIXTURE (P3-81) — must PASS: every non-excluded plugin rule is wired as a
// real key, and the excluded rule's name appears IN QUOTES inside a comment
// that explains the exclusion. Under the pre-P3-81 whole-file substring check
// that quoted mention tripped the CONTRADICTION arm (exit 1); with key parsing
// a comment is not a wiring, so this exits 0.
//
// Together with fail_comment_fooled this pins both directions of the change:
// a comment can no longer create a wiring, nor destroy one.

import axPlugin from '@ax/eslint-plugin-ax'

export default [
  {
    files: ['**/*.{ts,tsx}'],
    plugins: { ax: axPlugin },
    rules: {
      'ax/react-async-parallel': 'error',
      'ax/no-god-route': 'warn',
      // INTENTIONALLY never wired here — the rule is scoped to files under an
      // apps/ segment, and the trees this config lints ARE the shared catalog
      // rather than a consumer of it. Do NOT uncomment:
      // 'ax/no-app-local-ui-primitives': 'error',
    },
  },
]
