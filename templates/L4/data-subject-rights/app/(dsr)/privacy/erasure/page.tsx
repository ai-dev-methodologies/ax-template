/*
---
template_id: L4/data-subject-rights/app/(dsr)/privacy/erasure/page
layer: L4
domain: data-subject-rights
domain_mode: full_trio
backend_operation_id: dsrErasure
evidence:
  - source_type: internal
    rationale: "L4 data-subject-rights vertical — ERASURE page composing L3 detail-page + L2 ConfirmDialog (destructive); guards the right-to-be-forgotten request behind a confirm-dialog before calling dsrErasure, then renders the (partial or full) erasure manifest."
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

interface RetainedCategory {
  category: string
  legalBasis: string
}

interface ErasureManifest {
  requestId: string
  erasedAt: string
  legalBasis: string
  fullyErased: boolean
  retained: RetainedCategory[]
}

// ─── fetcher ────────────────────────────────────────────────────────────────

async function requestErasure(): Promise<ErasureManifest> {
  const res = await fetch('/api/me/dsr/erasure', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  })
  if (!res.ok) {
    throw await parseError(res, 'Failed to request erasure.')
  }
  return res.json() as Promise<ErasureManifest>
}

// ─── component ──────────────────────────────────────────────────────────────

/**
 * ErasurePage — L4 data-subject-rights erasure (GDPR Art 17) page.
 *
 * Composes:
 *   L3 detail-page  → page chrome (title, back link, sections slot)
 *   L2 ConfirmDialog → destructive confirmation before the irreversible request
 *
 * Erasure is consequential and irreversible, so the request is guarded behind a
 * destructive confirm-dialog. The result may be a PARTIAL erasure manifest when
 * a legal-hold retains some categories (Art 17(3)).
 *
 * Fork instructions:
 *   1. Replace fetch with your API client / tRPC mutation.
 *   2. On full erasure, log the subject out and redirect to a confirmation page.
 *   3. Surface the retained-categories manifest clearly when erasure is partial.
 */
export default function ErasurePage() {
  const [confirmOpen, setConfirmOpen] = React.useState(false)
  const [error, setError] = React.useState<string | null>(null)
  const [manifest, setManifest] = React.useState<ErasureManifest | null>(null)

  const mutation = useMutation({
    mutationFn: requestErasure,
    onSuccess: (result) => {
      setConfirmOpen(false)
      setManifest(result)
    },
    onError: (err: Error) => {
      setConfirmOpen(false)
      setError(err.message || 'Failed to request erasure.')
    },
  })

  const sectionsSlot = (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">
        Request permanent erasure of your personal data. This is irreversible.
        Some records may be retained where a legal obligation applies; you will
        see exactly what is kept and why.
      </p>
      {error && (
        <div role="alert" className="rounded-md bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {error}
        </div>
      )}
      {!manifest ? (
        <button
          type="button"
          onClick={() => {
            setError(null)
            setConfirmOpen(true)
          }}
          className="inline-flex h-10 items-center rounded-md bg-destructive px-5 text-sm font-medium text-destructive-foreground hover:bg-destructive/90"
        >
          Erase my data
        </button>
      ) : (
        <div className="rounded-lg border bg-card px-6 py-4 text-sm">
          <p className="font-medium">
            {manifest.fullyErased ? 'Your data has been erased.' : 'Partial erasure complete.'}
          </p>
          <p className="mt-1 text-muted-foreground">
            Erased {new Date(manifest.erasedAt).toLocaleString()} · basis: {manifest.legalBasis}
          </p>
          {manifest.retained.length > 0 && (
            <div className="mt-3">
              <p className="font-medium">Retained categories:</p>
              <ul className="mt-1 list-disc pl-5 text-muted-foreground">
                {manifest.retained.map((r) => (
                  <li key={r.category}>
                    {r.category} — {r.legalBasis}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}

      <ConfirmDialog
        open={confirmOpen}
        title="Permanently erase your data?"
        description="This action cannot be undone. Records held under a legal obligation may be retained; everything else will be permanently deleted."
        confirmLabel="Erase my data"
        cancelLabel="Cancel"
        destructive
        isLoading={mutation.isPending}
        onConfirm={() => mutation.mutate()}
        onCancel={() => setConfirmOpen(false)}
      />
    </div>
  )

  return (
    <DetailPage
      title="Erase my data"
      backHref="/privacy"
      backLabel="Back to my requests"
      sectionsSlot={sectionsSlot}
    />
  )
}
