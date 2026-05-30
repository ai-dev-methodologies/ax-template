/*
---
template_id: L4/data-subject-rights/app/(dsr)/privacy/portability/page
layer: L4
domain: data-subject-rights
domain_mode: full_trio
backend_operation_id: dsrPortability
evidence:
  - source_type: internal
    rationale: "L4 data-subject-rights vertical — PORTABILITY page composing L3 detail-page; lets the subject choose json/csv and calls dsrPortability, exposing the DSR-Schema-Version pinned download."
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

type ExportFormat = 'json' | 'csv'

interface DsrRequestEnvelope {
  requestId: string
  status: string
  dueAt: string
}

interface PortableBundle {
  request: DsrRequestEnvelope
  modules: Record<string, Record<string, unknown>>
}

// ─── fetcher ────────────────────────────────────────────────────────────────

async function requestPortability(format: ExportFormat): Promise<PortableBundle> {
  const res = await fetch(`/api/me/dsr/portability?format=${format}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  })
  if (!res.ok) {
    // 400 DSR_PORTABILITY_FORMAT_INVALID for an unsupported format.
    throw await parseError(res, 'Failed to produce a portable copy.')
  }
  return res.json() as Promise<PortableBundle>
}

// ─── component ──────────────────────────────────────────────────────────────

/**
 * PortabilityPage — L4 data-subject-rights portability (GDPR Art 20) page.
 *
 * Composes:
 *   L3 detail-page → page chrome (title, back link, sections slot)
 *
 * Fork instructions:
 *   1. Replace fetch with your API client / tRPC mutation.
 *   2. Read the DSR-Schema-Version response header and surface it on the download.
 *   3. For CSV, the backend emits one file per category as a zip — wire the
 *      blob download to the returned handle.
 */
export default function PortabilityPage() {
  const [format, setFormat] = React.useState<ExportFormat>('json')
  const [error, setError] = React.useState<string | null>(null)
  const [bundle, setBundle] = React.useState<PortableBundle | null>(null)

  const mutation = useMutation({
    mutationFn: () => requestPortability(format),
    onSuccess: (result) => setBundle(result),
    onError: (err: Error) => setError(err.message || 'Failed to produce a portable copy.'),
  })

  const sectionsSlot = (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">
        Receive the data you provided to us in a structured, machine-readable
        format you can transmit to another service.
      </p>

      <fieldset className="space-y-2">
        <legend className="text-sm font-medium">Export format</legend>
        <div className="flex gap-4">
          {(['json', 'csv'] as const).map((f) => (
            <label key={f} className="inline-flex items-center gap-2 text-sm">
              <input
                type="radio"
                name="format"
                value={f}
                checked={format === f}
                onChange={() => setFormat(f)}
              />
              {f.toUpperCase()}
            </label>
          ))}
        </div>
      </fieldset>

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
          {mutation.isPending ? 'Preparing export…' : `Export as ${format.toUpperCase()}`}
        </button>
      ) : (
        <div className="rounded-lg border bg-card px-6 py-4 text-sm">
          <p className="font-medium">Your portable copy is ready.</p>
          <p className="mt-1 text-muted-foreground">
            Tracking id: <span className="font-mono">{bundle.request.requestId}</span>
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
      title="Export my data"
      backHref="/privacy"
      backLabel="Back to my requests"
      sectionsSlot={sectionsSlot}
    />
  )
}
