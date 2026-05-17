/*
---
template_id: L4/payment/app/(payment)/methods/page
layer: L4
domain: payment
domain_mode: full_trio
backend_operation_id: listPayments
evidence:
  - source_type: internal
    rationale: "L4 payment vertical — METHODS LIST page composing L3 list-page + L2 DataTable, FilterBar, Pagination, EmptyState. Lists payment history with their payment method types via listPayments endpoint."
  - source_type: external
    citation: "TanStack Query v5 — useQuery for server-state data fetching"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useQuery"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices]
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
import SearchInput from 'templates/L2/blocks/search-input'

// ─── types ──────────────────────────────────────────────────────────────────

type PaymentStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'UNKNOWN' | 'REFUNDED' | 'VOIDED'

interface Payment {
  id: string
  orderId: string
  status: PaymentStatus
  amount: number
  currency: string
  paymentMethod: string
  createdAt: string
}

interface PaymentsPage {
  content: Payment[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

// ─── helpers ────────────────────────────────────────────────────────────────

function formatAmount(amount: number, currency: string): string {
  try {
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency,
      minimumFractionDigits: 0,
    }).format(amount / 100)
  } catch {
    return `${currency} ${(amount / 100).toFixed(2)}`
  }
}

const STATUS_BADGE_CLASS: Record<PaymentStatus, string> = {
  COMPLETED: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
  PENDING:   'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400',
  FAILED:    'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
  REFUNDED:  'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400',
  VOIDED:    'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300',
  UNKNOWN:   'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300',
}

// ─── columns ────────────────────────────────────────────────────────────────

const COLUMNS: ColumnDef<Payment>[] = [
  {
    key: 'createdAt',
    header: 'Date',
    sortable: true,
    cell: (row) =>
      new Date(row.createdAt).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      }),
  },
  { key: 'orderId', header: 'Order ID', cell: (row) => (
    <span className="font-mono text-xs">{row.orderId}</span>
  )},
  {
    key: 'amount',
    header: 'Amount',
    sortable: true,
    cell: (row) => (
      <span className="tabular-nums font-medium">
        {formatAmount(row.amount, row.currency)}
      </span>
    ),
  },
  { key: 'paymentMethod', header: 'Method' },
  {
    key: 'status',
    header: 'Status',
    cell: (row) => (
      <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_BADGE_CLASS[row.status] ?? STATUS_BADGE_CLASS.UNKNOWN}`}>
        {row.status}
      </span>
    ),
  },
]

// ─── fetcher ────────────────────────────────────────────────────────────────

async function fetchPayments(page: number, size: number): Promise<PaymentsPage> {
  const res = await fetch(`/api/payments?page=${page}&size=${size}`, {
    headers: { 'Content-Type': 'application/json' },
  })
  if (!res.ok) throw new Error(`Failed to fetch payments: ${res.status}`)
  return res.json() as Promise<PaymentsPage>
}

// ─── component ──────────────────────────────────────────────────────────────

/**
 * PaymentMethodsListPage — L4 payment history / methods list page.
 *
 * Composes:
 *   L3 list-page   → page chrome (title, create link, filter slot, list slot, pagination slot)
 *   L2 FilterBar   → filter group containing SearchInput (client-side filter by orderId)
 *   L2 DataTable   → server-paginated payment rows with sort
 *   L2 EmptyState  → rendered when data is empty
 *   L2 Pagination  → page controls wired to page/totalPages
 *
 * Fork instructions:
 *   1. Replace fetch('/api/payments') with your API client / tRPC call.
 *   2. Update COLUMNS to include your payment entity fields.
 *   3. Add server-side search/filter params to the fetch call.
 *   4. Wire row click to navigate to /methods/{id} for payment detail.
 */
export default function PaymentMethodsListPage() {
  const [page, setPage] = React.useState(0)
  const [search, setSearch] = React.useState('')
  const [sort, setSort] = React.useState<SortState | undefined>(undefined)

  const PAGE_SIZE = 20

  const { data, isLoading, isError } = useQuery<PaymentsPage>({
    queryKey: ['payments', page, PAGE_SIZE],
    queryFn: () => fetchPayments(page, PAGE_SIZE),
  })

  // Client-side search filter (orderId or paymentMethod contains search)
  const filtered = React.useMemo(() => {
    if (!data?.content) return []
    if (!search.trim()) return data.content
    const lower = search.toLowerCase()
    return data.content.filter(
      (p) =>
        p.orderId.toLowerCase().includes(lower) ||
        p.paymentMethod.toLowerCase().includes(lower)
    )
  }, [data?.content, search])

  const filterSlot = (
    <FilterBar>
      <SearchInput
        value={search}
        onChange={setSearch}
        placeholder="Search by order ID or method…"
        aria-label="Search payment history"
      />
    </FilterBar>
  )

  const listSlot = (
    <DataTable<Payment>
      columns={COLUMNS}
      data={filtered}
      getRowKey={(row) => row.id}
      sort={sort}
      onSort={setSort}
      isLoading={isLoading}
      emptySlot={
        isError ? (
          <EmptyState
            title="Failed to load payments"
            description="An error occurred while fetching your payment history. Please try again."
            actionLabel="Retry"
            onAction={() => window.location.reload()}
          />
        ) : (
          <EmptyState
            title="No payments yet"
            description="Your payment history will appear here after your first transaction."
            actionLabel="Make a payment"
            actionHref="/checkout"
          />
        )
      }
    />
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
      title="Payment History"
      description="View and manage your payments"
      filterSlot={filterSlot}
      listSlot={listSlot}
      paginationSlot={paginationSlot}
      createHref="/methods/new"
      createLabel="New payment"
    />
  )
}
