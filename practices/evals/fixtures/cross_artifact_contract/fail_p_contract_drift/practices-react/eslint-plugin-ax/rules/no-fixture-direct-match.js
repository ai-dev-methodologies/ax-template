/** fixture: an ESLint rule that IS covered in INDEX.md — isolates this fixture's failure to
 *  check (a) only, so it does not accidentally also trip check (b). */

/** @type {import("eslint").Rule.RuleModule} */
const rule = {
  meta: { type: 'problem', docs: { description: 'fixture rule covered by direct id match in INDEX.md' }, schema: [] },
  create() {
    return {}
  },
}

export default rule
