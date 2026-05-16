/**
 * ax/no-array-includes-in-loop
 *
 * Flags `<arr>.includes(x)` (or `<arr>.find(...)`) inside a callback for
 * .filter / .map / .forEach / .some / .every / .reduce when:
 *   - the array <arr> is an identifier (a name from the surrounding scope),
 *   - the array is NOT the same as the one being iterated.
 *
 * Pairs 1:1 with: practices-react/rules/js-set-map-lookups.md
 *
 * Conservative — only flags the simple "iterate items, look up by includes
 * in another array" pattern. False-negatives are acceptable; false-positives
 * are not (because the fix — building a Set — has a cost of its own).
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
        '`{{outerName}}.{{method}}(...)` inside an iterator callback is O(n) per iteration. Build a Set or Map of `{{outerName}}` once before the loop and use `has`/`get` instead.',
    },
  },

  create(context) {
    return {
      CallExpression(node) {
        const iter = isIteratorCall(node)
        if (!iter) return
        const iteratedName = getIdentifierName(iter.iteratedExpr)

        const body = callbackBody(iter.callback)
        walkExpressions(body, (n) => {
          if (n.type !== 'CallExpression') return
          const cal = n.callee
          if (!cal || cal.type !== 'MemberExpression' || cal.computed) return
          if (!HOT_LOOKUP_METHODS.has(cal.property.name)) return
          const outerName = getIdentifierName(cal.object)
          if (!outerName) return
          // Skip lookups on the iterated array itself
          // (filter(items, x => items.includes(x)) is unusual but not the
          // pattern this rule targets).
          if (iteratedName && outerName === iteratedName) return

          context.report({
            node: n,
            messageId: 'hotLookupInLoop',
            data: { outerName, method: cal.property.name },
          })
        })
      },
    }
  },
}

export default rule
