/**
 * ax/no-app-local-ui-primitives
 *
 * In a per-persona app (any file whose path contains an `apps/` segment),
 * it is an ERROR to:
 *   (a) define OR export a React component whose name matches a known catalog
 *       primitive (Button, Input, Label, Field, Card + Card* family, Alert,
 *       Badge, Spinner, Switch, …), OR
 *   (b) import a local `components/ui/**` module (a relative path containing a
 *       `components/ui` segment).
 *
 * Per-persona apps MUST consume these primitives from the shared catalog —
 * `@ax/ui` for design-system primitives, `@ax/blocks` for composed blocks.
 * Re-implementing them app-locally fragments the design system and is exactly
 * the bespoke-UI drift this rule exists to block.
 *
 * Pairs 1:1 with: practices-react/rules/no-app-local-ui-primitives.md
 *
 * Scope: only files under an `apps/` directory are checked. The root web-shell
 * app (which predates the catalog) is exempt because it lives at the package
 * root, not under `apps/`.
 *
 * Detection of "a React component":
 *   - A function (declaration / arrow / expression) whose name is a known
 *     catalog primitive AND which returns JSX, OR
 *   - A `forwardRef(...)` / `memo(...)` factory assigned to a known-primitive
 *     name (these are the common shadcn/Radix-style component shapes), OR
 *   - An `export { Button }` / `export { Foo as Button }` that surfaces a
 *     known-primitive name regardless of how it was produced.
 *
 * False-positive avoidance:
 *   - A local function named `Button` that does NOT return JSX (e.g. a helper)
 *     is ignored unless it is exported under a primitive name.
 *   - Importing the primitive from `@ax/ui` is the CORRECT pattern and is never
 *     flagged (imports are only flagged for local `components/ui/**` specifiers).
 */

// The catalog primitive names. Keep in sync with packages/ui/src/index.ts.
const CATALOG_PRIMITIVES = new Set([
  'Button',
  'Input',
  'Label',
  'Field',
  'Card',
  'CardHeader',
  'CardTitle',
  'CardDescription',
  'CardContent',
  'CardFooter',
  'Alert',
  'Badge',
  'Spinner',
  'Switch',
])

const CATALOG_PACKAGE = '@ax/ui'
const BLOCKS_PACKAGE = '@ax/blocks'

function isCatalogPrimitive(name) {
  return typeof name === 'string' && CATALOG_PRIMITIVES.has(name)
}

/** Is this file inside a per-persona app (path has an `apps/` segment)? */
function isAppFile(filename) {
  if (typeof filename !== 'string' || filename.length === 0) return false
  const normalized = filename.replace(/\\/g, '/')
  return /(^|\/)apps\//.test(normalized)
}

/** A relative specifier that points into a local `components/ui` tree. */
function isLocalComponentsUiImport(source) {
  if (typeof source !== 'string') return false
  const isRelative = source.startsWith('.') || source.startsWith('/')
  if (!isRelative) return false
  const normalized = source.replace(/\\/g, '/')
  return /(^|\/)components\/ui(\/|$)/.test(normalized)
}

function returnsJSX(fnNode) {
  if (!fnNode || typeof fnNode !== 'object') return false
  const body = fnNode.body
  if (!body) return false
  if (body.type === 'JSXElement' || body.type === 'JSXFragment') return true
  if (body.type !== 'BlockStatement') return false
  for (const stmt of body.body) {
    if (stmt && stmt.type === 'ReturnStatement' && stmt.argument) {
      if (containsJSX(stmt.argument)) return true
    }
  }
  return false
}

function containsJSX(expr) {
  if (!expr || typeof expr !== 'object') return false
  if (expr.type === 'JSXElement' || expr.type === 'JSXFragment') return true
  if (expr.type === 'ConditionalExpression') {
    return containsJSX(expr.consequent) || containsJSX(expr.alternate)
  }
  if (expr.type === 'LogicalExpression') {
    return containsJSX(expr.left) || containsJSX(expr.right)
  }
  return false
}

