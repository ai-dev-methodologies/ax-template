/** fixture: an ESLint rule with NO INDEX.md entry anywhere — neither its own id nor an
 *  aliased doc slug — must trip check (b). */

/** @type {import("eslint").Rule.RuleModule} */
const rule = {
  meta: { type: 'problem', docs: { description: 'fixture orphan rule, absent from INDEX.md' }, schema: [] },
  create() {
    return {}
  },
}

export default rule
