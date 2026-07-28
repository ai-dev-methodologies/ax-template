/// <reference types="@testing-library/jest-dom/vitest" />
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import ItemDetailView, {
  type Item,
} from '../../templates/L4/crud/app/(crud)/items/[id]/item-detail-view'

// BACKLOG P2-28 — FE render leg of the L4-page-render-testability closure (same class as
// frontend/tests/audit-log-redaction-render.vitest.tsx). Renders ItemDetailView DIRECTLY — the
// pure props->JSX component extracted from (crud)/items/[id]/page.tsx for exactly this reason
// (see that file's frontmatter). The page itself (useQuery) is NOT unit-renderable from this
// vitest project without a shared-config resolve.alias for the @tanstack/react-query bare
// specifier when imported from a templates/L4/... file living outside frontend/ — the same
// documented gap as cmdk (search-palette-hydration.spec.ts) and @tanstack/react-query
// (audit-log-redaction-render.vitest.tsx).

const BASE_ITEM: Item = {
  id: 'item_1',
  title: 'My First Item',
  createdBy: 'alice',
  createdAt: '2026-07-20T10:00:00Z',
}

describe('ItemDetailView — pure render of a crud item detail page (P2-28)', () => {
  it('renders the title and creator/date fields from the resolved item prop', () => {
    render(<ItemDetailView item={BASE_ITEM} />)

    // "My First Item" appears twice by design (the DetailPage <h1> heading AND
    // the Title field row in sectionsSlot) — assert the heading specifically,
    // plus that BOTH occurrences carry the resolved value (getAllByText).
    expect(screen.getByRole('heading', { name: 'My First Item' })).toBeInTheDocument()
    expect(screen.getAllByText('My First Item')).toHaveLength(2)
    expect(screen.getByText('alice')).toBeInTheDocument()
  })

  it('renders the description row only when description is present (null-safety branch)', () => {
    render(<ItemDetailView item={BASE_ITEM} />)
    expect(screen.queryByText('Description')).not.toBeInTheDocument()

    render(<ItemDetailView item={{ ...BASE_ITEM, description: 'a longer description' }} />)
    expect(screen.getByText('Description')).toBeInTheDocument()
    expect(screen.getByText('a longer description')).toBeInTheDocument()
  })

  it('renders the "Last updated" row only when updatedAt is present (null-safety branch)', () => {
    render(<ItemDetailView item={BASE_ITEM} />)
    expect(screen.queryByText('Last updated')).not.toBeInTheDocument()

    render(
      <ItemDetailView
        item={{ ...BASE_ITEM, updatedAt: '2026-07-21T11:00:00Z', updatedBy: 'bob' }}
      />,
    )
    expect(screen.getByText('Last updated')).toBeInTheDocument()
    expect(screen.getByText('bob')).toBeInTheDocument()
  })

  it('the Edit action link points at the item\'s own id', () => {
    render(<ItemDetailView item={BASE_ITEM} />)
    expect(screen.getByRole('link', { name: 'Edit' })).toHaveAttribute('href', '/items/item_1/edit')
  })

  it('NON-VACUITY: a different title DOES change the rendered DOM — proves the assertions above are capable of going RED, not vacuously passing', () => {
    render(<ItemDetailView item={{ ...BASE_ITEM, title: 'A totally different title' }} />)
    expect(screen.getByRole('heading', { name: 'A totally different title' })).toBeInTheDocument()
    expect(screen.queryByText('My First Item')).not.toBeInTheDocument()
  })
})
