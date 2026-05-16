/**
 * ax/prefer-functional-setstate
 *
 * Flags `setX(<expr referencing X>)` where setX is the second element of a
 * useState tuple and the expression directly references the corresponding
 * state variable. Suggests `setX(curr => ...)` functional form.
 *
 * Pairs 1:1 with: practices-react/rules/rerender-functional-setstate.md
 *
 * Detection:
 *   1. Track `const [x, setX] = useState(...)` pairs in scope.
 *   2. Inside the same scope, flag `setX(<expr>)` where the expression's
 *      AST contains an Identifier with name === x.
 *
 * Skip cases (acceptable direct setState):
 *   - `setX(literal)` — static value
 *   - `setX(newName)` — value from a function parameter (no x reference)
 *   - `setX(prev => ...)` — already functional
 */

function isUseStateCall(init) {
  if (!init || init.type !== 'CallExpression') return false
  const callee = init.callee
  if (callee.type === 'Identifier' && callee.name === 'useState') return true
  if (
    callee.type === 'MemberExpression' &&
    !callee.computed &&
    callee.property.name === 'useState'
  )
    return true
  return false
}

function expressionReferencesIdentifier(expr, name) {
  if (!expr || typeof expr !== 'object') return false
  if (Array.isArray(expr)) return expr.some((e) => expressionReferencesIdentifier(e, name))

  if (expr.type === 'Identifier' && expr.name === name) return true

  // Don't descend into nested function bodies — that's a separate scope and
  // the closure semantics there are different.
  if (
    expr.type === 'FunctionExpression' ||
    expr.type === 'ArrowFunctionExpression'
  ) {
    return false
  }

  for (const key of Object.keys(expr)) {
    if (key === 'parent' || key === 'loc' || key === 'range' || key === 'type')
      continue
    // Skip property keys of non-computed MemberExpressions — `obj.foo`'s
    // `foo` is the property name on `obj`, not a free reference to a
    // variable named `foo`. Without this skip, `setMessage(res.message)`
    // would falsely match the state name `message`.
    if (
      expr.type === 'MemberExpression' &&
      key === 'property' &&
      !expr.computed
    )
      continue
    // Same for ObjectExpression Property keys: `{ foo: x }` — `foo` is the
    // key, not a reference.
    if (expr.type === 'Property' && key === 'key' && !expr.computed) continue
    const child = expr[key]
    if (Array.isArray(child)) {
      if (expressionReferencesIdentifier(child, name)) return true
    } else if (child && typeof child === 'object' && child.type) {
      if (expressionReferencesIdentifier(child, name)) return true
    }
  }
  return false
}

/** @type {import("eslint").Rule.RuleModule} */
const rule = {
  meta: {
    type: 'problem',
    docs: {
      description:
        'Prefer `setX(prev => ...)` over `setX(<expr referencing X>)` — avoids stale closures and allows omitting X from the surrounding callback dep array.',
      recommended: true,
      url: 'https://github.com/ax-template/practices-react/blob/main/rules/rerender-functional-setstate.md',
    },
    schema: [],
    messages: {
      preferFunctional:
        '`{{setter}}({{var}}...)` directly references state `{{var}}` — risks stale closure. Use the functional form: `{{setter}}((curr) => ...)`.',
    },
  },

  create(context) {
    // Stack of scopes; each maps setterName -> stateName.
    const stack = []

    function pushScope() {
      stack.push(new Map())
    }
    function popScope() {
      stack.pop()
    }
    function recordPair(stateName, setterName) {
      if (stack.length === 0) return
      stack[stack.length - 1].set(setterName, stateName)
    }
    function lookupSetter(setterName) {
      for (let i = stack.length - 1; i >= 0; i--) {
        const m = stack[i]
        if (m.has(setterName)) return m.get(setterName)
      }
      return null
    }

    return {
      'FunctionDeclaration': pushScope,
      'FunctionDeclaration:exit': popScope,
      'FunctionExpression': pushScope,
      'FunctionExpression:exit': popScope,
      'ArrowFunctionExpression': pushScope,
      'ArrowFunctionExpression:exit': popScope,
      Program: pushScope,
      'Program:exit': popScope,

      VariableDeclarator(node) {
        if (
          node.id.type === 'ArrayPattern' &&
          isUseStateCall(node.init) &&
          node.id.elements.length >= 2
        ) {
          const stateEl = node.id.elements[0]
          const setterEl = node.id.elements[1]
          if (
            stateEl &&
            stateEl.type === 'Identifier' &&
            setterEl &&
            setterEl.type === 'Identifier'
          ) {
            recordPair(stateEl.name, setterEl.name)
          }
        }
      },

      CallExpression(node) {
        const callee = node.callee
        if (callee.type !== 'Identifier') return
        const stateName = lookupSetter(callee.name)
        if (!stateName) return
        if (node.arguments.length === 0) return
        const arg = node.arguments[0]
        // Functional form is already correct.
        if (
          arg.type === 'FunctionExpression' ||
          arg.type === 'ArrowFunctionExpression'
        )
          return
        if (!expressionReferencesIdentifier(arg, stateName)) return

        context.report({
          node,
          messageId: 'preferFunctional',
          data: { setter: callee.name, var: stateName },
        })
      },
    }
  },
}

export default rule
