/**
 * ax/no-cross-feature-deep-import  (FE-FEAT-ISOLATION)
 *
 * A file inside `src/features/<A>/` must not reach into ANOTHER feature's
 * internals. Importing `@/features/<B>/<slice>/<file>` (a deep path past the
 * slice barrel) from feature A is forbidden — features are siblings, composed
 * at the app layer or via the shared kernel, never coupled to each other's
 * internals.
 *
 * Allowed from a feature: another feature's BARREL (`@/features/<B>` or
 * `@/features/<B>/<slice>`), the shared kernel (`@/components/**`, `@/lib/**`,
 * `@ax/ui`, `@ax/blocks`), and the feature's own internals.
 *
 * Spec: docs/superpowers/specs/2026-06-08-frontend-decomposition-rules-design.md §4 (TIER-0).
 * Backend analog: HG-FEAT-ISOLATION.
 */

import { toSrcRelative, resolveImport, classifySrcPath } from '../lib/feature-layout.js'

/** @type {import("eslint").Rule.RuleModule} */
const rule = {
  meta: {
    type: 'problem',
    docs: {
      description:
        'A feature must not deep-import another feature’s internals; cross-feature reuse goes through the target’s barrel or the shared kernel.',
      recommended: true,
      url: 'https://github.com/ax-template/practices-react/blob/main/rules/no-cross-feature-deep-import.md',
    },
    schema: [],
    messages: {
      crossFeatureDeep:
        "Cross-feature deep import: feature '{{from}}' reaches into feature '{{to}}' internals ('{{source}}'). Import the target feature's barrel (@/features/{{to}} or @/features/{{to}}/<slice>) or move the shared code to the kernel.",
    },
  },

  create(context) {
    const filename =
      typeof context.filename === 'string' ? context.filename : context.getFilename()
    const importerSrcRel = toSrcRelative(filename)
    const importer = classifySrcPath(importerSrcRel)
    // Only files inside a feature are governed by this rule.
    if (importer.layer !== 'features' || !importer.feature) return {}

    return {
      ImportDeclaration(node) {
        const source = node.source && node.source.value
        const target = classifySrcPath(resolveImport(source, importerSrcRel))
        if (target.layer !== 'features' || !target.feature) return
        if (target.feature === importer.feature) return // own feature — fine
        if (target.isBarrel) return // cross-feature BARREL import is allowed
        context.report({
          node,
          messageId: 'crossFeatureDeep',
          data: { from: importer.feature, to: target.feature, source },
        })
      },
    }
  },
}

export default rule
