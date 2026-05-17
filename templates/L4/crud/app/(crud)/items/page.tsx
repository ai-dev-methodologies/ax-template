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

// ─── types ──────────────────────────────────────────────────────────────────

interface Item {
  id: string
  title: string
  description?: string
  createdBy: string
  createdAt: string
  updatedAt?: string
}

interface ItemsPage {
  content: Item[]
  totalElements: number
  totalPages: number
  page: number
  size: number
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
 *   L2 Pagination   → page controls wired to page/totalPages
 *   L2 BulkActionsBar → rendered above table when rows selected
 *
 * Fork instructions:
 *   1. Replace `fetch('/api/items')` with your API client / tRPC / SWR call.
 *   2. Update COLUMNS to match your entity fields.
 *   3. Implement bulk action handlers (e.g. batch delete) in handleBulkDelete.
 *   4. Wire sort state to query params or server-sort params as needed.
 */
export default function ItemsListPage() {
  const [page, setPage] = React.useState(0)
  const [search, setSearch] = React.useState('')
  const [sort, setSort] = React.useState<SortState | undefined>(undefined)
  const [selectedKeys, setSelectedKeys] = React.useState<Set<string>>(new Set())

  const PAGE_SIZE = 20

  const { data, isLoading, isError } = useQuery<ItemsPage>({
    queryKey: ['items', page, PAGE_SIZE],
    queryFn: () => fetchItems(page, PAGE_SIZE),
  })

  // Client-side search filter (title contains search string)
  const filtered = React.useMemo(() => {
    if (!data?.content) return []
    if (!search.trim()) return data.content
    const lower = search.toLowerCase()
    return data.content.filter((item) =>
      item.title.toLowerCase().includes(lower)
    )
  }, [data?.content, search])

  function handleBulkDelete() {
    // Fork: implement batch-delete API call using selectedKeys
    // e.g. await Promise.all([...selectedKeys].map(id => deleteItem(id)))
    setSelectedKeys(new Set())
  }

  const filterSlot = (
    <FilterBar>
      <SearchInput
        value={search}
        onChange={setSearch}
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
          actions={[
            {
              label: 'Delete selected',
              onClick: handleBulkDelete,
              variant: 'destructive',
            },
          ]}
        />
      )}
      <DataTable<Item>
        columns={COLUMNS}
        data={filtered}
        getRowKey={(row) => row.id}
        sort={sort}
        onSort={setSort}
        selectedKeys={selectedKeys}
        onSelectionChange={setSelectedKeys}
        isLoading={isLoading}
        emptySlot={
          isError ? (
            <EmptyState
              title="Failed to load items"
              description="An error occurred while fetching your items. Please try again."
              actionLabel="Retry"
              onAction={() => window.location.reload()}
            />
          ) : (
            <EmptyState
              title="No items yet"
              description="Create your first item to get started."
              actionLabel="Create item"
              actionHref="/items/new"
            />
          )
        }
      />
    </>
  )

  const paginationSlot = data && data.totalPages > 1 ? (
    <Pagination
      currentPage={data.page}
      totalPages={data.totalPages}
      onPageChange={setPage}
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
