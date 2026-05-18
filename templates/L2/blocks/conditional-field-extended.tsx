/*
---
template_id: L2/blocks/conditional-field-extended
layer: L2
provenance_class: internal_design
supersedes: L2/blocks/conditional-field (SP15 shell — deprecated, re-exports this file)
evidence:
  - source_type: external
    citation: "React Hook Form docs — useWatch (conditional field pattern)"
    url: "https://react-hook-form.com/docs/usewatch"
  - source_type: internal
    rationale: "Extended L2 form block — RHF useWatch-based conditional field rendering with animated mount/unmount."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/

'use client'

import * as React from 'react'
import { useWatch, useFormContext, type FieldValues, type Path } from 'react-hook-form'

// ─── types ────────────────────────────────────────────────────────────────────

export interface ConditionalFieldExtendedProps<TFieldValues extends FieldValues> {
  /**
   * The form field whose value controls visibility.
   * Observed via RHF useWatch — no extra re-renders.
   */
  watchField: Path<TFieldValues>
  /**
   * Condition function — return true to show children.
   * Receives the current watched value.
   */
  condition: (value: unknown) => boolean
  children: React.ReactNode
  /** If true, keep the DOM node mounted but visually hidden (preserves field values) */
  keepMounted?: boolean
  className?: string
}

/**
 * ConditionalFieldExtended — shows/hides form sections based on another field's value.
 *
 * Supersedes the SP15 shell `conditional-field.tsx` (which re-exports this file).
 *
 * Uses RHF `useWatch` to observe the target field without triggering a full form re-render.
 * Must be used inside a `<FormProvider>`.
 *
 * **keepMounted:** when true, children remain in the DOM (display:none) so their
 * RHF values are preserved. When false (default), children unmount and their values
 * are dropped from the form state — useful when the conditional section is truly
 * irrelevant to the submission.
 *
 * @example
 * // Show shipping fields only when "shipToAddress" is checked
 * <ConditionalFieldExtended
 *   watchField="shipToAddress"
 *   condition={(v) => v === true}
 * >
 *   <AddressFields prefix="shipping" />
 * </ConditionalFieldExtended>
 */
export default function ConditionalFieldExtended<TFieldValues extends FieldValues>({
  watchField,
  condition,
  children,
  keepMounted = false,
  className,
}: ConditionalFieldExtendedProps<TFieldValues>) {
  const { control } = useFormContext<TFieldValues>()
  const value = useWatch({ control, name: watchField })
  const isVisible = condition(value)

  if (keepMounted) {
    return (
      <div
        className={className}
        hidden={!isVisible}
        aria-hidden={!isVisible}
        inert={!isVisible ? ('' as unknown as boolean) : undefined}
      >
        {children}
      </div>
    )
  }

  if (!isVisible) return null

  return (
    <div className={className} role="group">
      {children}
    </div>
  )
}
