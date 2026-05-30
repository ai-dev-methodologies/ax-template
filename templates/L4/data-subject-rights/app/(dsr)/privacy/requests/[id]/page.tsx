/*
---
template_id: L4/data-subject-rights/app/(dsr)/privacy/requests/[id]/page
layer: L4
domain: data-subject-rights
domain_mode: full_trio
backend_operation_id: dsrGetRequest
evidence:
  - source_type: internal
    rationale: "L4 data-subject-rights vertical — request DETAIL page composing L3 detail-page; shows a single request's tracking envelope via dsrGetRequest and offers an extend-window action (dsrExtendRequest)."
  - source_type: external
    citation: "Next.js 15 App Router dynamic routes — params prop for [id] segment"
    url: "https://nextjs.org/docs/app/building-your-application/routing/dynamic-routes"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [other L4 domains]
---
*/
'use client'

import * as React from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import DetailPage from 'templates/L3/pages/detail-page/[id]/page'
import { parseError } from 'templates/L0/fork-receiver-kit/parse-error'

// ─── types ──────────────────────────────────────────────────────────────────

interface DsrRequest {
  requestId: string
  type: string
  status: string
  receivedAt: string
  dueAt: string
  closedAt?: string | null
  extensionDays: number
  slaBreached: boolean
}

// ─── fetchers ───────────────────────────────────────────────────────────────

async function fetchRequest(id: string): Promise<DsrRequest> {
  const res = await fetch(`/api/me/dsr/requests/${id}`, {
    headers: { 'Content-Type': 'application/json' },
  })
  // IDOR-safe: another subject's id returns 404, never 403.
  if (res.status === 404) throw new Error('Request not found')
  if (!res.ok) throw new Error(`Failed to load request: ${res.status}`)
  return res.json() as Promise<DsrRequest>
}

async function extendRequest(
  id: string,
  extensionDays: number,
  extensionReason: string
): Promise<DsrRequest> {
  const res = await fetch(`/api/me/dsr/requests/${id}/extend`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ extensionDays, extensionReason }),
  })
  if (!res.ok) {
    throw await parseError(res, 'Failed to extend the request.')
  }
  return res.json() as Promise<DsrRequest>
}

// ─── helpers ─────────────────────────────────────────────────────────────────

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// ─── component ──────────────────────────────────────────────────────────────

/**
 * RequestDetailPage — L4 data-subject-rights request tracking page (DSR-SLA-001).
 *
 * Composes:
 *   L3 detail-page → page chrome (title, back link, sections slot, actions slot)
 *
 * Shows a single request's tracking envelope (status, received_at, due_at,
 * closed_at, SLA breach) and offers an extend-window action (up to 60 days).
 *
 * Fork instructions:
 *   1. Replace fetch with your API client / tRPC query + mutation.
 *   2. Gate the extend action to controllers/admins if subjects cannot self-extend.
 *   3. Render type-specific result detail (access bundle / erasure manifest) here.
 */
export default function RequestDetailPage({ params }: { params: { id: string } }) {
  const { id } = params
  const queryClient = useQueryClient()
  const [extendOpen, setExtendOpen] = React.useState(false)
  const [extensionReason, setExtensionReason] = React.useState('')
  const [error, setError] = React.useState<string | null>(null)

  const { data: request, isLoading, isError } = useQuery<DsrRequest>({
    queryKey: ['dsr-request', id],
    queryFn: () => fetchRequest(id),
  })

  const extendMutation = useMutation({
    mutationFn: () => extendRequest(id, 60, extensionReason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dsr-request', id] })
      setExtendOpen(false)
      setExtensionReason('')
    },
    onError: (err: Error) => setError(err.message || 'Failed to extend the request.'),
  })

  if (isLoading) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" aria-label="Loading" />
      </div>
    )
  }

  if (isError || !request) {
    return (
      <div className="container mx-auto px-4 py-8 max-w-2xl">
        <div role="alert" className="rounded-lg border border-destructive/40 bg-destructive/5 px-6 py-4 text-sm text-destructive">
          Request not found or you do not have permission to view it.
          <a href="/privacy" className="ml-2 underline hover:no-underline">
            Back to my requests
          </a>
        </div>
      </div>
    )
  }

  const sectionsSlot = (
    <div className="space-y-4">
      <dl className="divide-y rounded-lg border bg-card">
        <div className="grid grid-cols-3 gap-4 px-6 py-4">
          <dt className="text-sm font-medium text-muted-foreground">Right</dt>
          <dd className="col-span-2 text-sm">{request.type}</dd>
        </div>
        <div className="grid grid-cols-3 gap-4 px-6 py-4">
          <dt className="text-sm font-medium text-muted-foreground">Status</dt>
          <dd className="col-span-2 text-sm">{request.status}</dd>
        </div>
        <div className="grid grid-cols-3 gap-4 px-6 py-4">
          <dt className="text-sm font-medium text-muted-foreground">Received</dt>
          <dd className="col-span-2 text-sm">{formatDate(request.receivedAt)}</dd>
        </div>
        <div className="grid grid-cols-3 gap-4 px-6 py-4">
          <dt className="text-sm font-medium text-muted-foreground">Due by</dt>
          <dd className="col-span-2 text-sm">
            {formatDate(request.dueAt)}
            {request.slaBreached && <span className="ml-2 text-destructive">(overdue)</span>}
            {request.extensionDays > 0 && (
              <span className="ml-2 text-muted-foreground">(+{request.extensionDays}d extension)</span>
            )}
          </dd>
        </div>
        {request.closedAt && (
          <div className="grid grid-cols-3 gap-4 px-6 py-4">
            <dt className="text-sm font-medium text-muted-foreground">Closed</dt>
            <dd className="col-span-2 text-sm">{formatDate(request.closedAt)}</dd>
          </div>
        )}
      </dl>

      {error && (
        <div role="alert" className="rounded-md bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {error}
        </div>
      )}

      {extendOpen && (
        <div className="space-y-2 rounded-lg border bg-card px-6 py-4">
          <label htmlFor="extension-reason" className="block text-sm font-medium">
            Reason for extension (up to 60 days)
          </label>
          <textarea
            id="extension-reason"
            value={extensionReason}
            onChange={(e) => setExtensionReason(e.target.value)}
            className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
            rows={3}
            placeholder="Why is more time needed (complexity / volume)?"
          />
          <button
            type="button"
            onClick={() => {
              setError(null)
              extendMutation.mutate()
            }}
            disabled={extendMutation.isPending || !extensionReason.trim()}
            className="inline-flex h-9 items-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-60"
          >
            {extendMutation.isPending ? 'Extending…' : 'Confirm extension'}
          </button>
        </div>
      )}
    </div>
  )

  const actionsSlot = (
    <button
      type="button"
      onClick={() => setExtendOpen((v) => !v)}
      className="inline-flex items-center rounded-md border bg-background px-4 py-2 text-sm font-medium hover:bg-accent transition-colors"
    >
      Extend window
    </button>
  )

  return (
    <DetailPage
      title={`Request ${request.requestId.slice(0, 8)}`}
      backHref="/privacy"
      backLabel="Back to my requests"
      actionsSlot={actionsSlot}
      sectionsSlot={sectionsSlot}
    />
  )
}
