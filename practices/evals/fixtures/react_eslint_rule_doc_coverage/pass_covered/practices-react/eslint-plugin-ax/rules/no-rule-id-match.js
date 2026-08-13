/** fixture: an ESLint rule covered via METHOD (2) — a doc under a DIFFERENT filename whose
 *  frontmatter verification.rule_id declares "ax/no-rule-id-match" (the
 *  bundle-barrel-imports.md / rerender-no-inline-components.md precedent). */

/** @type {import("eslint").Rule.RuleModule} */
const rule = {
  meta: { type: 'problem', docs: { description: 'fixture rule covered by verification.rule_id match' }, schema: [] },
  create() {
    return {}
  },
}

export default rule
