/*
---
template_id: L2/blocks/form-section
layer: L2
provenance_class: internal_design
status: deprecated
superseded_by: L2/blocks/form-section-extended
evidence:
  - source_type: internal
    rationale: "SP15 shell — deprecated. Use form-section-extended instead. Re-exports for import-path back-compat."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/

/**
 * @deprecated Use `form-section-extended` instead.
 * This SP15 shell is kept for import-path back-compat only.
 * All new code should import from `./form-section-extended`.
 */
export { default } from './form-section-extended'
export type { FormSectionExtendedProps as FormSectionProps } from './form-section-extended'
