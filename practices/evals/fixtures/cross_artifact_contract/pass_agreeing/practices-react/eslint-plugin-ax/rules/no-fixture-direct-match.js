/** fixture: an ESLint rule covered via METHOD (1) — its own id appears literally in INDEX.md. */

/** @type {import("eslint").Rule.RuleModule} */
const rule = {
  meta: { type: 'problem', docs: { description: 'fixture rule covered by direct id match in INDEX.md' }, schema: [] },
  create() {
    return {}
  },
}

export default rule
