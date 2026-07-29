/*
---
template_id: L4/api-key/app/(api-key)/api-key-list-view
layer: L4
domain: api-key
domain_mode: full_trio
evidence:
  - source_type: internal
    rationale: "Pure presentational extraction from (api-key)/page.tsx (BACKLOG P2-42
      render-testability pass-1 closure — same class as (crud)/items/[id]/item-detail-view.tsx):
      the page's data-fetch orchestration (useQuery) is a hard dependency-resolution boundary for
      a vitest that imports this file directly from outside frontend/ — the
      @tanstack/react-query bare specifier does not resolve for a module living in templates/L4/...
      (see frontend/tests/audit-log-redaction-render.vitest.tsx's own note on the same class of
      gap). templates/L2/blocks/virtualized-table has the SAME class of gap one level deeper (it
      statically imports @tanstack/react-virtual, unresolvable from a templates/L2/blocks/ file
      rendered outside frontend/ — see frontend/tests/session-management-view.vitest.tsx's own
      note), so this view takes the already-instantiated table as a `tableSlot` prop instead of
      constructing VirtualizedTable itself. templates/L2/blocks/{filter-bar,pagination,empty-state,
      error-boundary} have zero external-npm deps and are safe to import/render directly. NOTE:
      the pre-existing page.tsx called FilterBar with a `fields`/`onChange`/type:'select' shape
      that does not match FilterBar's actual multi-select chip props (`filters`/`activeValues`/
      `onFilter`) — that mismatch predates this conversion (the component would have thrown on
      `filters.map` the first time it was ever actually rendered) and is fixed here as a
      single-select-over-chips adapter so the extracted view is both correct and testable."
---
*/
import * as React from 'react'
import FilterBar from 'templates/L2/blocks/filter-bar'
import Pagination from 'templates/L2/blocks/pagination'
import EmptyState from 'templates/L2/blocks/empty-state'
import ErrorBoundary from 'templates/L2/blocks/error-boundary'

// ─── types ──────────────────────────────────────────────────────────────────

export type ApiKeyStatus = 'ACTIVE' | 'REVOKED'
export type ApiKeyScope = 'READ' | 'WRITE'

export interface ApiKeySummary {
  id: string
  prefix: string
  scope: ApiKeyScope
  status: ApiKeyStatus
  createdAt: string
  lastUsedAt: string | null
  revokedAt: string | null
}

export interface ApiKeyPage {
  content: ApiKeySummary[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

export interface ApiKeyFilters {
  status?: ApiKeyStatus | ''
  scope?: ApiKeyScope | ''
  page?: number
  size?: number
}

export interface ApiKeyListViewProps {
  data: ApiKeyPage | undefined
  error: Error | null
  isLoading: boolean
  filters: ApiKeyFilters
  onFilterChange: (key: keyof ApiKeyFilters, value: string) => void
  onPageChange: (page: number) => void
  onCreate: () => void
  /** The already-instantiated VirtualizedTable element (see evidence rationale above). */
  tableSlot: React.ReactNode
}

// ─── component ──────────────────────────────────────────────────────────────

/**
 * ApiKeyListView — pure presentational render of the admin API-key list.
 *
 * Deliberately has ZERO data-fetching dependencies (no useQuery) — the caller
 * (`(api-key)/page.tsx`) owns the query state, assembles the VirtualizedTable with its
 * row-click navigation bound in, and passes the resolved element in via `tableSlot`. This keeps
 * the component a plain props -> JSX function, which is what makes it renderable in a unit test
 * without a QueryClientProvider or the @tanstack/react-virtual dependency graph.
 */
export default function ApiKeyListView({
  data,
  error,
  isLoading,
  filters,
  onFilterChange,
  onPageChange,
  onCreate,
  tableSlot,
}: ApiKeyListViewProps) {
  return (
    <ErrorBoundary>
      <div className="space-y-4">
        {/* FilterBar is a multi-select chip toggle group (filters/activeValues/onFilter) —
            adapted here to single-select semantics: clicking a chip selects it as the sole
            active value for that key; clicking the already-active chip clears the filter. */}
        <FilterBar
          filters={[
            {
              key: 'status',
              label: 'Status',
              options: [
                { label: 'Active', value: 'ACTIVE' },
                { label: 'Revoked', value: 'REVOKED' },
              ],
            },
            {
              key: 'scope',
              label: 'Scope',
              options: [
                { label: 'Read', value: 'READ' },
                { label: 'Write', value: 'WRITE' },
              ],
            },
          ]}
          activeValues={{
            status: filters.status ? [filters.status] : [],
            scope: filters.scope ? [filters.scope] : [],
          }}
          onFilter={(key, values) => onFilterChange(key as keyof ApiKeyFilters, values[values.length - 1] ?? '')}
        />

        {isLoading ? (
          <div className="py-12 text-center text-sm text-muted-foreground">
            Loading API keys…
          </div>
        ) : error ? (
          <EmptyState
            title="Failed to load API keys"
            description={error.message}
          />
        ) : !data || data.content.length === 0 ? (
          <EmptyState
            title="No API keys yet"
            description="Create your first API key to integrate with the platform."
            actionSlot={
              <button
                type="button"
                className="rounded border bg-primary px-3 py-1.5 text-sm text-primary-foreground hover:opacity-90"
                onClick={onCreate}
              >
                Create key
              </button>
            }
          />
        ) : (
          <>
            {tableSlot}
            <Pagination
              page={data.page}
              totalPages={data.totalPages}
              onPageChange={onPageChange}
            />
          </>
        )}
      </div>
    </ErrorBoundary>
  )
}
