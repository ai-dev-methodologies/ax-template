/*
---
template_id: L4/billing/app/(billing)/invoices/billing-invoices-view
layer: L4
domain: billing
domain_mode: full_trio
evidence:
  - source_type: internal
    rationale: "Pure presentational extraction from (billing)/invoices/page.tsx (BACKLOG P2-42
      render-testability pass-1 closure — same class as (crud)/items/[id]/item-detail-view.tsx):
      splitting the resolved-invoices->JSX render surface into its own file makes it renderable
      from a vitest that imports this file directly from outside frontend/, independent of
      whatever data-fetch orchestration the page.tsx grows when the fork wires the real
      GET /api/billing/invoices (listInvoices) call in for the current static empty array.
      templates/L2/blocks/invoice-list is safe to import here (React-only, zero external-npm
      deps)."
---
*/
import * as React from 'react'
import { InvoiceList, type InvoiceItem } from 'templates/L2/blocks/invoice-list'

// ─── component ──────────────────────────────────────────────────────────────

/**
 * BillingInvoicesView — pure presentational render of the billing invoice list.
 *
 * Deliberately has ZERO data-fetching dependencies — the caller
 * (`(billing)/invoices/page.tsx`) owns the eventual GET /api/billing/invoices
 * orchestration and passes the resolved `invoices` array in. This keeps the
 * component a plain props -> JSX function, which is what makes it renderable
 * in a unit test without a QueryClientProvider.
 */
export default function BillingInvoicesView({ invoices }: { invoices: InvoiceItem[] }) {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">청구서</h1>
      <InvoiceList invoices={invoices} />
    </div>
  )
}
