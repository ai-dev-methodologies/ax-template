import { describe, it, expect } from 'vitest'

// S2.AUTHZ.XB — cross-boundary parity: the FE must not offer an approval-workflow (전자결재) action the
// BE would reject, and must not hide one the BE allows.
//
// Sibling of frontend/tests/page-envelope-parity.vitest.ts / money-contract-parity.vitest.ts (same
// pattern: one committed golden, two independent consumers — this test + backend/src/test/java/.../
// common/AuthorizedActionSetParityTest.java both read
// frontend/tests/_fixtures/authorized-actions.golden.json).
//
// HONEST SCOPE: the real BE (ApprovalRequestResponse / ApprovalStepResponse) emits NO computed
// action-set/permissions field — see the golden fixture's _provenance and this lane's final report. The
// selector below is this FE leg's best-effort reimplementation of the enforced guards (cited by
// file:line in each comment), exercised against the SAME golden the BE leg (which additionally calls
// the REAL ApprovalRequestStateMachine / ApprovalStepStateMachine for the lifecycle-transition checks)
// asserts against. Because there is no BE-emitted contract to import a type from, the request/step
// shapes below are declared locally rather than shared from templates/L0/fork-receiver-kit.

import golden from './_fixtures/authorized-actions.golden.json'

type RequestStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
type StepStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

interface StepFixture {
  id: string
  orderIndex: number
  approverUserId: string
  status: StepStatus
}

interface RequestFixture {
  requesterUserId: string
  status: RequestStatus
  steps: StepFixture[]
}

// Mirrors backend/src/main/java/com/ax/template/authblueprint/approvalworkflow/ApprovalRequestStateMachine.java:24-32
// (the ALLOWED-transition map — private on the BE, so this is a hand-derived copy, NOT a call into the
// real class; a BE-side change to this map would NOT automatically flip this FE leg red).
const REQUEST_TRANSITIONS: Record<RequestStatus, RequestStatus[]> = {
  DRAFT: ['SUBMITTED', 'CANCELLED'],
  SUBMITTED: ['APPROVED', 'REJECTED', 'CANCELLED'],
  APPROVED: [],
  REJECTED: [],
  CANCELLED: [],
}

// Mirrors ApprovalStepStateMachine.java:23-30 — same caveat as above.
const STEP_TRANSITIONS: Record<StepStatus, StepStatus[]> = {
  PENDING: ['APPROVED', 'REJECTED'],
  APPROVED: [],
  REJECTED: [],
}

/**
 * The FE's notion of "which actions to render" for a given (request, callerId). Cites the SAME guard
 * locations as the BE leg's computeBeAuthorizedActions:
 *  - view:            ApprovalRequestRepository.java:39-42 (findVisibleTo)
 *  - submit/cancel:    ApprovalService.java:245-251 (loadOwn) + REQUEST_TRANSITIONS above
 *  - approve/reject:   ApprovalService.java:195-202 (must be SUBMITTED), :209-217 (approver match),
 *                       :219-227 (strict ordering) + STEP_TRANSITIONS above
 */
function computeAuthorizedActions(request: RequestFixture, callerId: string): string[] {
  const actions = new Set<string>()

  const isRequester = callerId === request.requesterUserId
  const isApproverOnSomeStep = request.steps.some((s) => s.approverUserId === callerId)
  const visible = isRequester || (isApproverOnSomeStep && request.status === 'SUBMITTED')
  if (visible) actions.add('view')

  if (isRequester) {
    if (REQUEST_TRANSITIONS[request.status].includes('SUBMITTED')) actions.add('submit')
    if (REQUEST_TRANSITIONS[request.status].includes('CANCELLED')) actions.add('cancel')
  }

  if (request.status === 'SUBMITTED') {
    for (const target of request.steps) {
      if (target.approverUserId !== callerId) continue
      const earlierStepsClear = request.steps
        .filter((s) => s.orderIndex < target.orderIndex)
        .every((s) => s.status === 'APPROVED')
      if (!earlierStepsClear) continue
      if (STEP_TRANSITIONS[target.status].includes('APPROVED')) actions.add('approve')
      if (STEP_TRANSITIONS[target.status].includes('REJECTED')) actions.add('reject')
    }
  }

  return [...actions].sort()
}

