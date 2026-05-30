/*
---
template_id: L4/crud/app/(crud)/items/page
layer: L4
domain: crud
domain_mode: full_trio
backend_operation_id: listItems
evidence:
  - source_type: internal
    rationale: "L4 crud vertical — items LIST page composing L3 list-page + L2 DataTable, FilterBar, Pagination, EmptyState, BulkActionsBar, SearchInput."
  - source_type: external
    citation: "TanStack Query v5 — useQuery for server-state data fetching"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useQuery"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [other L4 domains]
---
*/
'use client'

import * as React from 'react'
import { useQuery } from '@tanstack/react-query'
import ListPage from 'templates/L3/pages/list-page/page'
import DataTable, { type ColumnDef, type SortState } from 'templates/L2/blocks/data-table'
import FilterBar from 'templates/L2/blocks/filter-bar'
import Pagination from 'templates/L2/blocks/pagination'
import EmptyState from 'templates/L2/blocks/empty-state'
import BulkActionsBar from 'templates/L2/blocks/bulk-actions-bar'
import SearchInput from 'templates/L2/blocks/search-input'
import { useUrlListState } from 'templates/L0/fork-receiver-kit/use-url-list-state'

// ─── types ──────────────────────────────────────────────────────────────────

interface Item {
  id: string
  title: string
  description?: string
  createdBy: string
  createdAt: string
  updatedAt?: string
}

// Canonical pagination envelope — the shape backend common/PageEnvelope emits
// (pagination-l0): { data, pagination:{ page (0-based), pageSize, totalElements,
// totalPages, hasMore } }. FMW3 aligned this off the stale Spring
// { content, totalElements, totalPages, page, size } shape so a fork copying
// this reference matches what the backend actually returns.
interface ItemsPage {
  data: Item[]
  pagination: {
    page: number
    pageSize: number
    totalElements: number
    totalPages: number
    hasMore: boolean
  }
}

// ─── columns ────────────────────────────────────────────────────────────────

const COLUMNS: ColumnDef<Item>[] = [
  { key: 'title', header: 'Title', sortable: true },
  {
    key: 'createdAt',
    header: 'Created',
    sortable: true,
    cell: (row) =>
      new Date(row.createdAt).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      }),
  },
  { key: 'createdBy', header: 'Created By' },
]

// ─── fetcher ────────────────────────────────────────────────────────────────

async function fetchItems(page: number, size: number): Promise<ItemsPage> {
  const res = await fetch(`/api/items?page=${page}&size=${size}`, {
    headers: { 'Content-Type': 'application/json' },
  })
  if (!res.ok) throw new Error(`Failed to fetch items: ${res.status}`)
  return res.json() as Promise<ItemsPage>
}

// ─── component ──────────────────────────────────────────────────────────────

/**
 * ItemsListPage — L4 crud list page.
 *
 * Composes:
 *   L3 list-page    → page chrome (title, create link, filter slot, list slot, pagination slot)
 *   L2 FilterBar    → filter group containing SearchInput
 *   L2 DataTable    → server-paginated item rows with sort + row selection
 *   L2 EmptyState   → rendered in DataTable's emptySlot when data is empty
 *   L2 Pagination   → page controls wired to PageEnvelope page/pageSize/total
 *   L2 BulkActionsBar → rendered above table when rows selected
 *
 * Fork instructions:
 *   1. Replace `fetch('/api/items')` with your API client / tRPC / SWR call.
 *   2. Update COLUMNS to match your entity fields.
 *   3. Implement bulk action handlers (e.g. batch delete) in handleBulkDelete.
 *   4. Wire sort state to query params or server-sort params as needed.
 */
