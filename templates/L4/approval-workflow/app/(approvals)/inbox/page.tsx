/*
---
template_id: L4/approval-workflow/app/(approvals)/inbox/page
layer: L4
domain: approval-workflow
domain_mode: full_trio
backend_operation_id: getMyApprovalInbox
evidence:
  - source_type: internal
    rationale: "L4 approval-workflow vertical — caller's pending step inbox. Action-driven surface; clicking a row goes to the request detail where approve/reject lives."
  - source_type: external
    citation: "TanStack Query v5 — useQuery for server-state"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useQuery"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
---
*/
'use client'

import * as React from 'react'
import { useRouter } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import EmptyState from 'templates/L2/blocks/empty-state'
import ErrorBoundary from 'templates/L2/blocks/error-boundary'
import { parseError } from '../../parse-error'

// ─── types ───────────────────────────────────────────────────────────────────

type StepStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

interface InboxEntry {
  requestId: string
  stepId: string
  type: string
  title: string
  status: StepStatus
  requesterUserId: string
  createdAt: string
}

interface InboxResponse {
  items: InboxEntry[]
  totalElements: number
}

// ─── data ─────────────────────────────────────────────────────────────────────

async function fetchInbox(): Promise<InboxResponse> {
  const res = await fetch('/api/approvals/inbox')
  // R43 iter5 (P1-iter4-N1 / P2-iter4-N1): adopt the shared parseError so
  // a structured server message ('일시 점검 중', 'access denied for tenant')
  // reaches the EmptyState instead of "HTTP 503".
  if (!res.ok) throw await parseError(res, 'Failed to load inbox')
  const data: InboxResponse = await res.json()
  // R43 iter1 (P2-F2): explicit oldest-first sort. The header copy
  // promises "oldest at the top" and we now enforce that client-side
  // rather than trusting the server's response order. createdAt is an
  // ISO string so lexicographic compare is correct.
  data.items = [...data.items].sort((a, b) => a.createdAt.localeCompare(b.createdAt))
  return data
}

function ageInHours(iso: string, now: Date): number {
  return Math.max(0, (now.getTime() - new Date(iso).getTime()) / 3_600_000)
}

function ageBadgeClass(hours: number): string {
  // R43 iter1 (P2-F1 partial): visual urgency cue derived from age.
  // Full amount-summary would require an InboxEntry DTO extension on the
  // backend; this gives approvers some priority signal in the meantime.
  if (hours >= 72) return 'bg-red-100 text-red-900'
  if (hours >= 24) return 'bg-amber-100 text-amber-900'
  return 'bg-muted text-muted-foreground'
}

function ageLabel(hours: number): string {
  if (hours < 1) return 'just now'
  if (hours < 24) return `${Math.floor(hours)}h waiting`
  return `${Math.floor(hours / 24)}d waiting`
}

// ─── page ────────────────────────────────────────────────────────────────────

/**
 * ApprovalInboxPage — caller's pending approval steps.
 *
 * Why this is the default landing:
 *   The most common reason a B2B-admin user opens this app is because
 *   someone is waiting on them. Surfacing pending count + oldest first
 *   minimizes the "where do I act?" friction.
 *
 * Caller-authentication-only (R38 rule): the server derives the caller
 * id from Authentication.getName(). The client never sends ?userId=.
 *
 * Sequential ordering invariant: the backend only places a step into
 * a caller's inbox once all earlier-orderIndex steps on the same
 * request are APPROVED. Approvers therefore see only steps they can
 * actually act on right now — not pending steps that are still blocked
 * by an upstream approver.
 */
export default function ApprovalInboxPage() {
  const router = useRouter()
  // R43 iter3 (P1-iter2-N3 / P2-iter2-N3): the age-badge clock must
  // advance even when the tab is left open. Tick once per minute so
  // a row that crossed the 24h or 72h threshold gets re-coloured
  // without a manual refresh.
  const [now, setNow] = React.useState(() => new Date())
  React.useEffect(() => {
    const t = setInterval(() => setNow(new Date()), 60_000)
    return () => clearInterval(t)
  }, [])
  const { data, error, isLoading } = useQuery({
    queryKey: ['approval-inbox'],
    queryFn: fetchInbox,
  })

  return (
    <ErrorBoundary>
      <div className="space-y-4">
        <header>
          <h1 className="text-lg font-semibold">Pending my approval</h1>
          <p className="text-sm text-muted-foreground">
            Requests where it is currently your turn to act. Oldest at the top —
            rows that have been waiting longer than a day are highlighted.
          </p>
        </header>

        {isLoading ? (
          <div className="py-12 text-center text-sm text-muted-foreground">Loading inbox…</div>
        ) : error ? (
          <EmptyState title="Failed to load inbox" description={(error as Error).message} />
        ) : !data || data.items.length === 0 ? (
          // R43 iter1 (P1-F9): empty state offers a path to file a new
          // request so a first-time visitor is not stuck.
          <EmptyState
            title="Nothing waiting on you"
            description="No requests are currently waiting on your approval. If you came here to file your own request, use the button below."
            actionLabel="File a new request"
            onAction={() => router.push('/approvals/new')}
          />
        ) : (
          <ul className="divide-y rounded border">
            {data.items.map((entry) => {
              const hours = ageInHours(entry.createdAt, now)
              return (
                <li
                  key={entry.stepId}
                  className="flex cursor-pointer items-center justify-between gap-4 px-4 py-3 hover:bg-muted"
                  onClick={() => router.push(`/approvals/${entry.requestId}`)}
                >
                  <div className="min-w-0 flex-1">
                    <div className="flex items-baseline gap-2">
                      <span className="rounded bg-muted px-2 py-0.5 text-xs uppercase">
                        {entry.type}
                      </span>
                      <span className="truncate text-sm font-medium">{entry.title}</span>
                    </div>
                    <div className="mt-1 text-xs text-muted-foreground">
                      Requested by <span className="font-mono">{entry.requesterUserId}</span>{' '}
                      · {new Date(entry.createdAt).toLocaleString()}
                    </div>
                  </div>
                  <div className="flex shrink-0 flex-col items-end gap-1">
                    <span
                      className={`rounded px-2 py-1 text-xs font-medium ${ageBadgeClass(hours)}`}
                      title="Time since the request was filed — older requests are prioritized"
                    >
                      {ageLabel(hours)}
                    </span>
                    <span className="rounded bg-amber-100 px-2 py-0.5 text-[10px] font-medium text-amber-900">
                      YOUR TURN
                    </span>
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
