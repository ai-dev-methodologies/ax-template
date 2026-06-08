import { RuleTester } from 'eslint'
import test from 'node:test'
import rule from '../rules/no-feature-internal-import.js'

const tester = new RuleTester({
  languageOptions: { ecmaVersion: 2024, sourceType: 'module' },
})

const APP = '/repo/frontend/src/app/(auth)/login/page.tsx'
const COMPONENT = '/repo/frontend/src/components/nav/Nav.tsx'
const FEATURE = '/repo/frontend/src/features/payment/panel/Panel.tsx'

test('ax/no-feature-internal-import — RuleTester suite', () => {
  tester.run('ax/no-feature-internal-import', rule, {
    valid: [
      // app importing a slice BARREL — the correct public access
      { code: `import { LoginForm } from '@/features/auth/login'`, filename: APP },
      // app importing a feature-level barrel — fine
      { code: `import { Auth } from '@/features/auth'`, filename: APP },
      // explicit index — fine
      { code: `import { X } from '@/features/auth/login/index'`, filename: APP },
      // non-feature -> shared/external — not this rule's concern
      { code: `import { Button } from '@/components/ui/button'`, filename: APP },
      // feature importer is governed by no-cross-feature-deep-import, not this rule
      { code: `import { x } from '@/features/auth/login/internal'`, filename: FEATURE },
    ],
    invalid: [
      // app reaching past the slice barrel into internals — FORBIDDEN
      {
        code: `import { LoginForm } from '@/features/auth/login/LoginForm'`,
        filename: APP,
        errors: [{ messageId: 'featureInternal' }],
      },
      // a shared component reaching into a feature slice internal — FORBIDDEN
      {
        code: `import { capture } from '@/features/payment/panel/capture'`,
        filename: COMPONENT,
        errors: [{ messageId: 'featureInternal' }],
      },
    ],
  })
})
