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
import EmptyState from 'templates/L2/blocks/empty-state'
import ErrorBoundary from 'templates/L2/blocks/error-boundary'
import { useCallerId, sameUser } from 'templates/L0/fork-receiver-kit/use-caller-id'
import { parseError } from 'templates/L0/fork-receiver-kit/parse-error'

// ─── types ───────────────────────────────────────────────────────────────────

type RequestStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
type StepStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

interface ApprovalStep {
  id: string
  orderIndex: number
  approverUserId: string
  status: StepStatus
  actedByUserId: string | null
  actedAt: string | null
  comment: string | null
}

interface ApprovalRequest {
  id: string
  requesterUserId: string
  type: string
  title: string
  status: RequestStatus
  payload: Record<string, unknown>
  steps: ApprovalStep[]
  createdAt: string
  submittedAt: string | null
  completedAt: string | null
}

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

// ─── helpers ──────────────────────────────────────────────────────────────────

interface ChainState {
  // R43 iter1 (P2-F5, P2-F14): explicit description of *why* the action
  // area is in the state it is. The detail page consumes this to render
  // either an action panel, a "waiting on someone" panel, or a
  // "chain halted" panel — instead of going silent.
  kind: 'actionable' | 'waiting' | 'completed' | 'halted' | 'draft' | 'cancelled'
  step?: ApprovalStep        // actionable: the caller's step
  waitingOn?: ApprovalStep   // waiting: the step we're blocked on
  haltedAt?: ApprovalStep    // halted: the step that rejected
  totalSteps: number
  currentIndex: number       // 0-based index of the next pending step
}

function describeChain(req: ApprovalRequest, callerId: string): ChainState {
  const sorted = [...req.steps].sort((a, b) => a.orderIndex - b.orderIndex)
  const totalSteps = sorted.length

  if (req.status === 'DRAFT')      return { kind: 'draft', totalSteps, currentIndex: 0 }
  if (req.status === 'CANCELLED')  return { kind: 'cancelled', totalSteps, currentIndex: totalSteps }
  if (req.status === 'APPROVED')   return { kind: 'completed', totalSteps, currentIndex: totalSteps }

  // Find the first non-APPROVED step in order.
  for (let i = 0; i < sorted.length; i++) {
    const step = sorted[i]
    if (step.status === 'REJECTED') {
      return { kind: 'halted', haltedAt: step, totalSteps, currentIndex: i }
    }
    if (step.status === 'PENDING') {
      // R43 iter3 (P2-iter2-N8): normalize caller-id comparison so a
      // trailing space or case mismatch on the server side does not
      // silently hide the action panel from the rightful approver.
      if (sameUser(step.approverUserId, callerId)) {
        return { kind: 'actionable', step, totalSteps, currentIndex: i }
      }
      return { kind: 'waiting', waitingOn: step, totalSteps, currentIndex: i }
    }
  }
  // All steps APPROVED but status not APPROVED — race state; treat as completed for UI.
  return { kind: 'completed', totalSteps, currentIndex: totalSteps }
}

function findUpstreamApprovedComments(
  req: ApprovalRequest,
  currentIndex: number,
): ApprovalStep[] {
  // R43 iter4 (P2-iter3-N2): return EVERY prior APPROVED step that
  // carries a non-empty comment, not just the immediately-previous one.
  // A long chain can carry a conditional approval two or three steps
  // back ("OK 단 amount 95M 로 negotiated") that a mid-chain approver
  // must see; the iter3 implementation silently dropped it when the
  // immediately-previous approver approved without comment.
  const sorted = [...req.steps].sort((a, b) => a.orderIndex - b.orderIndex)
  const out: ApprovalStep[] = []
  for (let i = 0; i < currentIndex; i++) {
    const s = sorted[i]
    if (s.status === 'APPROVED' && s.comment && s.comment.trim().length > 0) {
      out.push(s)
    }
  }
  return out
}