export default function ItemsListPage() {
  // URL is the source of truth for page/search/sort (the catalog's own
  // saved-view-must-be-url-state-or-server-persisted rule). useUrlListState
  // (L0) owns the query-string ↔ state seam; only the EPHEMERAL row selection
  // stays in component state. FMW4b: the L4 reference dogfoods the URL-state rule.
  const PAGE_SIZE = 20
  const list = useUrlListState({ defaultPageSize: PAGE_SIZE })
  const [selectedKeys, setSelectedKeys] = React.useState<Set<string>>(new Set())

  const { data, isLoading, isError } = useQuery<ItemsPage>({
    // PageEnvelope.pagination.page is 0-based; the URL state is 1-based, so
    // send list.page - 1 to the backend.
    queryKey: ['items', list.page, list.pageSize],
    queryFn: () => fetchItems(list.page - 1, list.pageSize),
  })

  // Client-side search filter (title contains search string)
  const filtered = React.useMemo(() => {
    if (!data?.data) return []
    const search = list.search
    if (!search || !search.trim()) return data.data
    const lower = search.toLowerCase()
    return data.data.filter((item) =>
      item.title.toLowerCase().includes(lower)
    )
  }, [data?.data, list.search])

  // Map the URL sort fields to the DataTable's SortState shape.
  const sort: SortState | undefined = list.sortField
    ? { field: list.sortField, direction: list.sortDirection ?? 'asc' }
    : undefined

  function handleBulkDelete() {
    // Fork: implement batch-delete API call using selectedKeys
    // e.g. await Promise.all([...selectedKeys].map(id => deleteItem(id)))
    setSelectedKeys(new Set())
  }

  const filterSlot = (
    <FilterBar>
      <SearchInput
        value={list.search ?? ''}
        onChange={list.setSearch}
        placeholder="Search items…"
        aria-label="Search items by title"
      />
    </FilterBar>
  )

  const listSlot = (
    <>
      {selectedKeys.size > 0 && (
        <BulkActionsBar
          selectedCount={selectedKeys.size}
          onClearSelection={() => setSelectedKeys(new Set())}
          actionsSlot={
            <button
              type="button"
              onClick={handleBulkDelete}
              className="inline-flex h-8 items-center rounded-md bg-destructive px-3 text-sm font-medium text-destructive-foreground hover:bg-destructive/90"
            >
              Delete selected
            </button>
          }
        />
      )}
      <DataTable<Item>
        columns={COLUMNS}
        data={filtered}
        getRowKey={(row) => row.id}
        sort={sort}
        onSort={(s) => list.setSort(s.field, s.direction)}
        selectedKeys={selectedKeys}
        onSelectionChange={setSelectedKeys}
        isLoading={isLoading}
        emptySlot={
          isError ? (
            <EmptyState
              title="Failed to load items"
              description="An error occurred while fetching your items. Please try again."
              actionSlot={
                <button
                  type="button"
                  onClick={() => window.location.reload()}
                  className="inline-flex h-9 items-center rounded-md border border-input bg-background px-4 text-sm font-medium hover:bg-accent"
                >
                  Retry
                </button>
              }
            />
          ) : (
            <EmptyState
              title="No items yet"
              description="Create your first item to get started."
              actionSlot={
                <a
                  href="/items/new"
                  className="inline-flex h-9 items-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:bg-primary/90"
                >
                  Create item
                </a>
              }
            />
          )
        }
      />
    </>
  )

  // PageEnvelope.pagination.page is 0-based; the Pagination block is 1-based,
  // so convert at the boundary (display page+1; store the chosen page-1).
  // URL state (1-based) drives the page; Pagination is 1-based too, so pass
  // list.page directly and route changes back through list.setPage.
  const paginationSlot = data && data.pagination.totalPages > 1 ? (
    <Pagination
      page={list.page}
      pageSize={list.pageSize}
      total={data.pagination.totalElements}
      onPageChange={list.setPage}
    />
  ) : null

  return (
    <ListPage
      title="Items"
      description="Manage your items"
      filterSlot={filterSlot}
      listSlot={listSlot}
      paginationSlot={paginationSlot}
      createHref="/items/new"
      createLabel="New item"
    />
  )
}
