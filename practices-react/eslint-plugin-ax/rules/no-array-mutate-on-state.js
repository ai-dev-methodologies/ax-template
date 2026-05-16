/**
 * ax/no-array-mutate-on-state
 *
 * Flags `.sort` / `.reverse` / `.splice` (and `arr[i] = v` mutation patterns)
 * on arrays that look like React props or state.
 *
 * Pairs 1:1 with: practices-react/rules/js-tosorted-immutable.md
 *
 * Heuristic for "looks like prop/state":
 *   - destructured directly from useState's tuple (left side of a const
 *     [name, setName] = useState(...))
 *   - listed in the function's parameter destructuring (function Foo({ items })
 *     {} or function Foo(props) and props.items)
 *
 * We do NOT try to follow values through intermediate variables — false
 * negatives are accepted; the rule is a tripwire for the obvious cases.
 */

const MUTATING_METHODS = new Set(['sort', 'reverse', 'splice'])

function paramIdentifierNames(params) {
  const names = new Set()
  function collect(p) {
    if (!p) return
    switch (p.type) {
      case 'Identifier':
        names.add(p.name)
        return
      case 'ObjectPattern':
        for (const prop of p.properties) {
          if (prop.type === 'Property') collect(prop.value)
          else if (prop.type === 'RestElement') collect(prop.argument)
        }
        return
      case 'ArrayPattern':
        for (const el of p.elements) collect(el)
        return
      case 'AssignmentPattern':
        collect(p.left)
        return
      case 'RestElement':
        collect(p.argument)
        return
      default:
        return
    }
  }
  for (const p of params) collect(p)
  return names
}

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

/** @type {import("eslint").Rule.RuleModule} */
const rule = {
  meta: {
    type: 'problem',
    docs: {
      description:
        'Disallow `.sort/.reverse/.splice` on arrays that look like React props or state. Use the ES2023 immutable variants (`.toSorted/.toReversed/.toSpliced`) or spread + sort.',
      recommended: true,
      url: 'https://github.com/ax-template/practices-react/blob/main/rules/js-tosorted-immutable.md',
    },
    schema: [],
    messages: {
      mutateOnState:
        "`{{name}}.{{method}}(...)` mutates the array — likely a prop or state. Use `.to{{method_pascal}}(...)` (ES2023) or `[...{{name}}].{{method}}(...)`.",
    },
  },

  create(context) {
    // Track names that are "from props or useState" within the current
    // FunctionDeclaration / FunctionExpression / ArrowFunctionExpression scope.
    const stack = []

    function pushScope(params) {
      stack.push({ propNames: paramIdentifierNames(params), stateNames: new Set() })
    }
    function popScope() {
      stack.pop()
    }
    function topScope() {
      return stack.length > 0 ? stack[stack.length - 1] : null
    }
    function nameIsStateOrProp(name) {
      if (!name) return false
      return stack.some((s) => s.propNames.has(name) || s.stateNames.has(name))
    }

    return {
      'FunctionDeclaration': (node) => pushScope(node.params),
      'FunctionDeclaration:exit': popScope,
      'FunctionExpression': (node) => pushScope(node.params),
      'FunctionExpression:exit': popScope,
      'ArrowFunctionExpression': (node) => pushScope(node.params),
      'ArrowFunctionExpression:exit': popScope,

      VariableDeclarator(node) {
        const scope = topScope()
        if (!scope) return
        // const [items, setItems] = useState(...)
        if (
          node.id.type === 'ArrayPattern' &&
          isUseStateCall(node.init)
        ) {
          const first = node.id.elements[0]
          if (first && first.type === 'Identifier') {
            scope.stateNames.add(first.name)
          }
        }
      },

      CallExpression(node) {
        const callee = node.callee
        if (callee.type !== 'MemberExpression' || callee.computed) return
        if (!MUTATING_METHODS.has(callee.property.name)) return
        if (callee.object.type !== 'Identifier') return
        const name = callee.object.name
        if (!nameIsStateOrProp(name)) return

        const method = callee.property.name
        const methodPascal = method.charAt(0).toUpperCase() + method.slice(1)
        context.report({
          node,
          messageId: 'mutateOnState',
          data: { name, method, method_pascal: methodPascal },
        })
      },
    }
  },
}

export default rule
