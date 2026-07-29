/// <reference types="@testing-library/jest-dom/vitest" />
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, within } from '@testing-library/react'
import FavoritesListView, {
  type FavoriteListResponse,
} from '../../templates/L4/favorites-bookmarks/app/(favorites)/favorites-list-view'

// BACKLOG P2-42 — FE render leg of the L4-page-render-testability pass-1 closure (same class as
// frontend/tests/item-detail-view.vitest.tsx). Renders FavoritesListView DIRECTLY — the pure
// props->JSX component extracted from (favorites)/page.tsx for exactly this reason. `renderCount`
// is a plain stub here (not the real RowCount, which has its OWN useQuery — see the view's own
// frontmatter for why that lazy-reveal widget cannot be defined inside this file).

const BASE_DATA: FavoriteListResponse = {
  items: [
    { id: 'fav_1', entityType: 'product', entityId: 'p_1', note: null, favoritedAt: '2026-07-20T00:00:00Z' },
    { id: 'fav_2', entityType: 'product', entityId: 'p_2', note: 'gift idea', favoritedAt: '2026-07-01T00:00:00Z' },
  ],
  totalElements: 2,
}

function baseProps(overrides: Partial<Parameters<typeof FavoritesListView>[0]> = {}) {
  return {
    data: BASE_DATA,
    error: null,
    isLoading: false,
    quotaErrorMessage: null,
    onDismissQuotaError: vi.fn(),
    addOtherErrorMessage: null,
    onDismissAddError: vi.fn(),
    removeErrorMessage: null,
    onDismissRemoveError: vi.fn(),
    formType: '',
    formId: '',
    formNote: '',
    onFormTypeChange: vi.fn(),
    onFormIdChange: vi.fn(),
    onFormNoteChange: vi.fn(),
    onSubmitAdd: vi.fn((e: React.FormEvent) => e.preventDefault()),
    addPending: false,
    onRequestRemove: vi.fn(),
    removePending: false,
    confirmingRemove: null,
    onCancelConfirmRemove: vi.fn(),
    onConfirmRemove: vi.fn(),
    renderCount: (entityType: string, entityId: string) => (
      <span data-testid={`count-${entityType}-${entityId}`}>stub count</span>
    ),
    ...overrides,
  }
}

describe('FavoritesListView — pure render of the caller favorites list (P2-42)', () => {
  it('renders rows from the resolved data prop, including the caller-supplied renderCount slot', () => {
    render(<FavoritesListView {...baseProps()} />)
    expect(screen.getByText('p_1')).toBeInTheDocument()
    expect(screen.getByText('gift idea')).toBeInTheDocument()
    expect(screen.getByTestId('count-product-p_1')).toBeInTheDocument()
  })

  it('renders the empty state when data has zero rows (null-safety branch)', () => {
    render(<FavoritesListView {...baseProps({ data: { items: [], totalElements: 0 } })} />)
    expect(screen.getByText('No favorites yet')).toBeInTheDocument()
  })

  it('clicking Remove on a note-less favorite calls onRequestRemove directly (no confirm gate needed by the view itself)', () => {
    const onRequestRemove = vi.fn()
    render(<FavoritesListView {...baseProps({ onRequestRemove })} />)
    fireEvent.click(screen.getByLabelText('Remove product/p_1 from favorites'))
    expect(onRequestRemove).toHaveBeenCalledWith(BASE_DATA.items[0])
  })

  it('renders the confirm dialog when confirmingRemove is set by the caller, and Confirm calls onConfirmRemove', () => {
    const onConfirmRemove = vi.fn()
    render(
      <FavoritesListView
        {...baseProps({
          confirmingRemove: { entityType: 'product', entityId: 'p_2', note: 'gift idea' },
          onConfirmRemove,
        })}
      />,
    )
    const dialog = screen.getByRole('alertdialog')
    expect(within(dialog).getByText(/gift idea/)).toBeInTheDocument()
    fireEvent.click(within(dialog).getByRole('button', { name: 'Remove' }))
    expect(onConfirmRemove).toHaveBeenCalled()
  })

  it('renders the quota banner only when quotaErrorMessage is set (null-safety branch)', () => {
    render(<FavoritesListView {...baseProps()} />)
    expect(screen.queryByText('Favorite cap reached')).not.toBeInTheDocument()

    render(<FavoritesListView {...baseProps({ quotaErrorMessage: 'cap reached, remove one first' })} />)
    expect(screen.getByText('Favorite cap reached')).toBeInTheDocument()
  })

  it('NON-VACUITY: a different entityId DOES change the rendered DOM — proves the assertions above are capable of going RED, not vacuously passing', () => {
    const mutated: FavoriteListResponse = {
      ...BASE_DATA,
      items: [{ ...BASE_DATA.items[0], entityId: 'a-totally-different-id' }, BASE_DATA.items[1]],
    }
    render(<FavoritesListView {...baseProps({ data: mutated })} />)
    expect(screen.getByText('a-totally-different-id')).toBeInTheDocument()
    expect(screen.queryByText('p_1')).not.toBeInTheDocument()
  })
})
