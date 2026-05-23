/*
---
template_id: L4/activity-feed/app/use-caller-id
layer: L4
domain: activity-feed
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 activity-feed vertical — shared caller-id hook with a production hard-stop. Same shape as L4/approval-workflow's hook (R43 closure) so fork-receivers learn one pattern."
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API1:2023 BOLA"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
---
*/

/**
 * useCallerId — single source of truth for the calling user id.
 *
 * Fork-receivers MUST replace this hook with their real session source
 * before deploying. The stub returns `'demo-user'` only in development
 * and throws in production so a missed integration cannot ship.
 */
export function useCallerId(): string {
  if (typeof process !== 'undefined' && process.env?.NODE_ENV === 'production') {
    // R44 iter2 (P2-F6): generic message — internal repo path and stub
    // identifier are scrubbed so a Sentry/DataDog dispatch does not
    // anchor 'demo-user' or 'templates/L4/...' in the vendor's trail.
    throw new Error('Identity provider not configured')
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
