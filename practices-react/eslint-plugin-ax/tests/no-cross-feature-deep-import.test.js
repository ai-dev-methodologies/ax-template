import { RuleTester } from 'eslint'
import test from 'node:test'
import rule from '../rules/no-cross-feature-deep-import.js'

const tester = new RuleTester({
  languageOptions: { ecmaVersion: 2024, sourceType: 'module' },
})

const BILLING = '/repo/frontend/src/features/billing/checkout/CheckoutForm.tsx'

test('ax/no-cross-feature-deep-import — RuleTester suite', () => {
  tester.run('ax/no-cross-feature-deep-import', rule, {
    valid: [
      // shared kernel — fine
      { code: `import { Button } from '@/components/ui/button'`, filename: BILLING },
      { code: `import { fmt } from '@/lib/format'`, filename: BILLING },
      { code: `import { Button } from '@ax/ui'`, filename: BILLING },
      // own feature internals — fine
      { code: `import { total } from '@/features/billing/checkout/total'`, filename: BILLING },
      // cross-feature BARREL import — allowed
      { code: `import { Payment } from '@/features/payment'`, filename: BILLING },
      { code: `import { PaymentPanel } from '@/features/payment/panel'`, filename: BILLING },
      // non-feature importer — not this rule's concern
      { code: `import { x } from '@/features/payment/panel/internal'`, filename: '/repo/frontend/src/app/page.tsx' },
    ],
    invalid: [
      // feature billing reaching into feature payment internals — FORBIDDEN
      {
        code: `import { capture } from '@/features/payment/panel/capture'`,
        filename: BILLING,
        errors: [{ messageId: 'crossFeatureDeep' }],
      },
      {
        code: `import { x } from '@/features/payment/panel/internal/x'`,
        filename: BILLING,
        errors: [{ messageId: 'crossFeatureDeep' }],
      },
    ],
  })
})
