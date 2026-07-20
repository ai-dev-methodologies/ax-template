/*
---
template_id: L0/fork-receiver-kit/parse-page-envelope
layer: L0
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "S2.QUERY-BOUNDS.XB — every L4 list page hand-types its own PageEnvelope-shaped interface (see templates/L4/crud/app/(crud)/items/page.tsx's inline `ItemsPage`) and there is no shared runtime check that an incoming response actually HAS the five canonical pagination.* members before the page dereferences them. A backend that renames/drops one (the exact drift common/PageEnvelope.java's javadoc documents for the pre-FMW3 era) fails silently — page/pageSize/etc. read as `undefined`, not a caught error. parsePageEnvelope closes that gap: it throws loud when a required member is missing or the wrong type, so a contract drift surfaces at the fetch boundary instead of downstream in a NaN page count."
imports_from: []
imports_forbidden: [L1, L2, L3, L4, app/, lib/]
---
*/

/**
 * The canonical `pagination` block every list endpoint emits — exactly the
 * five members `common/PageEnvelope.Pagination` pins (specs/pagination-l0.yaml
 * PAGE-OFFSET-001): `page` is 0-based, `totalPages` and `hasMore` are DERIVED
 * server-side (never client-computed), so a parsed value can be trusted as-is.
 */
export interface PageEnvelopePagination {
  page: number
  pageSize: number
  totalElements: number
  totalPages: number
  hasMore: boolean
}

/** The canonical `{ data, pagination }` envelope, generic over the mapped item type. */
export interface ParsedPageEnvelope<T> {
  data: T[]
  pagination: PageEnvelopePagination
}

/**
 * parsePageEnvelope — parse a raw (`JSON.parse`d / `res.json()`-awaited) list
 * response into the canonical `{ data, pagination }` shape, throwing a
 * `TypeError` the moment any required member is missing or mistyped rather
 * than letting a silently-`undefined` field reach the UI.
 *
 * Use at the fetch boundary, e.g.:
 * ```ts
 * const raw = await res.json()
 * const page = parsePageEnvelope<Item>(raw)
 * page.pagination.totalPages // trusted number, never undefined
 * ```
 */
export function parsePageEnvelope<T = unknown>(raw: unknown): ParsedPageEnvelope<T> {
  if (raw === null || typeof raw !== 'object') {
    throw new TypeError('parsePageEnvelope: expected an object envelope, got ' + typeof raw)
  }
  const envelope = raw as Record<string, unknown>

  if (!Array.isArray(envelope.data)) {
    throw new TypeError('parsePageEnvelope: missing or non-array "data"')
  }

  const rawPagination = envelope.pagination
  if (rawPagination === null || typeof rawPagination !== 'object') {
    throw new TypeError('parsePageEnvelope: missing "pagination" object')
  }
  const pagination = rawPagination as Record<string, unknown>

  for (const field of ['page', 'pageSize', 'totalElements', 'totalPages'] as const) {
    if (typeof pagination[field] !== 'number') {
      throw new TypeError(`parsePageEnvelope: pagination.${field} must be a number`)
    }
  }
  if (typeof pagination.hasMore !== 'boolean') {
    throw new TypeError('parsePageEnvelope: pagination.hasMore must be a boolean')
  }

  return {
    data: envelope.data as T[],
    pagination: {
      page: pagination.page as number,
      pageSize: pagination.pageSize as number,
      totalElements: pagination.totalElements as number,
      totalPages: pagination.totalPages as number,
      hasMore: pagination.hasMore as boolean,
    },
  }
}
