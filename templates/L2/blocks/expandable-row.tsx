/*
---
template_id: L2/blocks/expandable-row
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "L2 data block — table row with collapsible detail panel; expand state controlled (L4 manages expanded IDs); detailSlot injected by L4. Compatible with DataTable column definitions."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
import * as React from 'react'

import type { ColumnDef } from './data-table'

export interface ExpandableRowProps<Row = Record<string, unknown>> {
  row: Row
  rowKey: string
  columns: ColumnDef<Row>[]
  /** Whether this row is currently expanded */
  isExpanded: boolean
  /** Called when toggle button is clicked */
  onToggle: (key: string) => void
  /**
   * Content rendered inside the expanded panel.
   * L4 injects the detail view (related records, metadata, actions).
   */
  detailSlot: React.ReactNode
  isSelected?: boolean
  onSelectionChange?: (key: string) => void
}

/**
 * ExpandableRow — a table row with an inline detail panel.
 *
 * Renders as two `<tr>` elements: the main data row and a hidden/visible detail row.
 * L4 controls `isExpanded` and provides `detailSlot`.
 *
 * L4 usage (inside a <tbody> rendered by DataTable):
 *   const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set())
 *   data.map(row => (
 *     <ExpandableRow
 *       key={row.id}
 *       row={row}
 *       rowKey={row.id}
 *       columns={columns}
 *       isExpanded={expandedIds.has(row.id)}
 *       onToggle={id => toggleSet(expandedIds, id)}
 *       detailSlot={<OrderDetail orderId={row.id} />}
 *     />
 *   ))
 */
export default function ExpandableRow<Row = Record<string, unknown>>({
  row,
  rowKey,
  columns,
  isExpanded,
  onToggle,
  detailSlot,
  isSelected = false,
  onSelectionChange,
}: ExpandableRowProps<Row>) {
  const detailId = `expandable-detail-${rowKey}`

  return (
    <>
      {/* Main data row */}
      <tr
        data-selected={isSelected}
        data-expanded={isExpanded}
        className="border-b transition-colors hover:bg-muted/50 data-[selected=true]:bg-muted"
      >
        {/* Expand toggle */}
        <td className="w-8 px-2 py-2">
          <button
            type="button"
            aria-label={isExpanded ? 'Collapse row' : 'Expand row'}
            aria-expanded={isExpanded}
            aria-controls={detailId}
            onClick={() => onToggle(rowKey)}
            className="flex h-5 w-5 items-center justify-center rounded text-muted-foreground hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
          >
            <svg
              aria-hidden="true"
              width="12"
              height="12"
              viewBox="0 0 12 12"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.5"
              style={{
                transform: isExpanded ? 'rotate(90deg)' : 'rotate(0deg)',
                transition: 'transform 150ms ease',
              }}
            >
              <path d="M3 2l6 4-6 4" />
            </svg>
          </button>
        </td>

        {/* Selection checkbox */}
        {onSelectionChange !== undefined && (
          <td className="w-8 px-2 py-2">
            <input
              type="checkbox"
              aria-label={`Select row ${rowKey}`}
              checked={isSelected}
              onChange={() => onSelectionChange(rowKey)}
              className="h-4 w-4 rounded border-border"
            />
          </td>
        )}

        {/* Data cells */}
        {columns.map(col => (
          <td key={col.key} className="p-3 align-middle">
            {col.cell
              ? col.cell(row)
              : String((row as Record<string, unknown>)[col.key] ?? '')}
          </td>
        ))}
      </tr>

      {/* Detail panel row */}
      <tr
        id={detailId}
        aria-hidden={!isExpanded}
        className={isExpanded ? '' : 'hidden'}
      >
        <td
          colSpan={columns.length + 1 + (onSelectionChange !== undefined ? 1 : 0)}
          className="bg-muted/30 px-6 py-4"
        >
          {isExpanded ? detailSlot : null}
        </td>
      </tr>
    </>
  )
}
