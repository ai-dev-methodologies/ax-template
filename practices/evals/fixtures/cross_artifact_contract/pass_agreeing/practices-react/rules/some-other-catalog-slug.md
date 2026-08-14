---
title: Fixture doc covering no-fixture-alias-match via verification.rule_id (filename deliberately mismatched)
verification:
  type: lint
  rule_id: "ax/no-fixture-alias-match"
  status: shipped
---

## Fixture doc — verification.rule_id match

This file's name (`some-other-catalog-slug.md`) does NOT match the ESLint rule id
(`no-fixture-alias-match`) on purpose — it proves METHOD (2) (frontmatter
`verification.rule_id: "ax/no-fixture-alias-match"` + the doc's own slug being visible in
INDEX.md) is sufficient coverage on its own.
