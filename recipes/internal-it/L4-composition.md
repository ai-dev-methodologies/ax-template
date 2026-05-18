# L4 Composition — internal-it

> Which L4 domains to enable and how they wire together.

## Domain Wiring

```
crud (Ticket, SLAPolicy, Comment)
 └── POST /api/tickets → open / in-progress / resolved / closed state machine
      ↓ idempotency-key required on mutations (rule_ref: idempotency-key-on-mutations.md)
auth
 └── ASVS V4.1 role-aware authorization:
      - operator  (read all / mutate all / close)
      - approver  (read all / approve resolved → closed)
      - requester (read own / open / comment own)
      ↓
audit-log
 └── @Audited("ticket.state.changed")
      records operator + before/after state + at-timestamp
      ↓
scheduled-task (SLA-breach reminder)
 └── SlaBreachReminderTask — distributed lock (SCHED-LOCK-001) +
      idempotency on breach-event key (SCHED-IDEMPOTENT-001) — no double-send
      across nodes; JobHistory append per run (SCHED-EXECUTE-001)
      ↓
notification (assignee fanout)
 └── TicketAssigned / StatusChanged / SlaNearBreach events → fanout filtered by
      NotificationPreferences (NOTIF-PREF-001) + NOTIF-SEND-001 at-least-once
      delivery semantics
      ↓
webhook (external ITSM relay)
 └── WebhookDispatcher.emit("ticket.escalated", body) →
      per-endpoint HMAC-SHA256 over <timestamp>.<body> (WEBHOOK-SIGN-001/002 —
      RFC 2104 + OWASP ASVS V13.2.6 anchor shared with the inbound
      practices/rules/webhook-hmac-required.md receiver rule) →
      exponential backoff 30s × 2 up to 5 attempts (WEBHOOK-RETRY-001) →
      X-Webhook-Delivery-Id stable across retries (WEBHOOK-RETRY-002) →
      FAILED_PERMANENT terminal status + admin replay (WEBHOOK-DEAD-LETTER-001/002)
```

## Domain Configuration Notes

### `crud`
- Three CRUD entities: `Ticket`, `SlaPolicy`, `TicketComment`
- Ticket lifecycle states: `OPEN` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`
- Idempotency key required on mutations per
  `practices/rules/idempotency-key-on-mutations.md` (e.g. `POST /api/tickets`
  with stable `X-Idempotency-Key` from the requester UI to avoid double-creation
  on retry).
- Reference: `templates/L4/crud/`

### `auth`
- Three roles enforced by SecurityConfig: `OPERATOR`, `APPROVER`, `REQUESTER`.
- Operator-or-admin gating on `POST /api/tickets/{id}/transition` for moves
  beyond `RESOLVED`; approver-only on `RESOLVED → CLOSED`; requester reads
  own only on `GET /api/tickets/{id}`.
- ASVS V4.1.1 — explicit role enforcement at the controller boundary, not in
  the service layer.
- Reference: `templates/L4/auth/`

### `audit-log`
- Annotate `TicketService.transition()` with
  `@Audited(action = "ticket.state.changed")`.
- Records: `operator_id`, `target_id` (ticket UUID), `before_status`,
  `after_status`, `at`.
- Retention: ≥ 90 days per `specs/audit-log-l0.yaml#AUDIT-RETENTION-001`.
- Reference: `templates/L4/audit-log/`

### `scheduled-task` (SLA-breach reminder)
- Register `SlaBreachReminderTask` with cron (e.g. `0 */15 * * * *` every 15
  minutes).
- `LockingPolicy.tryAcquire("sla-breach-reminder", node-id)` before each tick
  (SCHED-LOCK-001 — multi-node-safe).
- JobHistory row appended per run (SCHED-EXECUTE-001).
- Idempotency: each tick stores `(ticket_id, breach_threshold)` in the
  reminder-emit table; if the task re-runs for the same row within the lock
  window, the duplicate is dropped (SCHED-IDEMPOTENT-001).
- Reference: `templates/L4/scheduled-task/`

### `notification`
- Subscribe to ticket events:
  - `TicketAssignedEvent` (assignee receives "ticket assigned to you")
  - `TicketStatusChangedEvent` (assignee + requester receive lifecycle update)
  - `SlaNearBreachEvent` (assignee receives "SLA breach in 30 min")
- Honor `NotificationPreferences` (NOTIF-PREF-001) — per-channel opt-out (an
  on-call operator may opt out of email but keep SMS).
- Use NOTIF-SEND-001 outbox delivery semantics for guaranteed at-least-once
  dispatch (the outbox-write happens inside the same transaction as the
  ticket-state-change row write).
- Channels: email (via email-outbox), in-app push (via notification-list L2).
- Reference: `templates/L4/notification/`

### `webhook` (external ITSM relay)
- The 6th enabled L4 — **FIRST CONSUMER of the webhook L4 primitive shipped in
  R9 SP45** (NET-NEW Spec Trio per TD-2026-05-22-025).
- Register destination endpoints via the admin API:
  - Jira inbound webhook (project-specific endpoint URL)
  - ServiceNow inbound integration endpoint
  - PagerDuty event-router URL
  - Slack incoming-webhook URL (for `#ops-incidents` channel)
  - 네이버웍스 / Toss-style internal Korean-enterprise endpoints (per the
    Korean evidence anchors in RECIPE.md)
- Outbound HMAC-SHA256 over `<X-Webhook-Timestamp>.<body>` per WEBHOOK-SIGN-001
  + WEBHOOK-SIGN-002. The cryptographic anchor (RFC 2104 + OWASP ASVS V13.2.6)
  is the SAME anchor reused by `practices/rules/webhook-hmac-required.md` for
  the inbound axis — sender + receiver are distinct catalog axes sharing
  identical construction.
- Retry shape: 30s × 2 up to 5 attempts (WEBHOOK-RETRY-001); stable
  X-Webhook-Delivery-Id across retries (WEBHOOK-RETRY-002).
- Dead-letter: terminal `FAILED_PERMANENT` row retained for admin inspection
  (WEBHOOK-DEAD-LETTER-001); admin replay creates a fresh delivery_id chain
  (WEBHOOK-DEAD-LETTER-002).
- Circuit-breaker: 90% rolling failure rate over 50 attempts auto-deactivates
  the endpoint and emits an audit-log row (WEBHOOK-CIRCUIT-001).
- Reference: `templates/L4/webhook/`

## Applied Recipe Annotation

Every L4 domain wired under this recipe **must** declare in its README.md:
```
applied_recipes:
  - internal-it
```
(Enforced by rule `business-domain-must-declare-applied-recipe` — SP37/R6
dual-form guard.)

The 6 affected L4 READMEs (alphabetical: `audit-log`, `auth`, `crud`,
`notification`, `scheduled-task`, `webhook`) carry `internal-it` in their
`applied_recipes:` plural lists.

- 5 READMEs (`audit-log`, `auth`, `crud`, `notification`, `scheduled-task`)
  receive an **alphabetical insertion** of `internal-it` into their existing
  plural lists.
- 1 README (`webhook`) is the **first-consumer-arrival event** — its
  `applied_recipes:` key is **born** in the same SP45b atomic commit with the
  single-entry list `[internal-it]`. Per the M6 Architect fix and TD-027
  Consequences, the entry carries an inline annotation
  `# verdict pending until SP46 — see _MANIFEST.yaml for active status` so the
  3-9 day partial-tag desync window is self-documenting for fork-receivers
  inspecting the webhook README in isolation. (Convention: TD-2026-05-21-024
  first-consumer-arrival.)
