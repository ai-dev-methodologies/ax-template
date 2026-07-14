---
title: "Sample internal_design rule whose external citation lacks the generic-principle caveat"
rule_id: sample-missing-caveat
impact: MEDIUM
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "RFC 7807 — Problem Details for HTTP APIs: a generic principle the design decision rests on."
    url: "https://www.rfc-editor.org/rfc/rfc7807"
    quoted_at: "2026-07-14"
---

# Sample rule (fixture)

FAIL fixture for evidence_guard [1] P2-20 caveat check: this rule is
`provenance_class: internal_design` and carries a `source_type: external`
evidence entry, but the entry is MISSING `anchors: generic_principle_only`.
evidence_guard must exit 1 (VIOLATION) on this catalog.
