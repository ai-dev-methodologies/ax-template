/*
---
template_id: L4/audit-log/app/(audit-log)/page
layer: L4
domain: audit-log
domain_mode: full_trio
backend_operation_id: listAuditLogs
evidence:
  - source_type: internal
    rationale: "L4 audit-log vertical — LIST page: VirtualizedTable (SP15) for >10k rows, FilterBar, Pagination, EmptyState, ErrorBoundary. Server-side pagination + filtering via URL query params."
  - source_type: external
    citation: "TanStack Query v5 — useQuery for server-state data fetching"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useQuery"
  - source_type: external
    citation: "@tanstack/react-virtual — Virtualize only what you see (SP15, AUDIT-FE-001)"
    url: "https://tanstack.com/virtual/latest"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
---
*/
'use client'

import * as React from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import VirtualizedTable, { type ColumnDef } from 'templates/L2/blocks/virtualized-table'
import FilterBar from 'templates/L2/blocks/filter-bar'
import Pagination from 'templates/L2/blocks/pagination'
import EmptyState from 'templates/L2/blocks/empty-state'
import ErrorBoundary from 'templates/L2/blocks/error-boundary'

// ─── types ───────────────────────────────────────────────────────────────────

type AuditOutcome = 'SUCCESS' | 'FAILURE'

interface AuditLogSummary {
  id: string
  actorId: string
  actorIp: string | null
  action: string
  resourceType: string
  resourceId: string | null
  outcome: AuditOutcome
  timestamp: string   // ISO 8601
}

