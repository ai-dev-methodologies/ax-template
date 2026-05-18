/*
---
template_id: L2/blocks/field-array
layer: L2
provenance_class: internal_design
status: deprecated
superseded_by: L2/blocks/field-array-extended
evidence:
  - source_type: internal
    rationale: "SP15 shell — deprecated. Use field-array-extended instead. Re-exports for import-path back-compat."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/

/**
 * @deprecated Use `field-array-extended` instead.
 * This SP15 shell is kept for import-path back-compat only.
 * All new code should import from `./field-array-extended`.
 */
export { default } from './field-array-extended'
export type { FieldArrayExtendedProps as FieldArrayProps } from './field-array-extended'
