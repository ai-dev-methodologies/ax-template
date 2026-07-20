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
  /**
   * BE-authoritative page count (common/PageEnvelope's `pagination.totalPages`,
   * S2.QUERY-BOUNDS.XB / CRUD-FE-003). When provided, this is used INSTEAD of
   * the client-side `ceil(total / pageSize)` derivation below — the two can
   * disagree if a caller's `total` isn't the exact row count the backend
   * paginated over (e.g. a client-filtered subset).
   */
  totalPages?: number
  /**
   * BE-authoritative "next page exists" flag (`pagination.hasMore`). When
   * provided, this replaces the `page >= totalPages` Next-button disable
   * check below with the server's own derivation — the two are equivalent
   * for an un-filtered list but `hasMore` is the source of truth.
   */
  hasMore?: boolean
}

/**
 * pageEnvelopeToPaginationProps — adapt the canonical `common/PageEnvelope`
 * pagination block into this component's props. `PageEnvelope.pagination.page`
 * is 0-based (Spring Data convention); `PaginationProps.page` is 1-based (the
 * existing crud/items list-page convention — see
 * templates/L4/crud/app/(crud)/items/page.tsx), so the 0-based-to-1-based
 * conversion happens once here instead of at every call site.
 */
export function pageEnvelopeToPaginationProps(
  pagination: {
    page: number
    pageSize: number
    totalElements: number
    totalPages: number
    hasMore: boolean
  },
  onPageChange: (page: number) => void,
): Pick<PaginationProps, 'page' | 'pageSize' | 'total' | 'totalPages' | 'hasMore' | 'onPageChange'> {
  return {
    page: pagination.page + 1,
    pageSize: pagination.pageSize,
    total: pagination.totalElements,
    totalPages: pagination.totalPages,
    hasMore: pagination.hasMore,
    onPageChange,
  }
}

export default function Pagination({
  page,
  pageSize,
  total,
  onPageChange,
  pageSizeOptions,
  onPageSizeChange,
  totalPages: totalPagesProp,
  hasMore: hasMoreProp,
}: PaginationProps) {
  // Math.max(1, …) wraps BOTH the derived AND the BE-authoritative `totalPages`
  // prop — a canonical empty `PageEnvelope` (`totalElements: 0, totalPages: 0`)
  // otherwise passes `totalPagesProp = 0` straight through, rendering the
  // contradictory "1 / 0" (display page 1 of a claimed 0 total pages). Clamping
  // to at least 1 makes the empty state read "1 / 1", consistent with the
  // client-derived branch's existing convention for an empty `total`.
  const totalPages = Math.max(1, totalPagesProp ?? Math.ceil(total / pageSize))
  const isLastPage = hasMoreProp === undefined ? page >= totalPages : !hasMoreProp
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
          disabled={isLastPage}
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
