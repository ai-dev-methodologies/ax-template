---
title: Move `await` into the branch that actually uses the result; place cheap guards first
impact: HIGH
impactDescription: "Avoids blocking on data the early-return path discards. Especially valuable when the skip path is common (cache hits, permission denials, validation failures) or the deferred operation is expensive."
tags:
  - async
  - await
  - conditional
  - early-return
  - optimization
applicable_to:
  - react
  - nextjs
  - vite
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-ASYNC-002"
verification:
  type: review
  status: manual
  notes: "Reviewer checks each async function: (a) cheap guards run before expensive awaits, (b) awaited results are referenced on every path that reaches them, (c) intentional side-effecting awaits (auth, validation, transaction) are not deferred."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
  completeness:
    status: complete
    amendments:
      - "Added side-effect caveat: don't defer awaits whose purpose IS the side effect (auth/validation/log/transaction)"
      - "Clarified relationship to async-parallel (different concern: necessity, not parallelism)"
  gap_check:
    status: complete
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: async-defer-await"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/async-defer-await.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "async-defer-await"
    quote: "Move await operations into the branches where they're actually used to avoid blocking code paths that don't need them."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - async-parallel
  - async-api-routes
---

## Move `await` into the branch that actually uses the result; place cheap guards first

**Impact: HIGH — Avoids blocking on data the early-return path discards. Especially valuable when the skip path is common (cache hits, permission denials, validation failures) or the deferred operation is expensive.**

### Incorrect — blocks both branches

```typescript
async function handleRequest(userId: string, skipProcessing: boolean) {
  const userData = await fetchUserData(userId)
  if (skipProcessing) return { skipped: true }
  return processUserData(userData)
}
```

### Correct — early-return first, then fetch

```typescript
async function handleRequest(userId: string, skipProcessing: boolean) {
  if (skipProcessing) return { skipped: true }
  const userData = await fetchUserData(userId)
  return processUserData(userData)
}
```

### Correct — cheapest validation first (guard before the dependent work it gates)

```typescript
async function updateResource(resourceId: string, userId: string) {
  const resource = await getResource(resourceId)
  if (!resource) return { error: 'Not found' }

  // Permission check only after resource exists
  const permissions = await fetchPermissions(userId)
  if (!permissions.canEdit) return { error: 'Forbidden' }

  return updateResourceData(resource, permissions)
}
```

### Side-effect caveat — do NOT defer these

Some `await`s exist for the side effect, not the return value. Don't reorder them:

- **Auth/session validation** at the top of a handler — moving it past business logic creates security bugs.
- **Input validation** that throws on bad shapes — better to fail fast than after expensive work.
- **Audit logs** that must record every request — deferring past an early return loses logs.
- **Transaction setup** (`BEGIN TRANSACTION`) — must precede the work it scopes.

The rule is "defer awaits whose **result** is unused on the early path", not "defer all awaits".

### Relationship to async-parallel

This is a different concern from async-parallel (sibling rule):
- `async-parallel` says: independent work should run **at the same time**.
- `async-defer-await` says: unneeded work should not run **at all**.

Both can apply at once. After cheap guards pass, kick off the required independent work in parallel.

Sources:

- [Vercel: async-defer-await](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/async-defer-await.md)
