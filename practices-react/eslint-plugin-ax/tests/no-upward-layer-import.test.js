import { RuleTester } from 'eslint'
import test from 'node:test'
import rule from '../rules/no-upward-layer-import.js'

const tester = new RuleTester({
  languageOptions: { ecmaVersion: 2024, sourceType: 'module' },
})

const APP = '/repo/frontend/src/app/(auth)/login/page.tsx'
const FEATURE = '/repo/frontend/src/features/auth/login/LoginForm.tsx'
const SHARED = '/repo/frontend/src/components/ui/button.tsx'

test('ax/no-upward-layer-import — RuleTester suite', () => {
  tester.run('ax/no-upward-layer-import', rule, {
    valid: [
      // app -> features (downward) — fine
      { code: `import { LoginForm } from '@/features/auth/login'`, filename: APP },
      // app -> shared — fine
      { code: `import { Button } from '@/components/ui/button'`, filename: APP },
      // features -> shared (downward) — fine
      { code: `import { fmt } from '@/lib/format'`, filename: FEATURE },
      // shared -> shared — fine
      { code: `import { cn } from '@/lib/cn'`, filename: SHARED },
      // bare/external — not our concern
      { code: `import React from 'react'`, filename: SHARED },
    ],
    invalid: [
      // shared (components) -> features (upward) — FORBIDDEN
      {
        code: `import { LoginForm } from '@/features/auth/login'`,
        filename: SHARED,
        errors: [{ messageId: 'upwardImport' }],
      },
      // shared -> app (upward) — FORBIDDEN
      {
        code: `import { metadata } from '@/app/layout'`,
        filename: SHARED,
        errors: [{ messageId: 'upwardImport' }],
      },
      // features -> app (upward) — FORBIDDEN
      {
        code: `import { something } from '@/app/providers'`,
        filename: FEATURE,
        errors: [{ messageId: 'upwardImport' }],
      },
    ],
  })
})
