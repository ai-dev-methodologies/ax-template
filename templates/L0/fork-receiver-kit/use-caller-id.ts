/*
---
template_id: L0/fork-receiver-kit/use-caller-id
layer: L0
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API1:2023 BOLA (Broken Object Level Authorization)"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/"
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API5:2023 BFLA (Broken Function Level Authorization)"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa5-broken-function-level-authorization/"
  - source_type: internal
    rationale: "R53 fork-receiver-kit consolidation — canonical caller-id hooks pulled out of every L4 vertical (use-caller-id.ts existed inline in 7 L4 directories with subtle divergence). Shipping the kit lets a single fix (e.g. the production hard-stop, the dev-as-admin opt-in) propagate across every L4 instead of requiring ~20 hand-mirrored edits."
imports_from: []
imports_forbidden: [L1, L2, L3, L4, app/, lib/]
---
*/

// ─── module-scoped warn dedup flags ──────────────────────────────────────────
//
// Each stub fires a single console.warn per session so a fork-receiver who
// forgot to wire their session sees the issue once at boot, not on every
// render. Module-scoped because the stubs are pure functions — no React
// state, no ref needed.

let warnedCallerId = false
let warnedCallerRole = false

// ─── useCallerId ─────────────────────────────────────────────────────────────

/**
 * useCallerId — returns the caller's stable user id (e.g. JWT sub claim) as a
 * string.
 *
 * <h2>Production hard-stop</h2>
 * When NODE_ENV === 'production' and no real identity provider has replaced
 * this stub, the hook throws. A fork-receiver who ships ax-template to prod
 * without wiring session retrieval gets a loud, scrubbed error message — the
 * stub never silently returns a fake id in prod.
 *
 * <h2>Development behavior</h2>
 * In any non-production env the hook returns the literal "demo-user" so the
 * UI is usable while a fork-receiver hooks up real session retrieval. A
 * single console.warn on first call reminds them.
 *
 * <h2>Anchored rule</h2>
 * R47 rbac-stub-default-fail-closed — the stub MUST fail closed (throw) in
 * prod, not silently return an admin or a default user.
 */
export function useCallerId(): string {
  if (typeof process !== 'undefined' && process.env?.NODE_ENV === 'production') {
    throw new Error('useCallerId: Identity provider not configured')
  }
  if (typeof console !== 'undefined' && !warnedCallerId) {
    warnedCallerId = true
    console.warn(
      '[ax-template] useCallerId stub returning demo-user. ' +
        'Wire your real session before deploying.',
    )
  }
  return 'demo-user'
}

// ─── useCallerRole ───────────────────────────────────────────────────────────

/**
 * useCallerRole — returns 'admin' or 'user' for the current session.
 *
 * <h2>Production hard-stop</h2>
 * Same fail-closed behaviour as {@link useCallerId} — throws in prod when
 * the stub has not been replaced.
 *
 * <h2>Development behavior</h2>
 * Returns 'user' by default so admin-only paths are visibly gated even while
 * a fork-receiver is wiring up real RBAC. Setting
 * {@code NEXT_PUBLIC_DEV_AS_ADMIN=1} flips to 'admin' for local exercise of
 * admin surfaces (admin pages, ROLE_ADMIN-gated mutations).
 *
 * <h2>Anchored rule</h2>
 * R47 rbac-stub-default-fail-closed — the dev stub MUST default to the
 * lower-privilege role. Defaulting to 'admin' would mask BFLA gaps during
 * development.
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
    typeof process !== 'undefined' &&
    process.env?.NEXT_PUBLIC_DEV_AS_ADMIN === '1'
  return devAsAdmin ? 'admin' : 'user'
}

// ─── helpers ─────────────────────────────────────────────────────────────────

/**
 * normalizeUserId — trim + null-safe coercion. Returns '' when the input is
 * missing so callers do not need defensive checks at every comparison site.
 */
export function normalizeUserId(raw: string | null | undefined): string {
  return (raw ?? '').trim()
}

/**
 * sameUser — string-equality compare that treats blank ids as never matching.
 * Catches the "two anonymous callers look equal" pitfall — useful for
 * polymorphic ownership checks where a missing id MUST NOT match anything.
 *
 * <h2>Jurisdiction: DISPLAY only — NOT an authorization comparator (P3-98)</h2>
 * This helper TRIMS (via {@link normalizeUserId}), and its vitest contract pins that on
 * purpose: it exists to label identity for a human — a "you" chip on a timeline row, a
 * "you are the actor" marker in a feed — where normalizing a fork-receiver session hook's
 * padded id is a kindness with no authorization consequence.
 *
 * Backends do not trim. Every id comparison the reference workloads ENFORCE is a bare
 * {@code String.equals} (e.g. `ApprovalActionGuards.isAssignedApprover` / `isRequester`,
 * `ApprovalService.validateApprovers`'s self-approve check). So using this helper on an
 * authorization path — anything that gates a mutation, or renders a backend verdict as
 * fact — offers or withholds an action the server decides differently. That is the
 * client/server identity divergence class P3-76 closed inside
 * `authorized-actions.ts` and P3-98 closed one layer up, in the L4 pages.
 *
 * For those comparisons import the exact mirror instead:
 * {@code import { sameId } from './authorized-actions'}. Whether `ALICE` is `alice`, or
 * ` bob ` is `bob`, is the identity provider's answer to give; a fork-receiver whose
 * backend really does normalize should change BOTH sides, not fold on the client only.
 */
export function sameUser(
  a: string | null | undefined,
  b: string | null | undefined,
): boolean {
  const na = normalizeUserId(a)
  const nb = normalizeUserId(b)
  if (na === '' || nb === '') return false
  return na === nb
}
