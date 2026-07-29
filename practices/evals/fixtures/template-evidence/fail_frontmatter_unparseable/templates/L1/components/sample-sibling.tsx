/*
---
template_id: L1/components/sample-sibling
layer: L1
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "Clean same-shape sibling. Present so the fixture isolates exactly ONE
      clause: without it the ZERO_VERIFIED backstop would also fire and the fixture would
      no longer prove that its own targeted check is what blocks."
dependencies: []
imports_from: [L1]
---
*/

export function SampleSibling() {
  return null
}
