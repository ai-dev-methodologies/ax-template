/*
---
template_id: L4/approval-workflow/app/use-caller-id
layer: L4
domain: approval-workflow
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 approval-workflow vertical — shared caller-id hook with a production hard-stop. Extracted to a single module so a fork-receiver replaces it in exactly one place (R43 iter3 / P1-iter2-N7 / P1-F1)."
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
 *
 * R43 iter3 (P1-iter2-N7 / P1-F1):
 *   This file replaces the inline `useCallerId` previously duplicated in
 *   `app/(approvals)/new/page.tsx` and `app/(approvals)/[id]/page.tsx`.
 *   The self-approval guard relies on every page agreeing on the caller
 *   id — putting both copies behind one symbol prevents drift.
 *
 * Replacement example (next-auth):
 *   export function useCallerId(): string {
 *     const { data: session } = useSession()
 *     const id = session?.user?.id
 *     if (!id) throw new Error('Unauthenticated')
 *     return id
 *   }
 */
export function useCallerId(): string {
  if (typeof process !== 'undefined' && process.env?.NODE_ENV === 'production') {
    throw new Error(
      'L4/approval-workflow: callerId stub is dev-only. Replace useCallerId() (templates/L4/approval-workflow/app/use-caller-id.ts) with your real session hook before deploying.',
    )
  }
  return 'demo-user'
}

/**
 * normalizeUserId — canonicalize approver/caller user-ids so equality
 * comparisons agree across pages.
 *
 * R43 iter3 (P2-iter2-N8): the /new page trims approver inputs but the
 * /[id] page used strict === against the raw stored value. A backend
 * that doesn't normalize whitespace would silently drop a CFO out of
 * their own action panel. Centralizing the rule here makes the
 * contract explicit.
 */
export function normalizeUserId(raw: string | null | undefined): string {
  return (raw ?? '').trim()
}

export function sameUser(a: string | null | undefined, b: string | null | undefined): boolean {
  const na = normalizeUserId(a)
  const nb = normalizeUserId(b)
  if (na === '' || nb === '') return false
  return na === nb
}
