/**
 * ax/no-feature-internal-import  (FE-PUBLISHED-API)
 *
 * A file OUTSIDE the feature tree (app/, components/, lib/, project root) must
 * consume a feature only through its published barrel — `@/features/<f>` or a
 * slice barrel `@/features/<f>/<slice>`. Reaching past the barrel into a slice's
 * internals (`@/features/<f>/<slice>/<file>`) from outside is forbidden.
 *
 * (Feature-to-feature deep imports are governed by ax/no-cross-feature-deep-import;
 * this rule targets NON-feature importers, so the two never double-report.)
 *
 * Spec: docs/superpowers/specs/2026-06-08-frontend-decomposition-rules-design.md §4 (TIER-0).
 * Backend analog: @PublishedApi default-deny / published-API-only access.
 */

import { toSrcRelative, resolveImport, classifySrcPath } from '../lib/feature-layout.js'

/** @type {import("eslint").Rule.RuleModule} */
const rule = {
  meta: {
    type: 'problem',
    docs: {
      description:
        'Outside a feature, import it only through its published barrel — never deep into a slice’s internals.',
      recommended: true,
      url: 'https://github.com/ax-template/practices-react/blob/main/rules/no-feature-internal-import.md',
    },
    schema: [],
    messages: {
      featureInternal:
        "Feature-internal import: '{{importerLayer}}' code reaches into feature '{{to}}' internals ('{{source}}'). Import the feature's published barrel (@/features/{{to}} or @/features/{{to}}/<slice>) instead.",
    },
  },

  create(context) {
    const filename =
      typeof context.filename === 'string' ? context.filename : context.getFilename()
    const importerSrcRel = toSrcRelative(filename)
    const importer = classifySrcPath(importerSrcRel)
    // Only NON-feature importers are governed here (feature->feature = the other rule).
    if (importer.layer === 'features') return {}
    // Files outside the src tree entirely are not governed.
    if (!importer.layer) return {}

    return {
      ImportDeclaration(node) {
        const source = node.source && node.source.value
        const target = classifySrcPath(resolveImport(source, importerSrcRel))
        if (target.layer !== 'features' || !target.feature) return
        if (target.isBarrel) return // barrel import is the correct public access
        context.report({
          node,
          messageId: 'featureInternal',
          data: { importerLayer: importer.layer, to: target.feature, source },
        })
      },
    }
  },
}

export default rule
