# L4 Composition — lms

> Which L4 domains to enable and how they wire together.

## Domain Wiring

```
auth
 └── instructor + admin role separation (ASVS-V4.1.1) gates course visibility
      ↓
crud (Course)
 └── POST /api/courses → draft/published/archived state machine
      ↓
crud (Lesson)
 └── POST /api/courses/{id}/lessons (ordered lesson sequence)
      ↓
crud (Enrollment)
 └── POST /api/courses/{id}/enrollments (bulk-enrollment idempotent — LMS-INV-005)
      ↓
audit-log
 └── @Audited("course.content.changed") + @Audited("course.visibility.changed")
      records operator + before/after
      ↓
scheduled-task
 └── Due-date reminder cron — distributed lock (SCHED-LOCK-001) +
      idempotency (SCHED-IDEMPOTENT-001) ensures no double-send on multi-node
      ↓
notification
 └── DueDateReached event → fanout filtered by NotificationPreferences
      (NOTIF-PREF-001) — learners receive reminder on opted-in channels only
```

## Domain Configuration Notes

### `crud`
- Three CRUD entities: `Course`, `Lesson`, `Enrollment`
- Lifecycle states (Course): `DRAFT` → `PUBLISHED` → `ARCHIVED`
- Soft-delete on Enrollment (`unenrolled_at`) — preserve learning-history audit trail
- Idempotency key required on POST per `practices/rules/idempotency-key-on-mutations.md`
- Reference: `templates/L4/crud/`

### `auth`
- Course-visibility transition gated by `hasAuthority('ROLE_ADMIN')` OR `course.authorId == principal.id`
- ASVS-V4.1.1 anchor (least-privilege enforcement at object level)
- Reference: `templates/L4/auth/`

### `audit-log`
- Annotate `CourseService.updateContent()` with `@Audited(action = "course.content.changed")`
- Annotate `CourseService.transitionVisibility()` with `@Audited(action = "course.visibility.changed")`
- Records: `operator_id`, `target_id`, `before_status`, `after_status`, `at`
- Retention: ≥90 days per `specs/audit-log-l0.yaml`
- Reference: `templates/L4/audit-log/`

### `scheduled-task`
- Register `DueDateReminderTask` with cron expression (e.g. `0 */15 * * * *`)
- `LockingPolicy.tryAcquire("due-date-reminder", node-id)` before scan/emit
- `SELECT FOR UPDATE SKIP LOCKED` on `scheduled_task_lock` row (or ShedLock)
- JobHistory row appended per run (SCHED-EXECUTE-001)
- Manual admin trigger routes through `executeWithLock()` (SCHED-IDEMPOTENT-001)
- Bulk-enrollment idempotency reuses the same `Idempotency-Key` header pattern
  (LMS-INV-005) — controller dedupes on (userId, courseId, idempotencyKey)
- Reference: `templates/L4/scheduled-task/`

### `notification`
- Subscribe to `DueDateReachedEvent` (lesson due-date within reminder window)
- Honor `NotificationPreferences` (NOTIF-PREF-001) — per-channel opt-out
- Channels: email (via email-outbox), in-app push (via notification-bell L2)
- Templates: `lesson_due_soon`, `lesson_overdue`, `course_completed`
- Reference: `templates/L4/notification/`

## Applied Recipe Annotation

Every L4 domain wired under this recipe **must** declare in its README.md:
```
applied_recipes:
  - lms
```
(Enforced by rule `business-domain-must-declare-applied-recipe` — SP37/R6 dual-form guard)

The 5 affected L4 READMEs (alphabetical: `audit-log`, `auth`, `crud`,
`notification`, `scheduled-task`) carry `lms` in their `applied_recipes:`
plural list. The R8 SP43 atomic commit appends `lms` alphabetically to 4 of
those L4 READMEs (`audit-log`, `auth`, `crud`, `notification` — all pre-existing
plural lists) and ships the *first* `applied_recipes:` key on
`templates/L4/scheduled-task/README.md` born `[cms, lms]` (R7 TD-020 follow-up
+ R8 TD-024 first-consumer-arrival convention).
