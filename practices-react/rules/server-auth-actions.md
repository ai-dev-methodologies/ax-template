---
title: Authenticate inside every Server Action — they are public mutation endpoints
impact: CRITICAL
impactDescription: "Server Actions can be invoked directly; middleware and layout guards do not protect them. Auth + authz + input validation must happen INSIDE each action."
tags:
  - server
  - server-actions
  - authentication
  - security
  - authorization
applicable_to:
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-SERVER-003"
verification:
  type: review
  status: manual
  notes: "Reviewer checks every `'use server'` function: (a) authenticate early, (b) validate untrusted input before using it, (c) authorize ownership/role before mutation, (d) return/throw a 401-style error on failure."
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
    notes: "`unauthorized()` helper is experimental — requires `experimental.authInterrupts`. Use a plain return/throw without it."
  completeness:
    status: complete
    amendments:
      - "Ordering: authenticate → validate → authorize → mutate"
      - "Noted `unauthorized()` experimental gate; fallback returns/throws plain 401"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: server-auth-actions"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-auth-actions.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "server-auth-actions"
    quote: "Server Actions are exposed as public endpoints, just like API routes. Always verify authentication and authorization inside each Server Action."
  - source_type: external
    citation: "Next.js Authentication guide — treat Server Actions with same security considerations as public-facing API endpoints"
    url: "https://nextjs.org/docs/app/guides/authentication"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - async-api-routes
---

## Authenticate inside every Server Action — they are public mutation endpoints

**Impact: CRITICAL — Server Actions (`'use server'` functions) can be invoked directly by any client; middleware and layout guards do not protect them. Auth, authz, and input validation must happen INSIDE each action.**

### Order: authenticate → validate → authorize → mutate

```typescript
'use server'
import { verifySession } from '@/lib/auth'
import { z } from 'zod'

const updateProfileSchema = z.object({
  userId: z.string().uuid(),
  name: z.string().min(1).max(100),
  email: z.string().email(),
})

export async function updateProfile(input: unknown) {
  // 1. AUTHENTICATE — does this caller have a valid session?
  const session = await verifySession()
  if (!session) return { error: 'Unauthorized', status: 401 }

  // 2. VALIDATE — refuse malformed input before using it
  const data = updateProfileSchema.parse(input)

  // 3. AUTHORIZE — does this session have permission for THIS data?
  if (session.user.id !== data.userId) {
    return { error: 'Can only update own profile', status: 403 }
  }

  // 4. MUTATE
  await db.user.update({
    where: { id: data.userId },
    data: { name: data.name, email: data.email },
  })
  return { success: true }
}
```

### Incorrect — no auth, anyone on the internet can call this

```typescript
'use server'
export async function deleteUser(userId: string) {
  await db.user.delete({ where: { id: userId } })
  return { success: true }
}
```

### Notes on Next.js helpers

- `unauthorized()` from `next/navigation` is currently **experimental** — gated by `experimental.authInterrupts` in next.config. Without the flag, return or throw a plain 401-shaped result.
- Don't rely on middleware-only auth. Middleware can be bypassed by direct Server Action invocation.
- Don't rely on layout-level guards. The action is independent of the layout that imported it.

Sources:

- [Vercel: server-auth-actions](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-auth-actions.md)
- [Next.js Authentication guide](https://nextjs.org/docs/app/guides/authentication)
