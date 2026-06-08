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
 * Spec: docs/superpowers/specs/2026-06-08-frontend-decomposition-rules-design.md §4 (TIER-1).
 * Backend analog: thin-controller.
 */

import { isRouteFile, hasUseClientDirective } from '../lib/feature-layout.js'

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
    const filename =
      typeof context.filename === 'string' ? context.filename : context.getFilename()
    if (!isRouteFile(filename)) return {}

    let isClientRoute = false

    return {
      Program(node) {
        isClientRoute = hasUseClientDirective(node)
      },
      CallExpression(node) {
        if (!isClientRoute) return
        const callee = node.callee
        let name = null
        if (callee.type === 'Identifier') {
          name = callee.name // useSWR(...), useQuery(...), fetch(...)
        } else if (callee.type === 'MemberExpression' && callee.object.type === 'Identifier' &&
          callee.object.name === 'axios') {
          name = 'axios.' + (callee.property.name || '') // axios.get(...)
        }
        if (name === 'fetch' || name === 'axios' || (name && name.startsWith('axios.')) ||
          CLIENT_DATA_HOOKS.has(name)) {
          context.report({ node, messageId: 'clientDataInRoute', data: { name } })
        }
      },
    }
  },
}

export default rule
