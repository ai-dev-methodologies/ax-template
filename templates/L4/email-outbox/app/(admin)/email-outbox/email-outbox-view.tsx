/*
---
template_id: L4/email-outbox/app/(admin)/email-outbox/email-outbox-view
layer: L4
domain: email-outbox
domain_mode: full_trio
evidence:
  - source_type: internal
    rationale: "Pure presentational extraction from (admin)/email-outbox/page.tsx (BACKLOG P2-28
      render-testability closure — same class as (audit-log)/[id]/audit-log-detail-view.tsx): the
      page's data-fetch/mutation orchestration (useQuery/useMutation/useQueryClient) is a hard
      dependency-resolution boundary for a vitest that imports this file directly from outside
      frontend/ — the @tanstack/react-query bare specifier does not resolve for a module living in
      templates/L4/... (see frontend/tests/audit-log-redaction-render.vitest.tsx's own note on the
      same class of gap). Splitting the resolved-data->JSX render surface (list + per-row
      retry/delete affordances + status filter + inline error banners) into its own file, taking
      every mutation trigger as a callback prop, makes the whole render path unit-testable without
      touching shared vitest config. templates/L2/blocks/{empty-state,error-boundary} are safe to
      import here (React-only, zero external-npm deps — unlike @tanstack/react-query) and are kept
      to preserve the exact EmptyState-driven sub-states, per FDW1 catalog-consistency."
---
*/
import * as React from 'react'
import EmptyState from 'templates/L2/blocks/empty-state'

// ─── types ───────────────────────────────────────────────────────────────────

export type OutboxStatus = 'PENDING' | 'RETRY' | 'SENT' | 'DLQ'

export interface OutboxResponse {
  id: string
  recipient: string
  templateCode: string
  subject: string
  status: OutboxStatus
  retryCount: number
  nextAttemptAt: string | null
  lastError: string | null
  createdAt: string
  sentAt: string | null
}

