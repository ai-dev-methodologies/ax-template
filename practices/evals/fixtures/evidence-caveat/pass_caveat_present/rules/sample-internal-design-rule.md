---
title: "Sample internal_design rule whose external citation carries the generic-principle caveat"
rule_id: sample-caveat-present
impact: MEDIUM
provenance_class: internal_design
evidence:
  - source_type: external
    anchors: generic_principle_only
    citation: "RFC 7807 — Problem Details for HTTP APIs: a generic principle the design decision rests on. (The specific rule is an ax-template layer decision, not an RFC requirement.)"
    url: "https://www.rfc-editor.org/rfc/rfc7807"
    quoted_at: "2026-07-14"
---

# Sample rule (fixture)

PASS fixture for evidence_guard [1] P2-20 caveat check: this rule is
`provenance_class: internal_design` and carries a `source_type: external`
evidence entry that correctly declares `anchors: generic_principle_only`.
evidence_guard must exit 0 on this catalog.
