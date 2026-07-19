// CLEAN — parses the BE's canonical PAGE-OFFSET-001 envelope shape exactly as
// backend/src/main/java/com/ax/template/authblueprint/common/PageEnvelope.java
// serializes it:
//   { data: T[], pagination: { page, pageSize, totalElements, totalPages, hasMore } }
//
// This is the FE half of the pagination-envelope contract-parity proof: the
// scenario's contract-parity script feeds a synthetic PageEnvelope-shaped
// sample through this parser and asserts every field survives the round trip
// under its CANONICAL name. No field is renamed, guessed, or dropped.

export interface PageEnvelopePagination {
  page: number
  pageSize: number
  totalElements: number
  totalPages: number
  hasMore: boolean
}

export interface PageEnvelope<T> {
  data: T[]
  pagination: PageEnvelopePagination
}

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
  const envelope = json as PageEnvelope<T>
  return {
    items: envelope.data,
    page: envelope.pagination.page,
    pageSize: envelope.pagination.pageSize,
    total: envelope.pagination.totalElements,
    totalPages: envelope.pagination.totalPages,
    hasMore: envelope.pagination.hasMore,
  }
}
