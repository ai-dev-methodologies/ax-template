/*
---
template_id: L0/fork-receiver-kit/use-url-list-state
layer: L0
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js App Router — useSearchParams / useRouter (URL is the source of truth for shareable, back-button-correct view state)"
    url: "https://nextjs.org/docs/app/api-reference/functions/use-search-params"
  - source_type: internal
    rationale: "FDW1 (frontend dogfood) rule-of-three: the catalog's own architecture rule saved-view-must-be-url-state-or-server-persisted MANDATES page/sort/search/filter live in the URL, but shipped ZERO helper — advanced-filter-builder's JSDoc even referenced a non-existent useUrlState hook. persona2 (useProductListParams) and persona3 (use-list-params) both hand-rolled the identical immutable-patcher + reset-page-on-filter logic. This generalises persona2's version (the cleaner of the two) into the catalog primitive so the most-emphasized architecture rule has an ergonomic easy-path."
imports_from: []
imports_forbidden: [L1, L2, L3, L4, app/, lib/]
---
*/
'use client'

import * as React from 'react'
import { usePathname, useRouter, useSearchParams } from 'next/navigation'

export type SortDirection = 'asc' | 'desc'

/** The view-state a paginated list reads from the URL. */
export interface ListState {
  page: number
  pageSize: number
  search?: string
  sortField?: string
  sortDirection?: SortDirection
  /** Domain filters declared via `filterKeys` (e.g. { status: 'ACTIVE' }). */
  filters: Record<string, string>
}

export interface UseUrlListStateOptions {
  /** Page size when `pageSize` is absent from the URL. Default 20. */
  defaultPageSize?: number
  /**
   * Query-string keys treated as domain filters. Only these are surfaced in
   * `filters` and accepted by `setFilter` — keeps unrelated params out.
   */
  filterKeys?: readonly string[]
}

export interface UrlListState extends ListState {
  setPage: (page: number) => void
  setPageSize: (pageSize: number) => void
  setSearch: (search: string | undefined) => void
  setSort: (field: string | undefined, direction?: SortDirection) => void
  setFilter: (key: string, value: string | undefined) => void
  /** Clear search, sort and every declared filter; return to page 1. */
  reset: () => void
}

const DEFAULT_PAGE_SIZE = 20

/**
 * useUrlListState — typed page/sort/search/filter state backed by the query
 * string. Every mutation funnels through ONE immutable patcher so the live
 * URLSearchParams is never mutated, and "reset to page 1 on any filtering
 * change" is enforced in a single place (changing the page itself does not
 * reset). Use it as the single source of truth for a CRUD list view; pass the
 * values straight to your list query and the setters to the table/filters.
 */
export function useUrlListState(options: UseUrlListStateOptions = {}): UrlListState {
  const { defaultPageSize = DEFAULT_PAGE_SIZE, filterKeys = [] } = options
  const router = useRouter()
  const pathname = usePathname()
  const searchParams = useSearchParams()

  const page = Math.max(1, Number(searchParams.get('page')) || 1)
  const pageSize = Math.max(1, Number(searchParams.get('pageSize')) || defaultPageSize)
  const search = searchParams.get('search') || undefined
  const sortField = searchParams.get('sortField') || undefined
  const sortDirection: SortDirection | undefined = sortField
    ? searchParams.get('sortDirection') === 'desc'
      ? 'desc'
      : 'asc'
    : undefined

  const filters: Record<string, string> = {}
  for (const key of filterKeys) {
    const value = searchParams.get(key)
    if (value) filters[key] = value
  }

  // Single immutable patcher. `resetPage` drops the page param so the user
  // lands on page 1 whenever the result set changes shape (search/sort/filter).
  const patch = React.useCallback(
    (changes: Record<string, string | undefined>, resetPage: boolean) => {
      const next = new URLSearchParams(searchParams.toString())
      if (resetPage) next.delete('page')
      for (const [key, value] of Object.entries(changes)) {
        if (value === undefined || value === '') next.delete(key)
        else next.set(key, value)
      }
      const qs = next.toString()
      router.replace(qs ? `${pathname}?${qs}` : pathname)
    },
    [router, pathname, searchParams],
  )

  return {
    page,
    pageSize,
    search,
    sortField,
    sortDirection,
    filters,
    setPage: (p) => patch({ page: String(Math.max(1, p)) }, false),
    setPageSize: (s) => patch({ pageSize: String(Math.max(1, s)) }, true),
    setSearch: (s) => patch({ search: s }, true),
    setSort: (field, direction = 'asc') =>
      patch({ sortField: field, sortDirection: field ? direction : undefined }, true),
    setFilter: (key, value) => patch({ [key]: value }, true),
    reset: () =>
      patch(
        {
          search: undefined,
          sortField: undefined,
          sortDirection: undefined,
          ...Object.fromEntries(filterKeys.map((k) => [k, undefined])),
        },
        true,
      ),
  }
}
