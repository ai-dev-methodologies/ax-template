/*
---
template_id: L4/approval-workflow/app/(approvals)/[id]/approval-detail-view
layer: L4
domain: approval-workflow
domain_mode: full_trio
evidence:
  - source_type: internal
    rationale: "Pure presentational extraction from (approvals)/[id]/page.tsx (BACKLOG P2-42
      render-testability pass-2 closure — same class as (crud)/items/[id]/item-detail-view.tsx):
      the page's data-fetch/mutation orchestration (useQuery/useMutation/useQueryClient/
      useCallerId/useRouter) is a hard dependency-resolution boundary for a vitest that imports
      this file directly from outside frontend/ — the @tanstack/react-query bare specifier does
      not resolve for a module living in templates/L4/... (see
      frontend/tests/audit-log-redaction-render.vitest.tsx's own note on the same class of gap).
      describeChain/findUpstreamApprovedComments/findDuplicateApprovers/StepTimeline are pure
      derivations over the resolved request (sameUser from use-caller-id — display only, the
      timeline's 'you' chip — and isRequester/can/actionableStepFor/stepGrants/sameId from
      authorized-actions, which own every authorization-adjacent comparison here per P3-98;
      both modules zero-external-dep per their
      own frontmatter) and move here unmodified — same class as tag-library-view's buildTree /
      comment-thread-view's buildTree. Approve/reject are threaded in as Promise-returning
      callbacks so this view can clear its own comment draft only on success, without lifting
      that state to the page (same pattern as tag-library-view's onCreateTag/onUpdateTag).
      templates/L2/blocks/{empty-state,error-boundary} have zero external-npm deps."
---
*/
import * as React from 'react'
import EmptyState from 'templates/L2/blocks/empty-state'
import ErrorBoundary from 'templates/L2/blocks/error-boundary'
import { sameUser } from 'templates/L0/fork-receiver-kit/use-caller-id'
import {
  can,
  actionableStepFor,
  stepGrants,
  sameId,
  isRequester as callerIsRequester,
} from 'templates/L0/fork-receiver-kit/authorized-actions'

// ─── types ───────────────────────────────────────────────────────────────────

export type RequestStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
export type StepStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface ApprovalStep {
  id: string
  orderIndex: number
  approverUserId: string
  status: StepStatus
  actedByUserId: string | null
  actedAt: string | null
  comment: string | null
  /**
   * P3-76 — the server's own per-step answer (`approve` / `reject`). Present on any backend
   * at or after that change; absent on an older fork-receiver backend, where
   * authorized-actions derives locally instead. Declared here because P2-53 gates each
   * action button on its own token: without the field on this type the step-scoped
   * server-first branch was unreachable through the view's own data shape.
   */
  allowedActions?: string[] | null
}

export interface ApprovalRequest {
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
  allowedActions?: string[] | null
}

// ─── chain derivation ────────────────────────────────────────────────────────

interface ChainState {
  kind: 'actionable' | 'waiting' | 'completed' | 'halted' | 'draft' | 'cancelled'
  step?: ApprovalStep
  waitingOn?: ApprovalStep
  haltedAt?: ApprovalStep
  totalSteps: number
  currentIndex: number
}

function describeChain(req: ApprovalRequest, callerId: string): ChainState {
  const sorted = [...req.steps].sort((a, b) => a.orderIndex - b.orderIndex)
  const totalSteps = sorted.length
  const actionable = actionableStepFor(req, callerId)

  if (req.status === 'DRAFT')      return { kind: 'draft', totalSteps, currentIndex: 0 }
  if (req.status === 'CANCELLED')  return { kind: 'cancelled', totalSteps, currentIndex: totalSteps }
  if (req.status === 'APPROVED')   return { kind: 'completed', totalSteps, currentIndex: totalSteps }

  for (let i = 0; i < sorted.length; i++) {
    const step = sorted[i]
    if (step.status === 'REJECTED') {
      return { kind: 'halted', haltedAt: step, totalSteps, currentIndex: i }
    }
    if (step.status === 'PENDING') {
      if (actionable && actionable.orderIndex === step.orderIndex) {
        return { kind: 'actionable', step, totalSteps, currentIndex: i }
      }
      return { kind: 'waiting', waitingOn: step, totalSteps, currentIndex: i }
    }
  }
  return { kind: 'completed', totalSteps, currentIndex: totalSteps }
}

