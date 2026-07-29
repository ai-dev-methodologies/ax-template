/*
---
template_id: L4/approval-workflow/app/(approvals)/[id]/page
layer: L4
domain: approval-workflow
domain_mode: full_trio
backend_operation_id: getApprovalRequest
evidence:
  - source_type: internal
    rationale: "L4 approval-workflow vertical — request detail with sequential timeline (Korean enterprise 결재선 pattern). Approve / reject / cancel actions gated by role and step state."
  - source_type: external
    citation: "TanStack Query v5 — useQuery + useMutation"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useQuery"
  - source_type: external
    citation: "Korean enterprise 결재선 (sequential approval chain) — pattern reference"
    url: "https://en.wikipedia.org/wiki/Approval_voting"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
---
*/
'use client'

import * as React from 'react'
import { useParams, useRouter } from 'next/navigation'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useCallerId } from 'templates/L0/fork-receiver-kit/use-caller-id'
import { parseError } from 'templates/L0/fork-receiver-kit/parse-error'
import ApprovalDetailView, { type ApprovalRequest } from './approval-detail-view'

// ─── data ─────────────────────────────────────────────────────────────────────

async function fetchRequest(id: string): Promise<ApprovalRequest> {
  const res = await fetch(`/api/approvals/${id}`)
  if (!res.ok) throw await parseError(res, 'Failed to load request')
  return res.json()
}

async function submitDraft(id: string): Promise<ApprovalRequest> {
  const res = await fetch(`/api/approvals/${id}/submit`, { method: 'POST' })
  if (!res.ok) throw await parseError(res, 'Failed to submit')
  return res.json()
}

async function cancelRequest(id: string): Promise<ApprovalRequest> {
  const res = await fetch(`/api/approvals/${id}/cancel`, { method: 'POST' })
  if (!res.ok) throw await parseError(res, 'Failed to cancel')
  return res.json()
}

async function approveStep(
  id: string,
  stepId: string,
  comment: string,
): Promise<ApprovalRequest> {
  const res = await fetch(`/api/approvals/${id}/steps/${stepId}/approve`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ comment }),
  })
  if (!res.ok) throw await parseError(res, 'Failed to approve')
  return res.json()
}

async function rejectStep(
  id: string,
  stepId: string,
  comment: string,
): Promise<ApprovalRequest> {
  const res = await fetch(`/api/approvals/${id}/steps/${stepId}/reject`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ comment }),
  })
  if (!res.ok) throw await parseError(res, 'Failed to reject')
  return res.json()
}

// ─── page ────────────────────────────────────────────────────────────────────

/**
 * ApprovalDetailPage — request detail + state-machine timeline + actions.
 *
 * Audience matrix:
 *   - Requester (DRAFT)         → Submit + Cancel buttons
 *   - Requester (SUBMITTED+)    → Cancel button only
 *   - Approver at current step  → Approve + Reject + comment box
 *   - Approver at later step    → Read-only (see "waiting on step N")
 *   - Approver at past step     → Read-only with their own decision shown
 *
 * Sequential ordering UI enforcement (mirrors the backend state machine):
 *   - describeChain(req, callerId) returns { kind: 'actionable', step } only
 *     when the next non-APPROVED step is PENDING and its approver matches
 *     the caller (sameUser-normalized). The Approve / Reject panel renders
 *     only when chain.kind === 'actionable'.
 *
 * Self-approval guard (R31 iter1+2 dogfood VIOLATION proof):
 *   - The timeline tags any step whose approver === requester as
 *     "cannot self-approve" so a user can spot a misconfigured chain
 *     before they even try to act. The Spring service rejects the
 *     action server-side regardless.
 */
export default function ApprovalDetailPage() {
  const params = useParams<{ id: string }>()
  const requestId = params.id
  const router = useRouter()
  const qc = useQueryClient()

  const callerId = useCallerId()

  const { data, error, isLoading } = useQuery({
    queryKey: ['approval-request', requestId],
    queryFn: () => fetchRequest(requestId),
  })

  const invalidate = () =>
    qc.invalidateQueries({ queryKey: ['approval-request', requestId] })

  // R43 iter4 (P2-iter3-N1): inbox refetch is best-effort. If a
  // network blip causes it to reject, we must NOT propagate that
  // failure into the action mutation's success pipeline — the action
  // already committed server-side, and surfacing a stale-chain error
  // would invite the approver to retry a no-longer-pending step.
  //
  // R43 iter5 (P2-iter4-N2): reconvergence happens through TanStack's
  // default `refetchOnMount: true` when the approver lands on
  // /approvals/inbox via the per-call router.push below. The inbox's
  // setInterval only ticks the wall clock used for age badges; it does
  // NOT refetch the query. A fork-receiver who removes the router.push
  // MUST add an explicit refetchInterval or invalidateQueries to keep
  // the action → inbox round-trip in sync.
  const refetchInbox = async () => {
    try {
      await qc.refetchQueries({ queryKey: ['approval-inbox'] })
    } catch {
      /* best-effort */
    }
  }

  const submit = useMutation({ mutationFn: () => submitDraft(requestId), onSuccess: invalidate })
  const cancel = useMutation({ mutationFn: () => cancelRequest(requestId), onSuccess: invalidate })
  const approve = useMutation({
    mutationFn: ({ stepId, comment }: { stepId: string; comment: string }) =>
      approveStep(requestId, stepId, comment),
  })
  const reject = useMutation({
    mutationFn: ({ stepId, comment }: { stepId: string; comment: string }) =>
      rejectStep(requestId, stepId, comment),
  })

  // R43 iter1 (P2-F7): jump back to inbox after a successful approve/reject so the
  // approver moves on to the next pending item instead of staring at a now-empty action
  // panel. R43 iter4 (P2-iter3-N1): the inbox refetch stays best-effort — a network blip
  // here must not surface as an error on the action the approver already committed.
  const handleApprove = async (stepId: string, comment: string) => {
    await approve.mutateAsync({ stepId, comment })
    invalidate()
    await refetchInbox()
    router.push('/approvals/inbox')
  }
  const handleReject = async (stepId: string, comment: string) => {
    await reject.mutateAsync({ stepId, comment })
    invalidate()
    await refetchInbox()
    router.push('/approvals/inbox')
  }

  return (
    <ApprovalDetailView
      request={data}
      isLoading={isLoading}
      error={error as Error | null}
      callerId={callerId}
      onBackToInbox={() => router.push('/approvals/inbox')}
      onCreateAnotherDraft={() => router.push('/approvals/new')}
      onSubmit={() => submit.mutate()}
      submitPending={submit.isPending}
      submitErrorMessage={submit.error?.message ?? null}
      onCancel={() => cancel.mutate()}
      cancelPending={cancel.isPending}
      cancelErrorMessage={cancel.error?.message ?? null}
      onApprove={handleApprove}
      approvePending={approve.isPending}
      approveErrorMessage={approve.error?.message ?? null}
      onReject={handleReject}
      rejectPending={reject.isPending}
      rejectErrorMessage={reject.error?.message ?? null}
    />
  )
}
