---
title: Use after() for best-effort post-response work (logs / analytics / cleanup) — never for critical operations
impact: MEDIUM
impactDescription: "Faster response times by deferring non-critical side effects until after the response ships. after() is best-effort; do not use as a durable queue for billing, notifications, or guaranteed side effects."
tags:
  - server
  - async
  - logging
  - analytics
  - side-effects
  - after
applicable_to:
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-SERVER-009"
verification:
  type: review
  status: manual
  notes: "Reviewer checks: (a) work inside after() is best-effort acceptable, (b) critical/billable work is NOT inside after() — it's awaited before response or sent to a durable queue, (c) platform timeout/cold-shutdown budget considered."
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
      - "Added durability warning: not for critical/guaranteed side effects"
      - "Added platform max-duration / cold-shutdown caveat"
      - "Noted behavior on static prerender / revalidation"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: server-after-nonblocking"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-after-nonblocking.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "server-after-nonblocking"
    quote: "Use Next.js's after() to schedule work that should execute after a response is sent."
  - source_type: external
    citation: "Next.js docs — after() runs even if the response fails or redirects; works in Server Components, Server Actions, and Route Handlers"
    url: "https://nextjs.org/docs/app/api-reference/functions/after"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - async-api-routes
---

## Use after() for best-effort post-response work — never for critical operations

**Impact: MEDIUM — Faster response times by deferring non-critical side effects until after the response ships. `after()` is best-effort; do not use as a durable queue for billing, notifications, or guaranteed side effects.**

### Correct use cases

- Analytics / metrics events
- Audit logging where occasional loss is acceptable
- Cache warming
- Cleanup tasks
- Best-effort notifications where missing one isn't critical

### Incorrect — logging blocks the response

```tsx
export async function POST(request: Request) {
  await updateDatabase(request)

  const userAgent = request.headers.get('user-agent') || 'unknown'
  await logUserAction({ userAgent })   // adds latency the user feels

  return Response.json({ status: 'success' })
}
```

### Correct — log after response

```tsx
import { after } from 'next/server'
import { headers, cookies } from 'next/headers'

export async function POST(request: Request) {
  await updateDatabase(request)

  after(async () => {
    const ua = (await headers()).get('user-agent') ?? 'unknown'
    const session = (await cookies()).get('session-id')?.value ?? 'anonymous'
    logUserAction({ ua, session })
  })

  return Response.json({ status: 'success' })
}
```

### Durability — what after() does NOT guarantee

- The function instance may be killed shortly after the response ships. Long-running `after()` work may be truncated.
- Platform max-duration limits apply. Vercel functions have a budget; exceeding it cancels in-flight work.
- `after()` is **not** a durable queue. If the work is critical (billing, payment confirmation, transactional email), use:
  - A durable queue (SQS, BullMQ, Inngest, QStash)
  - An external scheduler (cron, Trigger.dev)
  - Await the work before responding (accept the latency)

### Behavior on static prerender / revalidation

`after()` callbacks during static generation / ISR revalidation execute as part of the build/revalidation step, not at request time. Don't rely on per-request context (cookies, headers) without checking the call site is request-time.

### Available in (per Next docs)

- Server Components
- Server Actions / Server Functions
- Route Handlers

### Anti-pattern — billing inside after()

```tsx
// BAD: if the function instance dies before this runs, the user got the
// service for free.
after(async () => {
  await chargeCustomer(orderId, amount)
})
```

Charge before responding, or send to a durable queue that the billing worker processes. `after()` is **best effort**.

Sources:

- [Vercel: server-after-nonblocking](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/server-after-nonblocking.md)
- [Next.js docs — after()](https://nextjs.org/docs/app/api-reference/functions/after)