function findUpstreamApprovedComments(
  req: ApprovalRequest,
  currentIndex: number,
): ApprovalStep[] {
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
  chainKind: ChainState['kind']
}

function StepTimeline({ steps, callerId, requesterUserId, duplicates, chainKind }: TimelineProps) {
  const sorted = [...steps].sort((a, b) => a.orderIndex - b.orderIndex)
  const noActive = chainKind === 'halted' || chainKind === 'cancelled' || chainKind === 'draft'
  const firstPendingIndex = noActive ? -1 : sorted.findIndex((s) => s.status === 'PENDING')

  return (
    <ol className="space-y-2">
      {sorted.map((step, idx) => {
        const isCurrent = idx === firstPendingIndex
        // Display label only — the "you" chip. sameUser trims, which is a kindness for a
        // fork-receiver session hook with padded ids and has no authorization consequence.
        const isCaller = sameUser(step.approverUserId, callerId)
        // P3-98 — this badge asserts a BACKEND verdict as fact ("cannot self-approve",
        // ApprovalService.validateApprovers :297 `id.equals(requesterUserId)`), so BOTH
        // legs compare with the exact sameId mirror, never the trimming display helper
        // above: a padded id is NOT a self-approval to the server, so it must not be
        // flagged as one here.
        const isSelfApprovalAttempt =
          sameId(step.approverUserId, requesterUserId) &&
          !sameId(step.approverUserId, callerId)

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

// ─── component ──────────────────────────────────────────────────────────────

export interface ApprovalDetailViewProps {
  request: ApprovalRequest | undefined
  isLoading: boolean
  error: Error | null
  callerId: string

  onBackToInbox: () => void
  onCreateAnotherDraft: () => void

  onSubmit: () => void
  submitPending: boolean
  submitErrorMessage: string | null

  onCancel: () => void
  cancelPending: boolean
  cancelErrorMessage: string | null

  /** Resolves on success, rejects on failure — the view clears its own comment draft and
   *  navigates back to inbox only when this resolves (the page's onApprove/onReject wrap
   *  mutateAsync + the inbox refetch). */
  onApprove: (stepId: string, comment: string) => Promise<void>
  approvePending: boolean
  approveErrorMessage: string | null

  onReject: (stepId: string, comment: string) => Promise<void>
  rejectPending: boolean
  rejectErrorMessage: string | null
}

/**
 * ApprovalDetailView — pure presentational render of a request detail + state-machine
 * timeline + actions.
 *
 * Deliberately has ZERO data-fetching/mutation dependencies (no useQuery/useMutation) — the
 * caller (`(approvals)/[id]/page.tsx`) owns all query/mutation state and passes the resolved
 * `request` + Promise-returning action callbacks in. This keeps the component a plain
 * props -> JSX function, which is what makes it renderable in a unit test without a
 * QueryClientProvider. Owns its own local `comment` draft (pure UI state, cleared on a
 * successful approve/reject) and the destructive-action window.confirm gates (same precedent
 * as tag-library-view's unsaved-draft confirm / webhook-endpoints-view's delete confirm).
 */
export default function ApprovalDetailView({
  request,
  isLoading,
  error,
  callerId,
  onBackToInbox,
  onCreateAnotherDraft,
  onSubmit,
  submitPending,
  submitErrorMessage,
  onCancel,
  cancelPending,
  cancelErrorMessage,
  onApprove,
  approvePending,
  approveErrorMessage,
  onReject,
  rejectPending,
  rejectErrorMessage,
}: ApprovalDetailViewProps) {
  const [comment, setComment] = React.useState('')

  const chain = React.useMemo(
    () => (request ? describeChain(request, callerId) : null),
    [request, callerId],
  )
  const duplicates = React.useMemo(
    () => (request ? findDuplicateApprovers(request.steps) : new Set<string>()),
    [request],
  )
  const upstreamApproved = React.useMemo(
    () =>
      request && chain && chain.kind === 'actionable'
        ? findUpstreamApprovedComments(request, chain.currentIndex)
        : [],
    [request, chain?.kind, chain?.currentIndex],
  )

  if (isLoading) {
    return (
      <div className="py-12 text-center text-sm text-muted-foreground">
        Loading request…
      </div>
    )
  }
  if (error) {
    return <EmptyState title="Failed to load request" description={error.message} />
  }
  if (!request || !chain) {
    return <EmptyState title="Not found" description="This request does not exist or you do not have access." />
  }
  const data = request

  const isRequester = callerIsRequester(data, callerId)
  const maySubmit = can(data, callerId, 'submit')
  const mayCancel = can(data, callerId, 'cancel')
  const actionableStep = chain.kind === 'actionable' ? chain.step : null
  // P2-53 — per-BUTTON tokens, not one "this step is yours" answer. actionableStepFor
  // returns the step when EITHER token is granted, so rendering both buttons off it
  // flattened the granularity the server emits per step.
  const mayApproveStep =
    actionableStep !== null && stepGrants(data, actionableStep, callerId, 'approve')
  const mayRejectStep =
    actionableStep !== null && stepGrants(data, actionableStep, callerId, 'reject')

  const handleApprove = () => {
    if (!actionableStep || !mayApproveStep) return
    // Reset the draft only on success — on failure the error surfaces via
    // approveErrorMessage and the operator's comment stays so they can retry.
    onApprove(actionableStep.id, comment).then(
      () => setComment(''),
      () => {},
    )
  }

  const handleReject = () => {
    if (!actionableStep || !mayRejectStep) return
    const ok = window.confirm(
      `Reject this request? The chain will end and the requester will be notified with your comment:\n\n${comment}`,
    )
    if (!ok) return
    onReject(actionableStep.id, comment).then(
      () => setComment(''),
      () => {},
    )
  }

  const approvedAlready = data.steps.filter((s) => s.status === 'APPROVED')
  const hasApprovals = approvedAlready.length > 0
  const handleCancelInFlight = () => {
    if (hasApprovals) {
      const names = approvedAlready.map((s) => s.approverUserId).join(', ')
      const ok = window.confirm(
        `Cancel this request? ${approvedAlready.length} approver(s) (${names}) have already acted — their decisions will be voided in the audit trail.`,
      )
      if (!ok) return
    }
    onCancel()
  }

  return (
    <ErrorBoundary>
      <div className="space-y-6">
        <div className="flex justify-end">
          <button
            type="button"
            className="rounded border px-3 py-1 text-xs hover:bg-muted"
            onClick={onBackToInbox}
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

        {maySubmit && (
          <section className="space-y-2 rounded border p-3">
            <p className="text-sm text-muted-foreground">
              This request is a draft. Submit to start the approval chain, or
              discard if you no longer want to file it.
            </p>
            <p className="text-xs text-muted-foreground">
              Need to fix a typo in the payload or approver chain? Discard this
              draft and recreate from <code>/approvals/new</code>. Payload becomes
              immutable the moment you submit; we make editing inconvenient on
              purpose so an approver always sees the original document the
              requester filed.
            </p>
            {(submitErrorMessage || cancelErrorMessage) && (
              <div
                role="alert"
                className="rounded border border-red-300 bg-red-50 px-2 py-1 text-xs text-red-900"
              >
                {submitErrorMessage || cancelErrorMessage}
              </div>
            )}
            <div className="flex gap-2">
              <button
                type="button"
                className="rounded bg-foreground px-3 py-1.5 text-sm text-background hover:opacity-90 disabled:opacity-50"
                disabled={submitPending}
                onClick={onSubmit}
              >
                Submit for approval
              </button>
              <button
                type="button"
                className="rounded border px-3 py-1.5 text-sm hover:bg-muted disabled:opacity-50"
                disabled={cancelPending}
                onClick={onCancel}
              >
                Discard draft
              </button>
              <button
                type="button"
                className="rounded border px-3 py-1.5 text-sm hover:bg-muted"
                onClick={onCreateAnotherDraft}
                title="Opens the new-request form. This draft remains on /approvals/my until you discard it."
              >
                Create another draft
              </button>
            </div>
          </section>
        )}

        {mayCancel && data.status === 'SUBMITTED' && (
          <section className="space-y-2 rounded border p-3">
            <p className="text-sm text-muted-foreground">
              Your request is in flight. You can cancel until the chain
              completes; after that the outcome is final.
            </p>
            {hasApprovals && (
              <p className="text-xs text-amber-900">
                {approvedAlready.length} approver(s) have already acted on
                this request. Cancelling will void their decisions in the
                audit trail.
              </p>
            )}
            {cancelErrorMessage && (
              <div
                role="alert"
                className="rounded border border-red-300 bg-red-50 px-2 py-1 text-xs text-red-900"
              >
                {cancelErrorMessage}
              </div>
            )}
            <button
              type="button"
              className="rounded border px-3 py-1.5 text-sm hover:bg-muted disabled:opacity-50"
              disabled={cancelPending}
              onClick={handleCancelInFlight}
            >
              Cancel request
            </button>
          </section>
        )}

        {actionableStep && (
          <section className="space-y-2 rounded border-2 border-amber-400 bg-amber-50/50 p-3">
            <p className="text-sm font-medium text-amber-900">
              {/* P2-53 — "approval" only when this step actually grants approve; a
                  reject-only step is waiting on a decision, not an approval. */}
              YOUR TURN — this request is waiting on your{' '}
              {mayApproveStep ? 'approval' : 'decision'} at step{' '}
              {actionableStep.orderIndex + 1} of {chain.totalSteps}.
            </p>
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
            <p className="text-xs text-amber-900/80">
              {mayApproveStep &&
                (chain.currentIndex + 1 < chain.totalSteps ? (
                  <>
                    Approve → notifies step {chain.currentIndex + 2}{' '}
                    (<span className="font-mono">
                      {data.steps.find((s) => s.orderIndex === actionableStep.orderIndex + 1)?.approverUserId}
                    </span>) ·{' '}
                  </>
                ) : (
                  <>Approve → completes the chain · </>
                ))}
              {mayRejectStep && (
                <>Reject → ends the chain and notifies the requester. </>
              )}
              {/* P2-53 — the outcome prose describes only the actions this step actually
                  grants; a step granting one token must not advertise the other. */}
              Recorded in the audit trail and cannot be undone from this UI.
            </p>
            <textarea
              className="w-full rounded border px-2 py-1 text-sm"
              rows={3}
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder="Comment — required for reject; for approve use this to record conditions (e.g. '협조 결재선 추가 필요')"
            />
            {(approveErrorMessage || rejectErrorMessage) && (
              <div
                role="alert"
                className="rounded border border-red-300 bg-red-50 px-2 py-1 text-xs text-red-900"
              >
                {approveErrorMessage || rejectErrorMessage}
                {' '}— refresh the page; the chain state may have changed since you loaded it.
              </div>
            )}
            <div className="flex flex-wrap gap-2">
              {mayApproveStep && (
                <button
                  type="button"
                  className="rounded bg-green-600 px-3 py-1.5 text-sm text-white hover:bg-green-700 disabled:opacity-50"
                  disabled={approvePending || rejectPending}
                  onClick={handleApprove}
                >
                  Approve
                </button>
              )}
              {mayRejectStep && (
                <button
                  type="button"
                  className="rounded bg-red-600 px-3 py-1.5 text-sm text-white hover:bg-red-700 disabled:opacity-50"
                  disabled={approvePending || rejectPending || comment.trim().length === 0}
                  onClick={handleReject}
                  title={
                    comment.trim().length === 0
                      ? 'Add a reason in the comment box before rejecting.'
                      : undefined
                  }
                >
                  Reject
                </button>
              )}
            </div>
          </section>
        )}
      </div>
    </ErrorBoundary>
  )
}
