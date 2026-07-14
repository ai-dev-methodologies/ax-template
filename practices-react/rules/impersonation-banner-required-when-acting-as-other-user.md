---
title: "ImpersonationBanner must render whenever session.actingAs is non-null"
rule_id: impersonation-banner-required-when-acting-as-other-user
impact: HIGH
impactDescription: "Operating as another user without a visible ImpersonationBanner is a security vulnerability: the operator has no persistent visual signal of their elevated context, increasing the risk of accidental data modification or unauthorized action that is attributed to the wrong identity in audit logs."
tags:
  - security
  - impersonation
  - admin
  - a11y
  - l2-block
applicable_to:
  - react
  - nextjs
provenance_class: internal_design
protects_template_id: templates/L2/blocks/impersonation-banner.tsx
failing_fixture_path: practices-react/evals/fixtures/impersonation-banner-required-when-acting-as-other-user/
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-SECURITY-IMPERSONATION-001"
verification:
  type: script
  status: active
  notes: "The fixture runner checks canonical session.actingAs mutation patterns (direct assignment, immutable update, any helper returning {actingAs}) without a co-located <ImpersonationBanner>. The fail_helper_renamed_runAsUser fixture specifically validates that the rule is NOT bypassable by renaming the helper function."
evidence:
  - source_type: external
    anchors: generic_principle_only
    citation: "OWASP Session Management Cheat Sheet: Admin impersonation sessions must be visually distinct and audited; the impersonated identity must always be visible to the operator."
    url: "https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html"
    quoted_at: "2026-05-18"
  - source_type: external
    anchors: generic_principle_only
    citation: "WCAG 2.2 SC 1.3.1 Info and Relationships (Level A): Information, structure, and relationships conveyed through presentation are also available in text. A banner conveying impersonation context must be programmatically determinable."
    url: "https://www.w3.org/WAI/WCAG22/Understanding/info-and-relationships.html"
    quoted_at: "2026-05-18"
  - upstream_id: wcag-22-techniques-2026-05
    section: "SC 4.1.3 Status Messages — aria-live regions"
    quote: "determined through role or properties so they can be presented by assistive technologies"
decided_at: "2026-05-18"
---

## ImpersonationBanner must render whenever session.actingAs is non-null

**Impact: HIGH — Silently acting as another user without a banner is a security vulnerability. Every operator session with `session.actingAs !== null` must render `<ImpersonationBanner>`.**

### Why this rule exists

Admin impersonation is a high-privilege action. When an operator is viewing or modifying data as another user, this context must be:

1. **Persistently visible** — the operator always sees who they are acting as.
2. **Programmatically determinable** (WCAG 1.3.1) — the banner is machine-readable.
3. **Auditable** — the impersonation state is bound to the canonical `session.actingAs` field, not an implicit transient.

The rule fires on the **canonical session state mutation**, not on a specific helper function name. This means renaming `assumeUserId()` to `runAsUser()` or any other name does not bypass the rule.

### The violation — acting-as without banner (direct assignment)

```typescript
// ❌ WRONG — sets session.actingAs without rendering ImpersonationBanner
export async function assumeUser(userId: string) {
  // VIOLATION: canonical actingAs field set; no <ImpersonationBanner> in caller tree
  session.actingAs = userId
  return session
}
```

### The violation — helper rename bypass (Critic Soft Suggestion 2 — BLOCKED)

```typescript
// ❌ WRONG — renamed helper does NOT bypass the rule
// The rule matches {actingAs: ...} return shape, not the function name.
export function runAsUser(userId: string) {
  // VIOLATION: returns object with actingAs field without banner requirement met
  return { ...currentSession, actingAs: userId }
}
```

### The violation — immutable update without banner

```typescript
// ❌ WRONG — spreading {actingAs: id} is also a canonical mutation
const nextSession = { ...session, actingAs: targetUserId }
// VIOLATION: nextSession.actingAs is non-null; banner not rendered
router.push('/admin/dashboard')
```

### Correct — any helper name, banner always present

```typescript
// ✅ CORRECT — helper name is irrelevant. The per-file scanner requires <ImpersonationBanner>
// CO-LOCATED in the file that PERSISTS actingAs. A pure helper that merely computes {actingAs}
// is fine; the layout below both consumes it and renders the banner in the same file.
// Helper (any name) — pure computation, persists nothing:
export function runAsUser(userId: string) {
  return { ...currentSession, actingAs: userId }
}

// Root layout or admin layout (L4):
import ImpersonationBanner from 'templates/L2/blocks/impersonation-banner'

export default async function AdminLayout({ children }) {
  const session = await getAdminSession()
  return (
    <>
      {/* Banner renders iff session.actingAs is non-null */}
      <ImpersonationBanner
        session={session}
        onEndImpersonation={endImpersonation}
      />
      {children}
    </>
  )
}
```

### Correct — server component with cookie-driven session

```typescript
// ✅ CORRECT — server component reads session from cookie; banner in layout
// lib/admin-session.ts:
export async function getAdminSession(): Promise<AdminSession> {
  const cookie = (await cookies()).get('admin-session')?.value
  return cookie ? JSON.parse(decrypt(cookie)) : { actingAs: null }
}

// app/admin/layout.tsx:
export default async function AdminLayout({ children }) {
  const session = await getAdminSession()
  return (
    <>
      <ImpersonationBanner session={session} />
      <main id="main">{children}</main>
    </>
  )
}
```

### Rule detection scope

The fixture scanner detects the following patterns as violations (any file under `templates/` or `app/admin/`):

| Pattern | Detected | Explanation |
|---|---|---|
| `session.actingAs = userId` | ✅ | Direct assignment to canonical field |
| `{ ...session, actingAs: id }` | ✅ | Immutable update with actingAs key |
| `return { actingAs: userId }` | ✅ | Helper returning actingAs shape |
| Missing `<ImpersonationBanner>` in file | ✅ | Banner not co-located with actingAs set |
| `assumeUserId(id)` without banner | ✅ | Function name irrelevant |
| `runAsUser(id)` without banner | ✅ | Function name irrelevant |

The scanner does NOT fire on files that:
- Only read `session.actingAs` (guard checks, `if (session.actingAs)`)
- Import and render `<ImpersonationBanner>` in the same file
- Belong to the test/fixture directories themselves

### Why helper rename bypass is impossible

The rule matches the **shape** of the session mutation, not the function name:

```
// All of these trigger the rule (different names, same shape):
session.actingAs = id           ← direct
{ ...s, actingAs: id }          ← spread
{ actingAs: id, ...other }      ← leading key
return { actingAs: userId }     ← returned object
```

Renaming `assumeUserId` → `runAsUser` → `loginAsUser` does not change the
`actingAs` field in the returned/assigned object. The rule stays intact.

Reference: [OWASP Session Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html)

Reference: [templates/L2/blocks/impersonation-banner.tsx](../../templates/L2/blocks/impersonation-banner.tsx)
