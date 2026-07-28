/// <reference types="@testing-library/jest-dom/vitest" />
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import EmailOutboxView, {
  type OutboxPage,
} from '../../templates/L4/email-outbox/app/(admin)/email-outbox/email-outbox-view'

// BACKLOG P2-28 — FE render leg of the L4-page-render-testability closure (same class as
// frontend/tests/audit-log-redaction-render.vitest.tsx). Renders EmailOutboxView DIRECTLY — the
// pure props->JSX component extracted from (admin)/email-outbox/page.tsx for exactly this reason
// (see that file's frontmatter). The page itself (useQuery/useMutation/useQueryClient) is NOT
// unit-renderable from this vitest project without a shared-config resolve.alias for the
// @tanstack/react-query bare specifier when imported from a templates/L4/... file living outside
// frontend/ — the same documented gap as cmdk (search-palette-hydration.spec.ts) and
// @tanstack/react-query (audit-log-redaction-render.vitest.tsx / payment-success-view.vitest.tsx).

const BASE_PAGE: OutboxPage = {
  content: [
    {
      id: 'row_1',
      recipient: 'user@example.com',
      templateCode: 'welcome',
      subject: 'Welcome!',
      status: 'DLQ',
      retryCount: 3,
      nextAttemptAt: null,
      lastError: 'raw stack trace with a secret',
      createdAt: '2026-07-20T10:00:00Z',
      sentAt: null,
    },
    {
      id: 'row_2',
      recipient: 'other@example.com',
      templateCode: 'receipt',
      subject: 'Your receipt',
      status: 'SENT',
      retryCount: 0,
      nextAttemptAt: null,
      lastError: null,
      createdAt: '2026-07-20T09:00:00Z',
      sentAt: '2026-07-20T09:00:05Z',
    },
  ],
  page: 0,
  size: 20,
  totalElements: 2,
  totalPages: 1,
}

function baseProps(overrides: Partial<Parameters<typeof EmailOutboxView>[0]> = {}) {
  return {
    data: BASE_PAGE,
    error: null,
    isLoading: false,
    dataUpdatedAt: 0,
    statusFilter: '' as const,
    onStatusFilterChange: vi.fn(),
    onRefetch: vi.fn(),
    retryError: null,
    onDismissRetryError: vi.fn(),
    deleteError: null,
    onDismissDeleteError: vi.fn(),
    pendingRetryIds: new Set<string>(),
    pendingDeleteIds: new Set<string>(),
    onRetry: vi.fn(),
    onDelete: vi.fn(),
    sanitizeStoredError: (raw: string) => `[sanitized] ${raw.slice(0, 4)}`,
    ...overrides,
  }
}

describe('EmailOutboxView — pure render of the admin outbox monitor (P2-28)', () => {
  it('renders rows from the resolved data prop, with sanitized stored errors', () => {
    render(<EmailOutboxView {...baseProps()} />)

    expect(screen.getByText('Welcome!')).toBeInTheDocument()
    expect(screen.getByText('Your receipt')).toBeInTheDocument()
    // sanitizeStoredError is called via the injected prop, not inlined — the raw
    // secret-bearing string must never reach the DOM directly.
    expect(screen.getByText(/\[sanitized\] raw/)).toBeInTheDocument()
    expect(screen.queryByText('raw stack trace with a secret')).not.toBeInTheDocument()
  })

  it('a SENT row shows "sent" instead of a Retry button (canRetry null-safety branch)', () => {
    render(<EmailOutboxView {...baseProps()} />)
    expect(screen.getByLabelText('Retry email row_1')).toBeInTheDocument()
    expect(screen.queryByLabelText('Retry email row_2')).not.toBeInTheDocument()
    expect(screen.getByLabelText('Retry not available — already sent')).toBeInTheDocument()
  })

  it('clicking Retry confirms then calls onRetry with the row id', () => {
    const onRetry = vi.fn()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    render(<EmailOutboxView {...baseProps({ onRetry })} />)

    fireEvent.click(screen.getByLabelText('Retry email row_1'))

    expect(confirmSpy).toHaveBeenCalled()
    expect(onRetry).toHaveBeenCalledWith('row_1')
    confirmSpy.mockRestore()
  })

  it('declining the confirm dialog does NOT call onRetry', () => {
    const onRetry = vi.fn()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false)
    render(<EmailOutboxView {...baseProps({ onRetry })} />)

    fireEvent.click(screen.getByLabelText('Retry email row_1'))

    expect(onRetry).not.toHaveBeenCalled()
    confirmSpy.mockRestore()
  })

  it('renders the empty state when data has zero rows (null-safety branch)', () => {
    render(
      <EmailOutboxView
        {...baseProps({ data: { ...BASE_PAGE, content: [], totalElements: 0 } })}
      />,
    )
    expect(screen.getByText('No outbox rows match the filter')).toBeInTheDocument()
    expect(screen.queryByText('Welcome!')).not.toBeInTheDocument()
  })

  it('NON-VACUITY: a different subject DOES change the rendered DOM — proves the assertions above are capable of going RED, not vacuously passing', () => {
    const mutated: OutboxPage = {
      ...BASE_PAGE,
      content: [{ ...BASE_PAGE.content[0], subject: 'Totally different subject' }],
    }
    render(<EmailOutboxView {...baseProps({ data: mutated })} />)
    expect(screen.getByText('Totally different subject')).toBeInTheDocument()
    expect(screen.queryByText('Welcome!')).not.toBeInTheDocument()
  })
})
