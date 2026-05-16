---
title: API route / Server Action specialization — auth gate first, then start independent work eagerly
impact: HIGH
impactDescription: "Handler-shaped waterfall has the same mechanic as async-parallel but adds API-specific constraints: auth ordering, request cancellation, mutation ordering, and rate-limit gates. Apply init-early-await-late within those constraints."
tags:
  - api-routes
  - server-actions
  - waterfalls
  - parallelization
  - nextjs
applicable_to:
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-ASYNC-004"
verification:
  type: review
  status: manual
  notes: "Reviewer for each API route / Server Action: (a) auth/session check before business logic, (b) independent reads started eagerly after auth, (c) mutations and side-effect-producing calls respect required ordering, (d) cancellation/abort semantics if request can be aborted."
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
      - "Explicitly framed as API-route/SA specialization of async-parallel"
      - "Added auth-first ordering constraint"
      - "Added caveats for mutations, transactions, rate limits, cancellation, ordered side effects"
  gap_check:
    status: complete
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: async-api-routes"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/async-api-routes.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "async-api-routes"
    quote: "In API routes and Server Actions, start independent operations immediately, even if you don't await them yet."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - async-parallel
  - async-dependencies
  - async-defer-await
---

## API route / Server Action specialization — auth gate first, then start independent work eagerly

**Impact: HIGH — Handler-shaped waterfall has the same mechanic as `async-parallel` but adds API-specific constraints: auth ordering, request cancellation, mutation ordering, and rate-limit gates.**

### Scope

This rule narrows `async-parallel` to the API-route / Server-Action context. The mechanic is the same (init early, await late). The constraints below are extra.

### Correct — auth first, then parallelize independent reads

```typescript
export async function GET(request: Request) {
  // 1. Cheap gates first. If auth fails, return without doing any other work.
  const session = await auth()
  if (!session?.user) return Response.json({ error: 'unauthorized' }, { status: 401 })

  // 2. Independent reads — kick off concurrently.
  const configPromise = fetchConfig()
  const dataPromise = fetchData(session.user.id)

  // 3. Join at the latest moment.
  const [config, data] = await Promise.all([configPromise, dataPromise])

  return Response.json({ data, config })
}
```

### Incorrect — sequential await waterfall

```typescript
export async function GET(request: Request) {
  const session = await auth()
  const config = await fetchConfig()        // could have started in parallel
  const data = await fetchData(session.user.id)
  return Response.json({ data, config })
}
```

### API-specific constraints — don't apply init-early-await-late blindly

1. **Auth must come first.** Never start business reads/writes before the auth gate resolves. A pending-auth-but-already-started DB write is the canonical "Hyrum's law" footgun.

2. **Mutations have ordering.** `UPDATE x` then `UPDATE y` may differ from running both with `Promise.all` if the second depends on side effects of the first (triggers, denormalized fields).

3. **Transactions scope a unit.** Don't fire un-awaited promises outside a transaction and expect them to be part of it. Either await sequentially inside the transaction, or commit before parallelizing.

4. **Rate-limit gates count requests.** Starting 3 DB calls eagerly to "save latency" may exhaust a connection pool, paradoxically making the route slower. Profile under load.

5. **Request cancellation.** If the client aborts (closed tab, network failure), `request.signal` should propagate to all in-flight fetches you started. Otherwise you continue paying for work nobody is waiting for.

```typescript
export async function GET(request: Request) {
  const session = await auth()
  if (!session?.user) return Response.json({ error: 'unauthorized' }, { status: 401 })

  const signal = request.signal
  const configPromise = fetchConfig({ signal })
  const dataPromise = fetchData(session.user.id, { signal })

  const [config, data] = await Promise.all([configPromise, dataPromise])
  return Response.json({ data, config })
}
```

6. **Server Actions can be invoked multiple times concurrently.** A mutation that doesn't tolerate concurrent invocation needs idempotency keys or a serializing primitive — outside the scope of this rule.

### When NOT to parallelize

- The "independent" calls share a transaction or critical-section lock.
- One call's success is a precondition for the next (then it's a dependency — see `async-dependencies`).
- The downstream service can't handle the parallel load — sequential is the polite path.

Sources:

- [Vercel: async-api-routes](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/async-api-routes.md)
