/**
 * ax/no-array-includes-in-loop
 *
 * Flags `<arr>.includes(x)` (or `<arr>.find/.findIndex/.indexOf(...)`) inside:
 *   - a callback for .filter / .map / .forEach / .some / .every / .reduce, OR
 *   - the body of a `for (const x of <iterable>)` loop (FMW1 — FDW1 found the
 *     callback-only scope let a plain for-of O(n*m) lookup ship green),
 * when:
 *   - the looked-up array <arr> is an identifier (a name from the surrounding
 *     scope), and
 *   - the array is NOT the same as the one being iterated.
 *
 * Pairs 1:1 with: practices-react/rules/js-set-map-lookups.md
 *
 * Conservative — only flags the simple "iterate items, look up by includes in
 * another array" pattern. Plain `for (;;)` / `while` loops are intentionally
 * NOT covered: without a clear iterable their iteration count is ambiguous, and
 * the fix (building a Set) has a cost of its own — false-negatives are
 * acceptable here, false-positives are not.
 */

const ITERATOR_METHODS = new Set([
  'filter',
  'map',
  'forEach',
  'some',
  'every',
  'reduce',
  'reduceRight',
])

const HOT_LOOKUP_METHODS = new Set(['includes', 'find', 'findIndex', 'indexOf'])

function isIteratorCall(node) {
  if (!node || node.type !== 'CallExpression') return null
  const callee = node.callee
  if (callee.type !== 'MemberExpression' || callee.computed) return null
  if (!ITERATOR_METHODS.has(callee.property.name)) return null
  // First arg must be a callback (FunctionExpression / ArrowFunctionExpression).
  const fn = node.arguments[0]
  if (
    !fn ||
    (fn.type !== 'FunctionExpression' &&
      fn.type !== 'ArrowFunctionExpression')
  )
    return null
  return { iteratedExpr: callee.object, callback: fn }
}

function getIdentifierName(expr) {
  if (!expr) return null
  if (expr.type === 'Identifier') return expr.name
  return null
}

function callbackBody(fn) {
  if (fn.body.type === 'BlockStatement') return fn.body
  return fn.body
}

function walkExpressions(node, visit) {
  if (!node || typeof node !== 'object') return
  visit(node)
  for (const key of Object.keys(node)) {
    if (key === 'parent' || key === 'loc' || key === 'range') continue
    const child = node[key]
    if (Array.isArray(child)) {
      for (const c of child) walkExpressions(c, visit)
    } else if (child && typeof child === 'object' && child.type) {
      walkExpressions(child, visit)
    }
  }
}

/** @type {import("eslint").Rule.RuleModule} */
const rule = {
  meta: {
    type: 'suggestion',
    docs: {
      description:
        'Disallow `<arr>.includes(x)` (or similar O(n) lookups) inside iterator callbacks when the looked-up array is closed over from the surrounding scope. Build a Set/Map once instead.',
      recommended: true,
      url: 'https://github.com/ax-template/practices-react/blob/main/rules/js-set-map-lookups.md',
    },
    schema: [],
    messages: {
      hotLookupInLoop:
        '`{{outerName}}.{{method}}(...)` inside a loop is O(n) per iteration. Build a Set or Map of `{{outerName}}` once before the loop and use `has`/`get` instead.',
    },
  },

  create(context) {
    // Scan a loop body for an O(n) hot-lookup on a closed-over identifier that
    // is NOT the array being iterated. Shared by iterator callbacks + for-of.
    function reportHotLookups(body, iteratedName) {
      walkExpressions(body, (n) => {
        if (n.type !== 'CallExpression') return
        const cal = n.callee
        if (!cal || cal.type !== 'MemberExpression' || cal.computed) return
        if (!HOT_LOOKUP_METHODS.has(cal.property.name)) return
        const outerName = getIdentifierName(cal.object)
        if (!outerName) return
        // Skip lookups on the iterated array itself.
        if (iteratedName && outerName === iteratedName) return

        context.report({
          node: n,
          messageId: 'hotLookupInLoop',
          data: { outerName, method: cal.property.name },
        })
      })
    }

    return {
      CallExpression(node) {
        const iter = isIteratorCall(node)
        if (!iter) return
        reportHotLookups(callbackBody(iter.callback), getIdentifierName(iter.iteratedExpr))
      },

      // for (const x of items) { lookup.includes(x) }  — the iterable is the
      // for-of right-hand side; exclude it as the "iterated array".
      ForOfStatement(node) {
        reportHotLookups(node.body, getIdentifierName(node.right))
      },
    }
  },
}

export default rule
