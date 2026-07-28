/*
---
template_id: L1/components/sample-widget
layer: L1
provenance_class: locked_constraint
evidence:
  - source_type: upstream_id
    upstream_id: sample-src
    section: "Real section"
    quote: "All widgets are blue by default per the sample vendor's design system."
dependencies: []
imports_from: [L0]
imports_forbidden: [L2, L3, L4, app/, lib/auth/]
---
*/

export function SampleWidget() {
  return null
}
