---
title: Fixture doc covering no-rule-id-match via verification.rule_id (filename deliberately mismatched)
verification:
  type: lint
  rule_id: "ax/no-rule-id-match"
  status: shipped
---

## Fixture doc — verification.rule_id match

This file's name (`some-other-catalog-slug.md`) does NOT match the ESLint rule id
(`no-rule-id-match`) on purpose — it proves METHOD (2) (frontmatter
`verification.rule_id: "ax/no-rule-id-match"`) is sufficient coverage on its own, the
same pattern `bundle-barrel-imports.md` uses to cover `ax/no-broad-barrel-imports` and
`rerender-no-inline-components.md` uses to cover `ax/no-inline-component-definition`.

Also carries an UNRELATED top-level `rule_id:` field (no `ax/` prefix, no quotes) to prove
the guard does not confuse it with `verification.rule_id`:

rule_id: some-other-catalog-slug
