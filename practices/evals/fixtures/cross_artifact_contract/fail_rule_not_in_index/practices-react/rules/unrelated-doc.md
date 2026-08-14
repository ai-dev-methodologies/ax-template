---
title: An unrelated catalog doc that does not cover no-orphan-rule
verification:
  type: lint
  rule_id: "ax/no-some-other-rule"
  status: shipped
---

## An unrelated catalog doc

Exists only to prove the guard does not pass by accident when practices-react/rules/ is
non-empty but contains no entry (by direct id or verification.rule_id alias) for
`no-orphan-rule`.
