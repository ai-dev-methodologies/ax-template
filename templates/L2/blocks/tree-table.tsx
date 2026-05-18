/*
---
template_id: L2/blocks/tree-table
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "L2 data block — hierarchical table with recursive expand/collapse; depth capped at 5 (DepthExceededError thrown on violation). Composes ColumnDef from data-table but renders tree structure; does NOT import data-table directly to avoid circular composition."
dependencies: []
imports_from: [L1, L2]
imports_forbidden: [L4, app/, lib/]
---
*/
import * as React from 'react'

import type { ColumnDef } from './data-table'

// ─── depth guard ─────────────────────────────────────────────────────────────

export const TREE_TABLE_MAX_DEPTH = 5

export class DepthExceededError extends Error {
  constructor(depth: number) {
    super(`TreeTable row at depth ${depth} exceeds maximum depth of ${TREE_TABLE_MAX_DEPTH}`)
    this.name = 'DepthExceededError'
  }
}

function assertDepth(depth: number): void {
  if (depth > TREE_TABLE_MAX_DEPTH) throw new DepthExceededError(depth)
}

// ─── types ───────────────────────────────────────────────────────────────────

export interface TreeRow<Row = Record<string, unknown>> {
  /** Unique row identifier */
  id: string
  data: Row
  children?: TreeRow<Row>[]
}

export interface TreeTableProps<Row = Record<string, unknown>> {
  columns: ColumnDef<Row>[]
  rows: TreeRow<Row>[]
  /** Initially expanded row IDs (uncontrolled convenience prop) */
  defaultExpandedIds?: string[]
  /** Controlled expanded IDs; if provided, onExpand must also be provided */
  expandedIds?: string[]
  onExpand?: (ids: string[]) => void
  isLoading?: boolean
  emptySlot?: React.ReactNode
}

// ─── row renderer ────────────────────────────────────────────────────────────

interface TreeRowRendererProps<Row> {
  node: TreeRow<Row>
  columns: ColumnDef<Row>[]
  depth: number
  expandedIds: string[]
  onToggle: (id: string) => void
}

function TreeRowRenderer<Row>({
  node,
  columns,
  depth,
  expandedIds,
  onToggle,
}: TreeRowRendererProps<Row>) {
  assertDepth(depth)

  const hasChildren = Boolean(node.children?.length)
  const isExpanded = expandedIds.includes(node.id)

  return (
    <>
      <tr
        data-depth={depth}
        data-has-children={hasChildren}
        data-expanded={isExpanded}
        className="border-b transition-colors hover:bg-muted/50"
      >
        {/* Indent + toggle */}
        <td className="py-2 pr-2" style={{ paddingLeft: `${depth * 20 + 8}px`, width: 1 }}>
          {hasChildren ? (
            <button
              type="button"
              aria-label={isExpanded ? `Collapse ${node.id}` : `Expand ${node.id}`}
              aria-expanded={isExpanded}
              onClick={() => onToggle(node.id)}
              className="flex h-5 w-5 items-center justify-center rounded text-muted-foreground hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
            >
              <svg
                aria-hidden="true"
                width="10"
                height="10"
                viewBox="0 0 10 10"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.5"
                style={{
                  transform: isExpanded ? 'rotate(90deg)' : 'rotate(0deg)',
                  transition: 'transform 150ms ease',
                }}
              >
                <path d="M2 1.5l5 3.5-5 3.5" />
              </svg>
            </button>
          ) : (
            <span className="inline-block h-5 w-5" aria-hidden="true" />
          )}
        </td>

        {/* Data cells */}
        {columns.map(col => (
          <td key={col.key} className="p-3 align-middle">
            {col.cell
              ? col.cell(node.data)
              : String((node.data as Record<string, unknown>)[col.key] ?? '')}
          </td>
        ))}
      </tr>

      {/* Recursive children */}
      {hasChildren && isExpanded &&
        node.children!.map(child => (
          <TreeRowRenderer
            key={child.id}
            node={child}
            columns={columns}
            depth={depth + 1}
            expandedIds={expandedIds}
            onToggle={onToggle}
          />
        ))}
    </>
  )
}

// ─── component ───────────────────────────────────────────────────────────────

/**
 * TreeTable — hierarchical table with expand/collapse per row.
 *
 * Depth is capped at TREE_TABLE_MAX_DEPTH (5). Passing data deeper than 5 levels
 * throws DepthExceededError — validate server data before rendering.
 *
 * Supports both controlled (expandedIds + onExpand) and uncontrolled
 * (defaultExpandedIds) expansion state.
 *
 * L4 usage:
 *   <TreeTable
 *     columns={columns}
 *     rows={treeRows}
 *     defaultExpandedIds={[rootId]}
 *   />
 */
export default function TreeTable<Row = Record<string, unknown>>({
  columns,
  rows,
  defaultExpandedIds = [],
  expandedIds: controlledExpandedIds,
  onExpand,
  isLoading = false,
  emptySlot,
}: TreeTableProps<Row>) {
  const [internalExpandedIds, setInternalExpandedIds] = React.useState<string[]>(
    defaultExpandedIds,
  )

  const isControlled = controlledExpandedIds !== undefined
  const expandedIds = isControlled ? controlledExpandedIds : internalExpandedIds

  function handleToggle(id: string) {
    const next = expandedIds.includes(id)
      ? expandedIds.filter(eid => eid !== id)
      : [...expandedIds, id]

    if (isControlled) {
      onExpand?.(next)
    } else {
      setInternalExpandedIds(next)
    }
  }

  const totalColSpan = columns.length + 1 // +1 for indent/toggle column

  return (
    <div className="relative w-full overflow-auto rounded-md border border-border">
      <table className="w-full caption-bottom text-sm" aria-busy={isLoading}>
        <thead className="[&_tr]:border-b">
          <tr className="border-b transition-colors">
            <th className="h-10 w-8" aria-label="Expand" />
            {columns.map(col => (
              <th
                key={col.key}
                scope="col"
                style={col.width ? { width: col.width } : undefined}
                className="h-10 px-3 text-left align-middle font-medium text-muted-foreground"
              >
                {col.header}
              </th>
            ))}
          </tr>
        </thead>

        <tbody className="[&_tr:last-child]:border-0">
          {isLoading ? (
            <tr>
              <td colSpan={totalColSpan} className="py-12 text-center text-sm text-muted-foreground">
                Loading…
              </td>
            </tr>
          ) : rows.length === 0 ? (
            <tr>
              <td colSpan={totalColSpan}>{emptySlot}</td>
            </tr>
          ) : (
            rows.map(row => (
              <TreeRowRenderer
                key={row.id}
                node={row}
                columns={columns}
                depth={0}
                expandedIds={expandedIds}
                onToggle={handleToggle}
              />
            ))
          )}
        </tbody>
      </table>
    </div>
  )
}

// Re-export guard utilities
export { assertDepth }
