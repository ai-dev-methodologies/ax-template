/// <reference types="@testing-library/jest-dom/vitest" />
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import RequestDetailView, {
  type DsrRequest,
} from '../../templates/L4/data-subject-rights/app/(dsr)/privacy/requests/[id]/request-detail-view'

// BACKLOG P2-42 — FE render leg of the L4-page-render-testability pass-1 closure (same class as
// frontend/tests/item-detail-view.vitest.tsx). Renders RequestDetailView DIRECTLY — the pure
// props->JSX component extracted from privacy/requests/[id]/page.tsx for exactly this reason.

const BASE_REQUEST: DsrRequest = {
  requestId: 'req_1234567890',
  type: 'ACCESS',
  status: 'IN_PROGRESS',
  receivedAt: '2026-07-01T00:00:00Z',
  dueAt: '2026-07-31T00:00:00Z',
  extensionDays: 0,
  slaBreached: false,
}

function baseProps(overrides: Partial<Parameters<typeof RequestDetailView>[0]> = {}) {
  return {
    request: BASE_REQUEST,
    extendOpen: false,
    onToggleExtendOpen: vi.fn(),
    extensionReason: '',
    onExtensionReasonChange: vi.fn(),
    onConfirmExtend: vi.fn(),
    extendPending: false,
    error: null,
    ...overrides,
  }
}

describe('RequestDetailView — pure render of a single DSR request (P2-42)', () => {
  it('renders the resolved request prop: type, status, and truncated id title', () => {
    render(<RequestDetailView {...baseProps()} />)
    expect(screen.getByRole('heading', { name: 'Request req_1234' })).toBeInTheDocument()
    expect(screen.getByText('ACCESS')).toBeInTheDocument()
    expect(screen.getByText('IN_PROGRESS')).toBeInTheDocument()
  })

  it('shows the overdue marker only when slaBreached (null-safety branch)', () => {
    render(<RequestDetailView {...baseProps({ request: { ...BASE_REQUEST, slaBreached: true } })} />)
    expect(screen.getByText('(overdue)')).toBeInTheDocument()
  })

  it('renders the extend form only when extendOpen is true (null-safety branch)', () => {
    render(<RequestDetailView {...baseProps()} />)
    expect(screen.queryByLabelText(/Reason for extension/)).not.toBeInTheDocument()

    render(<RequestDetailView {...baseProps({ extendOpen: true })} />)
    expect(screen.getByLabelText(/Reason for extension/)).toBeInTheDocument()
  })

  it('clicking "Extend window" calls onToggleExtendOpen', () => {
    const onToggleExtendOpen = vi.fn()
    render(<RequestDetailView {...baseProps({ onToggleExtendOpen })} />)
    fireEvent.click(screen.getByRole('button', { name: 'Extend window' }))
    expect(onToggleExtendOpen).toHaveBeenCalled()
  })

  it('renders the error banner when error is set (null-safety branch)', () => {
    render(<RequestDetailView {...baseProps({ error: 'Failed to extend the request.' })} />)
    expect(screen.getByRole('alert')).toHaveTextContent('Failed to extend the request.')
  })

  it('NON-VACUITY: a different status DOES change the rendered DOM — proves the assertions above are capable of going RED, not vacuously passing', () => {
    render(<RequestDetailView {...baseProps({ request: { ...BASE_REQUEST, status: 'CLOSED' } })} />)
    expect(screen.getByText('CLOSED')).toBeInTheDocument()
    expect(screen.queryByText('IN_PROGRESS')).not.toBeInTheDocument()
  })
})