function findDuplicateApprovers(steps: ApprovalStep[]): Set<string> {
  // R43 iter1 (P2-F9): scan the chain for the same approver appearing
  // twice. Returns the set of user ids that occur more than once.
  const counts = new Map<string, number>()
  for (const s of steps) {
    counts.set(s.approverUserId, (counts.get(s.approverUserId) ?? 0) + 1)
  }
  const dups = new Set<string>()
  for (const [uid, n] of counts) {
    if (n > 1) dups.add(uid)
  }
  return dups
}

// ─── timeline ────────────────────────────────────────────────────────────────

interface TimelineProps {
  steps: ApprovalStep[]
  callerId: string
  requesterUserId: string
  duplicates: Set<string>
  // R43 iter7 (P2-F-CONVERGE-1): when the chain is halted, no step is
  // "current" — passing chainKind in lets the timeline suppress the
  // amber CURRENT highlight on downstream PENDING steps that are no
  // longer reachable. Without this, the header reads "halted at step 2"
  // while the timeline visibly highlights step 3 — a contradiction the
  // audit-trust persona caught in final convergence review.
  chainKind: ChainState['kind']
}

function StepTimeline({ steps, callerId, requesterUserId, duplicates, chainKind }: TimelineProps) {
  const sorted = [...steps].sort((a, b) => a.orderIndex - b.orderIndex)
  // R43 iter7 (P2-F-CONVERGE-1 + residual): no step is "current" in
  // states where the chain cannot progress through PENDING rows. For
  // halted (REJECTED upstream), cancelled, and draft (not yet submitted)
  // we suppress the amber CURRENT highlight entirely. The header reads
  // the same shape ("halted at … / Status: CANCELLED / Status: DRAFT")
  // and the timeline now agrees.
  const noActive = chainKind === 'halted' || chainKind === 'cancelled' || chainKind === 'draft'
  const firstPendingIndex = noActive ? -1 : sorted.findIndex((s) => s.status === 'PENDING')

  return (
    <ol className="space-y-2">
      {sorted.map((step, idx) => {
        const isCurrent = idx === firstPendingIndex
        const isCaller = sameUser(step.approverUserId, callerId)
        // R43 iter3 (P2-iter2-N7): when the caller IS the requester
        // (common in dogfood / test fixtures), suppress the redundant
        // "requester (cannot self-approve)" badge. The "you" badge
        // already communicates identity; the self-approval invariant
        // is only relevant to readers other than the requester.
        const isSelfApprovalAttempt =
          sameUser(step.approverUserId, requesterUserId) && !isCaller

        return (
          <li
            key={step.id}
            className={`flex gap-3 rounded border p-3 ${
              isCurrent ? 'border-amber-400 bg-amber-50' : ''
            }`}
          >
            <div className="shrink-0">
              <span
                className={`flex h-6 w-6 items-center justify-center rounded-full text-xs font-medium ${
                  step.status === 'APPROVED'
                    ? 'bg-green-600 text-white'
                    : step.status === 'REJECTED'
                      ? 'bg-red-600 text-white'
                      : isCurrent
                        ? 'bg-amber-500 text-white'
                        : 'bg-muted text-muted-foreground'
                }`}
              >
                {step.orderIndex + 1}
              </span>
            </div>
            <div className="min-w-0 flex-1">
              <div className="flex items-baseline justify-between gap-2">
                <span className="truncate text-sm font-medium">
                  {step.approverUserId}
                  {isCaller && (
                    <span className="ml-1 rounded bg-blue-100 px-1.5 py-0.5 text-xs text-blue-900">
                      you
                    </span>
                  )}
                  {isSelfApprovalAttempt && (
                    <span
                      className="ml-1 rounded bg-red-100 px-1.5 py-0.5 text-xs text-red-900"
                      title="A requester cannot approve their own request — the backend rejects this and the iter1+2 dogfood adds it as a VIOLATION proof"
                    >
                      requester (cannot self-approve)
                    </span>
                  )}
                  {duplicates.has(step.approverUserId) && (
                    <span
                      className="ml-1 rounded bg-red-100 px-1.5 py-0.5 text-xs text-red-900"
                      title="The same approver appears more than once in this chain. The backend rejects duplicate approvers; this is shown so a misconfigured chain is visible before the second occurrence fires."
                    >
                      duplicate approver
                    </span>
                  )}
                </span>
                <span
                  className={`shrink-0 rounded px-2 py-0.5 text-xs ${
                    step.status === 'APPROVED'
                      ? 'bg-green-100 text-green-900'
                      : step.status === 'REJECTED'
                        ? 'bg-red-100 text-red-900'
                        : isCurrent
                          ? 'bg-amber-100 text-amber-900'
                          : 'bg-muted text-muted-foreground'
                  }`}
                >
                  {isCurrent && step.status === 'PENDING' ? 'CURRENT' : step.status}
                </span>
              </div>
              {step.comment && (
                <div className="mt-1 whitespace-pre-wrap text-sm text-muted-foreground">
                  {step.comment}
                </div>
              )}
              {step.actedAt && step.actedByUserId && (
                <div className="mt-1 text-xs text-muted-foreground">
                  {step.status.toLowerCase()} by{' '}
                  <span className="font-mono">{step.actedByUserId}</span> ·{' '}
                  {new Date(step.actedAt).toLocaleString()}
                </div>
              )}
            </div>
          </li>
        )
      })}
    </ol>
  )
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
    onSuccess: async () => {
      invalidate()
      await refetchInbox()
    },
  })
  const reject = useMutation({
    mutationFn: ({ stepId, comment }: { stepId: string; comment: string }) =>
      rejectStep(requestId, stepId, comment),
    onSuccess: async () => {
      invalidate()
      await refetchInbox()
    },
  })

  const [comment, setComment] = React.useState('')

  // R43 iter6 (P1-iter5-N1): hooks MUST be called before any conditional
  // early return. Both useMemo blocks live here and are guarded against
  // the not-yet-loaded `data` inside the memo body so the hook ordering
  // stays stable across the loading → loaded transition.
  // R43 iter7 (P1-iter6-N1): `chain` is the single derivation; the
  // earlier `chainPreview` + fallback `??` pattern left a provably-
  // unreachable second describeChain() call below the !data guard.
  const chain = React.useMemo(
    () => (data ? describeChain(data, callerId) : null),
    [data, callerId],
  )
  const duplicates = React.useMemo(
    () => (data ? findDuplicateApprovers(data.steps) : new Set<string>()),
    [data],
  )
  // R43 iter6 (P1-iter5-N2): depend on primitive chain fields rather
  // than the chain object — describeChain returns a fresh literal on
  // every render so [data, chain] never hits the cache. The kind +
  // currentIndex pair fully captures when the upstream callout must
  // re-evaluate.
  const upstreamApproved = React.useMemo(
    () =>
      data && chain && chain.kind === 'actionable'
        ? findUpstreamApprovedComments(data, chain.currentIndex)
        : [],
    [data, chain?.kind, chain?.currentIndex],
  )

  if (isLoading) {
    return (
      <div className="py-12 text-center text-sm text-muted-foreground">
        Loading request…
      </div>
    )
  }
  if (error) {
    return <EmptyState title="Failed to load request" description={(error as Error).message} />
  }
  if (!data || !chain) {
    return <EmptyState title="Not found" description="This request does not exist or you do not have access." />
  }

  const isRequester = sameUser(data.requesterUserId, callerId)
  const actionableStep = chain.kind === 'actionable' ? chain.step : null

  return (
    <ErrorBoundary>
      <div className="space-y-6">
        {/* R43 iter3 (P2-iter2-N5): persistent back-to-inbox affordance
             visible in every chain state, not only in the actionable
             section. CFOs triaging many items pop in and out of detail. */}
        <div className="flex justify-end">
          <button
            type="button"
            className="rounded border px-3 py-1 text-xs hover:bg-muted"
            onClick={() => router.push('/approvals/inbox')}
          >
            ← Back to inbox
          </button>
        </div>
        <header className="space-y-1">
          <div className="flex items-baseline gap-2">
            <span className="rounded bg-muted px-2 py-0.5 text-xs uppercase">{data.type}</span>
            <h1 className="text-lg font-semibold">{data.title}</h1>
          </div>
          <div className="text-sm text-muted-foreground">
            Filed by <span className="font-mono">{data.requesterUserId}</span>
            {isRequester && (
              <span className="ml-1 rounded bg-blue-100 px-1.5 py-0.5 text-xs text-blue-900">
                you
              </span>
            )}{' '}
            · {new Date(data.createdAt).toLocaleString()}
          </div>
          <div className="text-sm">
            Status:{' '}
            <span
              className={
                data.status === 'APPROVED'
                  ? 'font-medium text-green-700'
                  : data.status === 'REJECTED' || data.status === 'CANCELLED'
                    ? 'font-medium text-red-700'
                    : 'font-medium'
              }
            >
              {data.status}
            </span>
            {/* R43 iter1 (P1-F11): progress indicator so the requester
                 (and any onlooker) can see at a glance how far through
                 the chain the request is, without scrolling the timeline. */}
            {chain.totalSteps > 0 && (
              <span className="ml-2 text-muted-foreground">
                · step {Math.min(chain.currentIndex + 1, chain.totalSteps)} of {chain.totalSteps}
                {chain.kind === 'waiting' && chain.waitingOn && (
                  <> · waiting on <span className="font-mono">{chain.waitingOn.approverUserId}</span></>
                )}
                {chain.kind === 'halted' && chain.haltedAt && (
                  <> · halted at <span className="font-mono">{chain.haltedAt.approverUserId}</span></>
                )}
              </span>
            )}
          </div>
        </header>

        {Object.keys(data.payload).length > 0 && (
          <section>
            <h2 className="mb-2 text-sm font-semibold uppercase text-muted-foreground">
              Payload
            </h2>
            <pre className="overflow-auto rounded border bg-muted/40 p-3 text-xs">
              {JSON.stringify(data.payload, null, 2)}
            </pre>
            {data.status === 'DRAFT' ? null : (
              <p className="mt-1 text-xs text-muted-foreground">
                The payload is locked from the moment the request was
                submitted — what every approver sees is exactly what the
                requester filed.
              </p>
            )}
          </section>
        )}

        <section>
          <h2 className="mb-2 text-sm font-semibold uppercase text-muted-foreground">
            Approval chain
          </h2>
          <StepTimeline
            steps={data.steps}
            callerId={callerId}
            requesterUserId={data.requesterUserId}
            duplicates={duplicates}
            chainKind={chain.kind}
          />
        </section>

        {/* R43 iter1 (P2-F14): explicit "chain halted" panel so the
             rejected state never goes silent. iter3 (P1-iter2-N6): copy
             branches on isRequester — only the requester should be told
             to file a replacement; other viewers see "no action required". */}
        {chain.kind === 'halted' && chain.haltedAt && (
          <section className="space-y-2 rounded border border-red-300 bg-red-50 p-3 text-sm text-red-900">
            <p className="font-medium">
              This chain was rejected at step {chain.currentIndex + 1} by{' '}
              <span className="font-mono">{chain.haltedAt.approverUserId}</span>.
            </p>
            {chain.haltedAt.comment && (
              <p className="whitespace-pre-wrap">
                <span className="font-medium">Reason:</span> {chain.haltedAt.comment}
              </p>
            )}
            <p>
              {isRequester
                ? 'No further approver action is possible. If you need a different outcome, file a new request — the original is preserved in audit.'
                : 'No further action required from you. The requester has been notified.'}
            </p>
          </section>
        )}

        {/* ─── Action area ────────────────────────────────────────────── */}

        {isRequester && data.status === 'DRAFT' && (
          <section className="space-y-2 rounded border p-3">
            <p className="text-sm text-muted-foreground">
              This request is a draft. Submit to start the approval chain, or
              discard if you no longer want to file it.
            </p>
            {/* R43 iter1 (P1-F2): explicit recovery path for the "I made
                 a typo in payload or approver chain" case. The catalog
                 backend treats payload as JPA updatable=false once
                 SUBMITTED, so the only fix is "discard + recreate". */}
            <p className="text-xs text-muted-foreground">
              Need to fix a typo in the payload or approver chain? Discard this
              draft and recreate from <code>/approvals/new</code>. Payload becomes
              immutable the moment you submit; we make editing inconvenient on
              purpose so an approver always sees the original document the
              requester filed.
            </p>
            {(submit.error || cancel.error) && (
              <div
                role="alert"
                className="rounded border border-red-300 bg-red-50 px-2 py-1 text-xs text-red-900"
              >
                {(submit.error || cancel.error)?.message}
              </div>
            )}
            <div className="flex gap-2">
              <button
                type="button"
                className="rounded bg-foreground px-3 py-1.5 text-sm text-background hover:opacity-90 disabled:opacity-50"
                disabled={submit.isPending}
                onClick={() => submit.mutate()}
              >
                Submit for approval
              </button>
              <button
                type="button"
                className="rounded border px-3 py-1.5 text-sm hover:bg-muted disabled:opacity-50"
                disabled={cancel.isPending}
                onClick={() => cancel.mutate()}
              >
                Discard draft
              </button>
              <button
                type="button"
                className="rounded border px-3 py-1.5 text-sm hover:bg-muted"
                onClick={() => router.push('/approvals/new')}
                title="Opens the new-request form. This draft remains on /approvals/my until you discard it."
              >
                Create another draft
              </button>
            </div>
          </section>
        )}

        {isRequester && data.status === 'SUBMITTED' && (
          <section className="space-y-2 rounded border p-3">
            <p className="text-sm text-muted-foreground">
              Your request is in flight. You can cancel until the chain
              completes; after that the outcome is final.
            </p>
            {/* R43 iter1 (P1-F5): when one or more approvers have already
                 acted, cancellation invalidates work they've already done.
                 We surface that consequence and require an explicit
                 confirm before firing. */}
            {(() => {
              const approvedAlready = data.steps.filter((s) => s.status === 'APPROVED')
              const hasApprovals = approvedAlready.length > 0
              const handle = () => {
                if (hasApprovals) {
                  const names = approvedAlready
                    .map((s) => s.approverUserId)
                    .join(', ')
                  const ok = window.confirm(
                    `Cancel this request? ${approvedAlready.length} approver(s) (${names}) have already acted — their decisions will be voided in the audit trail.`,
                  )
                  if (!ok) return
                }
                cancel.mutate()
              }
              return (
                <>
                  {hasApprovals && (
                    <p className="text-xs text-amber-900">
                      {approvedAlready.length} approver(s) have already acted on
                      this request. Cancelling will void their decisions in the
                      audit trail.
                    </p>
                  )}
                  {cancel.error && (
                    <div
                      role="alert"
                      className="rounded border border-red-300 bg-red-50 px-2 py-1 text-xs text-red-900"
                    >
                      {cancel.error.message}
                    </div>
                  )}
                  <button
                    type="button"
                    className="rounded border px-3 py-1.5 text-sm hover:bg-muted disabled:opacity-50"
                    disabled={cancel.isPending}
                    onClick={handle}
                  >
                    Cancel request
                  </button>
                </>
              )
            })()}
          </section>
        )}

        {actionableStep && (
          <section className="space-y-2 rounded border-2 border-amber-400 bg-amber-50/50 p-3">
            <p className="text-sm font-medium text-amber-900">
              YOUR TURN — this request is waiting on your approval at step{' '}
              {actionableStep.orderIndex + 1} of {chain.totalSteps}.
            </p>
            {/* R43 iter4 (P2-iter3-N2): every upstream APPROVED step
                 with a comment is surfaced — not just the immediately
                 previous one. Long Korean 결재선 can carry a conditional
                 approval ('OK 단 amount 95M 로 negotiated') two or three
                 steps back; the mid-chain approver must see all of them
                 without scrolling the timeline. */}
            {upstreamApproved.length > 0 && (
              <div className="space-y-1.5">
                {upstreamApproved.map((s) => (
                  <div
                    key={s.id}
                    className="rounded border border-green-300 bg-green-50 px-2 py-1.5 text-xs text-green-900"
                  >
                    <span className="font-medium">
                      Step {s.orderIndex + 1} (<span className="font-mono">{s.approverUserId}</span>) approved with note:
                    </span>
                    <div className="mt-1 whitespace-pre-wrap">{s.comment}</div>
                  </div>
                ))}
              </div>
            )}
            {/* R43 iter1 (P2-F4): downstream consequence helper so a
                 fast-moving approver knows what each button actually does. */}
            <p className="text-xs text-amber-900/80">
              {chain.currentIndex + 1 < chain.totalSteps ? (
                <>
                  Approve → notifies step {chain.currentIndex + 2}{' '}
                  (<span className="font-mono">
                    {data.steps.find((s) => s.orderIndex === actionableStep.orderIndex + 1)?.approverUserId}
                  </span>) ·{' '}
                </>
              ) : (
                <>Approve → completes the chain · </>
              )}
              Reject → ends the chain and notifies the requester. Both actions are recorded
              in the audit trail and cannot be undone from this UI.
            </p>
            <textarea
              className="w-full rounded border px-2 py-1 text-sm"
              rows={3}
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder="Comment — required for reject; for approve use this to record conditions (e.g. '협조 결재선 추가 필요')"
            />
            {/* R43 iter1 (P1-F7 / P2-F10): mutation errors visible in
                 the action panel rather than swallowed. iter7 trim: this
                 panel only fires approve / reject; submit / cancel are
                 wired into the DRAFT / SUBMITTED requester panels and
                 cannot reach here. */}
            {(approve.error || reject.error) && (
              <div
                role="alert"
                className="rounded border border-red-300 bg-red-50 px-2 py-1 text-xs text-red-900"
              >
                {(approve.error || reject.error)?.message}
                {' '}— refresh the page; the chain state may have changed since you loaded it.
              </div>
            )}
            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                className="rounded bg-green-600 px-3 py-1.5 text-sm text-white hover:bg-green-700 disabled:opacity-50"
                disabled={approve.isPending || reject.isPending}
                onClick={() => {
                  approve.mutate(
                    { stepId: actionableStep.id, comment },
                    {
                      onSuccess: () => {
                        setComment('')
                        // R43 iter1 (P2-F7): jump back to inbox so the
                        // approver moves on to the next pending item
                        // instead of staring at a now-empty action panel.
                        router.push('/approvals/inbox')
                      },
                    },
                  )
                }}
              >
                Approve
              </button>
              <button
                type="button"
                className="rounded bg-red-600 px-3 py-1.5 text-sm text-white hover:bg-red-700 disabled:opacity-50"
                // R43 iter1 (P1-F8): reject cannot be fired without a
                // comment — the requester needs to know why.
                disabled={approve.isPending || reject.isPending || comment.trim().length === 0}
                onClick={() => {
                  // R43 iter3 (P2-iter2-N1): reject is destructive and
                  // shares the comment field with approve. Misclick from
                  // a fast-moving approver who typed an approval note
                  // would otherwise fire that note as a rejection reason.
                  // A confirm with the chain consequence is the friction.
                  const ok = window.confirm(
                    `Reject this request? The chain will end and the requester will be notified with your comment:\n\n${comment}`,
                  )
                  if (!ok) return
                  reject.mutate(
                    { stepId: actionableStep.id, comment },
                    {
                      onSuccess: () => {
                        setComment('')
                        router.push('/approvals/inbox')
                      },
                    },
                  )
                }}
                title={
                  comment.trim().length === 0
                    ? 'Add a reason in the comment box before rejecting.'
                    : undefined
                }
              >
                Reject
              </button>
              {/* R43 iter4 (P1-iter3-N2): redundant Back-to-inbox
                   button removed — the persistent header link above
                   (right-aligned at the top of the page) serves every
                   chain state, including this one. */}
            </div>
          </section>
        )}
      </div>
    </ErrorBoundary>
  )
}
