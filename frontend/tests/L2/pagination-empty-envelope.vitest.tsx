/**
 * Codex wave-2 finding 6 — canonical EMPTY PageEnvelope render regression.
 *
 * Pre-fix defect (templates/L2/blocks/pagination.tsx):
 *   `totalPages = totalPagesProp ?? Math.max(1, Math.ceil(total / pageSize))`
 *   only clamped the CLIENT-DERIVED branch to a minimum of 1. When the
 *   BE-authoritative `totalPages` prop was supplied (via
 *   `pageEnvelopeToPaginationProps`) and equal to 0 — the canonical empty
 *   envelope `{ page: 0, pageSize: N, totalElements: 0, totalPages: 0,
 *   hasMore: false }` — it passed straight through unclamped, rendering the
 *   contradictory "1 / 0" (display page 1 of a claimed 0 total pages).
 *
 * Fix: `Math.max(1, totalPagesProp ?? Math.ceil(total / pageSize))` clamps
 * BOTH branches, so the empty envelope renders "1 / 1" — consistent with the
 * derived branch's pre-existing convention for an empty `total`.
 */
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import Pagination, { pageEnvelopeToPaginationProps } from '../../../templates/L2/blocks/pagination'

describe('Pagination — canonical empty PageEnvelope (codex wave-2 finding 6)', () => {
  it('renders "1 / 1", not "1 / 0", for an empty envelope (totalElements: 0, totalPages: 0)', () => {
    const props = pageEnvelopeToPaginationProps(
      { page: 0, pageSize: 20, totalElements: 0, totalPages: 0, hasMore: false },
      () => {},
    )

    render(<Pagination {...props} />)

    // Page indicator reads "1 / 1" — never the contradictory "1 / 0".
    expect(screen.getByText('1', { selector: '[aria-current="page"]' })).toBeInTheDocument()
    expect(screen.getByText('/ 1')).toBeInTheDocument()
    expect(screen.queryByText('/ 0')).not.toBeInTheDocument()

    // "No results" copy for the zero-total row count, and Next stays disabled
    // (hasMore: false) — the empty state is otherwise unchanged by the fix.
    expect(screen.getByText('No results')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Next page' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Previous page' })).toBeDisabled()
  })

  it('leaves a non-empty envelope unaffected — still renders "1 / 3"', () => {
    const props = pageEnvelopeToPaginationProps(
      { page: 0, pageSize: 10, totalElements: 25, totalPages: 3, hasMore: true },
      () => {},
    )

    render(<Pagination {...props} />)

    expect(screen.getByText('1', { selector: '[aria-current="page"]' })).toBeInTheDocument()
    expect(screen.getByText('/ 3')).toBeInTheDocument()
    expect(screen.getByText('1–10 of 25')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Next page' })).not.toBeDisabled()
  })

  it('leaves the client-derived (no BE totalPages prop) empty case unaffected — still "1 / 1"', () => {
    render(<Pagination page={1} pageSize={20} total={0} onPageChange={() => {}} />)

    expect(screen.getByText('/ 1')).toBeInTheDocument()
    expect(screen.getByText('No results')).toBeInTheDocument()
  })
})
