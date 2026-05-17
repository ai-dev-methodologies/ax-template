/*
---
template_id: L2/blocks/pagination
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "WAI-ARIA Pagination Pattern"
    url: "https://www.w3.org/WAI/ARIA/apg/patterns/landmarks/examples/general-principles.html"
  - source_type: internal
    rationale: "L2 data block — pagination controls with page/pageSize/total props; no router coupling."
dependencies: [button]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
import * as React from 'react'

export interface PaginationProps {
  page: number
  pageSize: number
  total: number
  onPageChange: (page: number) => void
  /** Page size options shown in selector */
  pageSizeOptions?: number[]
  onPageSizeChange?: (size: number) => void
}

export default function Pagination({
  page,
  pageSize,
  total,
  onPageChange,
  pageSizeOptions,
  onPageSizeChange,
}: PaginationProps) {
  const totalPages = Math.max(1, Math.ceil(total / pageSize))
  const from = Math.min((page - 1) * pageSize + 1, total)
  const to = Math.min(page * pageSize, total)

  return (
    <nav aria-label="Pagination" className="flex items-center gap-4">
      <p className="text-sm text-muted-foreground" aria-live="polite" aria-atomic>
        {total > 0 ? `${from}–${to} of ${total}` : 'No results'}
      </p>

      <div className="flex items-center gap-1">
        <button
          type="button"
          aria-label="Previous page"
          onClick={() => onPageChange(page - 1)}
          disabled={page <= 1}
          className="inline-flex h-8 w-8 items-center justify-center rounded-md border border-input bg-background text-sm hover:bg-accent hover:text-accent-foreground disabled:pointer-events-none disabled:opacity-50"
        >
          ‹
        </button>

        <span className="min-w-[2rem] text-center text-sm" aria-current="page">
          {page}
        </span>
        <span className="text-sm text-muted-foreground">/ {totalPages}</span>

        <button
          type="button"
          aria-label="Next page"
          onClick={() => onPageChange(page + 1)}
          disabled={page >= totalPages}
          className="inline-flex h-8 w-8 items-center justify-center rounded-md border border-input bg-background text-sm hover:bg-accent hover:text-accent-foreground disabled:pointer-events-none disabled:opacity-50"
        >
          ›
        </button>
      </div>

      {pageSizeOptions && onPageSizeChange && (
        <div className="flex items-center gap-2">
          <span className="text-sm text-muted-foreground">Rows:</span>
          <select
            value={pageSize}
            onChange={e => onPageSizeChange(Number(e.target.value))}
            aria-label="Rows per page"
            className="h-8 rounded-md border border-input bg-background px-2 text-sm focus:outline-none focus:ring-1 focus:ring-ring"
          >
            {pageSizeOptions.map(n => (
              <option key={n} value={n}>
                {n}
              </option>
            ))}
          </select>
        </div>
      )}
    </nav>
  )
}
