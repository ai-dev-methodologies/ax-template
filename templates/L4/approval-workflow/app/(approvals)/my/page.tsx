/*
---
template_id: L4/approval-workflow/app/(approvals)/my/page
layer: L4
domain: approval-workflow
domain_mode: full_trio
backend_operation_id: listMyApprovalRequests
evidence:
  - source_type: internal
    rationale: "L4 approval-workflow vertical — caller's filed requests across all statuses. Companion to /inbox (steps you act on); this view is requests YOU filed."
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
import { parseError } from 'templates/L0/fork-receiver-kit/parse-error'

// ─── types ───────────────────────────────────────────────────────────────────

type RequestStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'CANCELLED'

interface ApprovalRequestRow {
  id: string
  type: string
  title: string
  status: RequestStatus
  createdAt: string
  submittedAt: string | null
  completedAt: string | null
}

interface ApprovalListResponse {
  items: ApprovalRequestRow[]
  totalElements: number
}

// ─── data ─────────────────────────────────────────────────────────────────────

async function fetchMyRequests(): Promise<ApprovalListResponse> {
  const res = await fetch('/api/approvals')
  // R43 iter5 (P1-iter4-N1 / P2-iter4-N1): adopt the shared parseError so
  // a server detail message reaches the EmptyState rather than a bare
  // HTTP status — same contract as inbox and /[id].
  if (!res.ok) throw await parseError(res, 'Failed to load requests')
  return res.json()
}

// ─── helpers ──────────────────────────────────────────────────────────────────

function statusClass(s: RequestStatus): string {
  switch (s) {
    case 'APPROVED':
      return 'bg-green-100 text-green-900'
    case 'REJECTED':
      return 'bg-red-100 text-red-900'
    case 'CANCELLED':
      return 'bg-muted text-muted-foreground'
    case 'SUBMITTED':
      return 'bg-amber-100 text-amber-900'
    case 'DRAFT':
      return 'bg-blue-100 text-blue-900'
  }
}

// ─── page ────────────────────────────────────────────────────────────────────

/**
 * MyRequestsPage — caller's filed requests across all statuses.
 *
 * Caller-authentication-only (R38 rule): the server derives the caller
 * id from Authentication.getName(). The client never sends ?userId=.
 *
 * Why a separate page from /inbox:
 *   /inbox = "what's waiting on me as an approver"
 *   /my    = "what I asked someone else to approve"
 *   These two perspectives are conflated in many B2B approval tools and
 *   that conflation is a common source of "I can't find my request" tickets.
 */
export default function MyRequestsPage() {
  const router = useRouter()
  const { data, error, isLoading } = useQuery({
    queryKey: ['my-approval-requests'],
    queryFn: fetchMyRequests,
  })

  return (
    <ErrorBoundary>
      <div className="space-y-4">
        <header className="flex items-baseline justify-between">
          <div>
            <h1 className="text-lg font-semibold">My requests</h1>
            <p className="text-sm text-muted-foreground">
              Requests you filed. Includes drafts, in-flight, and completed.
            </p>
          </div>
          <button
            type="button"
            className="rounded bg-foreground px-3 py-1.5 text-sm text-background hover:opacity-90"
            onClick={() => router.push('/approvals/new')}
          >
            New request
          </button>
        </header>

        {isLoading ? (
          <div className="py-12 text-center text-sm text-muted-foreground">
            Loading requests…
          </div>
        ) : error ? (
          <EmptyState title="Failed to load requests" description={(error as Error).message} />
        ) : !data || data.items.length === 0 ? (
          <EmptyState
            title="No requests yet"
            description="You haven't filed any approval requests. Start with a new request."
            actionLabel="New request"
            onAction={() => router.push('/approvals/new')}
          />
        ) : (
          <ul className="divide-y rounded border">
            {data.items.map((row) => (
              <li
                key={row.id}
                className="flex cursor-pointer items-center justify-between gap-4 px-4 py-3 hover:bg-muted"
                onClick={() => router.push(`/approvals/${row.id}`)}
              >
                <div className="min-w-0 flex-1">
                  <div className="flex items-baseline gap-2">
                    <span className="rounded bg-muted px-2 py-0.5 text-xs uppercase">
                      {row.type}
                    </span>
                    <span className="truncate text-sm font-medium">{row.title}</span>
                  </div>
                  <div className="mt-1 text-xs text-muted-foreground">
                    Filed {new Date(row.createdAt).toLocaleString()}
                    {row.completedAt && ` · completed ${new Date(row.completedAt).toLocaleString()}`}
                  </div>
                </div>
                <span
                  className={`shrink-0 rounded px-2 py-1 text-xs font-medium ${statusClass(row.status)}`}
                >
                  {row.status}
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </ErrorBoundary>
  )
}
