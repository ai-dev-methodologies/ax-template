/*
---
template_id: L4/email-outbox/app/use-caller-id
layer: L4
domain: email-outbox
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 email-outbox vertical — shared caller-id + role hook. Admin-only domain; useCallerRole gates the entire surface."
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API5:2023 BFLA"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa5-broken-function-level-authorization/"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
---
*/

let warnedCallerId = false
let warnedCallerRole = false

export function useCallerId(): string {
  if (typeof process !== 'undefined' && process.env?.NODE_ENV === 'production') {
    throw new Error('useCallerId: Identity provider not configured')
  }
  if (typeof console !== 'undefined' && !warnedCallerId) {
    warnedCallerId = true
    console.warn('[ax-template] useCallerId stub returning demo-user. Wire your real session before deploying.')
  }
  return 'demo-user'
}

export function useCallerRole(): 'admin' | 'user' {
  if (typeof process !== 'undefined' && process.env?.NODE_ENV === 'production') {
    throw new Error('useCallerRole: Identity provider not configured')
  }
  if (typeof console !== 'undefined' && !warnedCallerRole) {
    warnedCallerRole = true
    console.warn(
      '[ax-template] useCallerRole stub active. Wire your real RBAC source. ' +
        'Set NEXT_PUBLIC_DEV_AS_ADMIN=1 to exercise the admin path locally.',
    )
  }
  const devAsAdmin =
    typeof process !== 'undefined' && process.env?.NEXT_PUBLIC_DEV_AS_ADMIN === '1'
  return devAsAdmin ? 'admin' : 'user'
}
