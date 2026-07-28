/*
---
template_id: L0/fork-receiver-kit/authorized-actions
layer: L0
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "S2.AUTHZ.XB / P2-39 — the approval-workflow authz action-set existed as TWO unlinked client-side copies: a selector local to frontend/tests/authz-action-parity.vitest.ts (which the golden fixture tested) and templates/L4/approval-workflow/app/(approvals)/[id]/page.tsx's describeChain (which shipped to fork-receivers and was tested by nothing). The tested copy and the shipped copy could disagree without any gate noticing — a parity test protecting zero shipped lines. This module is the ONE implementation both import. It prefers the server's own answer (ApprovalRequestResponse.allowedActions, added by P2-38b, computed by ApprovalActionEvaluator from the same ApprovalActionGuards predicates ApprovalService enforces) and falls back to local derivation only for a fork-receiver whose backend predates that field."
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API5:2023 Broken Function Level Authorization"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa5-broken-function-level-authorization/"
    anchors: generic_principle_only
imports_from: []
imports_forbidden: [L1, L2, L3, L4, app/, lib/]
---
*/

/**
 * The approval-workflow (전자결재) authorization action set — ONE implementation, shared
 * by the L4 detail page and the cross-boundary parity test.
 *
 * <h2>Server-first</h2>
 * When the response carries `allowedActions`, that array IS the answer: the backend
 * computed it from the very predicates it enforces, so client-side derivation can only
 * introduce disagreement. Local derivation exists solely as a documented fallback for a
 * fork-receiver running a backend older than P2-38b — and it is written to mirror the
 * backend's guards branch for branch, each one cited below.
 *
 * <h2>Why the fallback must keep the ordering check</h2>
 * A second-step approver on a SUBMITTED request is VISIBLE but not ACTIONABLE until every
 * earlier step is APPROVED. A selector that renders "approve" merely because the caller's
 * id appears in `steps[].approverUserId` offers an action the backend answers with
 * 409 STEP_OUT_OF_ORDER — the exact trap the golden fixture's
 * `second_step_approver_visible_but_out_of_order` row pins.
 */

export type ApprovalRequestStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'APPROVED'
  | 'REJECTED'
  | 'CANCELLED'

export type ApprovalStepStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export type ApprovalAction = 'view' | 'submit' | 'cancel' | 'approve' | 'reject'

export interface AuthorizedActionsStep {
  orderIndex: number
  approverUserId: string
  status: ApprovalStepStatus
  /** Set once the assigned approver decides. Drives the P3-63 terminal-visibility arm. */
  actedByUserId?: string | null
}

export interface AuthorizedActionsRequest {
  requesterUserId: string
  status: ApprovalRequestStatus
  steps: AuthorizedActionsStep[]
  /** P2-38b — the server's own answer. Present on any backend at or after that change. */
  allowedActions?: string[] | null
}

/**
 * Mirrors ApprovalRequestStateMachine's ALLOWED map. Only consulted by the fallback —
 * a backend that emits `allowedActions` has already applied the real table.
 */
const REQUEST_TRANSITIONS: Record<ApprovalRequestStatus, ApprovalRequestStatus[]> = {
  DRAFT: ['SUBMITTED', 'CANCELLED'],
  SUBMITTED: ['APPROVED', 'REJECTED', 'CANCELLED'],
  APPROVED: [],
  REJECTED: [],
  CANCELLED: [],
}

/** Mirrors ApprovalStepStateMachine's ALLOWED map. Fallback only, same caveat. */
const STEP_TRANSITIONS: Record<ApprovalStepStatus, ApprovalStepStatus[]> = {
  PENDING: ['APPROVED', 'REJECTED'],
  APPROVED: [],
  REJECTED: [],
}

const TERMINAL: ApprovalRequestStatus[] = ['APPROVED', 'REJECTED', 'CANCELLED']

const ACTION_ORDER: ApprovalAction[] = ['approve', 'cancel', 'reject', 'submit', 'view']

/**
 * Identity comparison used for every caller/approver match.
 *
 * Trimmed + case-folded, and an empty id never matches anything — a blank caller must not
 * be granted an action by accident. Deliberately duplicated from `use-caller-id.sameUser`
 * rather than imported: this module is a pure, dependency-free L0 leaf so a fork-receiver
 * can lift it alone, and the two definitions are pinned identical by the parity test.
 */
function sameId(a: string | null | undefined, b: string | null | undefined): boolean {
  const na = (a ?? '').trim().toLowerCase()
  const nb = (b ?? '').trim().toLowerCase()
  if (na === '' || nb === '') return false
  return na === nb
}