/** forwardRef(...) / memo(...) / React.forwardRef(...) factory call? */
function isComponentFactoryCall(node) {
  if (!node || node.type !== 'CallExpression') return false
  const callee = node.callee
  if (callee.type === 'Identifier') {
    return callee.name === 'forwardRef' || callee.name === 'memo'
  }
  if (callee.type === 'MemberExpression' && callee.property.type === 'Identifier') {
    return callee.property.name === 'forwardRef' || callee.property.name === 'memo'
  }
  return false
}

/** @type {import("eslint").Rule.RuleModule} */
const rule = {
  meta: {
    type: 'problem',
    docs: {
      description:
        'In per-persona apps (apps/**), disallow defining/exporting a component named like a catalog primitive, or importing a local components/ui/** module. Primitives MUST come from @ax/ui (blocks from @ax/blocks).',
      recommended: true,
      url: 'https://github.com/ax-template/practices-react/blob/main/rules/no-app-local-ui-primitives.md',
    },
    schema: [],
    messages: {
      localComponentDefinition:
        "App-local UI primitive '{{name}}' is forbidden. Per-persona apps must import '{{name}}' from the shared catalog ('{{pkg}}'), not define their own. Delete this and `import { {{name}} } from '{{pkg}}'`.",
      localComponentExport:
        "App-local export of catalog primitive '{{name}}' is forbidden. Re-export or import it from the shared catalog ('{{pkg}}') instead of shipping an app-local copy.",
      localComponentsUiImport:
        "App-local 'components/ui' module ('{{source}}') is forbidden. Per-persona apps must import primitives from '{{pkg}}' and composed blocks from '{{blocks}}'.",
    },
  },

  create(context) {
    const filename =
      typeof context.filename === 'string' ? context.filename : context.getFilename()
    if (!isAppFile(filename)) return {}

    function reportDefinition(node, name) {
      context.report({
        node,
        messageId: 'localComponentDefinition',
        data: { name, pkg: CATALOG_PACKAGE },
      })
    }

    return {
      // import X from './components/ui/button' (or any local components/ui module)
      ImportDeclaration(node) {
        const source = node.source && node.source.value
        if (isLocalComponentsUiImport(source)) {
          context.report({
            node,
            messageId: 'localComponentsUiImport',
            data: { source, pkg: CATALOG_PACKAGE, blocks: BLOCKS_PACKAGE },
          })
        }
      },

      // function Button() { return <.../> }
      FunctionDeclaration(node) {
        const name = node.id && node.id.name
        if (isCatalogPrimitive(name) && returnsJSX(node)) {
          reportDefinition(node, name)
        }
      },

      // const Button = () => <.../>   |   const Button = forwardRef(...)
      VariableDeclarator(node) {
        if (node.id.type !== 'Identifier') return
        const name = node.id.name
        if (!isCatalogPrimitive(name)) return
        const init = node.init
        if (!init) return
        if (
          (init.type === 'ArrowFunctionExpression' || init.type === 'FunctionExpression') &&
          returnsJSX(init)
        ) {
          reportDefinition(node, name)
          return
        }
        if (isComponentFactoryCall(init)) {
          reportDefinition(node, name)
        }
      },

      // export { Button }   |   export { Foo as Button }   |   export const Button = ...
      ExportNamedDeclaration(node) {
        // Skip re-exports FROM the catalog itself — re-exporting @ax/ui is fine.
        const fromSource = node.source && node.source.value
        if (fromSource === CATALOG_PACKAGE || fromSource === BLOCKS_PACKAGE) return
        for (const spec of node.specifiers || []) {
          if (spec.type !== 'ExportSpecifier') continue
          const exportedName = spec.exported && spec.exported.name
          if (isCatalogPrimitive(exportedName)) {
            context.report({
              node: spec,
              messageId: 'localComponentExport',
              data: { name: exportedName, pkg: CATALOG_PACKAGE },
            })
          }
        }
      },
    }
  },
}

export default rule
