---
title: Frontend full-trio MUST be gated by the spec's `domain_mode` declaration
impact: HIGH
impactDescription: "Generating a frontend full-trio for a `domain_mode: backend_only` L4 silently overrides the spec's deliberate design (e.g. server-to-server callback domains where no user-facing surface exists) and reopens fork-receiver autonomy decisions that the spec deliberately closed"
tags:
  - catalog-meta
  - spec-discipline
  - scope-discipline
  - frontend-trio
  - domain-mode
spec_ref: "specs/identity-verification-l0.yaml#L5"
verification:
  guard: l4_frontend_domain_mode_guard.sh
  source: "specs/identity-verification-l0.yaml"
  pattern: "domain_mode: backend_only declared at the top of the spec; templates/L4/identity-verification/ intentionally does NOT exist on disk; the catalog refuses to create a frontend trio for this domain even when an AI agent or master plan asks for one"
upstream:
  - "https://owasp.org/www-project-application-security-verification-standard/"
  - "https://www.rfc-editor.org/rfc/rfc2119"
evidence:
  - source_type: external
    citation: "RFC 2119 — Key words for use in RFCs to Indicate Requirement Levels"
    url: "https://www.rfc-editor.org/rfc/rfc2119"
    quote: "MUST. This word, or the terms REQUIRED or SHALL, mean that the definition is an absolute requirement of the specification."
    quoted_at: "2026-05-26"
---

## Frontend full-trio MUST be gated by the spec's `domain_mode` declaration

**Impact: HIGH — silent spec override exposes surfaces a spec deliberately closed**

The catalog's L4 spec files declare a `domain_mode` field at the top
(`backend_only` / `full_trio` / `frontend_only`). This field is a binding
RFC 2119 MUST: it states whether the domain has any user-facing surface at
all. Server-to-server domains (KYC callback, payment provider webhook,
OAuth token-exchange, server-driven scheduler) declare `backend_only`
because they have NO end-user UI by construction — the provider's own
SDK / web flow handles user identity collection, and the consuming app
only receives the result via signed callback.

Generating `templates/L4/<domain>/app/...` for a `backend_only` domain
silently overrides this design. The PII consequences can be severe: the
identity-verification spec stores CI / DI correlation tokens that are
intentionally NEVER user-visible (개인정보보호법 §24-1, 개인정보 보호위원회
가이드라인). An "admin verified-identities list" page would expose them in
the UI — a surface the spec closed deliberately, and that fork-receivers
who need a different exposure pattern (e.g. logging only via audit) cannot
quietly opt out of once the page exists on disk.

When an AI agent / master plan / persona asks for "L4 \<domain\> frontend
full-trio", the catalog MUST check the spec's `domain_mode` before
creating any file under `templates/L4/<domain>/app/`. If the field is
absent or set to `backend_only`, the work is re-scoped to backend
residual closure (entities, services, audits, admin endpoints). The
absence of `templates/L4/<domain>/` on disk is the spec speaking.

**Incorrect — generating a frontend trio for a `backend_only` domain:**

```text
# specs/identity-verification-l0.yaml — line 5
domain_mode: backend_only   # no frontend UI in scope; CI/DI callback is server-to-server

# AI agent or master plan asks: "create identity-verification frontend full-trio"
$ mkdir -p templates/L4/identity-verification/app/(admin)/verified-identities
$ # ❌ Creates /api/admin/identity-verification UI exposing CI/DI in an admin table
$ # ❌ Overrides the spec's explicit `backend_only` declaration
$ # ❌ Reopens the R2 closure (fork-receiver-owned admin) without spec amendment
```

**Correct — read `domain_mode` first, re-scope to backend residual closure:**

```text
$ grep '^domain_mode' specs/identity-verification-l0.yaml
domain_mode: backend_only

# domain_mode == backend_only → refuse frontend trio.
# Re-scope to backend residual closure:
$ # ✅ Add VerifiedIdentity entity + repository (IDV-CALLBACK-002 persistence)
$ # ✅ Add IdentityVerificationService with audit publish (IDV-AUDIT-001)
$ # ✅ Add IdentityVerificationAdminController @PreAuthorize ROLE_ADMIN
$ # ✅ Do NOT create templates/L4/identity-verification/
```

Reference: [OWASP ASVS V4 §1.2 — Authentication Architecture](https://owasp.org/www-project-application-security-verification-standard/)
Reference: [RFC 2119 — Key Words for Use in RFCs to Indicate Requirement Levels](https://www.rfc-editor.org/rfc/rfc2119)

## How to apply

```text
mode = read(specs/<domain>-l0.yaml#domain_mode)
if mode is null or mode == "backend_only":
  REFUSE.
  Re-scope to: <domain> backend residual closure.
elif mode == "full_trio" or mode == "frontend_only":
  Proceed.
else:
  STOP. Unknown domain_mode value — surface to user.
```

## Verification surface

Enforced mechanically by the 41st hard guard, R59 — see
[`practices/evals/l4_frontend_domain_mode_guard.sh`](../evals/l4_frontend_domain_mode_guard.sh).
The guard refuses to merge any commit where a `templates/L4/<domain>/app/`
tree exists but the matching `specs/<domain>-l0.yaml#domain_mode` is
`backend_only`, absent, or unknown. Fallback spec path
`specs/<domain>-frontend-l0.yaml` is also accepted (auth / crud).

## Anti-patterns

- "The spec is silent on `domain_mode`, so I assume `full_trio`" — NO. Absent
  is a design signal; treat as `backend_only` until the user opts in.
- "The master plan said to do it" — master plans can be wrong (R54 in
  ax-template was). The spec is the source of truth.
- "I'll just add the frontend; fork-receivers can delete it if they don't
  want it" — adding overrides the spec's design decision; deletion is a
  burden on every fork-receiver instead of zero burden if absent.
