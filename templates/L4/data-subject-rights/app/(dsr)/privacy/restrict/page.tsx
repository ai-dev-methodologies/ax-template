/*
---
template_id: L4/data-subject-rights/app/(dsr)/privacy/restrict/page
layer: L4
domain: data-subject-rights
domain_mode: full_trio
backend_operation_id: dsrRestrict
evidence:
  - source_type: internal
    rationale: "L4 data-subject-rights vertical — RESTRICT page composing L3 detail-page + L2 ConfirmDialog (consequential); freezes processing via dsrRestrict behind a confirm-dialog and offers a lift action (dsrLiftRestriction) requiring a justification."
  - source_type: external
    citation: "WAI-ARIA Authoring Practices — Alert Dialog Pattern"
    url: "https://www.w3.org/WAI/ARIA/apg/patterns/alertdialog/"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [other L4 domains]
---
*/
'use client'

import * as React from 'react'
import { useMutation } from '@tanstack/react-query'
import DetailPage from 'templates/L3/pages/detail-page/[id]/page'
import ConfirmDialog from 'templates/L2/blocks/confirm-dialog'
import { parseError } from 'templates/L0/fork-receiver-kit/parse-error'

// ─── types ──────────────────────────────────────────────────────────────────

interface DsrRequestEnvelope {
  requestId: string
  type: string
  status: string
  dueAt: string
}

// ─── fetchers ───────────────────────────────────────────────────────────────

async function requestRestrict(): Promise<DsrRequestEnvelope> {
  const res = await fetch('/api/me/dsr/restrict', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  })
  if (!res.ok) {
    throw await parseError(res, 'Failed to restrict processing.')
  }
  return res.json() as Promise<DsrRequestEnvelope>
}

async function liftRestriction(justification: string): Promise<DsrRequestEnvelope> {
  const res = await fetch('/api/me/dsr/restrict/lift', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ justification }),
  })
  if (!res.ok) {
    throw await parseError(res, 'Failed to lift the restriction.')
  }
  return res.json() as Promise<DsrRequestEnvelope>
}

// ─── component ──────────────────────────────────────────────────────────────

/**
 * RestrictPage — L4 data-subject-rights restriction (GDPR Art 18) page.
 *
 * Composes:
 *   L3 detail-page  → page chrome (title, back link, sections slot)
 *   L2 ConfirmDialog → consequential confirmation before freezing processing
 *
 * Restriction freezes processing WITHOUT deletion (data is retained); it is a
 * consequential state change, so it is guarded behind a confirm-dialog. The
 * lift action requires a justification (dsrLiftRestriction).
 *
 * Fork instructions:
 *   1. Replace fetch with your API client / tRPC mutations.
 *   2. Surface the current restriction status from the request envelope.
 *   3. Gate the lift action so only the subject (or a lawful ground) can lift.
 */
export default function RestrictPage() {
  const [confirmOpen, setConfirmOpen] = React.useState(false)
  const [liftJustification, setLiftJustification] = React.useState('')
  const [error, setError] = React.useState<string | null>(null)
  const [restricted, setRestricted] = React.useState<DsrRequestEnvelope | null>(null)

  const restrictMutation = useMutation({
    mutationFn: requestRestrict,
    onSuccess: (result) => {
      setConfirmOpen(false)
      setRestricted(result)
    },
    onError: (err: Error) => {
      setConfirmOpen(false)
      setError(err.message || 'Failed to restrict processing.')
    },
  })

  const liftMutation = useMutation({
    mutationFn: () => liftRestriction(liftJustification),
    onSuccess: () => {
      setRestricted(null)
      setLiftJustification('')
    },
    onError: (err: Error) => setError(err.message || 'Failed to lift the restriction.'),
  })

  const sectionsSlot = (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">
        Freeze processing of your personal data without deleting it. While
        restricted, we store your data but stop all non-essential processing
        until you lift the restriction.
      </p>
      {error && (
        <div role="alert" className="rounded-md bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {error}
        </div>
      )}

      {!restricted ? (
        <button
          type="button"
          onClick={() => {
            setError(null)
            setConfirmOpen(true)
          }}
          className="inline-flex h-10 items-center rounded-md bg-primary px-5 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          Restrict processing
        </button>
      ) : (
        <div className="space-y-3 rounded-lg border bg-card px-6 py-4 text-sm">
          <p className="font-medium">Processing is restricted.</p>
          <p className="text-muted-foreground">
            Tracking id: <span className="font-mono">{restricted.requestId}</span>
          </p>
          <div className="space-y-2">
            <label htmlFor="lift-justification" className="block text-sm font-medium">
              Justification to lift
            </label>
            <textarea
              id="lift-justification"
              value={liftJustification}
              onChange={(e) => setLiftJustification(e.target.value)}
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
              rows={3}
              placeholder="Why are you lifting the restriction?"
            />
            <button
              type="button"
              onClick={() => {
                setError(null)
                liftMutation.mutate()
              }}
              disabled={liftMutation.isPending || !liftJustification.trim()}
              className="inline-flex h-9 items-center rounded-md border border-input bg-background px-4 text-sm font-medium hover:bg-accent disabled:opacity-60"
            >
              {liftMutation.isPending ? 'Lifting…' : 'Lift restriction'}
            </button>
          </div>
        </div>
      )}

      <ConfirmDialog
        open={confirmOpen}
        title="Restrict processing of your data?"
        description="We will keep your data but stop all non-essential processing until you lift this restriction."
        confirmLabel="Restrict processing"
        cancelLabel="Cancel"
        isLoading={restrictMutation.isPending}
        onConfirm={() => restrictMutation.mutate()}
        onCancel={() => setConfirmOpen(false)}
      />
    </div>
  )

  return (
    <DetailPage
      title="Restrict processing"
      backHref="/privacy"
      backLabel="Back to my requests"
      sectionsSlot={sectionsSlot}
    />
  )
}
