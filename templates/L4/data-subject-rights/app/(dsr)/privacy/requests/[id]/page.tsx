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
import { parseError } from 'templates/L0/fork-receiver-kit/parse-error'
import RequestDetailView, { type DsrRequest } from './request-detail-view'

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

  return (
    <RequestDetailView
      request={request}
      extendOpen={extendOpen}
      onToggleExtendOpen={() => setExtendOpen((v) => !v)}
      extensionReason={extensionReason}
      onExtensionReasonChange={setExtensionReason}
      onConfirmExtend={() => {
        setError(null)
        extendMutation.mutate()
      }}
      extendPending={extendMutation.isPending}
      error={error}
    />
  )
}
