/**
 * ax/no-broad-barrel-imports
 *
 * Flags `import { ... } from 'X'` for X in a configurable set of known-expensive
 * barrel packages, when the import is NOT from a documented subpath.
 *
 * Pairs 1:1 with: practices-react/rules/bundle-barrel-imports.md
 *
 * The default expensive-package list is intentionally SMALL — it should be
 * configured per-project to reflect the bundler's auto-optimization. Next.js 16
 * default-optimizes many libraries (lucide-react, @mui/material, etc.); those
 * should typically be removed from the project's allowlist.
 *
 * Options (single object):
 *   - expensivePackages: string[]
 *       Packages where broad barrel imports are flagged.
 *       Default: a conservative set of known-large libraries with poor
 *       tree-shaking when not auto-optimized.
 *   - excludeOptimized: string[]
 *       Names from the project's bundler-auto-optimized list. If a package
 *       appears here it is silently ignored even when also listed in
 *       expensivePackages.
 */

const DEFAULT_EXPENSIVE = [
  // Libraries that historically ship 1000s of modules via a single barrel.
  // Most of these are auto-optimized by Next.js 16 — list provided so vanilla
  // bundlers (Vite/Rollup/esbuild without manual tree-shake setup) still get
  // flagged.
  'lodash',
  'ramda',
  'rxjs',
]

/** @type {import("eslint").Rule.RuleModule} */
const rule = {
  meta: {
    type: 'suggestion',
    docs: {
      description:
        'Disallow broad barrel imports from packages known to lack effective tree-shaking on the project bundler.',
      recommended: false,
      url: 'https://github.com/ax-template/practices-react/blob/main/rules/bundle-barrel-imports.md',
    },
    schema: [
      {
        type: 'object',
        properties: {
          expensivePackages: {
            type: 'array',
            items: { type: 'string' },
          },
          excludeOptimized: {
            type: 'array',
            items: { type: 'string' },
          },
        },
        additionalProperties: false,
      },
    ],
    messages: {
      broadBarrelImport:
        "Broad import from '{{name}}' may pull in many unused modules. Prefer a documented subpath import (e.g. '{{name}}/<member>'), or add this package to the project's bundler auto-optimization list.",
    },
  },

  create(context) {
    const options = context.options[0] || {}
    const expensive = new Set(options.expensivePackages ?? DEFAULT_EXPENSIVE)
    const excluded = new Set(options.excludeOptimized ?? [])

    function isExpensivePackage(source) {
      if (excluded.has(source)) return false
      if (expensive.has(source)) return true
      // Sub-paths like 'lodash/get' are documented subpaths — allow.
      // Only flag bare-package imports.
      return false
    }

    return {
      ImportDeclaration(node) {
        const src = node.source.value
        if (typeof src !== 'string') return
        if (!isExpensivePackage(src)) return

        // Allow default-only imports (e.g. `import _ from 'lodash'`) — the rule
        // targets `{ a, b, c }` named-imports that pull through the barrel.
        const hasNamedBarrel = node.specifiers.some(
          (s) => s.type === 'ImportSpecifier',
        )
        if (!hasNamedBarrel) return

        context.report({
          node,
          messageId: 'broadBarrelImport',
          data: { name: src },
        })
      },
    }
  },
}

export default rule
