/*
---
template_id: L2/blocks/crud-create-form
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "L2 CRUD block — generic create form driven by a field schema; domain-specific fields passed as props."
dependencies: [button, input, label, select, textarea]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
import * as React from 'react'

export type FieldType = 'text' | 'email' | 'password' | 'number' | 'textarea' | 'select'

export interface FieldDef {
  key: string
  label: string
  type: FieldType
  placeholder?: string
  required?: boolean
  options?: Array<{ value: string; label: string }>
}

export interface CrudCreateFormProps {
  fields: FieldDef[]
  onSubmit: (data: Record<string, unknown>) => void
  isLoading?: boolean
  submitLabel?: string
  /**
   * Server-side validation errors keyed by field key — the output of
   * L0/fork-receiver-kit/parse-field-errors. When present, the matching field
   * is marked aria-invalid and shows its message. This is the seam that lets a
   * problem+json 400 land on the right input (CRUD-FE-006); without it the
   * form could only show a top-level banner. FDW1/FMW2.
   */
  fieldErrors?: Record<string, string>
  /** Slot for additional content below fields */
  extraSlot?: React.ReactNode
}

export default function CrudCreateForm({
  fields,
  onSubmit,
  isLoading = false,
  submitLabel = 'Create',
  fieldErrors = {},
  extraSlot,
}: CrudCreateFormProps) {
  const [values, setValues] = React.useState<Record<string, string>>(() =>
    Object.fromEntries(fields.map(f => [f.key, '']))
  )

  function set(key: string, value: string) {
    setValues(prev => ({ ...prev, [key]: value }))
  }

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    onSubmit(values)
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-4">
      {fields.map(field => {
        const fieldError = fieldErrors[field.key]
        const errorId = fieldError ? `ccf-${field.key}-error` : undefined
        return (
        <div key={field.key} className="space-y-2">
          <label
            htmlFor={`ccf-${field.key}`}
            className="text-sm font-medium leading-none"
          >
            {field.label}
            {field.required && (
              <span aria-hidden="true" className="ml-1 text-destructive">*</span>
            )}
          </label>

          {field.type === 'textarea' ? (
            <textarea
              id={`ccf-${field.key}`}
              required={field.required}
              disabled={isLoading}
              aria-invalid={fieldError ? true : undefined}
              aria-describedby={errorId}
              value={values[field.key] ?? ''}
              onChange={e => set(field.key, e.target.value)}
              placeholder={field.placeholder}
              rows={4}
              className="flex min-h-[80px] w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
            />
          ) : field.type === 'select' ? (
            <select
              id={`ccf-${field.key}`}
              required={field.required}
              disabled={isLoading}
              aria-invalid={fieldError ? true : undefined}
              aria-describedby={errorId}
              value={values[field.key] ?? ''}
              onChange={e => set(field.key, e.target.value)}
              className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
            >
              <option value="">Select…</option>
              {(field.options ?? []).map(opt => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          ) : (
            <input
              id={`ccf-${field.key}`}
              type={field.type}
              required={field.required}
              disabled={isLoading}
              aria-invalid={fieldError ? true : undefined}
              aria-describedby={errorId}
              value={values[field.key] ?? ''}
              onChange={e => set(field.key, e.target.value)}
              placeholder={field.placeholder}
              className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
            />
          )}

          {fieldError && (
            <p id={errorId} role="alert" className="text-sm text-destructive">
              {fieldError}
            </p>
          )}
        </div>
        )
      })}

      {extraSlot}

      <button
        type="submit"
        disabled={isLoading}
        className="inline-flex w-full items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50"
      >
        {isLoading ? 'Saving…' : submitLabel}
      </button>
    </form>
  )
}
