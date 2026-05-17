/*
---
template_id: L2/blocks/bulk-actions-bar
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "L2 data block — contextual toolbar shown when rows are selected; actionsSlot injected by L4."
dependencies: [button]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
import * as React from 'react'

export interface BulkActionsBarProps {
  /** Number of currently selected rows */
  selectedCount: number
  /** Action buttons slot (Delete, Export, etc.) — injected by L4 */
  actionsSlot: React.ReactNode
  onClearSelection: () => void
}

export default function BulkActionsBar({
  selectedCount,
  actionsSlot,
  onClearSelection,
}: BulkActionsBarProps) {
  if (selectedCount === 0) return null

  return (
    <div
      role="toolbar"
      aria-label="Bulk actions"
      className="flex items-center gap-3 rounded-md border border-border bg-muted px-4 py-2"
    >
      <span className="text-sm font-medium">
        {selectedCount} {selectedCount === 1 ? 'item' : 'items'} selected
      </span>

      <div className="flex items-center gap-2">{actionsSlot}</div>

      <button
        type="button"
        aria-label="Clear selection"
        onClick={onClearSelection}
        className="ml-auto text-xs text-muted-foreground underline underline-offset-4 hover:text-foreground"
      >
        Clear
      </button>
    </div>
  )
}
