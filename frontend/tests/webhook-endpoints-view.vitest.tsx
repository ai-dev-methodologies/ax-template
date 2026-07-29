/// <reference types="@testing-library/jest-dom/vitest" />
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import WebhookEndpointsView, {
  type EndpointResponse,
  type EndpointWithSecret,
} from '../../templates/L4/webhook/app/(admin)/webhooks/webhook-endpoints-view'

// BACKLOG P2-42 — FE render leg of the L4-page-render-testability pass-2 closure (same class as
// frontend/tests/item-detail-view.vitest.tsx). Renders WebhookEndpointsView DIRECTLY — the pure
// props->JSX component extracted from (admin)/webhooks/page.tsx for exactly this reason.

const BASE_ENDPOINT: EndpointResponse = {
  id: 'ep_1',
  url: 'https://example.com/hooks/incoming',
  active: true,
  eventFilter: '*',
  createdAt: '2026-07-01T00:00:00Z',
  updatedAt: '2026-07-01T00:00:00Z',
}

function baseProps(overrides: Partial<Parameters<typeof WebhookEndpointsView>[0]> = {}) {
  return {
    data: [BASE_ENDPOINT],
    error: null,
    isLoading: false,
    revealedEndpoint: null,
    onAcknowledgeReveal: vi.fn(),
    registerErrorMessage: null,
    onDismissRegisterError: vi.fn(),
    deleteErrorMessage: null,
    onDismissDeleteError: vi.fn(),
    draftUrl: '',
    draftFilter: '',
    onDraftUrlChange: vi.fn(),
    onDraftFilterChange: vi.fn(),
    onSubmitRegister: vi.fn(),
    registerPending: false,
    onDelete: vi.fn(),
    deletePending: false,
    deletingId: null,
    ...overrides,
  }
}

describe('WebhookEndpointsView — pure render of the admin webhook endpoints surface (P2-42)', () => {
  it('renders rows from the resolved data prop', () => {
    render(<WebhookEndpointsView {...baseProps()} />)
    expect(screen.getByText('https://example.com/hooks/incoming')).toBeInTheDocument()
    expect(screen.getByText('Active')).toBeInTheDocument()
  })

  it('shows the SecretRevealPanel only when revealedEndpoint is set (null-safety branch)', () => {
    render(<WebhookEndpointsView {...baseProps()} />)
    expect(screen.queryByLabelText('Webhook signing secret (read-only)')).not.toBeInTheDocument()

    const withSecret: EndpointWithSecret = { ...BASE_ENDPOINT, signingSecret: 'whsec_abc123' }
    render(<WebhookEndpointsView {...baseProps({ revealedEndpoint: withSecret })} />)
    expect(screen.getByLabelText('Webhook signing secret (read-only)')).toHaveValue('whsec_abc123')
  })

  it('renders the empty state when there are no endpoints (null-safety branch)', () => {
    render(<WebhookEndpointsView {...baseProps({ data: [] })} />)
    expect(screen.getByText('No endpoints registered yet')).toBeInTheDocument()
  })

  it('clicking Delete then confirming calls onDelete with the endpoint id', () => {
    const onDelete = vi.fn()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    render(<WebhookEndpointsView {...baseProps({ onDelete })} />)
    fireEvent.click(screen.getByLabelText('Delete webhook endpoint https://example.com/hooks/incoming'))
    expect(confirmSpy).toHaveBeenCalled()
    expect(onDelete).toHaveBeenCalledWith('ep_1')
    confirmSpy.mockRestore()
  })

  it('declining the confirm dialog does NOT call onDelete', () => {
    const onDelete = vi.fn()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false)
    render(<WebhookEndpointsView {...baseProps({ onDelete })} />)
    fireEvent.click(screen.getByLabelText('Delete webhook endpoint https://example.com/hooks/incoming'))
    expect(onDelete).not.toHaveBeenCalled()
    confirmSpy.mockRestore()
  })

  it('NON-VACUITY: a different endpoint url DOES change the rendered DOM — proves the assertions above are capable of going RED, not vacuously passing', () => {
    render(
      <WebhookEndpointsView
        {...baseProps({ data: [{ ...BASE_ENDPOINT, url: 'https://a-totally-different.example.com/hooks' }] })}
      />,
    )
    expect(screen.getByText('https://a-totally-different.example.com/hooks')).toBeInTheDocument()
    expect(screen.queryByText('https://example.com/hooks/incoming')).not.toBeInTheDocument()
  })
})
