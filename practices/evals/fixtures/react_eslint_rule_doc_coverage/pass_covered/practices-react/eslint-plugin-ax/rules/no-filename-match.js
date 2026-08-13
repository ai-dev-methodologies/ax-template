/** fixture: an ESLint rule covered via METHOD (1) — filename match (no-filename-match.md). */

/** @type {import("eslint").Rule.RuleModule} */
const rule = {
  meta: { type: 'problem', docs: { description: 'fixture rule covered by filename match' }, schema: [] },
  create() {
    return {}
  },
}

export default rule
