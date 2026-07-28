import { describe, it, expect } from 'vitest'

// S2.AUTHZ.XB — cross-boundary parity: the FE must not offer an approval-workflow (전자결재) action the
// BE would reject, and must not hide one the BE allows.
//
// Sibling of frontend/tests/page-envelope-parity.vitest.ts / money-contract-parity.vitest.ts (same
// pattern: one committed golden, two independent consumers — this test + backend/src/test/java/.../
// common/AuthorizedActionSetParityTest.java both read
// frontend/tests/_fixtures/authorized-actions.golden.json).
//
// P2-38b + P2-39 changed what this test is worth:
//   • The BE now EMITS the action set (ApprovalRequestResponse.allowedActions, computed by
//     ApprovalActionEvaluator from the same ApprovalActionGuards predicates ApprovalService enforces),
//     so the BE leg no longer reimplements anything — it calls the production evaluator.
//   • The selector under test is no longer local to this file. It lives at
//     templates/L0/fork-receiver-kit/authorized-actions.ts and is imported BY THE SHIPPED L4 PAGE too
//     (templates/L4/approval-workflow/app/(approvals)/[id]/page.tsx). Before P2-39 this test exercised
//     a copy that shipped to nobody while the page shipped a second, untested copy — a parity test
//     protecting zero shipped lines. Now a RED here is a RED for the code fork-receivers actually run.

import golden from './_fixtures/authorized-actions.golden.json'
import {
  authorizedActions,
  deriveAuthorizedActions,
  actionableStepFor,
  canView,
  isNextActionableStep,
  type ApprovalAction,
  type AuthorizedActionsRequest,
} from '../../templates/L0/fork-receiver-kit/authorized-actions'

function findRow(label: string) {
  const row = golden.rows.find((r) => r.label === label)
  if (!row) throw new Error(`golden fixture row not found: ${label}`)
  return row
}

/** The golden rows carry no `allowedActions` (they describe the aggregate, not a response),
 *  so the shared selector exercises its derivation path for them — which is exactly the path
 *  that must agree with the BE. */
const asRequest = (row: { request: unknown }) => row.request as AuthorizedActionsRequest

describe('authorizedActions — parity with the BE-enforced action set (S2.AUTHZ.XB)', () => {
  it('matches the golden expectedActions for every row', () => {
    expect(golden.rows.length).toBeGreaterThan(0)
    for (const row of golden.rows) {
      const actual = authorizedActions(asRequest(row), row.callerId)
      expect(actual, `row '${row.label}'`).toEqual([...row.expectedActions].sort())
    }
  })

  it('requester on a DRAFT request may view/submit/cancel but not approve/reject', () => {
    const row = findRow('requester_draft_own_request')
    expect(authorizedActions(asRequest(row), row.callerId)).toEqual(['cancel', 'submit', 'view'])
  })

  it('the assigned first-step approver on a SUBMITTED request may approve/reject but not submit/cancel', () => {
    const row = findRow('approver_first_step_pending_submitted')
    expect(authorizedActions(asRequest(row), row.callerId)).toEqual(['approve', 'reject', 'view'])
  })

  it('a non-participant caller gets no actions at all, not even view (IDOR-safe deny-by-default)', () => {
    const row = findRow('non_participant_submitted')
    expect(authorizedActions(asRequest(row), row.callerId)).toEqual([])
  })

  it('a second-step approver is visible but NOT actionable while the first step is still pending', () => {
    const row = findRow('second_step_approver_visible_but_out_of_order')
    expect(authorizedActions(asRequest(row), row.callerId)).toEqual(['view'])
    // ... and the page-facing helper agrees: no action panel for them yet.
    expect(actionableStepFor(asRequest(row), row.callerId)).toBeNull()
  })

  // ── P3-63: the distinguishing pair ────────────────────────────────────────

  it('an approver who ACTED keeps view once the request goes terminal (P3-63)', () => {
    const row = findRow('acted_approver_keeps_view_after_terminal')
    expect(authorizedActions(asRequest(row), row.callerId)).toEqual(['view'])
    expect(canView(asRequest(row), row.callerId)).toBe(true)
  })

  it('an assigned-but-UNACTED approver gets nothing after an upstream rejection (P3-63 narrowness)', () => {
    const row = findRow('unacted_approver_no_view_after_terminal')
    expect(authorizedActions(asRequest(row), row.callerId)).toEqual([])
    expect(canView(asRequest(row), row.callerId)).toBe(false)
  })

  it('a DRAFT is not visible to its named approvers — it has not been sent to the 결재선 yet', () => {
    const row = findRow('assigned_approver_no_view_on_draft')
    expect(authorizedActions(asRequest(row), row.callerId)).toEqual([])
  })

  it('the requester keeps view-only access after the request resolves', () => {
    const row = findRow('requester_own_request_after_terminal_approval')
    expect(authorizedActions(asRequest(row), row.callerId)).toEqual(['view'])
  })
})

