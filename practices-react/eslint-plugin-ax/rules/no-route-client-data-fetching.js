/**
 * ax/no-route-client-data-fetching  (FE-ROUTE-THIN core, ERROR)
 *
 * A Next.js App Router route file (`app/**\/page|layout`) that is a CLIENT component
 * (`"use client"`) must not orchestrate client-side data fetching directly — no
 * `useSWR` / `useQuery` / `useMutation` / `useInfiniteQuery` / `axios` calls and no
 * raw `fetch(...)`. Delegate client data to a feature hook/container instead, so the
 * route stays thin.
 *
 * App Router correctness: SERVER components (no `"use client"`) may `await fetch()` —
 * that is the idiomatic server data layer and is NOT flagged. This rule only governs
 * `"use client"` route files.
 *
 * Caught (audit 2026-06-08): renamed/aliased imports from a data lib (`import { useSWR as
 * useFetch } from 'swr'`, `import http from 'axios'`) — bindings are tracked name-agnostically.
 * Honest limit: a route calling a LOCAL wrapper hook that internally uses useSWR (e.g.
 * `useDashboardData()` defined elsewhere) is NOT caught — seeing through the wrapper needs
 * data-flow analysis. That is exactly the intended pattern anyway (the wrapper IS the
 * feature hook), so it is acceptable; genuine in-route data orchestration is what this blocks.
 *
 * Spec: docs/superpowers/specs/2026-06-08-frontend-decomposition-rules-design.md §4 (TIER-1).
 * Backend analog: thin-controller.
 */

import { isRouteFile, hasUseClientDirective, layoutFrom } from '../lib/feature-layout.js'

// Packages whose imported bindings are client-side data orchestration.
const DATA_LIB = /^(swr|swr\/.*|@tanstack\/react-query|axios)$/
// Literal hook names — defensive fallback for an auto-imported / globally-provided hook.
const CLIENT_DATA_HOOKS = new Set([
  'useSWR', 'useSWRInfinite', 'useSWRMutation',
  'useQuery', 'useMutation', 'useInfiniteQuery', 'useQueryClient', 'useSuspenseQuery',
])

/** @type {import("eslint").Rule.RuleModule} */
const rule = {
  meta: {
    type: 'problem',
    docs: {
      description:
        'A "use client" route file (app/**/page|layout) must not call client data-fetching hooks (useSWR/useQuery/...) or raw fetch — delegate to a feature hook.',
      recommended: true,
      url: 'https://github.com/ax-template/practices-react/blob/main/rules/no-route-client-data-fetching.md',
    },
    schema: [],
    messages: {
      clientDataInRoute:
        "Client data-fetching in a route file ('{{name}}'). A \"use client\" route must stay thin — move '{{name}}' into a feature hook/container (@/features/<f>) and render its result here.",
    },
  },

  create(context) {
    const layout = layoutFrom(context.settings)
    const filename =
      typeof context.filename === 'string' ? context.filename : context.getFilename()
    if (!isRouteFile(filename, layout)) return {}

    let isClientRoute = false
    // local binding names imported from a data lib — name-agnostic, so `import { useSWR
    // as useFetch } from 'swr'` (audit HIGH bypass) is still caught.
    const dataBindings = new Set()

    return {
      Program(node) {
        isClientRoute = hasUseClientDirective(node)
      },
      ImportDeclaration(node) {
        const src = node.source && node.source.value
        if (typeof src !== 'string' || !DATA_LIB.test(src)) return
        for (const spec of node.specifiers || []) {
          if (spec.local && spec.local.name) dataBindings.add(spec.local.name)
        }
      },
      CallExpression(node) {
        if (!isClientRoute) return
        const callee = node.callee
        let name = null
        if (callee.type === 'Identifier') {
          name = callee.name // useFetch(...) / useSWR(...) / fetch(...)
        } else if (callee.type === 'MemberExpression' && callee.object.type === 'Identifier') {
          // a member call on a data binding (e.g. axios.get(...), an aliased axios.post(...))
          if (dataBindings.has(callee.object.name)) {
            context.report({ node, messageId: 'clientDataInRoute', data: { name: callee.object.name + '.' + (callee.property.name || '') } })
            return
          }
        }
        if (name === 'fetch' || dataBindings.has(name) || CLIENT_DATA_HOOKS.has(name)) {
          context.report({ node, messageId: 'clientDataInRoute', data: { name } })
        }
      },
    }
  },
}

export default rule
