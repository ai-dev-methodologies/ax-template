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
  deriveActionableStepFor,
  canView,
  isNextActionableStep,
  type ApprovalAction,
  type AuthorizedActionsRequest,
  type AuthorizedActionsStep,
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

type GoldenRow = (typeof golden.rows)[number]

/** A step id, as the golden spells it. Present on every fixture step; the selector itself
 *  never reads it, which is why it is not on AuthorizedActionsStep. */
const stepId = (step: AuthorizedActionsStep) => (step as unknown as { id: string }).id

/**
 * P3-76 — the same row as a WIRE response would arrive: every step carries the
 * server-computed `allowedActions` the golden records for it. This is the shape
 * `ApprovalStepResponse` now serializes, so feeding it to the selector exercises the
 * server-first path rather than the fallback.
 */
function asServerAnsweredRequest(row: GoldenRow): AuthorizedActionsRequest {
  const request = asRequest(row)
  const byStep = row.expectedStepActions as Record<string, string[]>
  return {
    ...request,
    steps: request.steps.map((s) => ({ ...s, allowedActions: byStep[stepId(s)] })),
  }
}

/** The step the golden says is actionable for this row's caller, or null if none is. */
function expectedActionableStepId(row: GoldenRow): string | null {
  const byStep = row.expectedStepActions as Record<string, string[]>
  const ids = Object.keys(byStep).filter((id) => byStep[id].length > 0)
  if (ids.length > 1) {
    throw new Error(`row '${row.label}': at most ONE step may be actionable, got ${ids.length}`)
  }
  return ids[0] ?? null
}

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

// ── P3-76: the step-scoped leg ──────────────────────────────────────────────
//
// The request-scoped array says whether the caller may approve SOMETHING; it cannot say
// WHICH step is theirs. Until ApprovalStepResponse.allowedActions existed, actionableStepFor
// — the decision the L4 action panel gates on — had no server answer to prefer and derived
// locally, so server-first held for submit/cancel only. These tests pin both halves: the
// selector consumes the server's per-step answer, and the fallback still agrees with it.

describe('actionableStepFor — server-first on the step-scoped field (P3-76)', () => {
  it('returns the step the server marked actionable, for every golden row', () => {
    for (const row of golden.rows) {
      const actual = actionableStepFor(asServerAnsweredRequest(row), row.callerId)
      const expectedId = expectedActionableStepId(row)
      expect(actual === null ? null : stepId(actual), `row '${row.label}'`).toEqual(expectedId)
    }
  })

  it('the local fallback agrees with the server on every row — the fallback cannot drift unnoticed', () => {
    for (const row of golden.rows) {
      const server = actionableStepFor(asServerAnsweredRequest(row), row.callerId)
      const derived = deriveActionableStepFor(asRequest(row), row.callerId)
      expect(
        derived === null ? null : stepId(derived),
        `row '${row.label}' — derivation disagrees with the BE-computed step set`,
      ).toEqual(server === null ? null : stepId(server))
    }
  })

  it("the fallback's step-scoped ACTIONS agree with the server's, not merely the step identity", () => {
    // The sibling test above compares WHICH step; this compares WHAT may be done to it.
    // Without it, a BE step-transition-table change (say PENDING may no longer go REJECTED)
    // that is synced into the golden leaves the TS STEP_TRANSITIONS mirror stale and
    // nothing in the step-scoped leg notices — the actionable step is unchanged, only its
    // action set shrank. This is the assertion that binds the mirror to the BE's table.
    for (const row of golden.rows) {
      const byStep = row.expectedStepActions as Record<string, string[]>
      const serverStepScoped = [...new Set(Object.values(byStep).flat())].sort()
      const derivedStepScoped = deriveAuthorizedActions(asRequest(row), row.callerId)
        .filter((a) => a === 'approve' || a === 'reject')
        .sort()
      expect(
        derivedStepScoped,
        `row '${row.label}' — the TS transition mirror disagrees with the BE's step action set`,
      ).toEqual(serverStepScoped)
    }
  })

  it('picks the SECOND step once the first is approved — not merely steps[0]', () => {
    const row = findRow('second_step_approver_actionable_after_first_approved')
    const actual = actionableStepFor(asServerAnsweredRequest(row), row.callerId)
    expect(actual).not.toBeNull()
    expect(actual!.orderIndex).toBe(1)
    expect(actual!.approverUserId).toBe('erin')
  })

  it('offers no step to the same approver while the first step is still pending', () => {
    const row = findRow('second_step_approver_visible_but_out_of_order')
    expect(actionableStepFor(asServerAnsweredRequest(row), row.callerId)).toBeNull()
  })

  it('believes the server over local derivation when the two disagree', () => {
    // The out-of-order row, but with the server (hypothetically) opening step 1. A client
    // that re-derived would withhold the panel and contradict the only party that enforces.
    const row = findRow('second_step_approver_visible_but_out_of_order')
    const request = asRequest(row)
    const opened: AuthorizedActionsRequest = {
      ...request,
      steps: request.steps.map((s) => ({
        ...s,
        allowedActions: s.orderIndex === 1 ? ['approve', 'reject'] : [],
      })),
    }
    const actual = actionableStepFor(opened, row.callerId)
    expect(actual?.orderIndex).toBe(1)
    // ... while the derivation fallback, given the same aggregate, still says nothing.
    expect(deriveActionableStepFor(request, row.callerId)).toBeNull()
  })

  it('treats an empty server set as a real "no", not a missing answer to fall back on', () => {
    // Every step carries the field and every set is empty: the server has spoken. A
    // presence check made PER STEP instead of across the list would read the empty array
    // as "unanswered" and silently re-derive.
    const row = findRow('approver_first_step_pending_submitted')
    const request = asRequest(row)
    const denied: AuthorizedActionsRequest = {
      ...request,
      steps: request.steps.map((s) => ({ ...s, allowedActions: [] })),
    }
    expect(actionableStepFor(denied, row.callerId)).toBeNull()
    // The same aggregate WITHOUT the field derives an actionable step — proving the null
    // above came from the server's answer and not from the aggregate being unactionable.
    expect(deriveActionableStepFor(request, row.callerId)).not.toBeNull()
  })

  it('falls back to derivation when no step carries the field (older fork-receiver backend)', () => {
    const row = findRow('approver_first_step_pending_submitted')
    const request = asRequest(row)
    expect(request.steps.every((s) => s.allowedActions === undefined)).toBe(true)
    const viaFallback = actionableStepFor(request, row.callerId)
    expect(viaFallback).toEqual(deriveActionableStepFor(request, row.callerId))
    expect(viaFallback).not.toBeNull()
  })

  it('never offers a step-scoped token outside {approve, reject}', () => {
    // view is request-scoped; submit/cancel act on the request. The contract's step block
    // declares only the two, and the golden must not claim otherwise.
    for (const row of golden.rows) {
      for (const actions of Object.values(row.expectedStepActions as Record<string, string[]>)) {
        for (const action of actions) {
          expect(['approve', 'reject'], `row '${row.label}'`).toContain(action)
        }
      }
    }
  })
})

