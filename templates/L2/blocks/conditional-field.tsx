/*
---
template_id: L2/blocks/conditional-field
layer: L2
provenance_class: internal_design
status: deprecated
superseded_by: L2/blocks/conditional-field-extended
evidence:
  - source_type: internal
    rationale: "SP15 shell — deprecated. Use conditional-field-extended instead. Re-exports for import-path back-compat."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/

/**
 * @deprecated Use `conditional-field-extended` instead.
 * This SP15 shell is kept for import-path back-compat only.
 * All new code should import from `./conditional-field-extended`.
 */
export { default } from './conditional-field-extended'
export type { ConditionalFieldExtendedProps as ConditionalFieldProps } from './conditional-field-extended'
