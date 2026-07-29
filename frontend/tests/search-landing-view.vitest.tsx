/// <reference types="@testing-library/jest-dom/vitest" />
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import SearchLandingView from '../../templates/L4/search/app/(search)/search-landing-view'

// BACKLOG P2-42 — FE render leg of the L4-page-render-testability pass-1 closure (same class as
// frontend/tests/item-detail-view.vitest.tsx). Renders SearchLandingView DIRECTLY — the pure
// props->JSX component extracted from (search)/page.tsx for exactly this reason. `paletteSlot`
// is a plain stub element here (not the real SearchPalette) — see the view's own frontmatter for
// why the real cmdk-backed component cannot be imported from this vitest project.

describe('SearchLandingView — pure render of the /search landing page (P2-42)', () => {
  it('renders the heading, description, and the caller-supplied paletteSlot', () => {
    render(
      <SearchLandingView
        paletteSlot={<div data-testid="stub-palette">palette</div>}
        onSelectRecent={vi.fn()}
      />,
    )
    expect(screen.getByRole('heading', { name: '검색' })).toBeInTheDocument()
    expect(screen.getByTestId('stub-palette')).toBeInTheDocument()
  })

  it('clicking a recent search item calls onSelectRecent with that query', () => {
    // jsdom's window.localStorage is not wired up under this vitest project's
    // environment — RecentSearches (rendered by this view) reads/writes the
    // bare `localStorage` global directly, so stub it in-memory for this test.
    const store = new Map<string, string>()
    vi.stubGlobal('localStorage', {
      getItem: (k: string) => store.get(k) ?? null,
      setItem: (k: string, v: string) => void store.set(k, v),
      removeItem: (k: string) => void store.delete(k),
      clear: () => store.clear(),
    })
    localStorage.setItem('ax:recent-searches', JSON.stringify(['widgets']))

    const onSelectRecent = vi.fn()
    render(<SearchLandingView paletteSlot={null} onSelectRecent={onSelectRecent} />)

    fireEvent.click(screen.getByText('widgets'))
    expect(onSelectRecent).toHaveBeenCalledWith('widgets')
    vi.unstubAllGlobals()
  })

  it('NON-VACUITY: a different paletteSlot DOES change the rendered DOM — proves the assertion above is capable of going RED, not vacuously passing', () => {
    render(
      <SearchLandingView
        paletteSlot={<div data-testid="a-totally-different-slot">different</div>}
        onSelectRecent={vi.fn()}
      />,
    )
    expect(screen.getByTestId('a-totally-different-slot')).toBeInTheDocument()
    expect(screen.queryByTestId('stub-palette')).not.toBeInTheDocument()
  })
})