export interface OutboxPage {
  content: OutboxResponse[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface EmailOutboxViewProps {
  data: OutboxPage | undefined
  error: Error | null
  isLoading: boolean
  dataUpdatedAt: number
  statusFilter: OutboxStatus | ''
  onStatusFilterChange: (status: OutboxStatus | '') => void
  onRefetch: () => void
  retryError: Error | null
  onDismissRetryError: () => void
  deleteError: Error | null
  onDismissDeleteError: () => void
  pendingRetryIds: Set<string>
  pendingDeleteIds: Set<string>
  onRetry: (id: string) => void
  onDelete: (id: string) => void
  /** Same shape server-side refusal mirror as the original page (SENT rows cannot be retried). */
  sanitizeStoredError: (raw: string) => string
}

// ─── helpers ──────────────────────────────────────────────────────────────────

function statusClass(s: OutboxStatus): string {
  switch (s) {
    case 'SENT':
      return 'bg-green-100 text-green-900'
    case 'DLQ':
      return 'bg-red-100 text-red-900'
    case 'RETRY':
      return 'bg-amber-100 text-amber-900'
    case 'PENDING':
    default:
      return 'bg-muted text-muted-foreground'
  }
}

function canRetry(s: OutboxStatus): boolean {
  return s !== 'SENT' // server-side refuses SENT retry; mirror for UX
}

// ─── component ──────────────────────────────────────────────────────────────

/**
 * EmailOutboxView — pure presentational render of the admin email-outbox monitor.
 *
 * Deliberately has ZERO data-fetching/mutation dependencies (no useQuery/useMutation) — the
 * caller (`(admin)/email-outbox/page.tsx`) owns all query/mutation state and passes the resolved
 * `data`, error objects, pending-id sets, and mutation-trigger callbacks in. This keeps the
 * component a plain props -> JSX function, which is what makes it renderable in a unit test
 * without a QueryClientProvider.
 *
 * Confirm-before-destructive-action (R50 destructive-action-confirm-with-side-effects) stays here
 * — it is an interaction/presentation concern (window.confirm), not data-fetching state; only the
 * post-confirmation trigger (`onRetry`/`onDelete`) crosses back out to the caller's mutations.
 */
export default function EmailOutboxView({
  data,
  error,
  isLoading,
  dataUpdatedAt,
  statusFilter,
  onStatusFilterChange,
  onRefetch,
  retryError,
  onDismissRetryError,
  deleteError,
  onDismissDeleteError,
  pendingRetryIds,
  pendingDeleteIds,
  onRetry,
  onDelete,
  sanitizeStoredError,
}: EmailOutboxViewProps) {
  return (
    <div className="space-y-4">
      <header className="flex items-baseline justify-between gap-2">
        <div>
          <h1 className="text-lg font-semibold">Email outbox</h1>
          <p className="text-sm text-muted-foreground">
            Transactional email queue. PENDING → SENT (success) or DLQ (after
            {' '}{3} retries). Retry resets a DLQ/RETRY row to PENDING for the
            next processQueue cycle. SENT rows cannot be retried (duplicate
            send) — server refuses with 409.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <span className="text-xs text-muted-foreground" aria-live="polite">
            {dataUpdatedAt ? `Updated ${new Date(dataUpdatedAt).toLocaleTimeString()}` : ''}
          </span>
          <button
            type="button"
            className="rounded border px-2 py-1 text-xs hover:bg-muted"
            onClick={onRefetch}
          >
            Refresh
          </button>
          <label className="flex items-center gap-2">
            <span className="text-xs text-muted-foreground">Status:</span>
            <select
              className="rounded border px-2 py-1 text-sm"
              value={statusFilter}
              onChange={(e) => onStatusFilterChange(e.target.value as OutboxStatus | '')}
            >
              <option value="">All</option>
              <option value="PENDING">Pending</option>
              <option value="RETRY">Retry</option>
              <option value="SENT">Sent</option>
              <option value="DLQ">Dead-letter</option>
            </select>
          </label>
        </div>
      </header>

      {(retryError || deleteError) && (
        <div className="space-y-1.5">
          {retryError && (
            <div
              role="alert"
              className="flex items-start justify-between gap-3 rounded border border-red-300 bg-red-50 px-3 py-1.5 text-sm text-red-900"
            >
              <span>Retry failed: {retryError.message}</span>
              <button
                type="button"
                className="shrink-0 text-xs underline"
                onClick={onDismissRetryError}
              >
                Dismiss
              </button>
            </div>
          )}
          {deleteError && (
            <div
              role="alert"
              className="flex items-start justify-between gap-3 rounded border border-red-300 bg-red-50 px-3 py-1.5 text-sm text-red-900"
            >
              <span>Delete failed: {deleteError.message}</span>
              <button
                type="button"
                className="shrink-0 text-xs underline"
                onClick={onDismissDeleteError}
              >
                Dismiss
              </button>
            </div>
          )}
        </div>
      )}

      {isLoading ? (
        <div className="py-12 text-center text-sm text-muted-foreground">
          Loading outbox…
        </div>
      ) : error ? (
        <EmptyState title="Failed to load outbox" description={error.message} />
      ) : !data || data.content.length === 0 ? (
        <EmptyState
          title="No outbox rows match the filter"
          description={
            statusFilter
              ? `No ${statusFilter} rows found. Switch to "All" to see other rows.`
              : 'No emails have been enqueued yet.'
          }
          actionLabel={statusFilter ? 'Show all' : undefined}
          onAction={statusFilter ? () => onStatusFilterChange('') : undefined}
        />
      ) : (
        <>
          {/* R60 dogfood F3 closure — show the operator the queue size so a
              5000-row outbox doesn't look like a 50-row outbox. */}
          <div className="text-xs text-muted-foreground">
            Showing {data.content.length} of {data.totalElements} row
            {data.totalElements === 1 ? '' : 's'}
            {data.totalPages > 1 && ` · page ${data.page + 1} of ${data.totalPages}`}
          </div>
          <ul className="divide-y rounded border">
            {data.content.map((row) => {
              const isRetrying = pendingRetryIds.has(row.id)
              const isDeleting = pendingDeleteIds.has(row.id)
              const retryable = canRetry(row.status)
              const handleRetry = () => {
                if (isRetrying) return
                // R50 destructive-action-confirm-with-side-effects: retry
                // will re-fire the send chain to the recipient.
                const ok = window.confirm(
                  `Retry this email?\n\ntemplate ${row.templateCode}\nto ${row.recipient}\n\nThe row's retryCount resets to 0 and the next processQueue cycle attempts another send. The recipient receives the email again — if the original eventually succeeded server-side they receive a duplicate.`,
                )
                if (!ok) return
                onRetry(row.id)
              }
              const handleDelete = () => {
                if (isDeleting) return
                // R50 destructive-action-confirm-with-side-effects: delete
                // removes the row from the outbox audit trail. SENT rows
                // lose their "we delivered this" record.
                const ok = window.confirm(
                  `Delete this outbox row?\n\ntemplate ${row.templateCode}\nto ${row.recipient}\n\nThe row is removed permanently. If status was SENT, the audit record of delivery is lost. If DLQ, the operator's failed-delivery evidence is lost.`,
                )
                if (!ok) return
                onDelete(row.id)
              }
              return (
                <li key={row.id} className="flex items-start gap-3 px-4 py-3">
                  <div className="min-w-0 flex-1 space-y-1">
                    <div className="flex items-center gap-2">
                      <span
                        className={`shrink-0 rounded px-1.5 py-0.5 text-[10px] uppercase ${statusClass(row.status)}`}
                      >
                        {row.status}
                      </span>
                      <span className="truncate text-sm font-medium">
                        {row.subject}
                      </span>
                      <span className="shrink-0 text-xs text-muted-foreground">
                        attempt {row.retryCount}
                      </span>
                    </div>
                    <div className="text-xs text-muted-foreground">
                      template <code>{row.templateCode}</code> · to{' '}
                      <span className="font-mono">{row.recipient}</span>
                      {row.sentAt && <> · sent {new Date(row.sentAt).toLocaleString()}</>}
                      {row.nextAttemptAt && row.status === 'RETRY' && (
                        <> · next retry at {new Date(row.nextAttemptAt).toLocaleString()}</>
                      )}
                    </div>
                    {row.lastError && (
                      <div className="rounded border border-red-200 bg-red-50/50 px-2 py-1 text-xs text-red-800">
                        last error: <code>{sanitizeStoredError(row.lastError)}</code>
                      </div>
                    )}
                  </div>
                  <div className="flex shrink-0 gap-2">
                    {retryable ? (
                      <button
                        type="button"
                        className="rounded border px-2 py-1 text-xs hover:bg-muted aria-busy:opacity-60 aria-disabled:opacity-50"
                        aria-busy={isRetrying || undefined}
                        aria-disabled={isRetrying || undefined}
                        aria-label={`Retry email ${row.id}`}
                        onClick={handleRetry}
                      >
                        {isRetrying ? 'Retrying…' : 'Retry'}
                      </button>
                    ) : (
                      <span
                        className="text-[10px] uppercase text-muted-foreground"
                        aria-label="Retry not available — already sent"
                      >
                        sent
                      </span>
                    )}
                    <button
                      type="button"
                      className="rounded border border-red-300 px-2 py-1 text-xs text-red-700 hover:bg-red-50 aria-busy:opacity-60 aria-disabled:opacity-50"
                      aria-busy={isDeleting || undefined}
                      aria-disabled={isDeleting || undefined}
                      aria-label={`Delete outbox row ${row.id}`}
                      onClick={handleDelete}
                    >
                      {isDeleting ? 'Deleting…' : 'Delete'}
                    </button>
                  </div>
                </li>
              )
            })}
          </ul>
        </>
      )}
    </div>
  )
}
