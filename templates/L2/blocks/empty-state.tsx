/*
---
template_id: L2/blocks/empty-state
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "L2 data block — empty state illustration with icon/title/description/action slots; purely presentational."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
import * as React from 'react'

export interface EmptyStateProps {
  /** Icon or illustration node */
  icon?: React.ReactNode
  title: string
  description?: string
  /** CTA button or link slot */
  actionSlot?: React.ReactNode
}

export default function EmptyState({
  icon,
  title,
  description,
  actionSlot,
}: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-4 py-16 text-center">
      {icon && (
        <div
          aria-hidden="true"
          className="flex h-16 w-16 items-center justify-center rounded-full bg-muted text-muted-foreground"
        >
          {icon}
        </div>
      )}

      <div className="space-y-2">
        <h3 className="text-lg font-semibold">{title}</h3>
        {description && (
          <p className="max-w-sm text-sm text-muted-foreground">{description}</p>
        )}
      </div>

      {actionSlot && <div>{actionSlot}</div>}
    </div>
  )
}
