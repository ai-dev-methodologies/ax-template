/// <reference types="@testing-library/jest-dom/vitest" />
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import DashboardView, {
  type AuthState,
} from '../../templates/L4/auth/app/(authenticated)/dashboard/dashboard-view'

// BACKLOG P2-42 — FE render leg of the L4-page-render-testability pass-1 closure (same class as
// frontend/tests/item-detail-view.vitest.tsx). Renders DashboardView DIRECTLY — the pure
// props->JSX component extracted from (authenticated)/dashboard/page.tsx for exactly this reason.

const BASE_AUTH_STATE: AuthState = {
  email: 'alice@example.com',
  role: 'ADMIN',
  emailVerified: true,
  linkedProviders: ['google'],
}

describe('DashboardView — pure render of the auth vertical placeholder dashboard (P2-42)', () => {
  it('renders the resolved authState prop: email, role, and verification status', () => {
    render(<DashboardView authState={BASE_AUTH_STATE} onLogout={vi.fn()} />)
    expect(screen.getByText('alice@example.com')).toBeInTheDocument()
    expect(screen.getByText('ADMIN')).toBeInTheDocument()
    expect(screen.getByText('Yes')).toBeInTheDocument()
    expect(screen.getByText('google')).toBeInTheDocument()
  })

  it('renders "Pending" when emailVerified is false, and omits linked providers when absent (null-safety branch)', () => {
    render(
      <DashboardView
        authState={{ ...BASE_AUTH_STATE, emailVerified: false, linkedProviders: undefined }}
        onLogout={vi.fn()}
      />,
    )
    expect(screen.getByText('Pending')).toBeInTheDocument()
    expect(screen.queryByText('Linked providers')).not.toBeInTheDocument()
  })

  it('renders "Loading profile…" while authState is null (null-safety branch)', () => {
    render(<DashboardView authState={null} onLogout={vi.fn()} />)
    expect(screen.getByText('Loading profile…')).toBeInTheDocument()
  })

  it('clicking "Sign out" calls onLogout', () => {
    const onLogout = vi.fn()
    render(<DashboardView authState={BASE_AUTH_STATE} onLogout={onLogout} />)
    fireEvent.click(screen.getByRole('button', { name: 'Sign out' }))
    expect(onLogout).toHaveBeenCalled()
  })

  it('NON-VACUITY: a different email DOES change the rendered DOM — proves the assertions above are capable of going RED, not vacuously passing', () => {
    render(
      <DashboardView
        authState={{ ...BASE_AUTH_STATE, email: 'a-totally-different@example.com' }}
        onLogout={vi.fn()}
      />,
    )
    expect(screen.getByText('a-totally-different@example.com')).toBeInTheDocument()
    expect(screen.queryByText('alice@example.com')).not.toBeInTheDocument()
  })
})
