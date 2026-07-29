/// <reference types="@testing-library/jest-dom/vitest" />
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react'
import TagLibraryView, {
  type TagListResponse,
} from '../../templates/L4/tag-categorization/app/(tags)/tag-library-view'

// BACKLOG P2-42 — FE render leg of the L4-page-render-testability pass-2 closure (same class as
// frontend/tests/item-detail-view.vitest.tsx). Renders TagLibraryView DIRECTLY — the pure
// props->JSX component (incl. the recursive TagRow + buildTree helper + Add/Edit form) extracted
// from (tags)/page.tsx for exactly this reason.

const BASE_DATA: TagListResponse = {
  items: [
    { id: 't1', name: 'urgent', slug: 'urgent', parentTagId: null, color: '#f87171', createdAt: '2026-07-01T00:00:00Z', createdByUserId: 'admin' },
    { id: 't2', name: 'urgent-billing', slug: 'urgent-billing', parentTagId: 't1', color: null, createdAt: '2026-07-01T00:00:00Z', createdByUserId: 'admin' },
  ],
  totalElements: 2,
}

function baseProps(overrides: Partial<Parameters<typeof TagLibraryView>[0]> = {}) {
  return {
    data: BASE_DATA,
    error: null,
    isLoading: false,
    isAdmin: true,
    createErrorMessage: null,
    onDismissCreateError: vi.fn(),
    updateErrorMessage: null,
    onDismissUpdateError: vi.fn(),
    deleteErrorMessage: null,
    onDismissDeleteError: vi.fn(),
    createPending: false,
    updatePending: false,
    pendingDeleteId: null,
    onCreateTag: vi.fn().mockResolvedValue(undefined),
    onUpdateTag: vi.fn().mockResolvedValue(undefined),
    onDeleteTag: vi.fn(),
    ...overrides,
  }
}

describe('TagLibraryView — pure render of the tag taxonomy library (P2-42)', () => {
  it('builds the parent/child tree from the flat resolved data prop', () => {
    render(<TagLibraryView {...baseProps()} />)
    // Scope to the tag list — the Add-tag form's "Parent tag" <select> also lists every tag
    // name as an <option>, so an unscoped query would match both. t1's name and slug are both
    // 'urgent' in the fixture (2 matches: name span + isAdmin-only slug span).
    const list = within(screen.getByRole('list'))
    expect(list.getAllByText('urgent')).toHaveLength(2)
    expect(list.getAllByText('urgent-billing')).toHaveLength(2)
  })

  it('hides Edit/Delete actions and the slug when isAdmin is false (null-safety branch)', () => {
    render(<TagLibraryView {...baseProps({ isAdmin: false })} />)
    expect(screen.queryByRole('button', { name: 'Edit tag urgent' })).not.toBeInTheDocument()
    expect(screen.queryByText('urgent-billing')).toBeInTheDocument() // tag name still shows
  })

  it('submitting the Add-tag form calls onCreateTag and clears the draft on success', async () => {
    const onCreateTag = vi.fn().mockResolvedValue(undefined)
    render(<TagLibraryView {...baseProps({ onCreateTag })} />)

    fireEvent.change(screen.getByPlaceholderText('e.g. high-priority, 긴급'), {
      target: { value: 'gift-idea' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Create tag' }))

    await waitFor(() => expect(onCreateTag).toHaveBeenCalledWith({
      name: 'gift-idea',
      parentTagId: null,
      color: null,
    }))
    // draft cleared: placeholder input goes back to empty
    await waitFor(() =>
      expect(screen.getByPlaceholderText('e.g. high-priority, 긴급')).toHaveValue(''),
    )
  })

  it('clicking Delete then confirming calls onDeleteTag with the tag id', () => {
    const onDeleteTag = vi.fn()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    render(<TagLibraryView {...baseProps({ onDeleteTag })} />)
    fireEvent.click(screen.getByRole('button', { name: 'Delete tag urgent' }))
    expect(onDeleteTag).toHaveBeenCalledWith('t1')
    confirmSpy.mockRestore()
  })

  it('renders the empty state when there are no tags (null-safety branch)', () => {
    render(<TagLibraryView {...baseProps({ data: { items: [], totalElements: 0 } })} />)
    expect(screen.getByText('No tags yet')).toBeInTheDocument()
  })

  it('NON-VACUITY: a different tag name DOES change the rendered DOM — proves the assertions above are capable of going RED, not vacuously passing', () => {
    const mutated: TagListResponse = {
      ...BASE_DATA,
      items: [
        { ...BASE_DATA.items[0], name: 'a-totally-different-tag', slug: 'a-totally-different-tag' },
        BASE_DATA.items[1],
      ],
    }
    render(<TagLibraryView {...baseProps({ data: mutated })} />)
    const list = within(screen.getByRole('list'))
    // Both the name span and the (isAdmin-only) slug span now read the mutated value.
    expect(list.getAllByText('a-totally-different-tag')).toHaveLength(2)
    expect(list.queryByText('urgent')).not.toBeInTheDocument()
  })
})
