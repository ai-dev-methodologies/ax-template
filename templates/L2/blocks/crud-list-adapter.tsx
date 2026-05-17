/*
---
template_id: L2/blocks/crud-list-adapter
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "L2 CRUD block — generic list renderer; item rendering delegate passed as render prop from L4."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
import * as React from 'react'

export interface CrudListAdapterProps<Item = Record<string, unknown>> {
  data: Item[]
  /** Key extractor for React list reconciliation */
  getItemKey: (item: Item) => string
  /** Render delegate — L4 injects the domain-specific row component */
  renderItem: (item: Item) => React.ReactNode
  /** Slot rendered when data is empty */
  emptySlot?: React.ReactNode
  /** Wrapper element type (default: 'ul') */
  as?: 'ul' | 'div'
}

export default function CrudListAdapter<Item = Record<string, unknown>>({
  data,
  getItemKey,
  renderItem,
  emptySlot,
  as: Tag = 'ul',
}: CrudListAdapterProps<Item>) {
  if (data.length === 0) {
    return emptySlot ? <>{emptySlot}</> : null
  }

  return (
    <Tag role={Tag === 'ul' ? 'list' : undefined} className="space-y-2">
      {data.map(item => (
        <li
          key={getItemKey(item)}
          role={Tag === 'ul' ? 'listitem' : undefined}
        >
          {renderItem(item)}
        </li>
      ))}
    </Tag>
  )
}
