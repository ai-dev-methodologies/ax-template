import { RuleTester } from 'eslint'
import test from 'node:test'
import rule from '../rules/no-broad-barrel-imports.js'

const tester = new RuleTester({
  languageOptions: { ecmaVersion: 2024, sourceType: 'module' },
})

test('ax/no-broad-barrel-imports — RuleTester suite', () => {
  tester.run('ax/no-broad-barrel-imports', rule, {
    valid: [
      // Subpath import — public exports map
      "import get from 'lodash/get'",
      // Default import only — not a barrel pull-through
      "import _ from 'lodash'",
      // Non-expensive package
      "import { Foo } from 'my-app-utils'",
      // Excluded (auto-optimized) via options
      {
        code: "import { Check, X } from 'lucide-react'",
        options: [
          {
            expensivePackages: ['lucide-react'],
            excludeOptimized: ['lucide-react'],
          },
        ],
      },
    ],
    invalid: [
      {
        code: "import { map, reduce } from 'lodash'",
        errors: [{ messageId: 'broadBarrelImport' }],
      },
      {
        code: "import { pipe, compose } from 'ramda'",
        errors: [{ messageId: 'broadBarrelImport' }],
      },
      // Custom expensive set
      {
        code: "import { Button } from '@my-org/giant-ui'",
        options: [{ expensivePackages: ['@my-org/giant-ui'] }],
        errors: [{ messageId: 'broadBarrelImport' }],
      },
    ],
  })
})
