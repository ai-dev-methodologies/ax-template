/// <reference types="@testing-library/jest-dom/vitest" />
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import ApiKeyListView, {
  type ApiKeyPage,
} from '../../templates/L4/api-key/app/(api-key)/api-key-list-view'

// BACKLOG P2-42 — FE render leg of the L4-page-render-testability pass-1 closure (same class as
// frontend/tests/item-detail-view.vitest.tsx). Renders ApiKeyListView DIRECTLY — the pure
// props->JSX component extracted from (api-key)/page.tsx for exactly this reason. `tableSlot` is
// a plain stub element here (not the real VirtualizedTable) — see the view's own frontmatter for
// why the real @tanstack/react-virtual-backed table cannot be imported from this vitest project.

const BASE_PAGE: ApiKeyPage = {
  content: [
    {
      id: 'key_1',
      prefix: 'ax_live_12',
      scope: 'READ',
      status: 'ACTIVE',
      createdAt: '2026-07-01T00:00:00Z',
      lastUsedAt: '2026-07-20T00:00:00Z',
      revokedAt: null,
    },
  ],
  totalElements: 1,
  totalPages: 1,
  page: 0,
  size: 50,
}

function baseProps(overrides: Partial<Parameters<typeof ApiKeyListView>[0]> = {}) {
  return {
    data: BASE_PAGE,
    error: null,
    isLoading: false,
    filters: {},
    onFilterChange: vi.fn(),
    onPageChange: vi.fn(),
    onCreate: vi.fn(),
    tableSlot: <div data-testid="stub-table">ax_live_12 row</div>,
    ...overrides,
  }
}

describe('ApiKeyListView — pure render of the admin API-key list (P2-42)', () => {
  it('renders the caller-supplied tableSlot and filter chips', () => {
    render(<ApiKeyListView {...baseProps()} />)
    expect(screen.getByTestId('stub-table')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Active' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Read' })).toBeInTheDocument()
  })

  it('clicking a status chip calls onFilterChange with the chosen value', () => {
    const onFilterChange = vi.fn()
    render(<ApiKeyListView {...baseProps({ onFilterChange })} />)
    fireEvent.click(screen.getByRole('button', { name: 'Active' }))
    expect(onFilterChange).toHaveBeenCalledWith('status', 'ACTIVE')
  })

  it('renders the empty state with a Create key action when there are no keys (null-safety branch)', () => {
    const onCreate = vi.fn()
    render(
      <ApiKeyListView
        {...baseProps({
          data: { ...BASE_PAGE, content: [], totalElements: 0 },
          onCreate,
        })}
      />,
    )
    expect(screen.getByText('No API keys yet')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Create key' }))
    expect(onCreate).toHaveBeenCalled()
  })

  it('renders the error state instead of tableSlot on error (null-safety branch)', () => {
    render(<ApiKeyListView {...baseProps({ error: new Error('boom') })} />)
    expect(screen.getByText('Failed to load API keys')).toBeInTheDocument()
    expect(screen.queryByTestId('stub-table')).not.toBeInTheDocument()
  })

  it('NON-VACUITY: a different tableSlot DOES change the rendered DOM — proves the assertions above are capable of going RED, not vacuously passing', () => {
    render(
      <ApiKeyListView
        {...baseProps({ tableSlot: <div data-testid="a-totally-different-slot">different</div> })}
      />,
    )
    expect(screen.getByTestId('a-totally-different-slot')).toBeInTheDocument()
    expect(screen.queryByTestId('stub-table')).not.toBeInTheDocument()
  })
})
