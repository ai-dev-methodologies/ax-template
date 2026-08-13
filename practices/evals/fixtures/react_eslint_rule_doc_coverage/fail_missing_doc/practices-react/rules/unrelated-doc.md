---
title: An unrelated catalog doc that does not cover no-orphan-rule
verification:
  type: lint
  rule_id: "ax/no-some-other-rule"
  status: shipped
---

## An unrelated catalog doc

This doc exists only to prove the guard does not pass by accident when the rules/
directory is non-empty but contains no entry (by filename or verification.rule_id)
for `no-orphan-rule`.
