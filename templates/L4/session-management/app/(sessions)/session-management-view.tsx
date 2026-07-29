/*
---
template_id: L4/session-management/app/(sessions)/session-management-view
layer: L4
domain: session-management
domain_mode: full_trio
evidence:
  - source_type: internal
    rationale: "Pure presentational extraction from (sessions)/page.tsx (BACKLOG P2-42
      render-testability pass-1 closure — same class as (crud)/items/[id]/item-detail-view.tsx):
      the page's data-fetch/mutation orchestration (useQuery/useMutation/useQueryClient) is a hard
      dependency-resolution boundary for a vitest that imports this file directly from outside
      frontend/ — the @tanstack/react-query bare specifier does not resolve for a module living in
      templates/L4/... (see frontend/tests/audit-log-redaction-render.vitest.tsx's own note on the
      same class of gap). templates/L2/blocks/virtualized-table has the SAME class of gap one
      level deeper (it statically imports @tanstack/react-virtual, unresolvable from a
      templates/L2/blocks/ file rendered outside frontend/ — see
      frontend/tests/L2/search-palette-hydration.spec.ts's cmdk note for the identical mechanism),
      so this view takes the already-instantiated table as a `tableSlot` prop instead of
      constructing VirtualizedTable itself. templates/L2/blocks/{empty-state,error-boundary} have
      zero external-npm deps and are safe to import/render directly."
---
*/
import * as React from 'react'
import EmptyState from 'templates/L2/blocks/empty-state'
import ErrorBoundary from 'templates/L2/blocks/error-boundary'

// ─── types ──────────────────────────────────────────────────────────────────

export type SessionStatus = 'ACTIVE' | 'REVOKED' | 'EXPIRED'

export interface SessionSummary {
  id: string
  status: SessionStatus
  jti: string
  deviceLabel: string | null
  ipAddressMasked: string
  userAgentSummary: string
  createdAt: string
  lastSeenAt: string | null
  expiresAt: string
  revokedAt: string | null
  revokedByUserId: string | null
  expired: boolean
}

export interface SessionListResponse {
  items: SessionSummary[]
  totalElements: number
}

export interface SessionManagementViewProps {
  data: SessionListResponse | undefined
  error: Error | null
  isLoading: boolean
  /** The already-instantiated VirtualizedTable element — kept out of this file because
   *  VirtualizedTable's '@tanstack/react-virtual' dependency cannot resolve when imported
   *  from a vitest that renders this view directly (see evidence rationale above). */
  tableSlot: React.ReactNode
  onRevokeOthers: () => void
  revokeOthersPending: boolean
}

// ─── component ──────────────────────────────────────────────────────────────

/**
 * SessionManagementView — pure presentational render of the caller's session inventory.
 *
 * Deliberately has ZERO data-fetching/mutation dependencies (no useQuery/useMutation) — the
 * caller (`(sessions)/page.tsx`) owns all query/mutation state, assembles the VirtualizedTable
 * with its row-level Revoke action bound in, and passes the resolved element in via `tableSlot`.
 * This keeps the component a plain props -> JSX function, which is what makes it renderable in a
 * unit test without a QueryClientProvider or the @tanstack/react-virtual dependency graph.
 */
export default function SessionManagementView({
  data,
  error,
  isLoading,
  tableSlot,
  onRevokeOthers,
  revokeOthersPending,
}: SessionManagementViewProps) {
  return (
    <ErrorBoundary>
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            Active sessions on your account. IPs are masked and User-Agent strings are
            summarized — the raw values are stored server-side for forensics but never
            leave the database layer.
          </p>
          <button
            type="button"
            className="rounded border px-3 py-1.5 text-sm hover:bg-muted disabled:opacity-50"
            disabled={revokeOthersPending}
            onClick={onRevokeOthers}
          >
            Revoke other sessions
          </button>
        </div>

        {isLoading ? (
          <div className="py-12 text-center text-sm text-muted-foreground">
            Loading sessions…
          </div>
        ) : error ? (
          <EmptyState
            title="Failed to load sessions"
            description={error.message}
          />
        ) : !data || data.items.length === 0 ? (
          <EmptyState
            title="No sessions"
            description="You have no recorded sessions yet."
          />
        ) : (
          tableSlot
        )}
      </div>
    </ErrorBoundary>
  )
}
