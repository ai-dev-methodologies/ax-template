---
title: Mutations that may NO-OP (skipped by server invariant) MUST surface the skipped outcome with the server's reason
impact: MEDIUM
impactDescription: "A green-toast 'Success' after a server-skipped mutation tells the operator the work happened when it did not — operator moves on assuming side effects landed"
tags:
  - mutation
  - server-skip
  - outcome-surfacing
  - distributed-lock
spec_ref: "specs/scheduled-task-l0.yaml#SCHED-LOCK-001"
verification:
  source: "templates/L4/scheduled-task/app/(admin)/scheduled-tasks/page.tsx"
  pattern: "trigger.onSuccess sets triggerOutcome state; render differentiates executed=true (green) vs executed=false (amber + reason string from server) instead of collapsing both into one success banner"
upstream:
  - "https://datatracker.ietf.org/doc/html/rfc9457"
  - "https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html"
evidence:
  - source_type: external
    citation: "RFC 9457 — Problem Details for HTTP APIs"
    url: "https://datatracker.ietf.org/doc/html/rfc9457"
    quote: "The 'detail' member is a JSON string containing a human-readable explanation specific to this occurrence of the problem."
    quoted_at: "2026-05-25"
  - source_type: external
    citation: "WCAG 2.2 — Success Criterion 4.1.3 Status Messages (Level AA)"
    url: "https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html"
    quote: "In content implemented using markup languages, status messages can be programmatically determined through role or properties such that they can be presented to the user by assistive technologies without receiving focus."
    quoted_at: "2026-05-25"
---

## Mutations that may NO-OP (skipped by server invariant) MUST surface the skipped outcome with the server's reason

**Impact: MEDIUM — silent server-skipped mutations train operators to trust outcomes that did not happen**

Some mutations have HTTP 200 success responses that mean "I received your request" — not "I executed the work". The catalog has several:

- **Scheduled-task trigger** — the backend acquires a `DatabaseAdvisoryLock` per task before running. If another instance already holds the lock, the trigger response is `{ executed: false, reason: 'another instance is running this task' }` with HTTP 200. The mutation did not run.
- **Activity-feed mark-read** on an event the caller cannot see — server returns 204 (RFC 9110 idempotent shape) but the read state did not change.
- **Webhook replay** when the partner circuit breaker is open — server queues the request but immediately moves it to DEAD_LETTER without sending. HTTP 200 with no actual delivery.
- **Approval-workflow self-approve attempt** — server-enforced invariant rejects with 409 + reason "requester cannot approve own request" (this one is rejected, not skipped, but the operator-side outcome is the same: "I clicked Approve and nothing happened").

The naive client pattern collapses all `onSuccess` into a single green banner — "Triggered", "Marked read", "Replayed", "Approved". The operator reads the banner and moves on. The mutation did not actually do the work.

The correct pattern requires three properties:
1. **Read the executed/skipped signal** from the response body (`executed: boolean`, or absence of the side effect's confirmation field)
2. **Render differentiated outcome** — green for executed, amber/yellow for skipped, with the server's `reason` quoted verbatim
3. **Don't collapse skipped into error** — a skipped mutation is not a failure (the server enforced an invariant correctly), so it does not belong in the error-alert banner. It belongs in a `role='status'` aria-live region with distinct styling.

**Incorrect — collapse executed/skipped into one success:**

```tsx
const trigger = useMutation({
  mutationFn: triggerTask,
  onSuccess: () => {
    // ❌ Single banner regardless of executed=true/false
    toast.success('Triggered')
  },
})
```

The SRE sees "Triggered" green. The work did not happen. They go back to triaging the next item.

**Correct — surface skipped with reason:**

```tsx
const [triggerOutcome, setTriggerOutcome] = React.useState<{
  taskId: string
  executed: boolean
  reason: string | null
} | null>(null)

const trigger = useMutation({
  mutationFn: triggerTask,
  onMutate: () => setTriggerOutcome(null),
  onSuccess: (resp, id) => {
    // Server may return executed: false when DatabaseAdvisoryLock blocks
    setTriggerOutcome({ taskId: id, executed: resp.executed, reason: resp.reason })
  },
})

// In JSX:
{triggerOutcome && (
  <div
    role="status"
    aria-live="polite"
    className={`rounded border px-3 py-1.5 text-sm ${
      triggerOutcome.executed
        ? 'border-green-300 bg-green-50 text-green-900'
        : 'border-amber-300 bg-amber-50 text-amber-900'
    }`}
  >
    {triggerOutcome.executed
      ? 'Trigger accepted — job queued for execution.'
      : `Trigger skipped — ${triggerOutcome.reason ?? 'another instance is running this task'}`}
  </div>
)}
```

Three properties confirmed:
- (1) reads `resp.executed`
- (2) green-vs-amber differentiation with the server's `resp.reason`
- (3) `role='status'` (not `role='alert'`) because skipped-by-invariant is not an error

**When to apply**: any mutation whose backend documents a "no-op success" path — distributed-lock skip, circuit-breaker skip, idempotent-already-applied skip, invariant-enforced skip. The catalog convention is to give those endpoints a discriminated response (`executed: boolean` plus `reason: string | null`) so the client can render unambiguously.

**When NOT to apply**: mutations where the server's contract guarantees side effects landed on every HTTP 200 (most CRUD). Single green toast / inline confirmation is fine there.

Pairs with `destructive-action-confirm-with-side-effects` — the confirm dialog tells the operator what *will* happen; this rule's outcome banner tells them what *did* happen.

Reference: [RFC 9457 — Problem Details for HTTP APIs](https://datatracker.ietf.org/doc/html/rfc9457)

Reference: [WCAG 2.2 SC 4.1.3 — Status Messages](https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html)
