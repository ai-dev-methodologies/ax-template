/// <reference types="@testing-library/jest-dom/vitest" />
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import BillingInvoicesView from '../../templates/L4/billing/app/(billing)/invoices/billing-invoices-view'
import type { InvoiceItem } from '../../templates/L2/blocks/invoice-list'

// BACKLOG P2-42 — FE render leg of the L4-page-render-testability pass-1 closure (same class as
// frontend/tests/item-detail-view.vitest.tsx). Renders BillingInvoicesView DIRECTLY — the pure
// props->JSX component extracted from (billing)/invoices/page.tsx for exactly this reason.

const BASE_INVOICE: InvoiceItem = {
  id: 'inv_1',
  subscriptionId: 'sub_1',
  amountDue: 990000,
  amountPaid: 990000,
  currency: 'KRW',
  status: 'PAID',
  issuedAt: '2026-07-01T00:00:00Z',
  paidAt: '2026-07-02T00:00:00Z',
  periodStart: '2026-07-01',
  periodEnd: '2026-07-31',
  providerInvoiceId: 'prov_1',
}

describe('BillingInvoicesView — pure render of the billing invoice list (P2-42)', () => {
  it('renders the resolved invoices prop as a table row', () => {
    render(<BillingInvoicesView invoices={[BASE_INVOICE]} />)
    expect(screen.getByRole('heading', { name: '청구서' })).toBeInTheDocument()
    expect(screen.getByText('2026-07-01 ~ 2026-07-31')).toBeInTheDocument()
    expect(screen.getByText('결제 완료')).toBeInTheDocument()
  })

  it('renders the empty state when invoices is an empty array (null-safety branch)', () => {
    render(<BillingInvoicesView invoices={[]} />)
    expect(screen.getByText('청구서가 없습니다.')).toBeInTheDocument()
  })

  it('NON-VACUITY: a different status DOES change the rendered DOM — proves the assertions above are capable of going RED, not vacuously passing', () => {
    render(<BillingInvoicesView invoices={[{ ...BASE_INVOICE, status: 'VOID' }]} />)
    expect(screen.getByText('무효')).toBeInTheDocument()
    expect(screen.queryByText('결제 완료')).not.toBeInTheDocument()
  })
})
