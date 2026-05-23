/*
---
template_id: L4/tag-categorization/app/use-caller-id
layer: L4
domain: tag-categorization
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 tag-categorization vertical — shared caller-id hook with a production hard-stop and a parallel admin-role hook. Establishes the role-gating surface that ROLE_ADMIN-only endpoints (POST/PUT/DELETE /api/tags) require."
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API1:2023 BOLA + API5:2023 BFLA"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa5-broken-function-level-authorization/"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
---
*/

// R45 iter2 (F12 low): one-time dev console warning so a fork-receiver
// who copies the file and forgets to wire useCallerId/useCallerRole sees
// the gap loudly without needing to read source comments.
let warnedCallerId = false
let warnedCallerRole = false

export function useCallerId(): string {
  if (typeof process !== 'undefined' && process.env?.NODE_ENV === 'production') {
    // R44 lesson (P2-F6): generic message; no internal path leak to Sentry/DataDog.
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

/**
 * useCallerRole — admin/user role for the calling user.
 *
 * Tag definition CRUD (POST/PUT/DELETE /api/tags) is gated by
 * ROLE_ADMIN server-side. The UI mirrors that gate so non-admins
 * see read-only views instead of buttons that would 403.
 *
 * R45 iter2 (F2 high): the dev stub defaults to **'user'** (not admin).
 * Principle-of-least-privilege: a fork-receiver who forgets to wire the
 * real RBAC source gets a read-only library, not an admin-by-default
 * surface. To exercise the admin path locally, set the env override
 * `NEXT_PUBLIC_DEV_AS_ADMIN=1` — that explicit opt-in is the dev
 * affordance for catalog-trio exploration.
 *
 * Fork-receivers MUST replace this with their real role source
 * (JWT claim, session, RBAC service). The production hard-stop throws
 * if NODE_ENV=production with no replacement so a missed integration
 * cannot ship.
 */
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

export function normalizeUserId(raw: string | null | undefined): string {
  return (raw ?? '').trim()
}

export function sameUser(a: string | null | undefined, b: string | null | undefined): boolean {
  const na = normalizeUserId(a)
  const nb = normalizeUserId(b)
  if (na === '' || nb === '') return false
  return na === nb
}
