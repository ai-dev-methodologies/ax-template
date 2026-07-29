---
title: Sample anchor target for the P3-90 anchors_rule axis fixture
impact: LOW
spec_ref: "specs/sample-l0.yaml#SAMPLE-001"
evidence:
  - source_type: external
    citation: "Fixture-only rule file — exists so the anchors axis has a resolvable target"
    url: "https://example.invalid/fixture"
---
Fixture-only rule body. Present so `anchors_rule: sample-anchor-rule.md` RESOLVES in
`pass_anchors_clean` and so the `fail_stale_anchors_rule` sibling differs by exactly one
thing: the anchor it names does not exist.
