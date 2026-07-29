/*
---
template_id: L4/api-key/app/(api-key)/page
layer: L4
domain: api-key
domain_mode: full_trio
backend_operation_id: listApiKeys
evidence:
  - source_type: internal
    rationale: "L4 api-key vertical — LIST page: paginated table of issued API keys with prefix + status + last-used. Plaintext secret NEVER appears in list (R38 pii-masked-at-dto-boundary)."
  - source_type: external
    citation: "TanStack Query v5 — useQuery for server-state data fetching"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useQuery"
  - source_type: external
    citation: "OWASP API Security — API3:2023 Broken Object Property Level Authorization"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa3-broken-object-property-level-authorization/"
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
import ApiKeyListView, {
  type ApiKeySummary,
  type ApiKeyPage,
  type ApiKeyFilters,
  type ApiKeyStatus,
  type ApiKeyScope,
} from './api-key-list-view'

// ─── constants ───────────────────────────────────────────────────────────────

const PAGE_SIZE = 50

// ─── column definitions ───────────────────────────────────────────────────────

const COLUMNS: ColumnDef<ApiKeySummary>[] = [
  {
    key: 'prefix',
    header: 'Key',
    render: (row) => (
      <span className="font-mono text-sm">{row.prefix}…</span>
    ),
    sortable: false,
  },
  {
    key: 'scope',
    header: 'Scope',
    render: (row) => row.scope,
    sortable: true,
  },
  {
    key: 'status',
    header: 'Status',
    render: (row) => (
      <span
        className={
          row.status === 'ACTIVE'
            ? 'text-green-600 font-medium'
            : 'text-muted-foreground'
        }
      >
        {row.status}
      </span>
    ),
    sortable: true,
  },
  {
    key: 'lastUsedAt',
    header: 'Last used',
    render: (row) =>
      row.lastUsedAt ? new Date(row.lastUsedAt).toLocaleString() : '—',
    sortable: true,
  },
  {
    key: 'createdAt',
    header: 'Created',
    render: (row) => new Date(row.createdAt).toLocaleString(),
    sortable: true,
  },
]

// ─── data fetching ───────────────────────────────────────────────────────────

async function fetchApiKeys(filters: ApiKeyFilters): Promise<ApiKeyPage> {
  const params = new URLSearchParams()
  if (filters.status) params.set('status', filters.status)
  if (filters.scope) params.set('scope', filters.scope)
  params.set('page', String(filters.page ?? 0))
  params.set('size', String(filters.size ?? PAGE_SIZE))

  const res = await fetch(`/api/api-keys?${params.toString()}`)
  if (!res.ok) {
    throw new Error(`Failed to load API keys (HTTP ${res.status})`)
  }
  return res.json()
}

// ─── page ────────────────────────────────────────────────────────────────────

/**
 * ApiKeyListPage — admin-only paginated list of issued API keys.
 *
 * Page semantics:
 *   - Server-side pagination + filtering via URL query params
 *   - Status + scope filters
 *   - Click a row → /api-key/[id] (rotation / revocation)
 *   - "Create key" CTA → /api-key/new (the one place the plaintext appears)
 *
 * Fork instructions:
 *   1. Replace `fetch('/api/api-keys')` with your typed client.
 *   2. Add a ROLE_ADMIN check in the route group layout — DO NOT rely on
 *      this page's UI to be the only gate; the Spring controller already
 *      enforces ROLE_ADMIN at the API surface.
 *   3. NEVER render the plaintext secret from this page. The list DTO
 *      should not even carry it.
 */
export default function ApiKeyListPage() {
  const router = useRouter()
  const searchParams = useSearchParams()

  const filters: ApiKeyFilters = React.useMemo(
    () => ({
      status: (searchParams.get('status') as ApiKeyStatus | null) ?? '',
      scope: (searchParams.get('scope') as ApiKeyScope | null) ?? '',
      page: Number(searchParams.get('page') ?? 0),
      size: PAGE_SIZE,
    }),
    [searchParams],
  )

  const { data, error, isLoading } = useQuery({
    queryKey: ['api-keys', filters],
    queryFn: () => fetchApiKeys(filters),
  })

  const updateFilter = React.useCallback(
    (key: keyof ApiKeyFilters, value: string) => {
      const next = new URLSearchParams(searchParams.toString())
      if (value) next.set(key, value)
      else next.delete(key)
      next.delete('page')
      router.push(`/api-key?${next.toString()}`)
    },
    [router, searchParams],
  )

  const updatePage = React.useCallback(
    (page: number) => {
      const next = new URLSearchParams(searchParams.toString())
      next.set('page', String(page))
      router.push(`/api-key?${next.toString()}`)
    },
    [router, searchParams],
  )

  return (
    <ApiKeyListView
      data={data}
      error={error as Error | null}
      isLoading={isLoading}
      filters={filters}
      onFilterChange={updateFilter}
      onPageChange={updatePage}
      onCreate={() => router.push('/api-key/new')}
      tableSlot={
        data ? (
          <VirtualizedTable
            data={data.content}
            columns={COLUMNS}
            onRowClick={(row) => router.push(`/api-key/${row.id}`)}
          />
        ) : null
      }
    />
  )
}