interface AuditLogPage {
  content: AuditLogSummary[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

interface AuditLogFilters {
  actorId?: string
  resourceType?: string
  resourceId?: string
  action?: string
  outcome?: AuditOutcome | ''
  from?: string
  to?: string
  page?: number
  size?: number
}

// ─── constants ───────────────────────────────────────────────────────────────

const PAGE_SIZE = 50

// ─── column definitions ───────────────────────────────────────────────────────

const COLUMNS: ColumnDef<AuditLogSummary>[] = [
  {
    key: 'timestamp',
    header: 'Timestamp',
    render: (row) => new Date(row.timestamp).toLocaleString(),
    sortable: true,
  },
  {
    key: 'actorId',
    header: 'Actor',
    render: (row) => row.actorId,
    sortable: false,
  },
  {
    key: 'action',
    header: 'Action',
    render: (row) => row.action,
    sortable: false,
  },
  {
    key: 'resourceType',
    header: 'Resource',
    render: (row) => `${row.resourceType}${row.resourceId ? ` / ${row.resourceId}` : ''}`,
    sortable: false,
  },
  {
    key: 'outcome',
    header: 'Outcome',
    render: (row) => (
      <span
        style={{
          color: row.outcome === 'SUCCESS' ? 'var(--color-success, green)' : 'var(--color-error, red)',
          fontWeight: 600,
        }}
      >
        {row.outcome}
      </span>
    ),
    sortable: false,
  },
]

// ─── filter field definitions (for FilterBar) ────────────────────────────────

const FILTER_FIELDS = [
  { key: 'actorId',      label: 'Actor',         type: 'text'   as const },
  { key: 'resourceType', label: 'Resource Type',  type: 'text'   as const },
  { key: 'action',       label: 'Action',         type: 'text'   as const },
  { key: 'outcome',      label: 'Outcome',        type: 'select' as const,
    options: [{ value: '', label: 'Any' }, { value: 'SUCCESS', label: 'Success' }, { value: 'FAILURE', label: 'Failure' }] },
  { key: 'from',         label: 'From',           type: 'datetime-local' as const },
  { key: 'to',           label: 'To',             type: 'datetime-local' as const },
]

// ─── API ──────────────────────────────────────────────────────────────────────

async function fetchAuditLogs(filters: AuditLogFilters): Promise<AuditLogPage> {
  const params = new URLSearchParams()
  if (filters.page != null) params.set('page', String(filters.page))
  if (filters.size != null) params.set('size', String(filters.size))
  if (filters.actorId)      params.set('actorId', filters.actorId)
  if (filters.resourceType) params.set('resourceType', filters.resourceType)
  if (filters.resourceId)   params.set('resourceId', filters.resourceId)
  if (filters.action)       params.set('action', filters.action)
  if (filters.outcome)      params.set('outcome', filters.outcome)
  if (filters.from)         params.set('from', filters.from)
  if (filters.to)           params.set('to', filters.to)

  const res = await fetch(`/api/audit-logs?${params.toString()}`)
  if (!res.ok) throw new Error(`listAuditLogs failed: ${res.status}`)
  return res.json()
}

// ─── component ────────────────────────────────────────────────────────────────

/**
 * AuditLogListPage — L4 audit-log list page.
 *
 * Renders VirtualizedTable for large datasets (AUDIT-FE-001).
 * FilterBar drives actorId/resourceType/action/outcome/date filters (AUDIT-FE-002).
 * Pagination component supports page navigation (AUDIT-FE-003).
 * EmptyState rendered when no entries match (AUDIT-FE-007).
 * ErrorBoundary wraps the table for graceful API failure (AUDIT-FE-008).
 *
 * All filter + pagination state is persisted in URL query params.
 *
 * Fork instructions:
 *   1. Add row-click navigation: router.push(`/audit-log/${row.id}`)
 *   2. Add column sort — wire onSort to actorId/timestamp URL params.
 *   3. Add date pickers with a proper date-range picker component if needed.
 *   4. For export button: add link to /audit-log/export (role-gated in the server component).
 */
export default function AuditLogListPage() {
  const router = useRouter()
  const searchParams = useSearchParams()

  // ─── derive filter state from URL ──────────────────────────────────────────
  const filters: AuditLogFilters = {
    page: Number(searchParams.get('page') ?? '0'),
    size: PAGE_SIZE,
    actorId:      searchParams.get('actorId')      ?? undefined,
    resourceType: searchParams.get('resourceType') ?? undefined,
    resourceId:   searchParams.get('resourceId')   ?? undefined,
    action:       searchParams.get('action')       ?? undefined,
    outcome:      (searchParams.get('outcome') as AuditOutcome | '') || undefined,
    from:         searchParams.get('from')         ?? undefined,
    to:           searchParams.get('to')           ?? undefined,
  }

  // ─── data fetching ─────────────────────────────────────────────────────────
  // `filters.actorId` (derived from `searchParams` above) is an ADMIN SEARCH FILTER —
  // it narrows the audit-log list BY actor, a role-gated admin capability — not the
  // caller's OWN authz identity. The heuristic `ax/no-caller-identity-from-props` rule
  // matches on the identity-shaped key name (`actorId`) and cannot statically
  // distinguish a filter facet from a caller-identity value; the backend enforces
  // role-gating server-side (the authoritative BFLA control here, mirroring
  // `caller-authentication-only-no-userid-param`). See docs/BACKLOG.md P3-55.
  // eslint-disable-next-line ax/no-caller-identity-from-props -- admin audit-log search filter BY actorId (role-gated server-side), not the caller's own identity — see docs/BACKLOG.md P3-55
  const { data, isLoading, error } = useQuery({
    queryKey: ['audit-logs', filters],
    // (round-11 sink narrowing: the local wrapper `fetchAuditLogs(filters)` is no longer
    // a lint sink — positional identity into a local data-wrapper is documented out of
    // scope — so the former second disable directive here would be unused and is removed.
    // The useQuery config above still needs its directive: `filters.actorId` is a
    // role-gated admin search facet, not the caller's own identity. See P3-55.)
    queryFn: () => fetchAuditLogs(filters),
    placeholderData: (prev) => prev,
  })

  // ─── filter change handler — updates URL ───────────────────────────────────
  function handleFilterChange(newFilters: Record<string, string>) {
    const params = new URLSearchParams(searchParams.toString())
    // Reset to page 0 when filter changes
    params.set('page', '0')
    for (const [key, value] of Object.entries(newFilters)) {
      if (value) {
        params.set(key, value)
      } else {
        params.delete(key)
      }
    }
    router.push(`?${params.toString()}`)
  }

  function handlePageChange(page: number) {
    const params = new URLSearchParams(searchParams.toString())
    params.set('page', String(page))
    router.push(`?${params.toString()}`)
  }

  function handleClearFilters() {
    router.push('?page=0')
  }

  function handleRowClick(row: AuditLogSummary) {
    router.push(`/audit-log/${row.id}`)
  }

  // ─── render ────────────────────────────────────────────────────────────────
  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Audit Log</h1>
        <a href="/audit-log/export" className="text-sm underline">
          Export
        </a>
      </div>

      {/* Filters (AUDIT-FE-002) */}
      <FilterBar
        fields={FILTER_FIELDS}
        values={{
          actorId:      filters.actorId      ?? '',
          resourceType: filters.resourceType ?? '',
          action:       filters.action       ?? '',
          outcome:      filters.outcome      ?? '',
          from:         filters.from         ?? '',
          to:           filters.to           ?? '',
        }}
        onChange={handleFilterChange}
        onClear={handleClearFilters}
      />

      {/* Table with error boundary (AUDIT-FE-001, AUDIT-FE-008) */}
      <ErrorBoundary fallback={<p>Failed to load audit logs. <button onClick={() => router.refresh()}>Retry</button></p>}>
        {error ? (
          <p className="text-destructive">
            Failed to load audit logs.{' '}
            <button onClick={() => router.refresh()} className="underline">Retry</button>
          </p>
        ) : data?.content.length === 0 ? (
          // Empty state (AUDIT-FE-007)
          <EmptyState
            title="No audit log entries"
            description="No entries match your current filters."
            action={{ label: 'Clear filters', onClick: handleClearFilters }}
          />
        ) : (
          // VirtualizedTable handles >10k rows (AUDIT-FE-001)
          <VirtualizedTable
            columns={COLUMNS}
            data={data?.content ?? []}
            getRowKey={(row) => row.id}
            isLoading={isLoading}
            onRowClick={handleRowClick}
            containerHeight={600}
            estimatedRowHeight={40}
            overscan={5}
          />
        )}
      </ErrorBoundary>

      {/* Pagination (AUDIT-FE-003) */}
      {data && data.totalPages > 1 && (
        <Pagination
          page={data.page}
          totalPages={data.totalPages}
          onPageChange={handlePageChange}
          totalElements={data.totalElements}
          pageSize={data.size}
        />
      )}
    </div>
  )
}
