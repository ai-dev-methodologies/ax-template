---
title: Pass minimal client DTOs across the RSC→Client boundary — never whole server entities
impact: HIGH
impactDescription: "RSC props are serialized into the RSC stream/payload sent to the browser. Passing whole entities (full user/order/product objects) bloats the payload and may leak secrets, internal IDs, and role metadata."
tags:
  - server
  - rsc
  - serialization
  - props
  - security
applicable_to:
  - react
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-SERVER-007"
verification:
  type: review
  status: manual
  notes: "Reviewer checks every Client Component prop crossing RSC boundary: (a) shape is a project-defined DTO, not an ORM/session entity, (b) no internal IDs / secrets / role flags / audit fields included that the client doesn't need."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
  completeness:
    status: complete
    amendments:
      - "Phrased as RSC stream/payload (transport-agnostic)"
      - "Added security framing: secrets, internal IDs, role metadata leak risk"
      - "Suggested DTO/view-model pattern"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: server-serialization"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-serialization.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "server-serialization"
    quote: "Only pass fields that the client actually uses."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - server-dedup-props
---

## Pass minimal client DTOs across the RSC→Client boundary — never whole server entities

**Impact: HIGH — RSC props are serialized into the RSC stream/payload sent to the browser. Whole-entity props bloat the payload AND can leak secrets, internal IDs, role flags, and audit metadata.**

### Incorrect — 50-field user entity, client uses one field

```tsx
async function Page() {
  const user = await fetchUser()  // returns 50 fields incl. hashedPassword
  return <Profile user={user} />
}
```

```tsx
'use client'
function Profile({ user }: { user: User }) {
  return <div>{user.name}</div>
}
```

Two bugs:
1. **Payload weight**: 49 unused fields shipped to the browser.
2. **Security leak**: `hashedPassword`, `mfaSecret`, `internalRoleId`, `auditLog`, `lastFailedLoginIp` — every field on the entity is now in the page source.

### Correct — explicit client DTO

```tsx
// app/types/client-dtos.ts
export type ClientUserDTO = { name: string; avatarUrl: string }

// Server Component
async function Page() {
  const user = await fetchUser()
  const dto: ClientUserDTO = { name: user.name, avatarUrl: user.avatarUrl }
  return <Profile user={dto} />
}
```

```tsx
'use client'
function Profile({ user }: { user: ClientUserDTO }) {
  return <div>{user.name}</div>
}
```

### Forbidden field categories

Never serialize across RSC→Client:
- Auth secrets: password hashes, MFA secrets, session tokens, API keys
- Role/permission internals: full role objects, ACL trees, internal IDs
- PII not displayed: SSN, government IDs, full DOB, full address (unless the component renders it)
- Audit metadata: created_by, internal_notes, soft_delete_metadata
- Other users' data: even if "the API returned it", filter it before passing

### Pattern — DTOs near the boundary

Define DTOs in a dedicated module (`app/types/client-dtos.ts` or per-feature `dto.ts`). The DTO is a contract: anything not on the type doesn't cross the boundary.

```typescript
// Mapping helper
export function toClientUserDTO(user: ServerUser): ClientUserDTO {
  return { name: user.name, avatarUrl: user.avatarUrl }
}
```

Then in the Server Component: `<Profile user={toClientUserDTO(user)} />`. The mapping function is the audit point — review changes there see security implications obviously.

### Related rule

Sibling rule `server-dedup-props` covers the (lower-impact) case of passing the same data in two shapes; this rule covers the (higher-impact) case of passing too much data in one shape.

Sources:

- [Vercel: server-serialization](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-serialization.md)
