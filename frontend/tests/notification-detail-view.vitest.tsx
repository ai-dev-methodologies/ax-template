/// <reference types="@testing-library/jest-dom/vitest" />
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import NotificationDetailView from '../../templates/L4/notification/app/(notification)/[id]/notification-detail-view'
import type { NotificationItemData } from '../../templates/L2/blocks/notification-item'

// BACKLOG P2-42 — FE render leg of the L4-page-render-testability pass-1 closure (same class as
// frontend/tests/item-detail-view.vitest.tsx). Renders NotificationDetailView DIRECTLY — the pure
// props->JSX component extracted from (notification)/[id]/page.tsx for exactly this reason.

const BASE_NOTIFICATION: NotificationItemData = {
  id: 'notif_1',
  type: 'ALERT',
  title: 'Your export is ready',
  body: 'The report you requested has finished generating.',
  status: 'UNREAD',
  actionUrl: '/reports/42',
  createdAt: '2026-07-01T00:00:00Z',
}

function baseProps(overrides: Partial<Parameters<typeof NotificationDetailView>[0]> = {}) {
  return {
    isLoading: false,
    notification: BASE_NOTIFICATION,
    onBack: vi.fn(),
    onDismiss: vi.fn(),
    dismissPending: false,
    dismissIsError: false,
    ...overrides,
  }
}

describe('NotificationDetailView — pure render of a single notification (P2-42)', () => {
  it('renders the resolved notification prop: title, body, and action CTA', () => {
    render(<NotificationDetailView {...baseProps()} />)
    expect(screen.getByRole('heading', { name: 'Your export is ready' })).toBeInTheDocument()
    expect(screen.getByText('The report you requested has finished generating.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'View details' })).toHaveAttribute('href', '/reports/42')
  })

  it('omits the action CTA when actionUrl is absent (null-safety branch)', () => {
    render(<NotificationDetailView {...baseProps({ notification: { ...BASE_NOTIFICATION, actionUrl: null } })} />)
    expect(screen.queryByRole('link', { name: 'View details' })).not.toBeInTheDocument()
  })

  it('renders a loading skeleton while isLoading (null-safety branch)', () => {
    render(<NotificationDetailView {...baseProps({ isLoading: true })} />)
    expect(screen.queryByRole('heading', { name: 'Your export is ready' })).not.toBeInTheDocument()
  })

  it('clicking Dismiss calls onDismiss with the notification id', () => {
    const onDismiss = vi.fn()
    render(<NotificationDetailView {...baseProps({ onDismiss })} />)
    fireEvent.click(screen.getByRole('button', { name: 'Dismiss this notification' }))
    expect(onDismiss).toHaveBeenCalledWith('notif_1')
  })

  it('NON-VACUITY: a different title DOES change the rendered DOM — proves the assertions above are capable of going RED, not vacuously passing', () => {
    render(
      <NotificationDetailView
        {...baseProps({ notification: { ...BASE_NOTIFICATION, title: 'A totally different title' } })}
      />,
    )
    expect(screen.getByRole('heading', { name: 'A totally different title' })).toBeInTheDocument()
    expect(screen.queryByText('Your export is ready')).not.toBeInTheDocument()
  })
})
