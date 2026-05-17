/*
---
template_id: L2/blocks/data-table
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "TanStack Table v8 — headless table core concepts"
    url: "https://tanstack.com/table/v8/docs/introduction"
  - source_type: internal
    rationale: "L2 data block — server-side sort/filter; data and sort callbacks passed as props from L4."
dependencies: [checkbox]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
import * as React from 'react'

export type SortDirection = 'asc' | 'desc'

export interface SortState {
  field: string
  direction: SortDirection
}

export interface ColumnDef<Row = Record<string, unknown>> {
  key: string
  header: string
  /** Width hint (e.g. '200px', '1fr') */
  width?: string
  /** Render cell content; defaults to String(row[key]) */
  cell?: (row: Row) => React.ReactNode
  sortable?: boolean
}

export interface DataTableProps<Row = Record<string, unknown>> {
  columns: ColumnDef<Row>[]
  data: Row[]
  /** Row key extractor */
  getRowKey: (row: Row) => string
  /** Current sort state (server-side) */
  sort?: SortState
  /** Called when column header clicked; L4 updates URL/query */
  onSort?: (state: SortState) => void
  /** Selected row keys (controlled) */
  selectedKeys?: Set<string>
  onSelectionChange?: (keys: Set<string>) => void
  isLoading?: boolean
  /** Slot rendered when data is empty */
  emptySlot?: React.ReactNode
}

export default function DataTable<Row = Record<string, unknown>>({
  columns,
  data,
  getRowKey,
  sort,
  onSort,
  selectedKeys,
  onSelectionChange,
  isLoading = false,
  emptySlot,
}: DataTableProps<Row>) {
  function handleHeaderClick(col: ColumnDef<Row>) {
    if (!col.sortable || !onSort) return
    const direction: SortDirection =
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

  return (
    <div className="relative w-full overflow-auto rounded-md border border-border">
      <table className="w-full caption-bottom text-sm" aria-busy={isLoading}>
        <thead className="[&_tr]:border-b">
          <tr className="border-b transition-colors hover:bg-muted/50">
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

        <tbody className="[&_tr:last-child]:border-0">
          {isLoading ? (
            <tr>
              <td
                colSpan={columns.length + (selectedKeys !== undefined ? 1 : 0)}
                className="py-12 text-center text-sm text-muted-foreground"
              >
                Loading…
              </td>
            </tr>
          ) : data.length === 0 ? (
            <tr>
              <td
                colSpan={columns.length + (selectedKeys !== undefined ? 1 : 0)}
              >
                {emptySlot}
              </td>
            </tr>
          ) : (
            data.map(row => {
              const key = getRowKey(row)
              const checked = selectedKeys?.has(key)
              return (
                <tr
                  key={key}
                  data-selected={checked}
                  className="border-b transition-colors hover:bg-muted/50 data-[selected=true]:bg-muted"
                >
                  {selectedKeys !== undefined && (
                    <td className="px-2 py-2">
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
              )
            })
          )}
        </tbody>
      </table>
    </div>
  )
}
