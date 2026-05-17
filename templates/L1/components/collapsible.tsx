/*
---
template_id: L1/components/collapsible
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: collapsible
    quote: "An interactive component which expands/collapses a panel."
  - source_type: upstream_id
    upstream_id: wcag-2-2
    section: 4.1.2-name-role-value
    quote: "For all user interface components, the name and role can be programmatically determined."
a11y_criteria:
  - "WCAG 2.2 SC 4.1.2 — aria-expanded managed by Radix"
  - "WCAG 2.2 SC 2.4.11 Focus Appearance"
  - "Trigger must be a <button> not a styled <div>"
dependencies: ["@radix-ui/react-collapsible"]
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#collapsible"
---
*/
import * as CollapsiblePrimitive from '@radix-ui/react-collapsible'

const Collapsible = CollapsiblePrimitive.Root
const CollapsibleTrigger = CollapsiblePrimitive.CollapsibleTrigger
const CollapsibleContent = CollapsiblePrimitive.CollapsibleContent

export { Collapsible, CollapsibleTrigger, CollapsibleContent }
