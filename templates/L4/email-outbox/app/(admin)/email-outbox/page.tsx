/*
---
template_id: L4/email-outbox/app/(admin)/email-outbox/page
layer: L4
domain: email-outbox
domain_mode: full_trio
backend_operation_id: listEmailOutbox
evidence:
  - source_type: internal
    rationale: "L4 email-outbox vertical — admin outbox monitor. 10s background poll + per-row Retry/Delete with R50 destructive-action-confirm + R50 stored-server-error-sanitize + R47 hooks-before-conditional-return / aria-busy / optimistic snapshot-rollback."
  - source_type: external
    citation: "TanStack Query v5 — useQuery + useMutation"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useQuery"
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API5:2023 BFLA"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa5-broken-function-level-authorization/"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
---
*/
'use client'

import * as React from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import EmptyState from 'templates/L2/blocks/empty-state'
import ErrorBoundary from 'templates/L2/blocks/error-boundary'
import { useCallerId, useCallerRole } from '../../use-caller-id'
import { parseError, sanitizeStoredError } from '../../parse-error'

// ─── types ───────────────────────────────────────────────────────────────────

type OutboxStatus = 'PENDING' | 'RETRY' | 'SENT' | 'DLQ'

interface OutboxResponse {
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

// ─── data ─────────────────────────────────────────────────────────────────────

async function fetchOutbox(status: OutboxStatus | ''): Promise<OutboxResponse[]> {
  const params = new URLSearchParams()
  if (status) params.set('status', status)
  const qs = params.toString()
  const res = await fetch(`/api/admin/email-outbox${qs ? `?${qs}` : ''}`)
  if (!res.ok) throw await parseError(res, 'Failed to load outbox')
  return res.json()
}

async function retryOutbox(id: string): Promise<OutboxResponse> {
  const res = await fetch(`/api/admin/email-outbox/${encodeURIComponent(id)}/retry`, {
    method: 'POST',
  })
  if (!res.ok) throw await parseError(res, 'Failed to retry')
  return res.json()
}

async function deleteOutbox(id: string): Promise<void> {
  const res = await fetch(`/api/admin/email-outbox/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  })
  if (!res.ok) throw await parseError(res, 'Failed to delete')
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

// ─── page ────────────────────────────────────────────────────────────────────

export default function EmailOutboxPage() {
  useCallerId()
  const role = useCallerRole()
  const qc = useQueryClient()

  // ─── all hooks ABOVE role gate (R47 hooks-before-conditional-return) ────

  const [statusFilter, setStatusFilter] = React.useState<OutboxStatus | ''>('')
  const [pendingRetryIds, setPendingRetryIds] = React.useState<Set<string>>(() => new Set())
  const [pendingDeleteIds, setPendingDeleteIds] = React.useState<Set<string>>(() => new Set())

  const { data, error, isLoading, dataUpdatedAt, refetch } = useQuery({
    queryKey: ['email-outbox', statusFilter],
    queryFn: () => fetchOutbox(statusFilter),
    // R50 incident-dashboard-background-poll-plus-refresh:
    refetchInterval: 10_000,
    refetchIntervalInBackground: true,
  })

  const retry = useMutation({
    mutationFn: retryOutbox,
    onMutate: (id: string) => {
      setPendingRetryIds((prev) => {
        const next = new Set(prev)
        next.add(id)
        return next
      })
    },
    onSettled: (_data, _err, id) => {
      setPendingRetryIds((prev) => {
        const next = new Set(prev)
        next.delete(id)
        return next
      })
      qc.invalidateQueries({ queryKey: ['email-outbox'] })
    },
  })

  const del = useMutation({
    mutationFn: deleteOutbox,
    onMutate: async (id: string) => {
      setPendingDeleteIds((prev) => {
        const next = new Set(prev)
        next.add(id)
        return next
      })
      // R47 optimistic-update-snapshot-rollback: drop the row from cache
      // immediately, restore on error.
      await qc.cancelQueries({ queryKey: ['email-outbox', statusFilter] })
      const previous = qc.getQueryData<OutboxResponse[]>(['email-outbox', statusFilter])
      qc.setQueryData<OutboxResponse[]>(['email-outbox', statusFilter], (old) =>
        old ? old.filter((r) => r.id !== id) : old,
      )
      return { previous }
    },
    onError: (_err, _id, ctx) => {
      if (ctx?.previous) qc.setQueryData(['email-outbox', statusFilter], ctx.previous)
      qc.invalidateQueries({ queryKey: ['email-outbox'] })
    },
    onSettled: (_data, _err, id) => {
      setPendingDeleteIds((prev) => {
        const next = new Set(prev)
        next.delete(id)
        return next
      })
      qc.invalidateQueries({ queryKey: ['email-outbox'] })
    },
  })

  if (role !== 'admin') {
    return (
      <EmptyState
        title="Admin access required"
        description="Email outbox is administrator-only. Ask an admin to grant your account ROLE_ADMIN."
      />
    )
  }

  return (
    <ErrorBoundary>
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
              onClick={() => refetch()}
            >
              Refresh
            </button>
            <label className="flex items-center gap-2">
              <span className="text-xs text-muted-foreground">Status:</span>
              <select
                className="rounded border px-2 py-1 text-sm"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value as OutboxStatus | '')}
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

        {(retry.error || del.error) && (
          <div className="space-y-1.5">
            {retry.error && (
              <div
                role="alert"
                className="flex items-start justify-between gap-3 rounded border border-red-300 bg-red-50 px-3 py-1.5 text-sm text-red-900"
              >
                <span>Retry failed: {retry.error.message}</span>
                <button
                  type="button"
                  className="shrink-0 text-xs underline"
                  onClick={() => retry.reset()}
                >
                  Dismiss
                </button>
              </div>
            )}
            {del.error && (
              <div
                role="alert"
                className="flex items-start justify-between gap-3 rounded border border-red-300 bg-red-50 px-3 py-1.5 text-sm text-red-900"
              >
                <span>Delete failed: {del.error.message}</span>
                <button
                  type="button"
                  className="shrink-0 text-xs underline"
                  onClick={() => del.reset()}
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
          <EmptyState title="Failed to load outbox" description={(error as Error).message} />
        ) : !data || data.length === 0 ? (
          <EmptyState
            title="No outbox rows match the filter"
            description={
              statusFilter
                ? `No ${statusFilter} rows found. Switch to "All" to see other rows.`
                : 'No emails have been enqueued yet.'
            }
            actionLabel={statusFilter ? 'Show all' : undefined}
            onAction={statusFilter ? () => setStatusFilter('') : undefined}
          />
        ) : (
          <ul className="divide-y rounded border">
            {data.map((row) => {
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
                retry.mutate(row.id)
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
                del.mutate(row.id)
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
        )}
      </div>
    </ErrorBoundary>
  )
}
