/*
---
template_id: L4/favorites-bookmarks/app/use-caller-id
layer: L4
domain: favorites-bookmarks
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 favorites-bookmarks vertical — shared caller-id hook with a production hard-stop. Same shape as L4/approval-workflow and L4/activity-feed (R43/R44 closure)."
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API1:2023 BOLA"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
---
*/

let warnedCallerId = false

export function useCallerId(): string {
  if (typeof process !== 'undefined' && process.env?.NODE_ENV === 'production') {
    throw new Error('useCallerId: Identity provider not configured')
  }
  if (typeof console !== 'undefined' && !warnedCallerId) {
    warnedCallerId = true
    console.warn(
      '[ax-template] useCallerId stub returning demo-user. Wire your real session before deploying.',
    )
  }
  return 'demo-user'
}

export function normalizeUserId(raw: string | null | undefined): string {
  return (raw ?? '').trim()
}

export function sameUser(a: string | null | undefined, b: string | null | undefined): boolean {
  const na = normalizeUserId(a)
  const nb = normalizeUserId(b)
  if (na === '' || nb === '') return false
  return na === nb
}
