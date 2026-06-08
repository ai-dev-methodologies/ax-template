import { RuleTester } from 'eslint'
import test from 'node:test'
import rule from '../rules/no-route-client-data-fetching.js'

const tester = new RuleTester({
  languageOptions: {
    ecmaVersion: 2024,
    sourceType: 'module',
    parserOptions: { ecmaFeatures: { jsx: true } },
  },
})

const ROUTE = '/repo/frontend/src/app/(authenticated)/dashboard/page.tsx'
const SERVER_ROUTE = '/repo/frontend/src/app/showcase/page.tsx'
const FEATURE = '/repo/frontend/src/features/auth/login/useLogin.ts'

test('ax/no-route-client-data-fetching — RuleTester suite', () => {
  tester.run('ax/no-route-client-data-fetching', rule, {
    valid: [
      // server route (no "use client") may await fetch — idiomatic App Router
      { code: `export default async function Page(){ const r = await fetch('/api/x'); return r }`, filename: SERVER_ROUTE },
      // client route that delegates (no data hooks) — fine
      { code: `'use client'\nimport { Panel } from '@/features/auth/login'\nexport default function Page(){ return <Panel/> }`, filename: ROUTE },
      // a feature hook (NOT a route) using useSWR — that's where data belongs
      { code: `import useSWR from 'swr'\nexport function useLogin(){ return useSWR('/api/me') }`, filename: FEATURE },
    ],
    invalid: [
      // client route calling useSWR — FORBIDDEN
      {
        code: `'use client'\nimport useSWR from 'swr'\nexport default function Page(){ const { data } = useSWR('/api/me'); return data }`,
        filename: ROUTE,
        errors: [{ messageId: 'clientDataInRoute' }],
      },
      // client route calling raw fetch — FORBIDDEN
      {
        code: `'use client'\nexport default function Page(){ fetch('/api/x'); return null }`,
        filename: ROUTE,
        errors: [{ messageId: 'clientDataInRoute' }],
      },
      // client route calling axios.get — FORBIDDEN
      {
        code: `'use client'\nimport axios from 'axios'\nexport default function Page(){ axios.get('/api/x'); return null }`,
        filename: ROUTE,
        errors: [{ messageId: 'clientDataInRoute' }],
      },
      // audit HIGH: RENAMED hook import must NOT bypass — `useSWR as useFetch`
      {
        code: `'use client'\nimport { useSWR as useFetch } from 'swr'\nexport default function Page(){ const { data } = useFetch('/api/me'); return data }`,
        filename: ROUTE,
        errors: [{ messageId: 'clientDataInRoute' }],
      },
      // audit HIGH: aliased axios (import axios as http) — member call still caught
      {
        code: `'use client'\nimport http from 'axios'\nexport default function Page(){ http.get('/api/x'); return null }`,
        filename: ROUTE,
        errors: [{ messageId: 'clientDataInRoute' }],
      },
    ],
  })
})
