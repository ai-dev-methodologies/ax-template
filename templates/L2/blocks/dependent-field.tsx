/*
---
template_id: L2/blocks/dependent-field
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "React Hook Form docs — useWatch (watch dependent field values)"
    url: "https://react-hook-form.com/docs/usewatch"
  - source_type: internal
    rationale: "L2 form block — renders a field whose options or value are derived from another field. New in SP27."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/

'use client'

import * as React from 'react'
import { useWatch, useFormContext, useController, type FieldValues, type Path } from 'react-hook-form'

// ─── types ────────────────────────────────────────────────────────────────────

export interface DependentOption {
  value: string
  label: string
}

export interface DependentFieldProps<TFieldValues extends FieldValues> {
  /** The field this select is bound to */
  name: Path<TFieldValues>
  /** The upstream field whose value drives the options list */
  dependsOn: Path<TFieldValues>
  /** Maps upstream value → available options for this field */
  optionsMap: (upstreamValue: unknown) => DependentOption[]
  label?: string
  placeholder?: string
  required?: boolean
  disabled?: boolean
  className?: string
}

/**
 * DependentField — a select whose options are driven by another field's value.
 *
 * Uses RHF `useWatch` on `dependsOn` to reactively recompute the options list.
 * When the upstream value changes, the selected value is cleared to prevent
 * stale selections.
 *
 * Must be used inside a `<FormProvider>`.
 *
 * @example
 * // Province select driven by Country select
 * <DependentField
 *   name="province"
 *   dependsOn="country"
 *   label="Province"
 *   optionsMap={(country) => PROVINCES_BY_COUNTRY[country as string] ?? []}
 * />
 */
export default function DependentField<TFieldValues extends FieldValues>({
  name,
  dependsOn,
  optionsMap,
  label,
  placeholder = 'Select…',
  required = false,
  disabled = false,
  className,
}: DependentFieldProps<TFieldValues>) {
  const { control } = useFormContext<TFieldValues>()
  const { field, fieldState: { error } } = useController({ control, name })

  const upstreamValue = useWatch({ control, name: dependsOn })
  const options = optionsMap(upstreamValue)

  // Clear selection when upstream changes
  const prevUpstreamRef = React.useRef<unknown>(upstreamValue)
  React.useEffect(() => {
    if (prevUpstreamRef.current !== upstreamValue) {
      field.onChange('')
      prevUpstreamRef.current = upstreamValue
    }
  }, [upstreamValue, field])

  const id = `dep-field-${String(name).replace(/\./g, '-')}`

  return (
    <div className={['space-y-2', className].filter(Boolean).join(' ')}>
      {label && (
        <label htmlFor={id} className="text-sm font-medium leading-none">
          {label}
          {required && <span aria-hidden="true" className="ml-1 text-destructive">*</span>}
        </label>
      )}

      <select
        id={id}
        {...field}
        required={required}
        disabled={disabled || options.length === 0}
        aria-invalid={!!error}
        aria-describedby={error ? `${id}-error` : undefined}
        className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
      >
        <option value="">{options.length === 0 ? 'Select upstream first' : placeholder}</option>
        {options.map(opt => (
          <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </select>

      {error && (
        <p id={`${id}-error`} role="alert" className="text-xs text-destructive">
          {error.message}
        </p>
      )}
    </div>
  )
}
