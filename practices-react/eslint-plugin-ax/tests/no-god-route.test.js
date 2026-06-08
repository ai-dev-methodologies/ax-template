import { RuleTester } from 'eslint'
import test from 'node:test'
import rule from '../rules/no-god-route.js'

const tester = new RuleTester({
  languageOptions: { ecmaVersion: 2024, sourceType: 'module' },
})

const ROUTE = '/repo/frontend/src/app/(authenticated)/dashboard/page.tsx'
const SERVER_ROUTE = '/repo/frontend/src/app/showcase/page.tsx'
const FEATURE = '/repo/frontend/src/features/auth/login/Big.tsx'

// build a client route body of N physical lines
function clientRoute(lines) {
  const body = Array.from({ length: lines - 3 }, (_, i) => `  const v${i} = ${i};`).join('\n')
  return `'use client'\nexport default function Page(){\n${body}\n  return null\n}`
}

test('ax/no-god-route — RuleTester suite', () => {
  tester.run('ax/no-god-route', rule, {
    valid: [
      // short client route — fine
      { code: `'use client'\nexport default function Page(){ return null }`, filename: ROUTE },
      // long SERVER route (no "use client") — not checked
      { code: clientRoute(200).replace("'use client'\n", ''), filename: SERVER_ROUTE },
      // long file that is NOT a route — not checked
      { code: clientRoute(200), filename: FEATURE },
      // exactly at threshold (custom maxLines option) — fine
      { code: clientRoute(50), filename: ROUTE, options: [{ maxLines: 120 }] },
    ],
    invalid: [
      // fat client route over the default threshold — WARN (advisory; surfaced as a finding)
      {
        code: clientRoute(140),
        filename: ROUTE,
        errors: [{ messageId: 'godRoute' }],
      },
    ],
  })
})
