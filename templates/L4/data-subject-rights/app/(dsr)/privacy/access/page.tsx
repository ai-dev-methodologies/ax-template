/*
---
template_id: L4/data-subject-rights/app/(dsr)/privacy/access/page
layer: L4
domain: data-subject-rights
domain_mode: full_trio
backend_operation_id: dsrOpenAccess
evidence:
  - source_type: internal
    rationale: "L4 data-subject-rights vertical — ACCESS page composing L3 detail-page; opens a subject-access request (dsrOpenAccess) and renders the returned export bundle handle."
  - source_type: external
    citation: "TanStack Query v5 — useMutation for POST requests"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useMutation"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [other L4 domains]
---
*/
'use client'

import * as React from 'react'
import { useMutation } from '@tanstack/react-query'
import DetailPage from 'templates/L3/pages/detail-page/[id]/page'
import { parseError } from 'templates/L0/fork-receiver-kit/parse-error'

// ─── types ──────────────────────────────────────────────────────────────────

interface DsrRequestEnvelope {
  requestId: string
  type: string
  status: string
  dueAt: string
}

interface AccessBundle {
  request: DsrRequestEnvelope
  modules: Record<string, Record<string, unknown>>
}

// ─── fetcher ────────────────────────────────────────────────────────────────

async function openAccess(): Promise<AccessBundle> {
  const res = await fetch('/api/me/dsr/access', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  })
  if (!res.ok) {
    throw await parseError(res, 'Failed to open access request.')
  }
  return res.json() as Promise<AccessBundle>
}

// ─── component ──────────────────────────────────────────────────────────────

/**
 * AccessRequestPage — L4 data-subject-rights access (GDPR Art 15) page.
 *
 * Composes:
 *   L3 detail-page → page chrome (title, back link, sections slot)
 *
 * Fork instructions:
 *   1. Render the per-module bundle generically; do not hardcode field names.
 *   2. Replace the download stub with your export-job poll (report-export-l0).
 *   3. Handle 409 DSR_ACCESS_IN_FLIGHT by surfacing the existing job status.
 */
export default function AccessRequestPage() {
  const [error, setError] = React.useState<string | null>(null)
  const [bundle, setBundle] = React.useState<AccessBundle | null>(null)

  const mutation = useMutation({
    mutationFn: openAccess,
    onSuccess: (result) => setBundle(result),
    onError: (err: Error) => setError(err.message || 'Failed to open access request.'),
  })

  const sectionsSlot = (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">
        Request a copy of all personal data we hold about you. We will assemble
        a single export bundle and respond within 30 days.
      </p>
      {error && (
        <div role="alert" className="rounded-md bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {error}
        </div>
      )}
      {!bundle ? (
        <button
          type="button"
          onClick={() => {
            setError(null)
            mutation.mutate()
          }}
          disabled={mutation.isPending}
          className="inline-flex h-10 items-center rounded-md bg-primary px-5 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-60"
        >
          {mutation.isPending ? 'Opening request…' : 'Request my data'}
        </button>
      ) : (
        <div className="rounded-lg border bg-card px-6 py-4 text-sm">
          <p className="font-medium">Access request accepted.</p>
          <p className="mt-1 text-muted-foreground">
            Tracking id: <span className="font-mono">{bundle.request.requestId}</span>
            {' · '}due by {new Date(bundle.request.dueAt).toLocaleDateString()}
          </p>
          <a
            href={`/privacy/requests/${bundle.request.requestId}`}
            className="mt-3 inline-flex underline hover:no-underline"
          >
            Track this request
          </a>
        </div>
      )}
    </div>
  )

  return (
    <DetailPage
      title="Request access to my data"
      backHref="/privacy"
      backLabel="Back to my requests"
      sectionsSlot={sectionsSlot}
    />
  )
}
