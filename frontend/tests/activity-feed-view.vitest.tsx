/// <reference types="@testing-library/jest-dom/vitest" />
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import ActivityFeedView, {
  type ActivityFeedResponse,
} from '../../templates/L4/activity-feed/app/(activities)/activity-feed-view'

// BACKLOG P2-42 — FE render leg of the L4-page-render-testability pass-2 closure (same class as
// frontend/tests/item-detail-view.vitest.tsx). Renders ActivityFeedView DIRECTLY — the pure
// props->JSX component extracted from (activities)/page.tsx for exactly this reason.

const BASE_DATA: ActivityFeedResponse = {
  items: [
    {
      id: 'act_1',
      actorUserId: 'alice',
      verb: 'comment',
      objectType: 'ticket',
      objectId: 'tk_1',
      subjectType: null,
      subjectId: null,
      metadata: {},
      youAreInAudience: true,
      createdAt: '2026-07-20T00:00:00Z',
      readAt: null,
    },
  ],
  page: 0,
  size: 30,
  totalElements: 1,
}

function baseProps(overrides: Partial<Parameters<typeof ActivityFeedView>[0]> = {}) {
  return {
    data: BASE_DATA,
    error: null,
    isLoading: false,
    dataUpdatedAt: 0,
    callerId: 'bob',
    unread: false,
    onToggleUnread: vi.fn(),
    onMarkAllRead: vi.fn(),
    markAllPending: false,
    readErrorMessage: null,
    onDismissReadError: vi.fn(),
    markAllErrorMessage: null,
    onDismissMarkAllError: vi.fn(),
    pendingReadIds: new Set<string>(),
    onMarkRead: vi.fn(),
    onPageChange: vi.fn(),
    ...overrides,
  }
}

describe('ActivityFeedView — pure render of the caller activity inbox (P2-42)', () => {
  it('renders rows from the resolved data prop, showing the raw actor id for a non-caller actor', () => {
    render(<ActivityFeedView {...baseProps()} />)
    expect(screen.getByText('alice')).toBeInTheDocument()
    expect(screen.getByText('commented on')).toBeInTheDocument()
  })

  it('shows "You" instead of the actor id when callerId matches the actor (null-safety branch)', () => {
    render(<ActivityFeedView {...baseProps({ callerId: 'alice' })} />)
    expect(screen.getByText('You')).toBeInTheDocument()
    expect(screen.queryByText('alice')).not.toBeInTheDocument()
  })

  it('renders the empty state when there is no activity (null-safety branch)', () => {
    render(<ActivityFeedView {...baseProps({ data: { ...BASE_DATA, items: [], totalElements: 0 } })} />)
    expect(screen.getByText('No activity yet')).toBeInTheDocument()
  })

  it('clicking Mark all read then confirming calls onMarkAllRead', () => {
    const onMarkAllRead = vi.fn()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    render(<ActivityFeedView {...baseProps({ onMarkAllRead })} />)
    fireEvent.click(screen.getByRole('button', { name: 'Mark all read' }))
    expect(onMarkAllRead).toHaveBeenCalled()
    confirmSpy.mockRestore()
  })

  it('clicking Mark read on a row calls onMarkRead with the activity id', () => {
    const onMarkRead = vi.fn()
    render(<ActivityFeedView {...baseProps({ onMarkRead })} />)
    fireEvent.click(screen.getByLabelText('Mark activity act_1 as read'))
    expect(onMarkRead).toHaveBeenCalledWith('act_1')
  })

  it('NON-VACUITY: a different actorUserId DOES change the rendered DOM — proves the assertions above are capable of going RED, not vacuously passing', () => {
    const mutated: ActivityFeedResponse = {
      ...BASE_DATA,
      items: [{ ...BASE_DATA.items[0], actorUserId: 'a-totally-different-user' }],
    }
    render(<ActivityFeedView {...baseProps({ data: mutated })} />)
    expect(screen.getByText('a-totally-different-user')).toBeInTheDocument()
    expect(screen.queryByText('alice')).not.toBeInTheDocument()
  })
})
