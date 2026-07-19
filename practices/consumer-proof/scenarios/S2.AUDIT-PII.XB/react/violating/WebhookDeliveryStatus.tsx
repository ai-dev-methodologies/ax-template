/*
---
template_id: S2.AUDIT-PII.XB/react/violating/WebhookDeliveryStatus
layer: scenario-fixture
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "VIOLATING fixture for the dogfood cell's ADDITIONAL REQUIREMENT — a FE surface for inbound webhook signature-verification status, mirroring BE WEBHOOK-SIGN (specs/webhook-signing-l0.yaml, WebhookSigningException.Kind: WEBHOOK_SIGNATURE_MALFORMED / WEBHOOK_TIMESTAMP_STALE / WEBHOOK_SIGNATURE_INVALID / WEBHOOK_EVENT_REPLAYED). This is the realistic AI-generated shape: the component accepts the signatureStatus field returned by the API but renders the SAME 'Delivered' success UI no matter what it is — a fail-open UX bug. The user never learns that verification actually failed."
dependencies: []
imports_from: []
imports_forbidden: [L4]
---
*/
'use client'

import * as React from 'react'

export type SignatureVerificationStatus =
  | 'VERIFIED'
  | 'WEBHOOK_SIGNATURE_MALFORMED'
  | 'WEBHOOK_TIMESTAMP_STALE'
  | 'WEBHOOK_SIGNATURE_INVALID'
  | 'WEBHOOK_EVENT_REPLAYED'

export interface WebhookDeliveryStatusProps {
  deliveryId: string
  signatureStatus: SignatureVerificationStatus
}

/**
 * VIOLATING: ignores signatureStatus entirely — always renders the generic
 * "Delivered" pill regardless of whether BE verification actually passed.
 * A REPLAYED or SIGNATURE_INVALID delivery looks identical to a VERIFIED
 * one to the admin reading this list.
 */
export function WebhookDeliveryStatus({ deliveryId, signatureStatus }: WebhookDeliveryStatusProps) {
  // signatureStatus is accepted but never inspected — the badge is static.
  void signatureStatus
  return (
    <span data-delivery-id={deliveryId} className="ax-status-pill ax-status-pill--success">
      Delivered
    </span>
  )
}
