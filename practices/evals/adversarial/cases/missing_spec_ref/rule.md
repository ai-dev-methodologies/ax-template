---
title: Intentionally broken rule (adversarial case — missing spec_ref)
impact: MEDIUM
impactDescription: "This is an adversarial fixture; it MUST be rejected by spec_ref_guard"
tags:
  - adversarial
verification:
  gradle_task: testPractices
  tag: PRACTICES-NONE
---

## Intentionally broken rule

This rule is missing the required `spec_ref` frontmatter field on purpose. The adversarial
runner copies this into `practices/rules/` and runs `spec_ref_guard.sh`. The guard MUST exit
non-zero, which the runner translates to a `BLOCK` verdict.

If the guard ever lets this case through, the runner returns `FAIL` and the rubric's
adversarial axis flags the regression.
