/// <reference types="@testing-library/jest-dom/vitest" />
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, within } from '@testing-library/react'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import ApprovalDetailView, {
  type ApprovalRequest,
} from '../../templates/L4/approval-workflow/app/(approvals)/[id]/approval-detail-view'

// BACKLOG P2-42 — FE render leg of the L4-page-render-testability pass-2 closure (same class as
// frontend/tests/item-detail-view.vitest.tsx). Renders ApprovalDetailView DIRECTLY — the pure
// props->JSX component (incl. describeChain/StepTimeline/duplicate-approver detection) extracted
// from (approvals)/[id]/page.tsx for exactly this reason.

const ACTIONABLE_REQUEST: ApprovalRequest = {
  id: 'req_1',
  requesterUserId: 'alice',
  type: 'PURCHASE',
  title: 'New laptop for engineering',
  status: 'SUBMITTED',
  payload: { amount: 2500000 },
  steps: [
    {
      id: 'step_1',
      orderIndex: 0,
      approverUserId: 'bob',
      status: 'PENDING',
      actedByUserId: null,
      actedAt: null,
      comment: null,
    },
  ],
  createdAt: '2026-07-01T00:00:00Z',
  submittedAt: '2026-07-01T00:01:00Z',
  completedAt: null,
}

function baseProps(overrides: Partial<Parameters<typeof ApprovalDetailView>[0]> = {}) {
  return {
    request: ACTIONABLE_REQUEST,
    isLoading: false,
    error: null,
    callerId: 'bob',
    onBackToInbox: vi.fn(),
    onCreateAnotherDraft: vi.fn(),
    onSubmit: vi.fn(),
    submitPending: false,
    submitErrorMessage: null,
    onCancel: vi.fn(),
    cancelPending: false,
    cancelErrorMessage: null,
    onApprove: vi.fn().mockResolvedValue(undefined),
    approvePending: false,
    approveErrorMessage: null,
    onReject: vi.fn().mockResolvedValue(undefined),
    rejectPending: false,
    rejectErrorMessage: null,
    ...overrides,
  }
}

describe('ApprovalDetailView — pure render of a request detail + timeline (P2-42)', () => {
  it('renders the resolved request: title, status, and the actionable panel for the assigned approver', () => {
    render(<ApprovalDetailView {...baseProps()} />)
    expect(screen.getByRole('heading', { name: 'New laptop for engineering' })).toBeInTheDocument()
    expect(screen.getByText('SUBMITTED')).toBeInTheDocument()
    expect(screen.getByText(/YOUR TURN/)).toBeInTheDocument()
  })

  it('does NOT show the actionable panel for a step-2 approver waiting on step 1 (ordering)', () => {
    const waitingCaller = { ...baseProps(), callerId: 'someone-else' }
    render(<ApprovalDetailView {...waitingCaller} />)
    expect(screen.queryByText(/YOUR TURN/)).not.toBeInTheDocument()
    expect(screen.getByText(/waiting on/)).toBeInTheDocument()
  })

  it('flags a duplicate approver in the timeline (null-safety / structural branch)', () => {
    const dup: ApprovalRequest = {
      ...ACTIONABLE_REQUEST,
      steps: [
        ...ACTIONABLE_REQUEST.steps,
        { id: 'step_2', orderIndex: 1, approverUserId: 'bob', status: 'PENDING', actedByUserId: null, actedAt: null, comment: null },
      ],
    }
    render(<ApprovalDetailView {...baseProps({ request: dup })} />)
    expect(screen.getAllByText('duplicate approver').length).toBeGreaterThan(0)
  })

  it('renders the not-found state when request is undefined and not loading/erroring (null-safety branch)', () => {
    render(<ApprovalDetailView {...baseProps({ request: undefined })} />)
    expect(screen.getByText('Not found')).toBeInTheDocument()
  })

  it('clicking Approve calls onApprove with the actionable step id and comment, then clears the draft', async () => {
    const onApprove = vi.fn().mockResolvedValue(undefined)
    render(<ApprovalDetailView {...baseProps({ onApprove })} />)

    const textarea = screen.getByPlaceholderText(/Comment/)
    fireEvent.change(textarea, { target: { value: 'looks good' } })
    fireEvent.click(screen.getByRole('button', { name: 'Approve' }))

    expect(onApprove).toHaveBeenCalledWith('step_1', 'looks good')
    await vi.waitFor(() => expect(textarea).toHaveValue(''))
  })

  it('Reject is disabled until a comment is entered, and requires confirm', () => {
    const onReject = vi.fn().mockResolvedValue(undefined)
    render(<ApprovalDetailView {...baseProps({ onReject })} />)

    expect(screen.getByRole('button', { name: 'Reject' })).toBeDisabled()

    fireEvent.change(screen.getByPlaceholderText(/Comment/), { target: { value: 'not needed' } })
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    fireEvent.click(screen.getByRole('button', { name: 'Reject' }))
    expect(confirmSpy).toHaveBeenCalled()
    expect(onReject).toHaveBeenCalledWith('step_1', 'not needed')
    confirmSpy.mockRestore()
  })

  it('shows the halted panel with the rejecting approver and reason (null-safety branch)', () => {
    const halted: ApprovalRequest = {
      ...ACTIONABLE_REQUEST,
      steps: [
        { id: 'step_1', orderIndex: 0, approverUserId: 'bob', status: 'REJECTED', actedByUserId: 'bob', actedAt: '2026-07-02T00:00:00Z', comment: 'budget too high' },
      ],
    }
    render(<ApprovalDetailView {...baseProps({ request: halted })} />)
    expect(screen.getByText(/This chain was rejected at step 1/)).toBeInTheDocument()
    // Appears twice by design: once in the timeline step's own comment row, once repeated
    // as the halted panel's "Reason".
    expect(screen.getAllByText('budget too high')).toHaveLength(2)
  })

  it('NON-VACUITY: a different title DOES change the rendered DOM — proves the assertions above are capable of going RED, not vacuously passing', () => {
    render(<ApprovalDetailView {...baseProps({ request: { ...ACTIONABLE_REQUEST, title: 'A totally different title' } })} />)
    expect(screen.getByRole('heading', { name: 'A totally different title' })).toBeInTheDocument()
    expect(screen.queryByText('New laptop for engineering')).not.toBeInTheDocument()
  })
})

