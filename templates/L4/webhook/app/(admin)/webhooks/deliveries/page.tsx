/*
---
template_id: L4/webhook/app/(admin)/webhooks/deliveries/page
layer: L4
domain: webhook
domain_mode: full_trio
backend_operation_id: listWebhookDeliveries
evidence:
  - source_type: internal
    rationale: "L4 webhook vertical — delivery monitor for SRE / incident response. Lists recent deliveries by status with replay action for failed / dead-letter rows. Lives behind ROLE_ADMIN gate."
  - source_type: external
    citation: "TanStack Query v5 — useQuery refetchInterval"
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
import { useCallerId, useCallerRole } from 'templates/L0/fork-receiver-kit/use-caller-id'
import { parseError } from 'templates/L0/fork-receiver-kit/parse-error'

// ─── types ───────────────────────────────────────────────────────────────────

type DeliveryStatus = 'PENDING' | 'PENDING_RETRY' | 'SUCCEEDED' | 'FAILED_PERMANENT'

interface DeliveryResponse {
  id: string
  endpointId: string
  eventType: string
  status: DeliveryStatus
  attemptCount: number
  nextAttemptAt: string | null
  lastResponseCode: number | null
  lastAttemptAt: string | null
  lastError: string | null
  createdAt: string
}

// ─── data ─────────────────────────────────────────────────────────────────────

async function fetchDeliveries(status?: DeliveryStatus | ''): Promise<DeliveryResponse[]> {
  const params = new URLSearchParams()
  if (status) params.set('status', status)
  const qs = params.toString()
  const res = await fetch(`/api/admin/webhook-deliveries${qs ? `?${qs}` : ''}`)
  if (!res.ok) throw await parseError(res, 'Failed to load deliveries')
  return res.json()
}

async function replayDelivery(id: string): Promise<void> {
  const res = await fetch(`/api/admin/webhook-deliveries/${encodeURIComponent(id)}/replay`, {
    method: 'POST',
  })
  if (!res.ok) throw await parseError(res, 'Failed to enqueue replay')
}

// ─── helpers ──────────────────────────────────────────────────────────────────

function statusClass(s: DeliveryStatus): string {
  switch (s) {
    case 'SUCCEEDED':
      return 'bg-green-100 text-green-900'
    case 'PENDING_RETRY':
      return 'bg-amber-100 text-amber-900'
    case 'FAILED_PERMANENT':
      return 'bg-red-100 text-red-900'
    case 'PENDING':
    default:
      return 'bg-muted text-muted-foreground'
  }
}

function canReplay(status: DeliveryStatus): boolean {
  // Replay is meaningful only when the retry chain stopped for good. SUCCEEDED
  // would be a no-op (or a duplicate); PENDING / PENDING_RETRY are still being
  // driven by the scheduler, so a manual replay would double-send.
  return status === 'FAILED_PERMANENT'
}

// R48 iter2 (F4 medium): defense-in-depth sanitization of the
// server-supplied lastError before rendering. Mirrors parse-error.ts
// deny-list — backend DTO sanitization is the canonical fix (deferred),
// but the SRE persona screen-shares this view during incident calls so
// the frontend MUST not relay raw stack traces / Bearer tokens /
// internal hostnames / credentials inline.
const LAST_ERROR_MAX = 200
function sanitizeLastError(raw: string | null): string {
  if (!raw) return ''
  const looksSensitive =
    /@[\w.-]+\.[A-Za-z]{2,}/.test(raw) ||
    /\b(?:sk-|pk-|Bearer\s+|jdbc:|-----BEGIN |ghp_|ghs_)/i.test(raw) ||
    /\b\d{1,3}(?:\.\d{1,3}){3}\b/.test(raw) ||
    /\.internal\b|\.local\b/.test(raw) ||
    /\d{6}-\d{7}/.test(raw) ||
    /01[016789]-?\d{3,4}-?\d{4}/.test(raw) ||
    /eyJ[A-Za-z0-9._-]{20,}/.test(raw)
  if (looksSensitive) {
    return '[redacted — see server logs]'
  }
  return raw.length <= LAST_ERROR_MAX ? raw : `${raw.slice(0, LAST_ERROR_MAX)}… [truncated]`
}

// ─── page ────────────────────────────────────────────────────────────────────

export default function WebhookDeliveriesPage() {
  useCallerId()
  const role = useCallerRole()
  const qc = useQueryClient()

  // ─── all hooks ABOVE the role gate ─────────────────────────────────────────

  const [statusFilter, setStatusFilter] = React.useState<DeliveryStatus | ''>('')
  const [pendingReplayIds, setPendingReplayIds] = React.useState<Set<string>>(
    () => new Set(),
  )

  const { data, error, isLoading, dataUpdatedAt, refetch } = useQuery({
    queryKey: ['webhook-deliveries', statusFilter],
    queryFn: () => fetchDeliveries(statusFilter || undefined),
    // 10s background poll — delivery rows transition PENDING → PENDING_RETRY →
    // SUCCEEDED / FAILED_PERMANENT during the retry windows; SRE eyes-on-the-board.
    //
    // R48 iter2 (F5 low): poll continues in background tabs so a
    // second-monitor incident-bridge view stays current. Without this
    // the v5 default pauses on hidden tabs.
    refetchInterval: 10_000,
    refetchIntervalInBackground: true,
  })

  const replay = useMutation({
    mutationFn: replayDelivery,
    onMutate: (id: string) => {
      setPendingReplayIds((prev) => {
        const next = new Set(prev)
        next.add(id)
        return next
      })
    },
    onSettled: (_data, _err, id) => {
      setPendingReplayIds((prev) => {
        const next = new Set(prev)
        next.delete(id)
        return next
      })
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['webhook-deliveries'] }),
  })

  if (role !== 'admin') {
    return (
      <EmptyState
        title="Admin access required"
        description="Webhook delivery monitoring is administrator-only. Ask an admin to grant your account ROLE_ADMIN."
      />
    )
  }

  return (
    <ErrorBoundary>
      <div className="space-y-4">
        <header className="flex items-baseline justify-between gap-2">
          <div>
            <h1 className="text-lg font-semibold">Deliveries</h1>
            <p className="text-sm text-muted-foreground">
              Recent webhook delivery attempts. Refreshes every 10 seconds.
              Replay re-enqueues a FAILED_PERMANENT (dead-letter) delivery —
              succeeded rows and rows still awaiting a scheduled retry cannot be
              replayed.
            </p>
          </div>
          <div className="flex items-center gap-3">
            {/* R48 iter2 (F5 low): visible 'last refreshed' indicator
                 + manual Refresh button so SRE can confirm staleness
                 explicitly during pager response. */}
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
                onChange={(e) => setStatusFilter(e.target.value as DeliveryStatus | '')}
              >
                <option value="">All</option>
                <option value="PENDING">Pending</option>
                <option value="PENDING_RETRY">Pending retry</option>
                <option value="SUCCEEDED">Succeeded</option>
                <option value="FAILED_PERMANENT">Failed (dead-letter)</option>
              </select>
            </label>
          </div>
        </header>

        {replay.error && (
          <div
            role="alert"
            className="flex items-start justify-between gap-3 rounded border border-red-300 bg-red-50 px-3 py-1.5 text-sm text-red-900"
          >
            <span>Replay failed: {replay.error.message}</span>
            <button
              type="button"
              className="shrink-0 text-xs underline"
              onClick={() => replay.reset()}
            >
              Dismiss
            </button>
          </div>
        )}

        {isLoading ? (
          <div className="py-12 text-center text-sm text-muted-foreground">
            Loading deliveries…
          </div>
        ) : error ? (
          <EmptyState title="Failed to load deliveries" description={(error as Error).message} />
        ) : !data || data.length === 0 ? (
          <EmptyState
            title="No deliveries match the filter"
            description={
              statusFilter
                ? `No ${statusFilter} deliveries found. Switch to "All" to see other rows.`
                : 'No webhook deliveries have been attempted yet.'
            }
            actionLabel={statusFilter ? 'Show all' : undefined}
            onAction={statusFilter ? () => setStatusFilter('') : undefined}
          />
        ) : (
          <ul className="divide-y rounded border">
            {data.map((d) => {
              const isReplaying = pendingReplayIds.has(d.id)
              const replayable = canReplay(d.status)
              return (
                <li key={d.id} className="flex items-start gap-3 px-4 py-3">
                  <div className="min-w-0 flex-1 space-y-1">
                    <div className="flex items-center gap-2">
                      <span
                        className={`shrink-0 rounded px-1.5 py-0.5 text-[10px] uppercase ${statusClass(d.status)}`}
                      >
                        {d.status}
                      </span>
                      <span className="truncate font-mono text-xs">{d.eventType}</span>
                      <span className="shrink-0 text-xs text-muted-foreground">
                        attempt {d.attemptCount}
                      </span>
                    </div>
                    <div className="text-xs text-muted-foreground">
                      endpoint <span className="font-mono">{d.endpointId}</span>
                      {d.lastAttemptAt && (
                        <>
                          {' '}· last attempt {new Date(d.lastAttemptAt).toLocaleString()}
                        </>
                      )}
                      {d.lastResponseCode !== null && <> · HTTP {d.lastResponseCode}</>}
                      {d.nextAttemptAt && d.status === 'PENDING_RETRY' && (
                        <> · next retry at {new Date(d.nextAttemptAt).toLocaleString()}</>
                      )}
                    </div>
                    {d.lastError && (
                      <div className="rounded border border-red-200 bg-red-50/50 px-2 py-1 text-xs text-red-800">
                        {/* R47 error-message-not-in-native-title: the
                             server's last error is shown inline (not in
                             a tooltip) so SRE sees it without hover.
                             R48 iter2 (F4): defense-in-depth — apply
                             PII/secret deny-list before rendering. */}
                        last error: <code>{sanitizeLastError(d.lastError)}</code>
                      </div>
                    )}
                  </div>
                  {replayable ? (
                    <button
                      type="button"
                      className="shrink-0 rounded border px-2 py-1 text-xs hover:bg-muted aria-busy:opacity-60 aria-disabled:opacity-50"
                      aria-busy={isReplaying || undefined}
                      aria-disabled={isReplaying || undefined}
                      aria-label={`Replay delivery ${d.id}`}
                      onClick={() => {
                        if (isReplaying) return
                        // R48 iter2 (F3 high): replay is destructive-
                        // by-side-effect — sends another HTTP POST to
                        // the partner. Korean enterprise partners often
                        // lack idempotent receivers, so duplicate
                        // delivery → duplicate side effects. Asymmetric
                        // with delete (which confirms) before iter2;
                        // now both confirm.
                        const ok = window.confirm(
                          `Re-enqueue this delivery?\n\n${d.eventType} (attempt ${d.attemptCount})\nendpoint ${d.endpointId}\n\nThis sends another HTTP POST to the partner endpoint. If the original eventually succeeded server-side, the partner receives duplicate side effects.`,
                        )
                        if (!ok) return
                        replay.mutate(d.id)
                      }}
                    >
                      {isReplaying ? 'Enqueuing…' : 'Replay'}
                    </button>
                  ) : (
                    // R48 iter2 (F6 low): explicit affordance when
                    // canReplay is false. Header copy mentions the
                    // rule but a per-row hint reduces SRE confusion
                    // during fast triage.
                    <span
                      className="shrink-0 text-[10px] uppercase text-muted-foreground"
                      aria-label={
                        d.status === 'SUCCEEDED'
                          ? 'Replay not available — already succeeded'
                          : 'Replay not available — retry chain still running'
                      }
                    >
                      no replay
                    </span>
                  )}
                </li>
              )
            })}
          </ul>
        )}
      </div>
    </ErrorBoundary>
  )
}
