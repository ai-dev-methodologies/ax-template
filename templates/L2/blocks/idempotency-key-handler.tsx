/*
---
template_id: L2/blocks/idempotency-key-handler
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Stripe API — Idempotent Requests"
    url: "https://stripe.com/docs/api/idempotent_requests"
  - source_type: internal
    rationale: "L2 payment block — generates/regenerates idempotency keys; exposes key via render prop for L4 usage."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/payment/]
---
*/
import * as React from 'react'

/** Generates a UUID-v4-like idempotency key */
function generateKey(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID()
  }
  // Fallback for environments without crypto.randomUUID
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`
}

export interface IdempotencyKeyHandlerProps {
  /**
   * Render prop — receives the current idempotency key and a regenerate function.
   * L4 passes the key with each payment mutation request.
   *
   * @param idempotencyKey - Current key (regenerated after each successful submit)
   * @param regenerate     - Call to get a fresh key before retrying a failed request
   */
  children: (idempotencyKey: string, regenerate: () => void) => React.ReactNode
}

/**
 * IdempotencyKeyHandler — L2 payment block.
 *
 * Manages the lifecycle of a payment idempotency key.
 * Key is stable for the component lifetime; call `regenerate()` after a
 * successful payment (or before retrying a failed one) to get a new key.
 *
 * L4 usage:
 *   <IdempotencyKeyHandler>
 *     {(key, regenerate) => (
 *       <PaymentCheckoutForm
 *         onSubmit={async (values) => {
 *           await payAction({ ...values, idempotencyKey: key })
 *           regenerate() // next submit gets a fresh key
 *         }}
 *       />
 *     )}
 *   </IdempotencyKeyHandler>
 */
export default function IdempotencyKeyHandler({ children }: IdempotencyKeyHandlerProps) {
  const [key, setKey] = React.useState<string>(generateKey)

  function regenerate() {
    setKey(generateKey())
  }

  return <>{children(key, regenerate)}</>
}
