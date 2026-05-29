/*
---
template_id: L0/fork-receiver-kit/use-idempotency-key
layer: L0
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Stripe API — Idempotent Requests (client sends a stable Idempotency-Key; retries reuse it so the server dedups; a new logical request uses a new key)"
    url: "https://docs.stripe.com/api/idempotent_requests"
  - source_type: internal
    rationale: "FDW1 (frontend dogfood) rule-of-three: every persona improvised key generation for a generic create — junior used crypto.randomUUID in a ref, senior a useRef regenerated after success, staff reused the payment-flavored L2 idempotency-key-handler render-prop. The only catalog artifact was payment-scoped (L2/blocks/idempotency-key-handler, a render-prop). This is the domain-neutral HOOK form, pairing with backend common/IdempotencyKeyStore (idempotency-l0)."
imports_from: []
imports_forbidden: [L1, L2, L3, L4, app/, lib/]
---
*/
'use client'

import * as React from 'react'

/** UUID v4 where available, with a non-crypto fallback for old runtimes. */
function generateKey(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  // Fallback — not cryptographically strong, but unique enough for dedup keys.
  return 'idem-' + Math.abs(hash(String(performance.now()) + ':' + counter())).toString(36)
}

let _counter = 0
function counter(): number {
  _counter += 1
  return _counter
}
function hash(s: string): number {
  let h = 0
  for (let i = 0; i < s.length; i += 1) {
    h = (h * 31 + s.charCodeAt(i)) | 0
  }
  return h
}

export interface IdempotencyKey {
  /** Stable key for the CURRENT logical request — send as `Idempotency-Key`. */
  key: string
  /**
   * Start a NEW logical request (fresh key). Call this AFTER a mutation
   * SUCCEEDS, so the next create/submit is deduped independently.
   *
   * Do NOT call it before retrying a FAILED request — a retry must reuse the
   * same key so the server recognises it as the same operation and does not
   * double-apply it.
   */
  regenerate: () => void
}

/**
 * useIdempotencyKey — manage the lifecycle of a single `Idempotency-Key` for a
 * create/mutation form. The key is generated once on mount and is stable across
 * re-renders (and across retries); `regenerate()` mints a fresh one for the
 * next logical request. Pairs with backend `common/IdempotencyKeyStore`.
 *
 * @example
 *   const { key, regenerate } = useIdempotencyKey()
 *   async function onSubmit(values) {
 *     await api.create(values, { idempotencyKey: key })  // retries reuse `key`
 *     regenerate()                                        // next create is fresh
 *   }
 */
export function useIdempotencyKey(): IdempotencyKey {
  const [key, setKey] = React.useState<string>(() => generateKey())
  const regenerate = React.useCallback(() => setKey(generateKey()), [])
  return { key, regenerate }
}
