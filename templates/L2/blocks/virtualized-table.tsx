/*
---
template_id: L2/blocks/virtualized-table
layer: L2
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: tanstack-virtual-2026-05
    section: "Core API: useVirtualizer"
    quote: "Virtualize only what you see. @tanstack/react-virtual gives you the primitives to render only the visible rows of a large dataset, keeping DOM node count proportional to the viewport rather than the data size."
  - source_type: upstream_id
    upstream_id: tanstack-virtual-2026-05
    section: "Rendering virtual items"
    quote: "Only getVirtualItems().length DOM nodes are rendered at any time (proportional to viewport height / item height), regardless of total count."
dependencies: ["@tanstack/react-virtual"]
imports_from: [L1, L2]
imports_forbidden: [L4, app/, lib/auth/, lib/payment/]
---
*/
import * as React from 'react'
import { useVirtualizer } from '@tanstack/react-virtual'

// Re-export ColumnDef/SortState from data-table to keep the props API compatible.
// L4 code can use the same column definitions for both DataTable and VirtualizedTable.
export type { ColumnDef, SortState, DataTableProps } from './data-table'

// ─── types ────────────────────────────────────────────────────────────────────

import type { ColumnDef, SortState } from './data-table'

export interface VirtualizedTableProps<Row = Record<string, unknown>> {
  columns: ColumnDef<Row>[]
  data: Row[]
  getRowKey: (row: Row) => string
  /** Current sort state (server-side). */
  sort?: SortState
  /** Called when column header clicked; L4 updates URL/query. */
  onSort?: (state: SortState) => void
  /** Selected row keys (controlled). */
  selectedKeys?: Set<string>
  onSelectionChange?: (keys: Set<string>) => void
  isLoading?: boolean
  /** Slot rendered when data is empty. */
  emptySlot?: React.ReactNode
  /**
   * Container height (px). Controls the scrollable viewport.
   * @default 400
   */
  containerHeight?: number
  /**
   * Estimated row height (px) for the virtualizer's initial layout calculation.
   * @default 40
   */
  estimatedRowHeight?: number
  /**
   * Extra rows to render beyond the visible area.
   * @default 5
   */
  overscan?: number
}

// ─── component ────────────────────────────────────────────────────────────────

/**
 * VirtualizedTable — drop-in upgrade over DataTable for large datasets.
 *
 * Uses @tanstack/react-virtual (useVirtualizer) to keep DOM node count
 * proportional to the viewport, not the data size. Suitable for >1000 rows.
 *
 * L4 usage (audit-log, large entity lists):
 *   import VirtualizedTable from 'templates/L2/blocks/virtualized-table'
 *   <VirtualizedTable
 *     columns={columns}
 *     data={rows}
 *     getRowKey={r => r.id}
 *     containerHeight={600}
 *   />
 */
export default function VirtualizedTable<Row = Record<string, unknown>>({
  columns,
  data,
  getRowKey,
  sort,
  onSort,
  selectedKeys,
  onSelectionChange,
  isLoading = false,
  emptySlot,
  containerHeight = 400,
  estimatedRowHeight = 40,
  overscan = 5,
}: VirtualizedTableProps<Row>) {
  const parentRef = React.useRef<HTMLDivElement>(null)

  const virtualizer = useVirtualizer({
    count: data.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => estimatedRowHeight,
    overscan,
  })

  function handleHeaderClick(col: ColumnDef<Row>) {
    if (!col.sortable || !onSort) return
    const direction =
      sort?.field === col.key && sort.direction === 'asc' ? 'desc' : 'asc'
    onSort({ field: col.key, direction })
  }

  function toggleRow(key: string) {
    if (!onSelectionChange || !selectedKeys) return
    const next = new Set(selectedKeys)
    if (next.has(key)) next.delete(key)
    else next.add(key)
    onSelectionChange(next)
  }

  const totalSize = virtualizer.getTotalSize()
  const virtualItems = virtualizer.getVirtualItems()

  return (
    <div className="relative w-full rounded-md border border-border">
      {/* Sticky header */}
      <div className="overflow-hidden">
        <table className="w-full text-sm">
          <thead className="border-b bg-background">
            <tr>
              {selectedKeys !== undefined && (
                <th className="h-10 w-8 px-2" aria-label="Selection" />
              )}
              {columns.map(col => (
                <th
                  key={col.key}
                  scope="col"
                  style={col.width ? { width: col.width } : undefined}
                  onClick={() => handleHeaderClick(col)}
                  aria-sort={
                    sort?.field === col.key
                      ? sort.direction === 'asc'
                        ? 'ascending'
                        : 'descending'
                      : col.sortable
                      ? 'none'
                      : undefined
                  }
                  className={[
                    'h-10 px-3 text-left align-middle font-medium text-muted-foreground',
                    col.sortable ? 'cursor-pointer select-none hover:text-foreground' : '',
                  ].join(' ')}
                >
                  {col.header}
                  {sort?.field === col.key && (
                    <span aria-hidden="true" className="ml-1">
                      {sort.direction === 'asc' ? '↑' : '↓'}
                    </span>
                  )}
                </th>
              ))}
            </tr>
          </thead>
        </table>
      </div>

      {/* Virtualized body */}
      <div
        ref={parentRef}
        style={{ height: `${containerHeight}px`, overflow: 'auto' }}
        aria-busy={isLoading}
        data-testid="virtualized-table-viewport"
      >
        {isLoading ? (
          <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
            Loading…
          </div>
        ) : data.length === 0 ? (
          <div className="flex h-full items-center justify-center">
            {emptySlot}
          </div>
        ) : (
          <div style={{ height: `${totalSize}px`, position: 'relative' }}>
            {virtualItems.map(virtualItem => {
              const row = data[virtualItem.index]
              const key = getRowKey(row)
              const checked = selectedKeys?.has(key)
              return (
                <div
                  key={virtualItem.key}
                  data-index={virtualItem.index}
                  ref={virtualizer.measureElement}
                  style={{
                    position: 'absolute',
                    top: 0,
                    left: 0,
                    width: '100%',
                    transform: `translateY(${virtualItem.start}px)`,
                  }}
                >
                  <table className="w-full text-sm">
                    <tbody>
                      <tr
                        data-selected={checked}
                        className="border-b transition-colors hover:bg-muted/50 data-[selected=true]:bg-muted"
                      >
                        {selectedKeys !== undefined && (
                          <td className="w-8 px-2 py-2">
                            <input
                              type="checkbox"
                              aria-label={`Select row ${key}`}
                              checked={checked ?? false}
                              onChange={() => toggleRow(key)}
                              className="h-4 w-4 rounded border-border"
                            />
                          </td>
                        )}
                        {columns.map(col => (
                          <td key={col.key} className="p-3 align-middle">
                            {col.cell
                              ? col.cell(row)
                              : String((row as Record<string, unknown>)[col.key] ?? '')}
                          </td>
                        ))}
                      </tr>
                    </tbody>
                  </table>
                </div>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}