// ─── P2-53 — per-BUTTON server granularity ───────────────────────────────────────────────
//
// The server emits allowedActions PER STEP (P3-76). actionableStepFor answers *which* step
// is the caller's and returns it when EITHER token is granted, so a panel that renders both
// buttons off that single answer flattens the granularity: a step granting only `reject`
// still offered Approve. Each button must consult its own token.
//
// MUTATION (RED-on-revert): in approval-detail-view.tsx, replace the two `stepGrants(...)`
// calls with one either-token value (e.g. `const mayApproveStep = actionableStep !== null`
// and the same for mayRejectStep) — the two absence assertions below go RED.

function withStepActions(allowedActions: string[]): ApprovalRequest {
  return {
    ...ACTIONABLE_REQUEST,
    steps: [{ ...ACTIONABLE_REQUEST.steps[0], allowedActions }],
  }
}

describe('ApprovalDetailView — each action button gates on its OWN server token (P2-53)', () => {
  it('server grants only approve → Approve rendered, Reject ABSENT', () => {
    render(<ApprovalDetailView {...baseProps({ request: withStepActions(['approve']) })} />)
    expect(screen.getByText(/YOUR TURN/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Approve' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Reject' })).not.toBeInTheDocument()
  })

  it('server grants only reject → Reject rendered, Approve ABSENT', () => {
    render(<ApprovalDetailView {...baseProps({ request: withStepActions(['reject']) })} />)
    // The panel headline must not promise an approval it will not offer.
    expect(screen.getByText(/YOUR TURN — this request is waiting on your decision/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reject' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Approve' })).not.toBeInTheDocument()
  })

  it('server grants both → both rendered (the granular check is not vacuously hiding buttons)', () => {
    render(
      <ApprovalDetailView {...baseProps({ request: withStepActions(['approve', 'reject']) })} />,
    )
    expect(screen.getByRole('button', { name: 'Approve' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reject' })).toBeInTheDocument()
  })

  it('server grants neither on the caller-assigned step → no action buttons at all', () => {
    render(<ApprovalDetailView {...baseProps({ request: withStepActions([]) })} />)
    expect(screen.queryByRole('button', { name: 'Approve' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Reject' })).not.toBeInTheDocument()
  })

  it('no step-scoped allowedActions at all (pre-P3-76 backend) → derivation still grants both', () => {
    // ACTIONABLE_REQUEST carries no allowedActions on its step, so stepGrants falls back to
    // local derivation, which mirrors deriveAuthorizedActions: a PENDING first step assigned
    // to the caller on a SUBMITTED request grants approve AND reject.
    render(<ApprovalDetailView {...baseProps()} />)
    expect(screen.getByRole('button', { name: 'Approve' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reject' })).toBeInTheDocument()
  })

  it('the outcome prose advertises only the granted action', () => {
    const { unmount } = render(
      <ApprovalDetailView {...baseProps({ request: withStepActions(['reject']) })} />,
    )
    expect(screen.getByText(/Reject → ends the chain/)).toBeInTheDocument()
    expect(screen.queryByText(/Approve →/)).not.toBeInTheDocument()
    unmount()

    render(<ApprovalDetailView {...baseProps({ request: withStepActions(['approve']) })} />)
    expect(screen.getByText(/Approve →/)).toBeInTheDocument()
    expect(screen.queryByText(/Reject → ends the chain/)).not.toBeInTheDocument()
  })
})

// ─── P3-98 — identity-comparator jurisdiction in the L4 authz paths ──────────────────────
//
// use-caller-id.sameUser TRIMS (its own vitest contract pins that) and is a DISPLAY helper.
// The backend's self-approve guard is exact — ApprovalService.validateApprovers uses
// `id.equals(requesterUserId)`. So every comparison in an L4 page that gates a mutation or
// renders that verdict as fact must use the exact `sameId` mirror exported from
// authorized-actions, or the UI claims a padded id is a self-approval when the server does
// not.

describe('ApprovalDetailView — the self-approval badge uses the EXACT comparator (P3-98)', () => {
  const paddedRequesterAsApprover: ApprovalRequest = {
    ...ACTIONABLE_REQUEST,
    requesterUserId: 'alice',
    steps: [
      // Padded relative to requesterUserId: NOT a self-approval to the backend.
      { id: 'step_1', orderIndex: 0, approverUserId: ' alice ', status: 'PENDING', actedByUserId: null, actedAt: null, comment: null },
    ],
  }

  it('a padded requester id is NOT flagged as a self-approval attempt', () => {
    // MUTATION (RED-on-revert): switch either sameId leg back to sameUser in
    // approval-detail-view.tsx's StepTimeline and this assertion goes RED.
    render(
      <ApprovalDetailView
        {...baseProps({ request: paddedRequesterAsApprover, callerId: 'carol' })}
      />,
    )
    expect(screen.queryByText('requester (cannot self-approve)')).not.toBeInTheDocument()
  })

  it('NON-VACUITY: the exactly-equal requester id IS flagged', () => {
    const exact: ApprovalRequest = {
      ...paddedRequesterAsApprover,
      steps: [{ ...paddedRequesterAsApprover.steps[0], approverUserId: 'alice' }],
    }
    render(<ApprovalDetailView {...baseProps({ request: exact, callerId: 'carol' })} />)
    expect(screen.getByText('requester (cannot self-approve)')).toBeInTheDocument()
  })
})

// The new-request form is a page (useMutation + useRouter + next/navigation), not a
// ledgered presentational view, so its submit gate has no render surface here. This is the
// executable lock on its comparator choice — the same "the scan IS the test" posture as
// frontend/tests/auth-shape-lock.vitest.ts, which exists because templates/L4 is outside
// every tsc glob (P2-23).
describe('approval-workflow new-request form — submit gate uses the exact comparator (P3-98)', () => {
  const NEW_FORM = resolve(
    __dirname,
    '../../templates/L4/approval-workflow/app/(approvals)/new/page.tsx',
  )
  const source = readFileSync(NEW_FORM, 'utf8')
  // Match only real call sites: `sameUser(` / `sameId(`. A mention inside the P3-98
  // rationale comments (which name both helpers on purpose) has no open paren.
  const sameUserCalls = source.match(/\bsameUser\(/g) ?? []
  const sameIdCalls = source.match(/\bsameId\(/g) ?? []

  it('imports sameId from authorized-actions and never calls sameUser', () => {
    expect(source).toMatch(
      /import\s*\{\s*sameId\s*\}\s*from\s*'templates\/L0\/fork-receiver-kit\/authorized-actions'/,
    )
    // MUTATION (RED-on-revert): restore `sameUser(a, callerId)` in the selfApprovalAt
    // findIndex (or the per-row isSelf) and this goes RED.
    expect(sameUserCalls).toHaveLength(0)
  })

  it('NON-VACUITY: the exact comparator is actually CALLED, not merely imported', () => {
    // Both the submit gate (selfApprovalAt) and the per-row red border must use it.
    expect(sameIdCalls.length).toBeGreaterThanOrEqual(2)
  })
})
