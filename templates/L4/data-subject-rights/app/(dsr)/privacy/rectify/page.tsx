/*
---
template_id: L4/data-subject-rights/app/(dsr)/privacy/rectify/page
layer: L4
domain: data-subject-rights
domain_mode: full_trio
backend_operation_id: dsrRectify
evidence:
  - source_type: internal
    rationale: "L4 data-subject-rights vertical — RECTIFY page composing L3 edit-page + L2 CrudEditForm; field-level correction form (dsrRectify) wiring parse-field-errors → fieldErrors."
  - source_type: external
    citation: "TanStack Query v5 — useMutation for PATCH requests"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useMutation"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [other L4 domains]
---
*/
'use client'

import * as React from 'react'
import { useMutation } from '@tanstack/react-query'
import EditPage from 'templates/L3/pages/edit-page/[id]/page'
import CrudEditForm, { type FieldDef } from 'templates/L2/blocks/crud-edit-form'
import { parseError } from 'templates/L0/fork-receiver-kit/parse-error'
import { parseFieldErrors } from 'templates/L0/fork-receiver-kit/parse-field-errors'

// ─── fields ─────────────────────────────────────────────────────────────────

// Mirrors the backend RectifyRequest body {fieldPath, currentValue,
// correctedValue, justification}. Fork: constrain `fieldPath` to your recipe's
// rectifiable_fields allowlist (e.g. a select) so non-editable fields are not
// offered — the server still rejects them with 422 DSR_FIELD_NOT_RECTIFIABLE.
const RECTIFY_FIELDS: FieldDef[] = [
  {
    key: 'fieldPath',
    label: 'Field to correct',
    type: 'text',
    placeholder: 'e.g. profile.displayName',
    required: true,
  },
  {
    key: 'currentValue',
    label: 'Current (incorrect) value',
    type: 'text',
    placeholder: 'The value on record now',
  },
  {
    key: 'correctedValue',
    label: 'Corrected value',
    type: 'text',
    placeholder: 'What it should be',
    required: true,
  },
  {
    key: 'justification',
    label: 'Why is this inaccurate?',
    type: 'textarea',
    placeholder: 'Explain the correction…',
    required: true,
  },
]

// ─── types ──────────────────────────────────────────────────────────────────

interface RectifyRequest {
  fieldPath: string
  currentValue?: string
  correctedValue: string
  justification: string
}

interface DsrRequestEnvelope {
  requestId: string
  status: string
}

/** Error thrown on a failed rectify — carries the per-field validation map. */
type RectifyError = Error & { fieldErrors?: Record<string, string> }

// ─── fetcher ────────────────────────────────────────────────────────────────

async function rectify(body: RectifyRequest): Promise<DsrRequestEnvelope> {
  const res = await fetch('/api/me/dsr/rectify', {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) {
    // 422 DSR_FIELD_NOT_RECTIFIABLE / 409 DSR_RECTIFY_STALE carry field-level
    // detail; parse both the top-level message and the per-field map.
    const fieldErrors = await parseFieldErrors(res.clone())
    const error: RectifyError = await parseError(res, 'Failed to submit correction.')
    error.fieldErrors = fieldErrors
    throw error
  }
  return res.json() as Promise<DsrRequestEnvelope>
}

// ─── component ──────────────────────────────────────────────────────────────

/**
 * RectifyPage — L4 data-subject-rights rectification (GDPR Art 16) page.
 *
 * Composes:
 *   L3 edit-page    → page chrome (title, cancel link, form card)
 *   L2 CrudEditForm → schema-driven correction form
 *
 * Fork instructions:
 *   1. Constrain fieldPath to your rectifiable_fields allowlist.
 *   2. Replace fetch with your API client / tRPC mutation.
 *   3. On success, route to /privacy or the request detail page.
 */
export default function RectifyPage() {
  const [error, setError] = React.useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = React.useState<Record<string, string>>({})

  const mutation = useMutation({
    mutationFn: rectify,
    onSuccess: () => {
      window.location.href = '/privacy'
    },
    onError: (err: RectifyError) => {
      setError(err.message || 'Failed to submit correction.')
      setFieldErrors(err.fieldErrors ?? {})
    },
  })

  function handleSubmit(data: Record<string, unknown>) {
    setError(null)
    setFieldErrors({})
    mutation.mutate({
      fieldPath: String(data.fieldPath ?? ''),
      currentValue: data.currentValue ? String(data.currentValue) : undefined,
      correctedValue: String(data.correctedValue ?? ''),
      justification: String(data.justification ?? ''),
    })
  }

  return (
    <EditPage
      title="Correct my data"
      cancelHref="/privacy"
      formSlot={
        <>
          {error && (
            <div role="alert" className="mb-4 rounded-md bg-destructive/10 px-4 py-3 text-sm text-destructive">
              {error}
            </div>
          )}
          <CrudEditForm
            fields={RECTIFY_FIELDS}
            initialValues={{}}
            onSubmit={handleSubmit}
            isLoading={mutation.isPending}
            submitLabel="Submit correction"
            fieldErrors={fieldErrors}
          />
        </>
      }
    />
  )
}