describe('server-first — the BE answer wins over local derivation (P2-38b)', () => {
  it('uses response.allowedActions verbatim when the backend supplied it', () => {
    const row = findRow('non_participant_submitted')
    const withServerAnswer = {
      ...asRequest(row),
      // A backend that (hypothetically) granted view would be believed — the client does
      // not second-guess the only party that can actually enforce the decision.
      allowedActions: ['view'] as string[],
    }
    expect(authorizedActions(withServerAnswer, row.callerId)).toEqual(['view'])
    // ... while the derivation fallback, given the same aggregate, still says nothing.
    expect(deriveAuthorizedActions(asRequest(row), row.callerId)).toEqual([])
  })

  it('ignores unknown action tokens the server might add later', () => {
    const row = findRow('approver_first_step_pending_submitted')
    const withUnknown = {
      ...asRequest(row),
      allowedActions: ['approve', 'teleport', 'view'] as string[],
    }
    expect(authorizedActions(withUnknown, row.callerId)).toEqual(['approve', 'view'])
  })

  it('falls back to derivation when the field is absent (older fork-receiver backend)', () => {
    const row = findRow('approver_first_step_pending_submitted')
    const request = asRequest(row)
    expect(request.allowedActions).toBeUndefined()
    expect(authorizedActions(request, row.callerId)).toEqual(
      deriveAuthorizedActions(request, row.callerId),
    )
  })
})

describe('mutation lock — the ordering guard is load-bearing in the SHIPPED selector', () => {
  it('the out-of-order row depends on isNextActionableStep, not merely on approver identity', () => {
    const row = findRow('second_step_approver_visible_but_out_of_order')
    const request = asRequest(row)
    const target = request.steps.find((s) => s.approverUserId === row.callerId)
    if (!target) throw new Error('fixture must assign the caller a step')

    // The caller IS the assigned approver of a step on a SUBMITTED request …
    expect(target.approverUserId).toBe(row.callerId)
    expect(request.status).toBe('SUBMITTED')
    // … and the ONLY thing withholding approve/reject is the ordering predicate.
    expect(isNextActionableStep(request, target)).toBe(false)

    // Proof that this row has teeth: a selector that drops the ordering check offers
    // approve/reject where the BE answers 409 STEP_OUT_OF_ORDER.
    const withoutOrderingGuard = (req: AuthorizedActionsRequest, callerId: string) => {
      const out = new Set<ApprovalAction>()
      if (canView(req, callerId)) out.add('view')
      for (const step of req.steps) {
        if (step.approverUserId !== callerId) continue
        if (step.status === 'PENDING') {
          out.add('approve')
          out.add('reject')
        }
      }
      return [...out].sort()
    }
    const broken = withoutOrderingGuard(request, row.callerId)
    expect(broken).toEqual(['approve', 'reject', 'view'])
    expect(broken).not.toEqual([...row.expectedActions].sort())
    expect(broken).not.toEqual(authorizedActions(request, row.callerId))
  })
})