// ── P3-76 follow-up: id-comparison semantics ────────────────────────────────
//
// The BE compares ids with a bare String.equals (ApprovalActionGuards :42/:63/:109).
// authorized-actions.sameId used to trim + toLowerCase, so a caller id differing only in
// case or padding was AUTHORIZED by this fallback and REJECTED by the server — the
// fallback offering an action the BE answers with 403/404, the exact divergence this
// module exists to prevent. sameId is now EXACT; these are the cross-leg locks.
//
// The golden rows carry the BE-side expectation ([] on both the request- and step-scoped
// legs), and the sweeps above already assert every row on both, so BE and FE are pinned to
// the SAME denial. These cases name the semantics explicitly.

describe('id comparison is EXACT — matching the BE String.equals (P3-76 follow-up)', () => {
  const distorted = [
    'mixed_case_requester_id_denied',
    'whitespace_padded_requester_id_denied',
    'mixed_case_approver_id_denied',
    'whitespace_padded_approver_id_denied',
  ]

  it('denies every case- or padding-distorted caller id, on both entry points', () => {
    for (const label of distorted) {
      const row = findRow(label)
      expect(authorizedActions(asRequest(row), row.callerId), `row '${label}'`).toEqual([])
      expect(deriveAuthorizedActions(asRequest(row), row.callerId), `row '${label}'`).toEqual([])
      expect(deriveActionableStepFor(asRequest(row), row.callerId), `row '${label}'`).toBeNull()
      expect(canView(asRequest(row), row.callerId), `row '${label}'`).toBe(false)
    }
  })

  it('a mixed-case caller is not the requester — DRAFT stays invisible to them', () => {
    const row = findRow('mixed_case_requester_id_denied')
    expect(row.callerId).toBe('ALICE')
    expect(asRequest(row).requesterUserId).toBe('alice')
    // The exact-cased requester DOES get the actions, so the denial is about the id, not
    // about the aggregate being unactionable.
    expect(deriveAuthorizedActions(asRequest(row), 'alice')).toEqual(['cancel', 'submit', 'view'])
  })

  it('a padded approver id is not the approver — no step becomes actionable', () => {
    const row = findRow('whitespace_padded_approver_id_denied')
    expect(row.callerId).toBe(' carol ')
    expect(deriveActionableStepFor(asRequest(row), row.callerId)).toBeNull()
    // … while the exact id is actionable on the very same aggregate.
    expect(deriveActionableStepFor(asRequest(row), 'carol')).not.toBeNull()
    expect(deriveAuthorizedActions(asRequest(row), 'carol')).toEqual(['approve', 'reject', 'view'])
  })

  it('the empty-never-matches rule survives the exactness change', () => {
    // Deliberately STRICTER than Java's "".equals(""): the fallback may withhold an action
    // the BE would allow, never offer one it would refuse.
    const row = findRow('mixed_case_approver_id_denied')
    expect(authorizedActions(asRequest(row), '')).toEqual([])
    expect(authorizedActions(asRequest(row), null)).toEqual([])
    expect(authorizedActions(asRequest(row), undefined)).toEqual([])
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
