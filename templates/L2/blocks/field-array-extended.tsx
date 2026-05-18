/*
---
template_id: L2/blocks/field-array-extended
layer: L2
provenance_class: internal_design
supersedes: L2/blocks/field-array (SP15 shell — deprecated, re-exports this file)
evidence:
  - source_type: external
    citation: "React Hook Form docs — useFieldArray"
    url: "https://react-hook-form.com/docs/usefieldarray"
  - source_type: external
    citation: "Zod docs — array schema (.array(), .min(), .max())"
    url: "https://zod.dev/?id=arrays"
  - source_type: internal
    rationale: "Extended L2 form block — full RHF useFieldArray integration with Zod validation, add/remove/reorder."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/

'use client'

import * as React from 'react'
import {
  useFieldArray,
  useFormContext,
  type FieldValues,
  type Path,
  type ArrayPath,
  type FieldArray,
} from 'react-hook-form'

// ─── types ────────────────────────────────────────────────────────────────────

export interface FieldArrayExtendedProps<
  TFieldValues extends FieldValues,
  TFieldArrayName extends ArrayPath<TFieldValues>,
> {
  /** RHF field array name — must match the path in your Zod schema */
  name: TFieldArrayName
  /** Label shown above the field array */
  label?: string
  /** Factory for the default item appended on Add — must match the array item schema */
  defaultItem: FieldArray<TFieldValues, TFieldArrayName>
  /** Render function for each item row. Receives index and remove handler. */
  renderItem: (params: {
    index: number
    remove: () => void
    isOnly: boolean   // true when only one item remains (disable remove button)
  }) => React.ReactNode
  /** Label for the "Add item" button */
  addLabel?: string
  /** Minimum items (remove disabled when at minimum). Default 1. */
  minItems?: number
  /** Maximum items (add disabled when at maximum). No default. */
  maxItems?: number
  className?: string
}

/**
 * FieldArrayExtended — dynamic list of repeating form fields backed by RHF useFieldArray.
 *
 * Supersedes the SP15 shell `field-array.tsx` (which re-exports this file for back-compat).
 *
 * Must be used inside a `<FormProvider>` (or `useForm()` + `FormProvider` wrapper).
 *
 * @example
 * const form = useForm<z.infer<typeof schema>>({ resolver: zodResolver(schema) })
 *
 * <FormProvider {...form}>
 *   <FieldArrayExtended
 *     name="addresses"
 *     label="Addresses"
 *     defaultItem={{ street: '', city: '' }}
 *     renderItem={({ index, remove, isOnly }) => (
 *       <div className="flex gap-2">
 *         <input {...form.register(`addresses.${index}.street`)} />
 *         <button type="button" onClick={remove} disabled={isOnly}>Remove</button>
 *       </div>
 *     )}
 *   />
 * </FormProvider>
 */
export default function FieldArrayExtended<
  TFieldValues extends FieldValues,
  TFieldArrayName extends ArrayPath<TFieldValues>,
>({
  name,
  label,
  defaultItem,
  renderItem,
  addLabel = 'Add item',
  minItems = 1,
  maxItems,
  className,
}: FieldArrayExtendedProps<TFieldValues, TFieldArrayName>) {
  const { control, formState: { errors } } = useFormContext<TFieldValues>()
  const { fields, append, remove } = useFieldArray({ control, name })

  const atMin = fields.length <= minItems
  const atMax = maxItems !== undefined && fields.length >= maxItems

  // ─── errors at array level (e.g. z.array().min(2) message) ────────────────
  const arrayError = (errors as Record<string, { message?: string }>)[name as string]?.message

  return (
    <fieldset className={['space-y-3', className].filter(Boolean).join(' ')}>
      {label && (
        <legend className="text-sm font-medium leading-none">
          {label}
        </legend>
      )}

      <ol className="space-y-2">
        {fields.map((field, index) => (
          <li key={field.id}>
            {renderItem({
              index,
              remove: () => remove(index),
              isOnly: atMin,
            })}
          </li>
        ))}
      </ol>

      {arrayError && (
        <p role="alert" className="text-xs text-destructive">
          {arrayError}
        </p>
      )}

      <button
        type="button"
        disabled={atMax}
        onClick={() => append(defaultItem)}
        className="inline-flex items-center gap-1 rounded-md border border-dashed border-border px-3 py-1.5 text-sm text-muted-foreground hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:opacity-40"
      >
        <span aria-hidden="true">+</span>
        {addLabel}
      </button>
    </fieldset>
  )
}