/** Deliberately DROPS the strict-ordering guard (ApprovalService.java:219-227) — used only to prove
 *  that the "second_step_approver_visible_but_out_of_order" golden row has real discriminating power:
 *  a selector that forgets the ordering check renders "approve" where the BE would 409. */
function computeAuthorizedActionsWithoutOrderingGuard(request: RequestFixture, callerId: string): string[] {
  const actions = new Set<string>()
  const isRequester = callerId === request.requesterUserId
  const isApproverOnSomeStep = request.steps.some((s) => s.approverUserId === callerId)
  const visible = isRequester || (isApproverOnSomeStep && request.status === 'SUBMITTED')
  if (visible) actions.add('view')
  if (isRequester) {
    if (REQUEST_TRANSITIONS[request.status].includes('SUBMITTED')) actions.add('submit')
    if (REQUEST_TRANSITIONS[request.status].includes('CANCELLED')) actions.add('cancel')
  }
  if (request.status === 'SUBMITTED') {
    for (const target of request.steps) {
      if (target.approverUserId !== callerId) continue
      // NO earlierStepsClear check here — this is the deliberate omission.
      if (STEP_TRANSITIONS[target.status].includes('APPROVED')) actions.add('approve')
      if (STEP_TRANSITIONS[target.status].includes('REJECTED')) actions.add('reject')
    }
  }
  return [...actions].sort()
}

function findRow(label: string) {
  const row = golden.rows.find((r) => r.label === label)
  if (!row) throw new Error(`golden fixture row not found: ${label}`)
  return row
}

describe('computeAuthorizedActions — parity with the BE-enforced action set (S2.AUTHZ.XB)', () => {
  it('matches the golden expectedActions for every row', () => {
    expect(golden.rows.length).toBeGreaterThan(0)
    for (const row of golden.rows) {
      const actual = computeAuthorizedActions(row.request as RequestFixture, row.callerId)
      expect(actual, `row '${row.label}'`).toEqual([...row.expectedActions].sort())
    }
  })

  it('requester on a DRAFT request may view/submit/cancel but not approve/reject', () => {
    const row = findRow('requester_draft_own_request')
    expect(computeAuthorizedActions(row.request as RequestFixture, row.callerId)).toEqual([
      'cancel',
      'submit',
      'view',
    ])
  })

  it('the assigned first-step approver on a SUBMITTED request may approve/reject but not submit/cancel', () => {
    const row = findRow('approver_first_step_pending_submitted')
    expect(computeAuthorizedActions(row.request as RequestFixture, row.callerId)).toEqual([
      'approve',
      'reject',
      'view',
    ])
  })

  it('a non-participant caller gets no actions at all, not even view (IDOR-safe deny-by-default)', () => {
    const row = findRow('non_participant_submitted')
    expect(computeAuthorizedActions(row.request as RequestFixture, row.callerId)).toEqual([])
  })

  it('a second-step approver is visible but NOT actionable while the first step is still pending', () => {
    const row = findRow('second_step_approver_visible_but_out_of_order')
    expect(computeAuthorizedActions(row.request as RequestFixture, row.callerId)).toEqual(['view'])
  })

  it('an approver who already acted loses even view once the request goes terminal', () => {
    const row = findRow('approver_loses_visibility_after_request_terminal')
    expect(computeAuthorizedActions(row.request as RequestFixture, row.callerId)).toEqual([])
  })

  it('the requester keeps view-only access after the request resolves', () => {
    const row = findRow('requester_own_request_after_terminal_approval')
    expect(computeAuthorizedActions(row.request as RequestFixture, row.callerId)).toEqual(['view'])
  })
})

describe('mutation lock — the ordering-guard omission is caught by the golden, not silently passed', () => {
  it('a selector without the strict-ordering guard WRONGLY offers approve/reject on the out-of-order row', () => {
    const row = findRow('second_step_approver_visible_but_out_of_order')
    const broken = computeAuthorizedActionsWithoutOrderingGuard(row.request as RequestFixture, row.callerId)
    // Proves the row has teeth: the naive (guard-less) selector diverges from both the golden and the
    // guarded selector on exactly the row designed to catch this class of bug.
    expect(broken).toEqual(['approve', 'reject', 'view'])
    expect(broken).not.toEqual([...row.expectedActions].sort())
    expect(broken).not.toEqual(computeAuthorizedActions(row.request as RequestFixture, row.callerId))
  })
})
