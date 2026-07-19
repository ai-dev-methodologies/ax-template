/*
---
template_id: S2.AUDIT-PII.XB/react/clean/WebhookDeliveryStatus
layer: scenario-fixture
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "CLEAN rewrite — surfaces the BE WEBHOOK-SIGN verification outcome (specs/webhook-signing-l0.yaml, WebhookSigningException.Kind) to the user instead of a static success pill. Any non-VERIFIED status renders a distinct role=alert state with the literal text 'Signature could not be verified', naming the specific reason where the BE gives one (malformed / stale timestamp / bad signature / replayed event), mirroring the design-token + role=status/alert convention of templates/L2/blocks/status-badge.tsx and rate-limit-banner.tsx."
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

const FAILURE_REASON: Record<Exclude<SignatureVerificationStatus, 'VERIFIED'>, string> = {
  WEBHOOK_SIGNATURE_MALFORMED: 'the signature header was malformed',
  WEBHOOK_TIMESTAMP_STALE: 'the delivery timestamp was outside the allowed window',
  WEBHOOK_SIGNATURE_INVALID: 'the signature did not match',
  WEBHOOK_EVENT_REPLAYED: 'this event id was already delivered (possible replay)',
}

/**
 * CLEAN: branches on signatureStatus. VERIFIED renders the success pill;
 * every other status renders a distinct role="alert" warning naming the
 * failure and telling the user the signature could not be verified — the
 * BE-only WEBHOOK-SIGN outcome is no longer invisible at the FE boundary.
 */
export function WebhookDeliveryStatus({ deliveryId, signatureStatus }: WebhookDeliveryStatusProps) {
  if (signatureStatus === 'VERIFIED') {
    return (
      <span data-delivery-id={deliveryId} className="ax-status-pill ax-status-pill--success">
        Delivered
      </span>
    )
  }

  const reason = FAILURE_REASON[signatureStatus]
  return (
    <span
      role="alert"
      data-delivery-id={deliveryId}
      className="ax-status-pill ax-status-pill--danger"
    >
      Signature could not be verified — {reason}
    </span>
  )
}