function isKnownAction(value: string): value is ApprovalAction {
  return (ACTION_ORDER as string[]).includes(value)
}

/** Requester arm — mirrors ApprovalActionGuards.isRequester. */
export function isRequester(
  request: AuthorizedActionsRequest,
  callerId: string | null | undefined,
): boolean {
  return sameId(request.requesterUserId, callerId)
}

/** Mirrors ApprovalActionGuards.isActionable — exactly SUBMITTED. */
export function isActionable(request: AuthorizedActionsRequest): boolean {
  return request.status === 'SUBMITTED'
}

/** Mirrors ApprovalActionGuards.isNextActionableStep — strict 결재선 ordering. */
export function isNextActionableStep(
  request: AuthorizedActionsRequest,
  step: AuthorizedActionsStep,
): boolean {
  return request.steps
    .filter((s) => s.orderIndex < step.orderIndex)
    .every((s) => s.status === 'APPROVED')
}

/** Mirrors ApprovalActionGuards.hasActed — the P3-63 acted-approver qualifier. */
export function hasActed(
  step: AuthorizedActionsStep,
  callerId: string | null | undefined,
): boolean {
  return step.status !== 'PENDING' && sameId(step.actedByUserId, callerId)
}

/**
 * Mirrors ApprovalActionGuards.canView — requester (any status) OR assigned approver while
 * SUBMITTED OR (P3-63) an approver who ACTED, once the request is terminal.
 */
export function canView(
  request: AuthorizedActionsRequest,
  callerId: string | null | undefined,
): boolean {
  if (isRequester(request, callerId)) return true
  const submitted = request.status === 'SUBMITTED'
  const terminal = TERMINAL.includes(request.status)
  return request.steps.some(
    (s) =>
      (submitted && sameId(s.approverUserId, callerId)) ||
      (terminal && hasActed(s, callerId)),
  )
}

/**
 * The step this caller may act on right now, or null. This is the single decision the L4
 * detail page needs in order to render its action panel — it must NOT re-derive it.
 */
export function actionableStepFor(
  request: AuthorizedActionsRequest,
  callerId: string | null | undefined,
): AuthorizedActionsStep | null {
  if (!isActionable(request)) return null
  for (const step of [...request.steps].sort((a, b) => a.orderIndex - b.orderIndex)) {
    if (!sameId(step.approverUserId, callerId)) continue
    if (!isNextActionableStep(request, step)) continue
    if (STEP_TRANSITIONS[step.status].length === 0) continue
    return step
  }
  return null
}

/**
 * The action set for `callerId`, sorted. Prefers the server's `allowedActions` when the
 * response carries it; otherwise derives locally from the mirrored guards.
 */
export function authorizedActions(
  request: AuthorizedActionsRequest,
  callerId: string | null | undefined,
): ApprovalAction[] {
  const server = request.allowedActions
  if (Array.isArray(server)) {
    // Set membership, not Array.includes inside the filter — ax/no-array-includes-in-loop.
    const granted = new Set(server)
    return ACTION_ORDER.filter((a) => granted.has(a))
  }
  return deriveAuthorizedActions(request, callerId)
}

/**
 * Local derivation — the documented fallback. Exported so the parity test can assert that
 * it agrees with the server's answer on every golden row, which is what keeps the fallback
 * from silently drifting away from the backend it stands in for.
 */
export function deriveAuthorizedActions(
  request: AuthorizedActionsRequest,
  callerId: string | null | undefined,
): ApprovalAction[] {
  const actions = new Set<ApprovalAction>()

  if (canView(request, callerId)) actions.add('view')

  if (isRequester(request, callerId)) {
    if (REQUEST_TRANSITIONS[request.status].includes('SUBMITTED')) actions.add('submit')
    if (REQUEST_TRANSITIONS[request.status].includes('CANCELLED')) actions.add('cancel')
  }

  if (isActionable(request)) {
    for (const step of request.steps) {
      if (!sameId(step.approverUserId, callerId)) continue
      if (!isNextActionableStep(request, step)) continue
      if (STEP_TRANSITIONS[step.status].includes('APPROVED')) actions.add('approve')
      if (STEP_TRANSITIONS[step.status].includes('REJECTED')) actions.add('reject')
    }
  }

  return ACTION_ORDER.filter((a) => actions.has(a))
}

/** Convenience predicate for render-time gating: `can(req, caller, 'approve')`. */
export function can(
  request: AuthorizedActionsRequest,
  callerId: string | null | undefined,
  action: ApprovalAction,
): boolean {
  return authorizedActions(request, callerId).includes(action)
}

export { isKnownAction }
