/** fixture: an ESLint rule covered via METHOD (2) — a doc under a DIFFERENT filename whose
 *  frontmatter verification.rule_id declares "ax/no-fixture-alias-match", and THAT doc's
 *  filename slug ("some-other-catalog-slug") is what appears in INDEX.md. This fixture's
 *  failure is entirely check (a); check (b) must stay clean here. */

/** @type {import("eslint").Rule.RuleModule} */
const rule = {
  meta: { type: 'problem', docs: { description: 'fixture rule covered by aliased doc slug in INDEX.md' }, schema: [] },
  create() {
    return {}
  },
}

export default rule
