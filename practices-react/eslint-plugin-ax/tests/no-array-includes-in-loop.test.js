import { RuleTester } from 'eslint'
import test from 'node:test'
import rule from '../rules/no-array-includes-in-loop.js'

const tester = new RuleTester({
  languageOptions: { ecmaVersion: 2024, sourceType: 'module' },
})

test('ax/no-array-includes-in-loop — RuleTester suite', () => {
  tester.run('ax/no-array-includes-in-loop', rule, {
    valid: [
      // Using a Set — correct pattern
      `
        const allowedIds = new Set(['a', 'b'])
        items.filter((it) => allowedIds.has(it.id))
      `,
      // .includes on the iterated array itself (unusual but not our target)
      `
        items.filter((it) => items.includes(it.parent))
      `,
      // Non-iterator method
      `
        const x = allowed.includes(value)
      `,
    ],
    invalid: [
      {
        code: `
          const allowedIds = ['a', 'b']
          items.filter((it) => allowedIds.includes(it.id))
        `,
        errors: [{ messageId: 'hotLookupInLoop' }],
      },
      {
        code: `
          const allowed = ['a', 'b']
          orders.map((o) => ({ ...o, user: users.find((u) => u.id === o.userId) }))
        `,
        errors: [{ messageId: 'hotLookupInLoop' }],
      },
      {
        code: `
          orders.forEach((o) => { if (allowed.indexOf(o.id) > -1) doIt(o) })
        `,
        errors: [{ messageId: 'hotLookupInLoop' }],
      },
    ],
  })
})
