---
title: "Impersonation bypass via helper rename is not permitted"
rule_id: no-impersonation-bypass-via-helper-rename
impact: HIGH
impactDescription: "Renaming the admin impersonation helper (assumeUserId, runAsUser, becomeUser, etc.) does NOT exempt the caller from rendering ImpersonationBanner. The rule matches the canonical session shape — session.actingAs or any returned {actingAs: ...} object — not the function name. Any bypass attempt is a HIGH security violation."
tags:
  - security
  - impersonation
  - admin
  - l2-block
applicable_to:
  - react
  - nextjs
provenance_class: internal_design
protects_template_id: templates/L2/blocks/impersonation-banner.tsx
failing_fixture_path: practices-react/evals/fixtures/impersonation-banner-required-when-acting-as-other-user/fail_helper_renamed_runAsUser/
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-SECURITY-IMPERSONATION-001"
see_also: practices-react/rules/impersonation-banner-required-when-acting-as-other-user.md
verification:
  type: script
  status: active
  notes: "Specialization of impersonation-banner-required-when-acting-as-other-user. The fixture fail_helper_renamed_runAsUser explicitly validates that runAsUser() without <ImpersonationBanner> is detected. The rule matches {actingAs: ...} return shape, not the helper function name."
evidence:
  - source_type: external
    citation: "OWASP Session Management Cheat Sheet: Admin impersonation sessions must be visually distinct; a renamed helper wrapper does not change the security requirement."
    url: "https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "WCAG 2.2 SC 1.3.1 Info and Relationships (Level A): Impersonation context conveyed through presentation must also be available in text — renaming the helper does not satisfy this requirement."
    url: "https://www.w3.org/WAI/WCAG22/Understanding/info-and-relationships.html"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## Impersonation bypass via helper rename is not permitted

**Impact: HIGH — This rule is a specialization of `impersonation-banner-required-when-acting-as-other-user` that explicitly blocks the bypass pattern where a developer renames the admin impersonation helper to evade detection.**

### The bypass attempt (BLOCKED)

```typescript
// ❌ WRONG — renamed helper does NOT bypass the impersonation-banner rule
// Renaming assumeUserId() → runAsUser() → becomeUser() → loginAsUser()
// does NOT change the requirement to render <ImpersonationBanner>.

export function runAsUser(userId: string) {
  // VIOLATION: returns {actingAs: userId} shape; the rule matches the shape, not the name
  return { ...currentSession, actingAs: userId }
}

// Caller — no <ImpersonationBanner> in render tree:
const session = runAsUser(targetId)
router.push('/admin/dashboard')
// ↑ BLOCKED: session.actingAs is non-null; no banner rendered
```

### Why rename bypass is impossible

The rule scanner matches the **canonical session state mutation shape**, not any specific function name:

| Trigger pattern | Detected |
|---|---|
| `session.actingAs = userId` | Yes — direct assignment |
| `{ ...session, actingAs: id }` | Yes — spread update |
| `return { actingAs: userId }` | Yes — returned object shape |
| `runAsUser(id)` without banner | Yes — shape detected regardless of name |
| `becomeUser(id)` without banner | Yes — shape detected regardless of name |

### The fix (any helper name is fine)

```typescript
// ✅ CORRECT — helper name is irrelevant; banner is wired at the layout level

// Helper (any name):
export function runAsUser(userId: string) {
  return { ...currentSession, actingAs: userId }
}

// Admin layout — banner always rendered when actingAs is non-null:
export default async function AdminLayout({ children }) {
  const session = await getAdminSession()
  return (
    <>
      <ImpersonationBanner session={session} onEndImpersonation={endImpersonation} />
      <main>{children}</main>
    </>
  )
}
```

### Relation to parent rule

This rule (`no-impersonation-bypass-via-helper-rename`) is a named specialization of
[`impersonation-banner-required-when-acting-as-other-user`](impersonation-banner-required-when-acting-as-other-user.md).
It exists as a separate canonical id so that eval harnesses, contracts, and PRD traceability
tables can reference the bypass-specific behavior explicitly.

Reference: [OWASP Session Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html)
