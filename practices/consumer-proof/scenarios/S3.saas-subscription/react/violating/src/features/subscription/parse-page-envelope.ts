// VIOLATING — pagination-envelope contract-parity.
// A realistic AI-generated FE parser that GUESSES a flat page-response shape
// ({ items, total, hasNextPage }) instead of matching the BE's ACTUAL
// canonical PAGE-OFFSET-001 envelope
// (backend/src/main/java/com/ax/template/authblueprint/common/PageEnvelope.java):
//   { data: T[], pagination: { page, pageSize, totalElements, totalPages, hasMore } }
//
// This compiles and "looks right" in isolation, but every field silently
// reads through to `undefined` at runtime against the real BE response —
// exactly the drift PageEnvelope.java's own doc comment warns about ("every
// domain re-typed the page response and the shapes DIVERGED"). Nothing in
// TypeScript's structural typing catches this because the guessed shape is
// still valid TS on its own; only a contract-parity check against the real
// BE source, exercised in one integrated run, catches it.

export interface ParsedPage<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
  totalPages: number
  hasMore: boolean
}

/** Parse a raw JSON response body into the FE list-state shape. */
export function parsePageEnvelope<T>(json: unknown): ParsedPage<T> {
  const legacy = json as {
    items: T[]
    page: number
    pageSize: number
    total: number
    totalPages: number
    hasNextPage: boolean
  }
  return {
    items: legacy.items,
    page: legacy.page,
    pageSize: legacy.pageSize,
    total: legacy.total,
    totalPages: legacy.totalPages,
    hasMore: legacy.hasNextPage,
  }
}
