/*
---
template_id: L4/billing/app/(billing)/subscriptions/page
layer: L4
domain: billing
domain_mode: full_trio
backend_operation_id: listSubscriptions
evidence:
  - source_type: internal
    rationale: "L4 billing — subscription list page, operationId=listSubscriptions."
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/payment]
---
*/
import React from 'react'
import BillingHistory from '@/templates/L2/blocks/billing-history'

/**
 * SubscriptionsPage — lists the user's billing subscriptions.
 *
 * Fork: replace the static empty state with real data fetch via
 * GET /api/subscriptions (operationId: listSubscriptions).
 */
export default function SubscriptionsPage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">구독 관리</h1>
      <BillingHistory events={[]} />
    </div>
  )
}
