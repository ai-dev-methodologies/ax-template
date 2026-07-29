/*
---
template_id: L4/billing/app/(billing)/invoices/page
layer: L4
domain: billing
domain_mode: full_trio
backend_operation_id: listInvoices
evidence:
  - source_type: internal
    rationale: "L4 billing — invoice list page, operationId=listInvoices."
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/payment]
---
*/
import React from 'react'
import BillingInvoicesView from './billing-invoices-view'

/**
 * InvoicesPage — displays billing invoices for the authenticated user.
 *
 * Fork: replace the empty invoices array with a real server-side data fetch
 * via GET /api/billing/invoices (operationId: listInvoices).
 */
export default function InvoicesPage() {
  return <BillingInvoicesView invoices={[]} />
}
