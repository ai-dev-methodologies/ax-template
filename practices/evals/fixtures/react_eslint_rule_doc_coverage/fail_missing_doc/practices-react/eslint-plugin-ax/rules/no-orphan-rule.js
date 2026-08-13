/** fixture: an ESLint rule with NO catalog doc anywhere — must trip the guard. */

/** @type {import("eslint").Rule.RuleModule} */
const rule = {
  meta: { type: 'problem', docs: { description: 'fixture orphan rule' }, schema: [] },
  create() {
    return {}
  },
}

export default rule
