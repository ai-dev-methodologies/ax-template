/*
---
template_id: L2/blocks/form-error-summary
layer: L2
provenance_class: internal_design
status: deprecated
superseded_by: L2/blocks/form-error-summary-extended
evidence:
  - source_type: internal
    rationale: "SP15 shell — deprecated. Use form-error-summary-extended instead. Re-exports for import-path back-compat."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/

/**
 * @deprecated Use `form-error-summary-extended` instead.
 * This SP15 shell is kept for import-path back-compat only.
 * All new code should import from `./form-error-summary-extended`.
 */
export { default } from './form-error-summary-extended'
export type { FormErrorSummaryExtendedProps as FormErrorSummaryProps } from './form-error-summary-extended'
