/*
---
template_id: L1/components/sample-widget
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: sample-src-2026-05
    section: "Sample Section"
    quote: "A registered snapshot id with a non-empty section and quote."
  - source_type: external
    citation: "Sample external standard — clause 1"
    url: "https://example.invalid/standard#clause-1"
  - source_type: internal
    rationale: "Fixture-local design note; internal entries must carry a rationale."
dependencies: [@scoped/pkg]
imports_from: [L1]
---
*/

export function SampleWidget() {
  return null
}
