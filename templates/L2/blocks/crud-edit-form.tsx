/*
---
template_id: L2/blocks/crud-edit-form
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "L2 CRUD block — generic edit form with initialValues prop; field schema same as CrudCreateForm."
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

export interface CrudEditFormProps {
  fields: FieldDef[]
  initialValues: Record<string, unknown>
  onSubmit: (data: Record<string, unknown>) => void
  isLoading?: boolean
  submitLabel?: string
  /** Slot for delete action, change-log, etc. */
  extraSlot?: React.ReactNode
}

export default function CrudEditForm({
  fields,
  initialValues,
  onSubmit,
  isLoading = false,
  submitLabel = 'Save changes',
  extraSlot,
}: CrudEditFormProps) {
  const [values, setValues] = React.useState<Record<string, string>>(() =>
    Object.fromEntries(fields.map(f => [f.key, String(initialValues[f.key] ?? '')]))
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
      {fields.map(field => (
        <div key={field.key} className="space-y-2">
          <label
            htmlFor={`cef-${field.key}`}
            className="text-sm font-medium leading-none"
          >
            {field.label}
            {field.required && (
              <span aria-hidden="true" className="ml-1 text-destructive">*</span>
            )}
          </label>

          {field.type === 'textarea' ? (
            <textarea
              id={`cef-${field.key}`}
              required={field.required}
              disabled={isLoading}
              value={values[field.key] ?? ''}
              onChange={e => set(field.key, e.target.value)}
              placeholder={field.placeholder}
              rows={4}
              className="flex min-h-[80px] w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
            />
          ) : field.type === 'select' ? (
            <select
              id={`cef-${field.key}`}
              required={field.required}
              disabled={isLoading}
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
              id={`cef-${field.key}`}
              type={field.type}
              required={field.required}
              disabled={isLoading}
              value={values[field.key] ?? ''}
              onChange={e => set(field.key, e.target.value)}
              placeholder={field.placeholder}
              className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
            />
          )}
        </div>
      ))}

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
