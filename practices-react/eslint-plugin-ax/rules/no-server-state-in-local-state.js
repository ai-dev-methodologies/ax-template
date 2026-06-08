/**
 * ax/no-server-state-in-local-state  (FE-STATE-BOUNDARY, advisory/warn)
 *
 * Do not copy server state (a SWR / TanStack-Query result's `.data`) into local
 * component state via `useState(...)`. The query cache is the source of truth;
 * mirroring it into `useState` desynchronizes the two and defeats revalidation.
 *
 * Heuristic + honest limit (spec §4): this flags only the direct, unambiguous shape
 * `useState(useSWR(...).data)` / `useState(useQuery(...).data)` — copying a result
 * through an intermediate variable is NOT caught (kept advisory for that reason).
 *
 * Spec: docs/superpowers/specs/2026-06-08-frontend-decomposition-rules-design.md §4 (TIER-1, advisory).
 * Backend analog: the heuristic TIER-1 guards' "정직한 한계".
 */

const QUERY_CALLS = new Set([
  'useSWR', 'useSWRInfinite', 'useQuery', 'useInfiniteQuery', 'useSuspenseQuery',
])

/** Is `node` a `<queryCall>(...).data` member access? */
function isQueryDotData(node) {
  return (
    node &&
    node.type === 'MemberExpression' &&
    node.property &&
    node.property.name === 'data' &&
    node.object &&
    node.object.type === 'CallExpression' &&
    node.object.callee &&
    node.object.callee.type === 'Identifier' &&
    QUERY_CALLS.has(node.object.callee.name)
  )
}

/** @type {import("eslint").Rule.RuleModule} */
const rule = {
  meta: {
    type: 'suggestion',
    docs: {
      description:
        'Do not seed useState with a query/SWR result (.data) — the query cache is the source of truth.',
      recommended: false,
      url: 'https://github.com/ax-template/practices-react/blob/main/rules/no-server-state-in-local-state.md',
    },
    schema: [],
    messages: {
      serverStateCopied:
        'Server state copied into local state: useState() seeded from a query result (.data). Read it from the query cache directly instead of mirroring it into useState.',
    },
  },

  create(context) {
    return {
      CallExpression(node) {
        if (node.callee.type !== 'Identifier' || node.callee.name !== 'useState') return
        const arg = node.arguments[0]
        if (arg && isQueryDotData(arg)) {
          context.report({ node, messageId: 'serverStateCopied' })
        }
      },
    }
  },
}

export default rule
