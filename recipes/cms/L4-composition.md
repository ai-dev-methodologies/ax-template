# L4 Composition — cms

> Which L4 domains to enable and how they wire together.

## Domain Wiring

```
crud (Content)
 └── POST /api/content → draft/scheduled/published/archived state machine
      ↓ slug uniqueness (server-side per locale + content-type)
audit-log
 └── @Audited("content.publish_state.changed")
      records operator + before/after state
      ↓
scheduled-task (scheduled-publish)
 └── ScheduledPublishTask — distributed lock (SCHED-LOCK-001) +
      idempotency (SCHED-IDEMPOTENT-001) — no double-publish on multi-node
      ↓
scheduled-task (scheduled-archive / content-expiry)
 └── ContentExpiryTask — JobHistory append per run (SCHED-EXECUTE-001) +
      audit retention (AUDIT-RETENTION-001) preserves archive trail
      ↓
notification
 └── ReviewRequested / Approved / Rejected events → fanout filtered by
      NotificationPreferences (NOTIF-PREF-001) + NOTIF-SEND-001 delivery semantics
```

## Domain Configuration Notes

### `crud`
- Single CRUD entity: `Content`
- Lifecycle states: `DRAFT` → `SCHEDULED` → `PUBLISHED` → `ARCHIVED`
- Slug uniqueness enforced server-side per (locale, content_type) combination
  (CMS-INV-005) — validation rule `specs/crud-security.yaml#CRUD-VAL-1`
- Idempotency key required on mutations per
  `practices/rules/idempotency-key-on-mutations.md`
- Reference: `templates/L4/crud/`

### `audit-log`
- Annotate `ContentService.transitionPublishState()` with
  `@Audited(action = "content.publish_state.changed")`
- Records: `operator_id`, `target_id`, `before_status`, `after_status`, `at`
- Retention: ≥90 days per `specs/audit-log-l0.yaml#AUDIT-RETENTION-001`
  (covers scheduled-archive trail as well)
- Reference: `templates/L4/audit-log/`

### `scheduled-task` (scheduled-publish)
- Register `ScheduledPublishTask` with cron (e.g. `0 * * * * *` minute-granular)
- `LockingPolicy.tryAcquire("scheduled-publish", node-id)` before each tick
- `SELECT ... FOR UPDATE` on the `scheduled_task_lock` row (row-PRESENT branch:
  pessimistic lock spanning the staleness test AND the takeover write; row-ABSENT
  branch: arbitrated by the lock table's PRIMARY KEY) — NOT `SKIP LOCKED`, which H2
  does not support and which makes a held row look ABSENT to the loser instead of
  making it wait (BACKLOG P2-48/P2-61). Or ShedLock.
- JobHistory row appended per run (SCHED-EXECUTE-001)
- Idempotency: each Content row carries `publish_idempotency_key`; if the
  task re-runs on the same row within the lock window, the second run is a
  no-op (SCHED-IDEMPOTENT-001)
- Reference: `templates/L4/scheduled-task/`

### `scheduled-task` (scheduled-archive / expiry)
- Register `ContentExpiryTask` with separate cron (e.g. `0 0 * * * *` hourly)
- Same lock + JobHistory + idempotency discipline as scheduled-publish
- Sets Content state ARCHIVED; audit-log row emitted via the same
  `@Audited("content.publish_state.changed")` annotation (CMS-INV-003 binds
  SCHED-EXECUTE-001 + AUDIT-RETENTION-001 together)
- Reference: `templates/L4/scheduled-task/`

### `notification`
- Subscribe to editorial workflow events:
  - `ReviewRequestedEvent` (author submits for editorial review)
  - `ReviewApprovedEvent` (editor approves)
  - `ReviewRejectedEvent` (editor rejects with comment)
- Honor `NotificationPreferences` (NOTIF-PREF-001) — per-channel opt-out
- Use NOTIF-SEND-001 delivery semantics for guaranteed at-least-once dispatch
- Channels: email (via email-outbox), in-app push (via notification-list L2)
- Templates: `review_requested`, `review_approved`, `review_rejected`
- Reference: `templates/L4/notification/`

## Applied Recipe Annotation

Every L4 domain wired under this recipe **must** declare in its README.md:
```
applied_recipes:
  - cms
```
(Enforced by rule `business-domain-must-declare-applied-recipe` — SP37/R6 dual-form guard)

The 4 affected L4 READMEs (alphabetical: `audit-log`, `crud`, `notification`,
`scheduled-task`) carry `cms` in their `applied_recipes:` plural list. The
R8 SP43 atomic commit appends `cms` alphabetically to 3 of those L4 READMEs
(`audit-log`, `crud`, `notification` — all pre-existing plural lists) and
ships the *first* `applied_recipes:` key on
`templates/L4/scheduled-task/README.md` born `[cms, lms]` (R7 TD-020 follow-up
+ R8 TD-024 first-consumer-arrival convention).

Optional bindings (`auth`, `search`) carry `cms` as a parenthetical override-
allowed declaration; the SP43 atomic commit also appends `cms` to those L4
READMEs' plural lists for inventory completeness.
