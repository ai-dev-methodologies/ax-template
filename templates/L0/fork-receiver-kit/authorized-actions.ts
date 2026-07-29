/*
---
template_id: L0/fork-receiver-kit/authorized-actions
layer: L0
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "S2.AUTHZ.XB / P2-39 — the approval-workflow authz action-set existed as TWO unlinked client-side copies: a selector local to frontend/tests/authz-action-parity.vitest.ts (which the golden fixture tested) and templates/L4/approval-workflow/app/(approvals)/[id]/page.tsx's describeChain (which shipped to fork-receivers and was tested by nothing). The tested copy and the shipped copy could disagree without any gate noticing — a parity test protecting zero shipped lines. This module is the ONE implementation both import. It prefers the server's own answer (ApprovalRequestResponse.allowedActions, added by P2-38b, computed by ApprovalActionEvaluator from the same ApprovalActionGuards predicates ApprovalService enforces) and falls back to local derivation only for a fork-receiver whose backend predates that field. P3-76 extended the same posture to actionableStepFor — the decision an action panel gates on — once ApprovalStepResponse began carrying step-scoped allowedActions; before that the request-scoped array could not identify WHICH step was the caller's, so this helper was derivation-only."
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
 * P3-76 completed that posture at the STEP level: the request-scoped array says whether
 * the caller may approve something, never which step is theirs, so `actionableStepFor` —
 * the decision an action panel actually gates on — was pure local derivation until the
 * steps started carrying their own `allowedActions`. Both entry points are now
 * server-first with a derivation fallback of the same shape.
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
  /**
   * P3-76 — the server's own answer for THIS step (`approve` / `reject` only). Present on
   * any backend at or after that change. The request-scoped array cannot say WHICH step is
   * the caller's, so this is the field an action panel should gate on.
   */
  allowedActions?: string[] | null
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
 * <h3>EXACT, because the backend is exact (P3-76 follow-up)</h3>
 * This used to trim and case-fold. The backend does neither: every id comparison the
 * approval domain enforces is a bare {@code String.equals} —
 * `ApprovalActionGuards.isAssignedApprover` (:42), `isRequester` (:63) and `hasActed`
 * (:109). So a caller id differing only in case or padding was AUTHORIZED by this
 * fallback and REJECTED by the server: the fallback offered an action the BE answers
 * with 403/404, which is precisely the divergence this module exists to prevent. The
 * server is authoritative, so the fold and the trim are gone.
 *
 * Normalizing identity for an authorization decision is a policy choice, not a
 * convenience: whether `ALICE` is `alice` is the identity provider's answer to give, and
 * a client that assumes one cannot be right when the server assumes the other. A
 * fork-receiver whose backend really is case-insensitive should change BOTH sides.
 *
 * The empty-never-matches rule stays. It makes this comparison marginally STRICTER than
 * the backend's (Java's `"".equals("")` is true), which is the safe direction: the
 * fallback may withhold an action the BE would allow, never offer one it would refuse.
 * In practice unreachable — an approval request cannot be created with a blank approver.
 *
 * <p>NOT identical to `use-caller-id.sameUser`, which trims: that is a general-purpose UI
 * identity helper with its own tested contract, while this one mirrors an enforcement
 * predicate. Deliberately duplicated rather than imported so this module stays a pure,
 * dependency-free L0 leaf a fork-receiver can lift alone.
 */
function sameId(a: string | null | undefined, b: string | null | undefined): boolean {
  const na = a ?? ''
  const nb = b ?? ''
  if (na === '' || nb === '') return false
  return na === nb
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
 *
 * <h3>Server-first (P3-76)</h3>
 * When the steps carry `allowedActions`, THAT is the answer: the backend computed it from
 * the same guards it enforces, per step. Before P3-76 only the request-scoped array
 * existed, which says whether the caller may approve SOMETHING but not WHICH step — so
 * this helper had no server answer to prefer and derived locally, reintroducing one level
 * down the client-side authorization guess the request-scoped field removed.
 *
 * Presence is decided across the whole step list, not per step: a backend at or after
 * P3-76 emits the field on EVERY step (empty where the caller may do nothing), so a single
 * step carrying it means the server has spoken and an empty sibling is a real "no", not a
 * missing answer to fall back on.
 */
export function actionableStepFor(
  request: AuthorizedActionsRequest,
  callerId: string | null | undefined,
): AuthorizedActionsStep | null {
  const ordered = [...request.steps].sort((a, b) => a.orderIndex - b.orderIndex)
  if (ordered.some((s) => Array.isArray(s.allowedActions))) {
    for (const step of ordered) {
      // Set membership, not Array.includes inside the loop — ax/no-array-includes-in-loop.
      const granted = new Set(step.allowedActions ?? [])
      if (granted.has('approve') || granted.has('reject')) return step
    }
    return null
  }
  return deriveActionableStepFor(request, callerId)
}

/**
 * Local derivation of {@link actionableStepFor} — the documented fallback for a
 * fork-receiver backend that predates P3-76. Exported so the parity test can assert it
 * agrees with the server's per-step answer on every golden row, which is what keeps the
 * fallback from drifting away from the backend it stands in for.
 */
export function deriveActionableStepFor(
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
