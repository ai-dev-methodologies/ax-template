/*
---
template_id: L2/blocks/column-picker
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "L2 data block — column visibility toggle using checkbox list; state fully prop-driven."
dependencies: [checkbox, label, button, popover]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
import * as React from 'react'

export interface ColumnPickerColumn {
  key: string
  label: string
  /** If true, column cannot be hidden */
  required?: boolean
}

export interface ColumnPickerProps {
  columns: ColumnPickerColumn[]
  /** Currently visible column keys */
  visible: string[]
  onChange: (visible: string[]) => void
}

export default function ColumnPicker({
  columns,
  visible,
  onChange,
}: ColumnPickerProps) {
  const [open, setOpen] = React.useState(false)

  function toggleColumn(key: string) {
    const next = visible.includes(key)
      ? visible.filter(k => k !== key)
      : [...visible, key]
    onChange(next)
  }

  return (
    <div className="relative inline-block">
      <button
        type="button"
        aria-haspopup="listbox"
        aria-expanded={open}
        onClick={() => setOpen(v => !v)}
        className="inline-flex items-center gap-1 rounded-md border border-input bg-background px-3 py-1.5 text-sm font-medium hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
      >
        Columns
        <span aria-hidden="true" className="text-xs">
          {open ? '▲' : '▼'}
        </span>
      </button>

      {open && (
        <div
          role="listbox"
          aria-label="Toggle columns"
          aria-multiselectable="true"
          className="absolute right-0 top-full z-50 mt-1 w-48 rounded-md border border-border bg-background p-2 shadow-md"
        >
          {columns.map(col => {
            const checked = visible.includes(col.key)
            return (
              <label
                key={col.key}
                className="flex cursor-pointer items-center gap-2 rounded-sm px-2 py-1.5 text-sm hover:bg-accent"
              >
                <input
                  type="checkbox"
                  role="option"
                  aria-selected={checked}
                  checked={checked}
                  disabled={col.required}
                  onChange={() => toggleColumn(col.key)}
                  className="h-3.5 w-3.5 rounded border-border"
                />
                {col.label}
              </label>
            )
          })}
        </div>
      )}
    </div>
  )
}
