/// <reference types="@testing-library/jest-dom/vitest" />
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import SessionManagementView, {
  type SessionListResponse,
} from '../../templates/L4/session-management/app/(sessions)/session-management-view'

// BACKLOG P2-42 — FE render leg of the L4-page-render-testability pass-1 closure (same class as
// frontend/tests/item-detail-view.vitest.tsx). Renders SessionManagementView DIRECTLY — the pure
// props->JSX component extracted from (sessions)/page.tsx for exactly this reason. `tableSlot` is
// a plain stub element here (not the real VirtualizedTable) — see the view's own frontmatter for
// why the real @tanstack/react-virtual-backed table cannot be imported from this vitest project.

const BASE_DATA: SessionListResponse = {
  items: [
    {
      id: 'sess_1',
      status: 'ACTIVE',
      jti: 'jti_1',
      deviceLabel: 'MacBook Pro',
      ipAddressMasked: '203.0.113.x',
      userAgentSummary: 'Chrome 124 on macOS',
      createdAt: '2026-07-01T00:00:00Z',
      lastSeenAt: '2026-07-20T00:00:00Z',
      expiresAt: '2026-08-01T00:00:00Z',
      revokedAt: null,
      revokedByUserId: null,
      expired: false,
    },
  ],
  totalElements: 1,
}

function baseProps(overrides: Partial<Parameters<typeof SessionManagementView>[0]> = {}) {
  return {
    data: BASE_DATA,
    error: null,
    isLoading: false,
    tableSlot: <div data-testid="stub-table">MacBook Pro row</div>,
    onRevokeOthers: vi.fn(),
    revokeOthersPending: false,
    ...overrides,
  }
}

describe('SessionManagementView — pure render of the caller session inventory (P2-42)', () => {
  it('renders the caller-supplied tableSlot when data is present', () => {
    render(<SessionManagementView {...baseProps()} />)
    expect(screen.getByTestId('stub-table')).toBeInTheDocument()
    expect(screen.queryByText('No sessions')).not.toBeInTheDocument()
  })

  it('renders the loading state instead of tableSlot while isLoading (null-safety branch)', () => {
    render(<SessionManagementView {...baseProps({ isLoading: true })} />)
    expect(screen.getByText('Loading sessions…')).toBeInTheDocument()
    expect(screen.queryByTestId('stub-table')).not.toBeInTheDocument()
  })

  it('renders the error state instead of tableSlot on error (null-safety branch)', () => {
    render(
      <SessionManagementView
        {...baseProps({ error: new Error('network down') })}
      />,
    )
    expect(screen.getByText('Failed to load sessions')).toBeInTheDocument()
    expect(screen.getByText('network down')).toBeInTheDocument()
  })

  it('renders the empty state when there are no sessions (null-safety branch)', () => {
    render(
      <SessionManagementView
        {...baseProps({ data: { items: [], totalElements: 0 } })}
      />,
    )
    expect(screen.getByText('No sessions')).toBeInTheDocument()
  })

  it('clicking "Revoke other sessions" calls onRevokeOthers', () => {
    const onRevokeOthers = vi.fn()
    render(<SessionManagementView {...baseProps({ onRevokeOthers })} />)
    fireEvent.click(screen.getByRole('button', { name: 'Revoke other sessions' }))
    expect(onRevokeOthers).toHaveBeenCalled()
  })

  it('NON-VACUITY: a different tableSlot DOES change the rendered DOM — proves the assertions above are capable of going RED, not vacuously passing', () => {
    render(
      <SessionManagementView
        {...baseProps({ tableSlot: <div data-testid="a-totally-different-slot">different</div> })}
      />,
    )
    expect(screen.getByTestId('a-totally-different-slot')).toBeInTheDocument()
    expect(screen.queryByTestId('stub-table')).not.toBeInTheDocument()
  })
})
