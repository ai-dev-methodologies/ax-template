/**
 * ax/no-inline-component-definition
 *
 * Flags React component definitions (functions starting with a CapitalLetter
 * AND returning JSX) declared inside another React component function body.
 *
 * Pairs 1:1 with: practices-react/rules/rerender-no-inline-components.md
 *
 * Detection:
 *   1. We are inside a function whose name (or surrounding declarator name)
 *      starts with a CapitalLetter — "the outer component".
 *   2. Inside its BlockStatement body, we find a FunctionDeclaration / arrow
 *      assignment whose name ALSO starts with a CapitalLetter AND whose body
 *      directly returns JSX.
 *
 * False-positive avoidance:
 *   - Inline render helpers called as plain functions (renderItem(x)) are
 *     ignored because they don't typically have a Capitalized name. If the
 *     helper IS Capitalized AND returns JSX, that IS the bug we're catching.
 *   - useMemo/useCallback factories returning JSX are NOT flagged here
 *     (separate concern; the memo prevents reidentification per-render).
 */

function isCapitalized(name) {
  return typeof name === 'string' && /^[A-Z]/.test(name)
}

function returnsJSX(fnNode) {
  if (!fnNode) return false
  const body = fnNode.body
  if (!body) return false
  // Arrow expression body: `() => <JSX />`
  if (body.type === 'JSXElement' || body.type === 'JSXFragment') return true
  if (body.type !== 'BlockStatement') return false

  // Look for a top-level `return <JSX />` statement (skip nested funcs/loops).
  for (const stmt of body.body) {
    if (!stmt) continue
    if (stmt.type === 'ReturnStatement' && stmt.argument) {
      const a = stmt.argument
      if (a.type === 'JSXElement' || a.type === 'JSXFragment') return true
      // Wrapped in parens / conditional — recursive check
      if (containsTopJSX(a)) return true
    }
  }
  return false
}

function containsTopJSX(expr) {
  if (!expr || typeof expr !== 'object') return false
  if (expr.type === 'JSXElement' || expr.type === 'JSXFragment') return true
  if (expr.type === 'ConditionalExpression') {
    return containsTopJSX(expr.consequent) || containsTopJSX(expr.alternate)
  }
  if (expr.type === 'LogicalExpression') {
    return containsTopJSX(expr.left) || containsTopJSX(expr.right)
  }
  // Don't descend into call expressions or new sub-trees that don't fit the
  // "directly returns JSX" pattern.
  return false
}

/** @type {import("eslint").Rule.RuleModule} */
const rule = {
  meta: {
    type: 'problem',
    docs: {
      description:
        'Disallow defining a React component inside another React component — creates a new component type per render and remounts on every parent render (lost state, focus, animations).',
      recommended: true,
      url: 'https://github.com/ax-template/practices-react/blob/main/rules/rerender-no-inline-components.md',
    },
    schema: [],
    messages: {
      innerComponent:
        "Component '{{name}}' is defined inside '{{outer}}'. This creates a new component type per render → full remount → lost state. Define '{{name}}' at module scope and pass data as props.",
    },
  },

  create(context) {
    // Track stack of "outer component names". When entering a function body
    // that LOOKS like a component, push its name. Inner function definitions
    // detected inside trigger the rule.
    const stack = []

    function enterFunction(node, nameHint) {
      const name = nameHint || functionNameFromContext(node)
      stack.push({ name, isComponent: isCapitalized(name) })
    }
    function exitFunction() {
      stack.pop()
    }

    function functionNameFromContext(node) {
      if (node.id && node.id.name) return node.id.name
      const parent = node.parent
      if (!parent) return null
      if (parent.type === 'VariableDeclarator' && parent.id.type === 'Identifier')
        return parent.id.name
      if (parent.type === 'Property' && parent.key && parent.key.name)
        return parent.key.name
      return null
    }

    function reportIfInnerComponent(node, name) {
      if (!isCapitalized(name)) return
      if (!returnsJSX(node)) return
      // Check whether we are currently inside another component scope.
      // Note: enterFunction pushes BEFORE the body is visited, so on enter we
      // peek at index length-2 (the parent function's frame, if any).
      if (stack.length < 2) return
      const parent = stack[stack.length - 2]
      if (!parent || !parent.isComponent) return
      context.report({
        node,
        messageId: 'innerComponent',
        data: { name, outer: parent.name || '<anonymous>' },
      })
    }

    return {
      FunctionDeclaration(node) {
        enterFunction(node, node.id ? node.id.name : null)
        reportIfInnerComponent(node, node.id ? node.id.name : null)
      },
      'FunctionDeclaration:exit': exitFunction,

      FunctionExpression(node) {
        enterFunction(node, null)
        const name =
          node.id && node.id.name
            ? node.id.name
            : node.parent && node.parent.type === 'VariableDeclarator' && node.parent.id.type === 'Identifier'
              ? node.parent.id.name
              : null
        reportIfInnerComponent(node, name)
      },
      'FunctionExpression:exit': exitFunction,

      ArrowFunctionExpression(node) {
        enterFunction(node, null)
        const name =
          node.parent && node.parent.type === 'VariableDeclarator' && node.parent.id.type === 'Identifier'
            ? node.parent.id.name
            : null
        reportIfInnerComponent(node, name)
      },
      'ArrowFunctionExpression:exit': exitFunction,
    }
  },
}

export default rule
