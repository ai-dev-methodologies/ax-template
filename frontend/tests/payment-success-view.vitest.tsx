/// <reference types="@testing-library/jest-dom/vitest" />
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import PaymentSuccessView, {
  type PaymentResponse,
} from '../../templates/L4/payment/app/(payment)/success/[orderId]/payment-success-view'

// BACKLOG P2-28 — FE render leg of the L4-page-render-testability closure (same class as
// frontend/tests/audit-log-redaction-render.vitest.tsx). Renders PaymentSuccessView DIRECTLY —
// the pure props->JSX component extracted from (payment)/success/[orderId]/page.tsx for exactly
// this reason (see that file's frontmatter). The page itself (useQuery) is NOT unit-renderable
// from this vitest project without a shared-config resolve.alias for the @tanstack/react-query
// bare specifier when imported from a templates/L4/... file living outside frontend/ — the same
// documented gap as cmdk (search-palette-hydration.spec.ts) and @tanstack/react-query
// (audit-log-redaction-render.vitest.tsx). Editing vitest.config.ts is out of scope here (shared
// config).

const BASE_PAYMENT: PaymentResponse = {
  id: 'pay_1',
  orderId: 'order_abc123',
  status: 'COMPLETED',
  amount: 1099,
  currency: 'USD',
  createdAt: '2026-07-20T10:30:00Z',
}

describe('PaymentSuccessView — pure render of a completed payment receipt (P2-28)', () => {
  it('renders the receipt fields from the resolved payment prop', () => {
    render(<PaymentSuccessView payment={BASE_PAYMENT} />)

    expect(screen.getByText('Payment successful')).toBeInTheDocument()
    expect(screen.getByText('order_abc123')).toBeInTheDocument()
    expect(screen.getByText('COMPLETED')).toBeInTheDocument()
    // 1099 minor units USD -> $10.99 major, via the L0 money kit (never a raw / 100).
    expect(screen.getByText('$10.99')).toBeInTheDocument()
  })

  it('formats a zero-decimal currency (KRW) without injecting cents', () => {
    render(
      <PaymentSuccessView
        payment={{ ...BASE_PAYMENT, amount: 12900, currency: 'KRW' }}
      />,
    )
    // 12900 minor units KRW -> ₩12,900 (KRW has 0 minor digits; minor === major).
    expect(screen.getByText('₩12,900')).toBeInTheDocument()
    expect(screen.queryByText(/12,900\.00/)).not.toBeInTheDocument()
  })

  it('renders the provider reference row only when providerRef is present (null-safety branch)', () => {
    render(<PaymentSuccessView payment={BASE_PAYMENT} />)
    expect(screen.queryByText('Reference')).not.toBeInTheDocument()

    render(<PaymentSuccessView payment={{ ...BASE_PAYMENT, providerRef: 'ref_xyz' }} />)
    expect(screen.getByText('Reference')).toBeInTheDocument()
    expect(screen.getByText('ref_xyz')).toBeInTheDocument()
  })

  it('NON-VACUITY: a different orderId DOES change the rendered DOM — proves the assertions above are capable of going RED, not vacuously passing', () => {
    render(<PaymentSuccessView payment={{ ...BASE_PAYMENT, orderId: 'order_other' }} />)
    expect(screen.getByText('order_other')).toBeInTheDocument()
    expect(screen.queryByText('order_abc123')).not.toBeInTheDocument()
  })
})
