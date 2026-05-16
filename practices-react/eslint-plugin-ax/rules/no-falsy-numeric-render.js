/**
 * ax/no-falsy-numeric-render
 *
 * Flags `<LeftSide> && <JSX>` where LeftSide is statically known to be
 * numeric or unknown-shape (could be 0, NaN, '', null, undefined) — these
 * render literal "0"/"NaN" in JSX when falsy.
 *
 * Pairs 1:1 with: practices-react/rules/rendering-conditional-render.md
 *
 * Heuristic — flag when LeftSide is:
 *   - a numeric literal (e.g. `0 && ...`)
 *   - a MemberExpression that "looks numeric" (.length / .count / .total / ...
 *     based on name pattern — see NUMERIC_NAME_PATTERN)
 *   - a CallExpression that "looks numeric" (similar name patterns)
 *   - a BinaryExpression that produces a number (+ - * / %)
 *   - an Identifier whose name matches NUMERIC_NAME_PATTERN
 *
 * Real booleans (BinaryExpression with comparison ops, LogicalExpression with
 * boolean operands, UnaryExpression !, Boolean(x) calls) are intentionally
 * NOT flagged.
 */

const NUMERIC_NAME_PATTERN =
  /^(length|size|count|total|num|number|index|idx|width|height|x|y|i|n|len)$/i

const NUMERIC_BINARY_OPS = new Set(['+', '-', '*', '/', '%', '**'])

const BOOLEAN_BINARY_OPS = new Set([
  '==',
  '!=',
  '===',
  '!==',
  '<',
  '<=',
  '>',
  '>=',
  'in',
  'instanceof',
])

function looksNumericName(name) {
  if (typeof name !== 'string') return false
  return NUMERIC_NAME_PATTERN.test(name)
}

function isNumericLeft(node) {
  if (!node) return false
  switch (node.type) {
    case 'Literal':
      return typeof node.value === 'number'
    case 'Identifier':
      return looksNumericName(node.name)
    case 'MemberExpression':
      if (node.computed) return false
      return looksNumericName(node.property.name)
    case 'CallExpression': {
      const callee = node.callee
      if (callee.type === 'Identifier') return looksNumericName(callee.name)
      if (callee.type === 'MemberExpression' && !callee.computed) {
        return looksNumericName(callee.property.name)
      }
      return false
    }
    case 'BinaryExpression':
      // Numeric only if the operator is arithmetic.
      return NUMERIC_BINARY_OPS.has(node.operator)
    case 'UnaryExpression':
      return ['+', '-', '~'].includes(node.operator)
    default:
      return false
  }
}

function isExplicitBooleanLeft(node) {
  if (!node) return false
  if (node.type === 'BinaryExpression' && BOOLEAN_BINARY_OPS.has(node.operator)) return true
  if (node.type === 'UnaryExpression' && node.operator === '!') return true
  if (node.type === 'CallExpression') {
    const callee = node.callee
    if (callee.type === 'Identifier' && callee.name === 'Boolean') return true
  }
  return false
}

/** Recursively look for a `<x> && <JSX>` pattern under a JSXExpressionContainer. */
function findLogicalAndsInJSX(container, report) {
  function visit(node) {
    if (!node) return
    if (node.type !== 'LogicalExpression') return
    if (node.operator !== '&&') return

    const { left, right } = node
    // Right side must look like JSX to trigger the rendered-falsy concern.
    const rightIsJSX =
      right.type === 'JSXElement' || right.type === 'JSXFragment'
    if (!rightIsJSX) {
      visit(left)
      return
    }

    if (isExplicitBooleanLeft(left)) {
      // Safe.
      return
    }
    if (isNumericLeft(left)) {
      report(node)
      return
    }
    // For all other shapes we don't have enough info — leave it.
  }
  visit(container.expression)
}

/** @type {import("eslint").Rule.RuleModule} */
const rule = {
  meta: {
    type: 'problem',
    docs: {
      description:
        'Disallow `<numeric-looking expr> && <JSX>` — when the expression is 0/NaN/empty it renders as visible text. Use ternary or Boolean() cast.',
      recommended: true,
      url: 'https://github.com/ax-template/practices-react/blob/main/rules/rendering-conditional-render.md',
    },
    schema: [],
    messages: {
      falsyNumeric:
        'Numeric (or unknown-shape) left side in `&& <JSX>` may render the literal value when falsy (0/NaN/empty string). Use `cond > 0 ? <JSX> : null` or `Boolean(cond) && <JSX>`.',
    },
  },

  create(context) {
    return {
      JSXExpressionContainer(node) {
        findLogicalAndsInJSX(node, (offending) =>
          context.report({ node: offending, messageId: 'falsyNumeric' }),
        )
      },
    }
  },
}

export default rule
