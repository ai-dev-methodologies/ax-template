/// <reference types="@testing-library/jest-dom/vitest" />
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import FeatureFlagsListView, {
  type FeatureFlag,
} from '../../templates/L4/feature-flags/app/(admin)/feature-flags/feature-flags-list-view'

// BACKLOG P2-42 — FE render leg of the L4-page-render-testability pass-1 closure (same class as
// frontend/tests/item-detail-view.vitest.tsx). Renders FeatureFlagsListView DIRECTLY — the pure
// props->JSX component extracted from (admin)/feature-flags/page.tsx (an async Server Component,
// not renderable directly by @testing-library/react) for exactly this reason.

const BASE_FLAG: FeatureFlag = {
  name: 'new-checkout',
  enabled: true,
  description: 'Enables the redesigned checkout flow',
  updatedAt: '2026-07-01T00:00:00Z',
}

describe('FeatureFlagsListView — pure render of the admin feature-flags list (P2-42)', () => {
  it('renders the resolved flags prop as a table row', () => {
    render(<FeatureFlagsListView flags={[BASE_FLAG]} totalElements={1} apiBase="" />)
    expect(screen.getByText('new-checkout')).toBeInTheDocument()
    expect(screen.getByText('Enables the redesigned checkout flow')).toBeInTheDocument()
    expect(screen.getByText('1 flag(s) defined')).toBeInTheDocument()
  })

  it('renders "No flags defined" when flags is an empty array (null-safety branch)', () => {
    render(<FeatureFlagsListView flags={[]} totalElements={0} apiBase="" />)
    expect(screen.getByText('No flags defined.')).toBeInTheDocument()
  })

  it('renders "—" for a null description (null-safety branch)', () => {
    render(
      <FeatureFlagsListView
        flags={[{ ...BASE_FLAG, description: null }]}
        totalElements={1}
        apiBase=""
      />,
    )
    expect(screen.getByText('—')).toBeInTheDocument()
  })

  it('NON-VACUITY: a different flag name DOES change the rendered DOM — proves the assertions above are capable of going RED, not vacuously passing', () => {
    render(
      <FeatureFlagsListView
        flags={[{ ...BASE_FLAG, name: 'a-totally-different-flag' }]}
        totalElements={1}
        apiBase=""
      />,
    )
    expect(screen.getByText('a-totally-different-flag')).toBeInTheDocument()
    expect(screen.queryByText('new-checkout')).not.toBeInTheDocument()
  })
})
