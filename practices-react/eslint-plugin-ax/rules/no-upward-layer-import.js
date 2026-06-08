/**
 * ax/no-upward-layer-import  (FE-LAYER-DIRECTION)
 *
 * Layers are single-direction: app (top) -> features -> shared (components/ui +
 * lib) (bottom). A module must not import from a HIGHER layer:
 *   - shared (components/lib) must not import features or app
 *   - features must not import app
 * (Same-layer feature<->feature is governed by ax/no-cross-feature-deep-import,
 * not this rule.)
 *
 * Spec: docs/superpowers/specs/2026-06-08-frontend-decomposition-rules-design.md §4 (TIER-0).
 * Backend analog: layering (controller->service->repository direction).
 */

import { toSrcRelative, resolveImport, classifySrcPath, rankOf } from '../lib/feature-layout.js'

/** @type {import("eslint").Rule.RuleModule} */
const rule = {
  meta: {
    type: 'problem',
    docs: {
      description:
        'Enforce single-direction layer imports: app -> features -> shared (components/lib). No upward imports.',
      recommended: true,
      url: 'https://github.com/ax-template/practices-react/blob/main/rules/no-upward-layer-import.md',
    },
    schema: [],
    messages: {
      upwardImport:
        "Upward layer import: '{{importerLayer}}' must not import from the higher '{{targetLayer}}' layer ('{{source}}'). Allowed direction is app -> features -> shared (components/lib).",
    },
  },

  create(context) {
    const filename =
      typeof context.filename === 'string' ? context.filename : context.getFilename()
    const importerSrcRel = toSrcRelative(filename)
    const importer = classifySrcPath(importerSrcRel)
    if (!importer.layer || rankOf(importer.layer) === 0) return {} // outside the layered tree

    return {
      ImportDeclaration(node) {
        const source = node.source && node.source.value
        const target = classifySrcPath(resolveImport(source, importerSrcRel))
        if (rankOf(target.layer) === 0) return // bare/out-of-tree import — not our concern
        // same-layer feature<->feature is handled by no-cross-feature-deep-import
        if (target.layer === 'features' && importer.layer === 'features') return
        if (rankOf(target.layer) > rankOf(importer.layer)) {
          context.report({
            node,
            messageId: 'upwardImport',
            data: { importerLayer: importer.layer, targetLayer: target.layer, source },
          })
        }
      },
    }
  },
}

export default rule
