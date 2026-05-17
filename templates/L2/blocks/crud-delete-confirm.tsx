/*
---
template_id: L2/blocks/crud-delete-confirm
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "L2 CRUD block — destructive action confirmation modal; uses dialog semantics. Actual delete via onConfirm prop."
dependencies: [alert-dialog, button]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
import * as React from 'react'

export interface CrudDeleteConfirmProps {
  /** Human-readable label of the item being deleted */
  itemLabel: string
  onConfirm: () => void
  onCancel: () => void
  isLoading?: boolean
  /** Override dialog title (default: 'Delete <itemLabel>?') */
  title?: string
  /** Override dialog body text */
  description?: string
}

export default function CrudDeleteConfirm({
  itemLabel,
  onConfirm,
  onCancel,
  isLoading = false,
  title,
  description,
}: CrudDeleteConfirmProps) {
  const dialogTitle = title ?? `Delete ${itemLabel}?`
  const dialogDesc =
    description ??
    `This action cannot be undone. "${itemLabel}" will be permanently deleted.`

  return (
    <div
      role="alertdialog"
      aria-modal="true"
      aria-labelledby="cdc-title"
      aria-describedby="cdc-desc"
      className="rounded-lg border border-border bg-background p-6 shadow-lg max-w-md w-full space-y-4"
    >
      <h2 id="cdc-title" className="text-lg font-semibold">
        {dialogTitle}
      </h2>

      <p id="cdc-desc" className="text-sm text-muted-foreground">
        {dialogDesc}
      </p>

      <div className="flex justify-end gap-3">
        <button
          type="button"
          onClick={onCancel}
          disabled={isLoading}
          className="inline-flex items-center rounded-md border border-input bg-background px-4 py-2 text-sm font-medium shadow-sm hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50"
        >
          Cancel
        </button>

        <button
          type="button"
          onClick={onConfirm}
          disabled={isLoading}
          className="inline-flex items-center rounded-md bg-destructive px-4 py-2 text-sm font-medium text-destructive-foreground shadow hover:bg-destructive/90 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50"
        >
          {isLoading ? 'Deleting…' : 'Delete'}
        </button>
      </div>
    </div>
  )
}
