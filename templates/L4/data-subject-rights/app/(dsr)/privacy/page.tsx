/*
---
template_id: L4/data-subject-rights/app/(dsr)/privacy/page
layer: L4
domain: data-subject-rights
domain_mode: full_trio
backend_operation_id: dsrGetRequest
evidence:
  - source_type: internal
    rationale: "L4 data-subject-rights vertical — privacy DASHBOARD page composing L3 list-page + L2 DataTable, EmptyState; lists the subject's DSR requests with status + SLA due date."
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
import DataTable, { type ColumnDef } from 'templates/L2/blocks/data-table'
import EmptyState from 'templates/L2/blocks/empty-state'
import { useUrlListState } from 'templates/L0/fork-receiver-kit/use-url-list-state'

// ─── types ──────────────────────────────────────────────────────────────────

// Mirrors the backend DsrRequestResponse tracking envelope (DSR-SLA-001).
interface DsrRequest {
  requestId: string
  type: 'access' | 'rectify' | 'erasure' | 'portability' | 'restrict'
  status: string
  receivedAt: string
  dueAt: string
  closedAt?: string | null
  extensionDays: number
  slaBreached: boolean
}

// The console aggregates the subject's own requests. Each row is tracked via
// the dsrGetRequest endpoint; a fork-receiver MAY expose a list endpoint, but
// the catalog backend surface is per-request (IDOR-safe by Authentication).
interface DsrRequestList {
  data: DsrRequest[]
}

// ─── columns ────────────────────────────────────────────────────────────────

const COLUMNS: ColumnDef<DsrRequest>[] = [
  { key: 'type', header: 'Right', sortable: true },
  { key: 'status', header: 'Status' },
  {
    key: 'dueAt',
    header: 'Due by',
    sortable: true,
    cell: (row) =>
      new Date(row.dueAt).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      }),
  },
  {
    key: 'slaBreached',
    header: 'SLA',
    cell: (row) =>
      row.slaBreached ? (
        <span className="text-destructive">Overdue</span>
      ) : (
        <span className="text-muted-foreground">On track</span>
      ),
  },
]

// ─── fetcher ────────────────────────────────────────────────────────────────

async function fetchMyRequests(): Promise<DsrRequestList> {
  // Fork: the backend tracks each request via GET /api/me/dsr/requests/{id}
  // (dsrGetRequest). Replace with your aggregation endpoint or maintain a
  // client-side list of the ids the subject has opened.
  const res = await fetch('/api/me/dsr/requests', {
    headers: { 'Content-Type': 'application/json' },
  })
  if (!res.ok) throw new Error(`Failed to load DSR requests: ${res.status}`)
  return res.json() as Promise<DsrRequestList>
}

// ─── component ──────────────────────────────────────────────────────────────

/**
 * PrivacyDashboardPage — L4 data-subject-rights console landing page.
 *
 * Composes:
 *   L3 list-page  → page chrome (title, action slot, list slot)
 *   L2 DataTable  → the subject's DSR requests with status + SLA due date
 *   L2 EmptyState → rendered in DataTable's emptySlot when the subject has none
 *
 * Fork instructions:
 *   1. Replace fetchMyRequests with your aggregation endpoint or a stored id list.
 *   2. Link each row to /privacy/requests/{id} for the tracking detail page.
 *   3. Wire row click → router.push(`/privacy/requests/${row.requestId}`).
 */
export default function PrivacyDashboardPage() {
  // URL is the source of truth for sort/filter (catalog url-state rule).
  const list = useUrlListState({ defaultPageSize: 20 })

  const { data, isLoading, isError } = useQuery<DsrRequestList>({
    queryKey: ['dsr-requests'],
    queryFn: fetchMyRequests,
  })

  const rows = React.useMemo(() => {
    if (!data?.data) return []
    const status = list.search
    if (!status || !status.trim()) return data.data
    const lower = status.toLowerCase()
    return data.data.filter((r) => r.status.toLowerCase().includes(lower))
  }, [data?.data, list.search])

  const listSlot = (
    <DataTable<DsrRequest>
      columns={COLUMNS}
      data={rows}
      getRowKey={(row) => row.requestId}
      isLoading={isLoading}
      emptySlot={
        isError ? (
          <EmptyState
            title="Failed to load your requests"
            description="An error occurred while loading your privacy requests. Please try again."
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
            title="No data requests yet"
            description="Exercise a data-subject right to get started. Each request is tracked against a 30-day response window."
            actionSlot={
              <a
                href="/privacy/access"
                className="inline-flex h-9 items-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:bg-primary/90"
              >
                Request my data
              </a>
            }
          />
        )
      }
    />
  )

  return (
    <ListPage
      title="My data requests"
      description="Access, correct, export, restrict, or erase your personal data — and track each request's response deadline."
      listSlot={listSlot}
      createHref="/privacy/access"
      createLabel="New request"
    />
  )
}
