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
      // for-of with a Set — correct pattern
      `
        const allowedIds = new Set(['a', 'b'])
        for (const it of items) { if (allowedIds.has(it.id)) doIt(it) }
      `,
      // for-of with .includes on the iterated array itself — not our target
      `
        for (const it of items) { if (items.includes(it.parent)) doIt(it) }
      `,
      // lookup on a name DECLARED inside the loop body (per-iteration local,
      // often a string) — not the closed-over-array pattern; must NOT flag.
      `
        files.forEach((f) => {
          const src = read(f)
          if (src.includes('template_id:')) keep(f)
        })
      `,
      // same via for-of
      `
        for (const f of files) {
          const src = read(f)
          if (src.includes('x') && src.includes('y')) keep(f)
        }
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
      // for-of body with a closed-over array lookup — O(n*m) (FMW1 broadening)
      {
        code: `
          const allowed = ['a', 'b']
          for (const o of orders) { if (allowed.includes(o.id)) doIt(o) }
        `,
        errors: [{ messageId: 'hotLookupInLoop' }],
      },
      // for-of body with .find on a closed-over array
      {
        code: `
          for (const o of orders) { const u = users.find((x) => x.id === o.userId); render(u) }
        `,
        errors: [{ messageId: 'hotLookupInLoop' }],
      },
    ],
  })
})
