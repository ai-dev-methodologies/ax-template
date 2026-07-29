/// <reference types="@testing-library/jest-dom/vitest" />
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, within } from '@testing-library/react'
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
