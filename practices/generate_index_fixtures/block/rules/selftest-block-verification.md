---
title: "Selftest Block-style Verification Rule"
impact: "MEDIUM"
tags:
  - selftest
  - block
verification:
  gradle_task: testSelftestBlock
  tag: SELFTEST-BLOCK-001
---

Fixture-only rule for `practices/generate_index_selftest.sh`. Exercises the
block-mapping `verification:` shape (`gradle_task` + `tag` keys on separate
lines) — expected classification is `gradle:testSelftestBlock`.
