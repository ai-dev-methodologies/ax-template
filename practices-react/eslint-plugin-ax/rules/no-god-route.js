/**
 * ax/no-god-route  (FE-ROUTE-THIN size signal, advisory/warn)
 *
 * A Next.js App Router CLIENT route file (`app/**\/page|layout` with `"use client"`)
 * that grows past a generous line threshold is a "god route" smell — it is doing the
 * job of a feature container (form state, business logic, inline UI) instead of
 * delegating to `@/features/<f>`.
 *
 * Honest limit (spec §4, ralplan codex critic): line count is a gameable proxy, so
 * this ships as ADVISORY (warn), not a hard block. It surfaces fat routes as a
 * visible TIER-2 remediation signal (extract to features/<f> containers) without
 * breaking the reference app. Server route files are NOT checked (their size is
 * usually data-layer, not UI logic).
 *
 * Spec: docs/superpowers/specs/2026-06-08-frontend-decomposition-rules-design.md §4 (TIER-1, advisory).
 * Backend analog: HG-ANTI-GODSERVICE-TX (heuristic with honest limit).
 */

import { isRouteFile, hasUseClientDirective } from '../lib/feature-layout.js'

const DEFAULT_MAX_LINES = 100

/** @type {import("eslint").Rule.RuleModule} */
const rule = {
  meta: {
    type: 'suggestion',
    docs: {
      description:
        'A "use client" route file should stay thin; one exceeding the line threshold likely belongs in a feature container.',
      recommended: false,
      url: 'https://github.com/ax-template/practices-react/blob/main/rules/no-god-route.md',
    },
    schema: [
      {
        type: 'object',
        properties: { maxLines: { type: 'integer', minimum: 1 } },
        additionalProperties: false,
      },
    ],
    messages: {
      godRoute:
        'God route: this "use client" route file is {{lines}} lines (> {{max}}). Extract the form/business logic + inline UI into a @/features/<f> container and keep the route thin.',
    },
  },

  create(context) {
    const filename =
      typeof context.filename === 'string' ? context.filename : context.getFilename()
    if (!isRouteFile(filename)) return {}
    const max = (context.options[0] && context.options[0].maxLines) || DEFAULT_MAX_LINES
    const sourceCode = context.sourceCode || context.getSourceCode()

    return {
      Program(node) {
        if (!hasUseClientDirective(node)) return
        const lines = sourceCode.lines.length
        if (lines > max) {
          context.report({ node, messageId: 'godRoute', data: { lines, max } })
        }
      },
    }
  },
}

export default rule
