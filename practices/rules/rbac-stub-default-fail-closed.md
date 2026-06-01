---
title: RBAC role stub MUST default to least-privilege role — never 'admin' in dev
impact: HIGH
impactDescription: "A stub returning 'admin' by default exposes admin UI on every staging/preview deployment where the fork-receiver forgot to wire the real role source"
tags:
  - rbac
  - authz
  - bfla
  - least-privilege
  - dev-stub
spec_ref: "specs/tag-categorization-l0.yaml#TAG-AUTHZ-001"
verification:
  type: review
  source: "templates/L4/tag-categorization/app/use-caller-id.ts"
  pattern: "useCallerRole() returns 'user' in dev by default; admin path requires explicit `NEXT_PUBLIC_DEV_AS_ADMIN=1` env opt-in"
upstream:
  - "https://owasp.org/API-Security/editions/2023/en/0xa5-broken-function-level-authorization/"
  - "https://owasp.org/www-project-application-security-verification-standard/"
evidence:
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API5:2023 Broken Function Level Authorization"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa5-broken-function-level-authorization/"
    quote: "Authorization checks for a function or resource are usually managed via configuration or code level. Implementing proper checks can be a confusing task since modern applications can contain many types of roles, groups, and complex user hierarchies (e.g. sub-users, or users with more than one role)."
    quoted_at: "2026-05-25"
---

## RBAC role stub MUST default to least-privilege role — never 'admin' in dev

**Impact: HIGH — fail-OPEN defaults travel further than fork-receivers realize**

When a catalog template ships a stub for the calling user's role (or any other authorization claim), the dev default decides what every fork-receiver sees on day one. If that default is `'admin'`, every staging deployment, every Vercel preview, every QA environment, every demo to a stakeholder presents admin UI to whoever is viewing — including users who *should* be locked out. The server's `@PreAuthorize` / RBAC gate eventually rejects the request, but the UI has already lied about availability.

The principle of least privilege says: when in doubt, default to the most restricted role and require explicit opt-in to widen. The dev stub follows the same rule. Default to `'user'` (or whatever your least-privileged role is). Provide an explicit env-var opt-in (e.g. `NEXT_PUBLIC_DEV_AS_ADMIN=1`) that catalog devs flip when they need to exercise the admin path. Emit a one-shot `console.warn` on first call so the stub is visible to a fork-receiver inspecting their devtools.

The production hard-stop is a separate rule (a stub that ships to production should `throw new Error('Identity provider not configured')` so a missed integration cannot ship silently). The least-privilege default protects everything *between* dev and production — staging, preview, QA — where the stub is still active and a wrong default exposes admin UI to non-admin viewers.

**Incorrect — admin by default; staging deploys with stub still wired silently expose admin UI:**

```ts
export function useCallerRole(): 'admin' | 'user' {
  if (process.env.NODE_ENV === 'production') {
    throw new Error('Identity provider not configured')
  }
  return 'admin'                       // ❌ Every preview / staging / QA env shows admin UI to everyone
}
```

**Correct — user by default; admin requires explicit env opt-in; one-shot dev warning:**

```ts
let warnedCallerRole = false

export function useCallerRole(): 'admin' | 'user' {
  if (process.env.NODE_ENV === 'production') {
    throw new Error('useCallerRole: Identity provider not configured')
  }
  if (!warnedCallerRole) {
    warnedCallerRole = true
    console.warn(
      '[ax-template] useCallerRole stub active. Wire your real RBAC source. ' +
        'Set NEXT_PUBLIC_DEV_AS_ADMIN=1 to exercise the admin path locally.',
    )
  }
  const devAsAdmin = process.env.NEXT_PUBLIC_DEV_AS_ADMIN === '1'
  return devAsAdmin ? 'admin' : 'user'      // ✅ Least privilege by default
}
```

This applies symmetrically to **any** authorization-related stub a catalog template ships: role, permissions array, feature-flag boolean, tenant id, team membership. A stub that returns "yes" by default is the wrong default. Return "no" by default; require explicit dev opt-in.

Reference: [OWASP API Security Top 10 (2023) — API5:2023 BFLA](https://owasp.org/API-Security/editions/2023/en/0xa5-broken-function-level-authorization/)

Reference: [OWASP ASVS V4 — Access Control Design](https://owasp.org/www-project-application-security-verification-standard/)
