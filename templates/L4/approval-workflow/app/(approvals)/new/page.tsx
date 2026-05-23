/*
---
template_id: L4/approval-workflow/app/(approvals)/new/page
layer: L4
domain: approval-workflow
domain_mode: full_trio
backend_operation_id: createApprovalRequest
evidence:
  - source_type: internal
    rationale: "L4 approval-workflow vertical — new request draft form. Type / title / payload + ordered approver chain entry. Surfaces duplicate-approver + self-approve guards client-side (defense-in-depth above server)."
  - source_type: external
    citation: "TanStack Query v5 — useMutation for POST flows"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useMutation"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
---
*/
'use client'

import * as React from 'react'
import { useRouter } from 'next/navigation'
import { useMutation } from '@tanstack/react-query'
import ErrorBoundary from 'templates/L2/blocks/error-boundary'
import { useCallerId, sameUser } from '../../use-caller-id'
import { parseError } from '../../parse-error'

// ─── types ───────────────────────────────────────────────────────────────────

interface CreateApprovalRequest {
  type: string
  title: string
  payload: Record<string, unknown>
  approverUserIds: string[]
}

interface CreateApprovalResponse {
  id: string
}

// ─── data ─────────────────────────────────────────────────────────────────────

async function createRequest(body: CreateApprovalRequest): Promise<CreateApprovalResponse> {
  const res = await fetch('/api/approvals', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw await parseError(res, 'Failed to create request')
  return res.json()
}

// ─── page ────────────────────────────────────────────────────────────────────

/**
 * NewApprovalRequestPage — draft a new request with an ordered approver chain.
 *
 * The form starts a DRAFT; the user must visit the detail page and press
 * "Submit for approval" to start the chain. This two-step pattern (draft
 * then submit) is intentional — it lets the user review the chain composition
 * before the first approver is notified.
 *
 * UI defense-in-depth for the iter1+2 dogfood rules (the server is still
 * the source of truth, but flagging early reduces cycle time):
 *   - Self-approve guard: if any approver in the chain equals the
 *     stubbed caller id, the row gets a red warning AND submit is blocked.
 *   - Duplicate-approver guard: same approver listed twice triggers
 *     a red warning AND submit is blocked.
 *
 * Fork instructions:
 *   1. Replace the stub `callerId` with your real session hook.
 *   2. Replace the freeform `payload` text area with a typed form for
 *      your actual request types (discount approval, leave request, etc.).
 *   3. Replace the freeform approver text inputs with an autocomplete
 *      against your user directory — typos here become "approver chain
 *      contains a user that does not exist" errors when the request is
 *      submitted.
 */
export default function NewApprovalRequestPage() {
  const router = useRouter()
  const callerId = useCallerId()

  const [type, setType] = React.useState('discount-approval')
  const [title, setTitle] = React.useState('')
  const [payloadJson, setPayloadJson] = React.useState('{\n  "amount": 0,\n  "currency": "KRW"\n}')
  const [approverInputs, setApproverInputs] = React.useState<string[]>([''])

  const create = useMutation({
    mutationFn: createRequest,
    onSuccess: (resp) => router.push(`/approvals/${resp.id}`),
  })

  // ─── client-side validation (defense-in-depth) ──────────────────────────────

  const approvers = approverInputs.map((s) => s.trim()).filter((s) => s.length > 0)
  const selfApprovalAt = approvers.findIndex((a) => sameUser(a, callerId))
  const seen = new Set<string>()
  let duplicateAt = -1
  for (let i = 0; i < approvers.length; i++) {
    if (seen.has(approvers[i])) {
      duplicateAt = i
      break
    }
    seen.add(approvers[i])
  }

  // R43 iter3 (P1-iter2-N5): debounce JSON validation so the user does
  // not see a constant "Invalid JSON" red line while typing a multi-line
  // payload. Re-validate 400ms after the last keystroke, or on blur.
  const [debouncedPayload, setDebouncedPayload] = React.useState(payloadJson)
  React.useEffect(() => {
    const handle = setTimeout(() => setDebouncedPayload(payloadJson), 400)
    return () => clearTimeout(handle)
  }, [payloadJson])

  let payloadParseError: string | null = null
  try {
    JSON.parse(debouncedPayload || '{}')
  } catch (e) {
    payloadParseError = (e as Error).message
  }

  // R43 iter4 (P1-iter3-N1): block submit while the debounce window is
  // still open. Without this, a fast typist who fixes a syntax error
  // and clicks Save inside the 400ms gap submits the unparsed raw
  // payload — the validator says "OK" because it's looking at stale
  // debouncedPayload, and the onSubmit JSON.parse(payloadJson) throws.
  const validationPending = payloadJson !== debouncedPayload

  // R43 iter3 (P1-iter2-N2): "dirty" flag for the unsaved-form-state
  // confirm. We compare against initial defaults rather than tracking
  // every keystroke so the cancel confirm only fires when the user
  // actually has typed work to lose.
  const isDirty =
    title.trim().length > 0 ||
    approvers.length > 0 ||
    payloadJson.trim() !== '{\n  "amount": 0,\n  "currency": "KRW"\n}'

  const confirmCancel = React.useCallback(() => {
    if (
      isDirty &&
      !window.confirm(
        'Discard this draft? Anything you typed (title, payload, approvers) will be lost.',
      )
    ) {
      return
    }
    router.push('/approvals/my')
  }, [isDirty, router])

  const titleMissing = title.trim().length === 0
  const noApprovers = approvers.length === 0
  const submitBlocked =
    titleMissing ||
    noApprovers ||
    selfApprovalAt !== -1 ||
    duplicateAt !== -1 ||
    payloadParseError !== null ||
    validationPending

  // ─── render ─────────────────────────────────────────────────────────────────

  const setApprover = (idx: number, val: string) => {
    setApproverInputs((prev) => prev.map((a, i) => (i === idx ? val : a)))
  }
  const removeApprover = (idx: number) => {
    setApproverInputs((prev) => prev.filter((_, i) => i !== idx))
  }
  const addApprover = () => setApproverInputs((prev) => [...prev, ''])

  return (
    <ErrorBoundary>
      <div className="max-w-2xl space-y-4">
        <header>
          <h1 className="text-lg font-semibold">New approval request</h1>
          <p className="text-sm text-muted-foreground">
            This creates a DRAFT. Review the payload and approver chain, then
            press <strong>Submit for approval</strong> on the next screen to
            start the chain. Once submitted, the payload becomes immutable —
            edits require canceling and recreating.
          </p>
        </header>

        {/* R43 iter1 (P1-F3): explicit warning that the raw-JSON
             payload is a template placeholder. Fork-receivers should
             replace it with a typed form per request type before
             going live; this banner makes that obligation hard to miss. */}
        <div className="rounded border border-amber-300 bg-amber-50 px-3 py-2 text-xs text-amber-900">
          <strong>Template note (R43):</strong> the payload field below
          accepts raw JSON for demonstration. Real deployments MUST
          replace this with a typed form per request <code>type</code>{' '}
          (e.g. discount-approval → amount + currency + customer fields)
          so non-technical requesters cannot mistype a numeric value
          (extra zero on a 100M KRW field).
        </div>

        <form
          className="space-y-4 rounded border p-4"
          onSubmit={(e) => {
            e.preventDefault()
            if (submitBlocked) return
            create.mutate({
              type,
              title: title.trim(),
              // R43 iter5 (P1-iter4-N2): parse from debouncedPayload (the
              // string the validator approved) so the validate→submit pair
              // is locked to the same input by construction. submitBlocked
              // gates `validationPending` and `payloadParseError`, both
              // computed from debouncedPayload — using it here keeps the
              // two sides of the gate consistent.
              payload: JSON.parse(debouncedPayload || '{}'),
              approverUserIds: approvers,
            })
          }}
        >
          <label className="block space-y-1">
            <span className="text-sm font-medium">Type</span>
            <input
              type="text"
              className="w-full rounded border px-2 py-1 text-sm"
              value={type}
              onChange={(e) => setType(e.target.value)}
            />
          </label>

          <label className="block space-y-1">
            <span className="text-sm font-medium">Title</span>
            <input
              type="text"
              className="w-full rounded border px-2 py-1 text-sm"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Short summary the approvers see first"
            />
            {titleMissing && (
              <span className="text-xs text-red-600">Title is required.</span>
            )}
          </label>

          <label className="block space-y-1">
            <span className="text-sm font-medium">Payload (JSON)</span>
            <textarea
              className="w-full rounded border px-2 py-1 font-mono text-xs"
              rows={6}
              value={payloadJson}
              onChange={(e) => setPayloadJson(e.target.value)}
            />
            <span className="text-xs text-muted-foreground">
              Once you submit, the payload is locked — approvers always see
              the exact document you filed (no silent edits after the fact).
            </span>
            {payloadParseError && (
              <span className="block text-xs text-red-600">
                Invalid JSON: {payloadParseError}
              </span>
            )}
          </label>

          <fieldset className="space-y-2">
            <legend className="text-sm font-medium">Approver chain (in order)</legend>
            <p className="text-xs text-muted-foreground">
              Each approver acts sequentially — step 2 cannot act until step 1
              approves. The same user cannot appear twice, and you cannot list
              yourself.
            </p>
            <ul className="space-y-1">
              {approverInputs.map((val, idx) => {
                // R43 iter4 (P1-iter3-N3 / P2-iter3-N4): use the shared
                // sameUser() normalization so a fork-receiver's session
                // hook with whitespace / case quirks does not silently
                // skip the per-row red border while still blocking submit.
                const isSelf = sameUser(val, callerId) && val.trim().length > 0
                const isDup = idx === duplicateAt
                return (
                  <li key={idx} className="flex items-center gap-2">
                    <span className="w-6 text-xs text-muted-foreground">
                      {idx + 1}.
                    </span>
                    <input
                      type="text"
                      className={`flex-1 rounded border px-2 py-1 text-sm ${
                        isSelf || isDup ? 'border-red-500' : ''
                      }`}
                      value={val}
                      onChange={(e) => setApprover(idx, e.target.value)}
                      placeholder="approver user id"
                    />
                    <button
                      type="button"
                      className="rounded border px-2 py-1 text-xs hover:bg-muted disabled:opacity-50"
                      disabled={approverInputs.length === 1}
                      aria-label={
                        approverInputs.length === 1
                          ? 'At least one approver is required — cannot remove the last row'
                          : `Remove approver step ${idx + 1}`
                      }
                      title={
                        approverInputs.length === 1
                          ? 'At least one approver is required.'
                          : undefined
                      }
                      onClick={() => removeApprover(idx)}
                    >
                      Remove
                    </button>
                  </li>
                )
              })}
            </ul>
            <button
              type="button"
              className="rounded border px-2 py-1 text-xs hover:bg-muted"
              onClick={addApprover}
            >
              + Add approver
            </button>
            {selfApprovalAt !== -1 && (
              <p className="text-xs text-red-600">
                Step {selfApprovalAt + 1} is you — a requester cannot approve their
                own request (server-enforced; surfaced here so you fix it before
                submit).
              </p>
            )}
            {duplicateAt !== -1 && (
              <p className="text-xs text-red-600">
                Step {duplicateAt + 1} is a duplicate of an earlier step.
              </p>
            )}
            {noApprovers && !titleMissing && (
              <p className="text-xs text-red-600">
                At least one approver is required.
              </p>
            )}
          </fieldset>

          {create.error && (
            <div className="rounded border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-900">
              {(create.error as Error).message}
            </div>
          )}

          <div className="flex gap-2">
            <button
              type="submit"
              className="rounded bg-foreground px-3 py-1.5 text-sm text-background hover:opacity-90 disabled:opacity-50"
              disabled={submitBlocked || create.isPending}
            >
              Save draft
            </button>
            <button
              type="button"
              className="rounded border px-3 py-1.5 text-sm hover:bg-muted"
              onClick={confirmCancel}
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </ErrorBoundary>
  )
}
