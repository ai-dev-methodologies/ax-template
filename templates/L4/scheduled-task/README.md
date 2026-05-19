# L4 / scheduled-task — Distributed-Lock Cron Scheduling Domain

**Tenant model**: `single` — per [`specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001`](../../../specs/multi-tenant-l0.yaml). This L4 reference workload ships as **single-tenant**. Recipes composing this domain into a multi-tenant SaaS (e.g. `b2b-admin` with `tenant_model: multi`) MUST adopt one of `MULTI-TENANT-ISOLATION-001` (Hibernate filter row-level) / `-002` (schema-per-tenant) / `-003` (AOP guard) plus `MULTI-TENANT-PROPAGATION-001` (request-scoped TenantContext) + `-002` (async propagation) before production. fork-receivers MUST NOT assume cross-tenant data isolation in this L4 as-shipped.

**ax-template R7 SP41** — Scheduled Task domain reference workload (catalog-only L4
primitive completion; spec + contract + manifest authored R3, README + scaffold
landed R7).

## Domain Mode

`backend_only` — distributed-lock cron jobs + job history + manual admin trigger;
no frontend UI in scope. R7 introduces the L4 README + scaffold. R8 LMS/CMS recipes
will be the first downstream consumers of this primitive.

## Overview

A backend-only scheduling domain: register cron tasks, acquire a per-task distributed
lock (DB-row `SELECT FOR UPDATE SKIP LOCKED`), execute on the holding node, record
every run in `JobHistory`, and expire stale locks after a configurable TTL so crashed
nodes self-heal.

Spec Trio anchors:
- `specs/scheduled-task-l0.yaml` (10 backend items: REGISTER + LOCK + EXECUTE + IDEMPOTENCY families)
- `contracts/scheduled-task-openapi.yaml`
- `blueprints/scheduled-task-manifest.yaml`

## Spec Trio (backend_only)

| File | Purpose |
|------|---------|
| `specs/scheduled-task-l0.yaml` | 10 compliance items across REGISTER / LOCK / EXECUTE / IDEMPOTENCY families |
| `contracts/scheduled-task-openapi.yaml` | OpenAPI 3.0 contract for the management endpoints (register, list, triggerManual, history) |
| `blueprints/scheduled-task-manifest.yaml` | Policy manifest (register · lock · execute · idempotency sections) |

## Compliance items (spec_ref summary)

| Spec ID | Chapter | Requirement (excerpt) |
|---|---|---|
| `SCHED-REGISTER-001` | REGISTER | `register()` persists a new task with cron expression and status `REGISTERED` |
| `SCHED-LOCK-001` | LOCK | `executeWithLock()` acquires a distributed lock before running; skips if held |
| `SCHED-LOCK-002` | LOCK | Stale locks expire after `lock_ttl_seconds` and are re-acquirable |
| `SCHED-EXECUTE-001` | EXECUTE | Every run records JobHistory with start/end time, status, and error message |
| `SCHED-IDEMPOTENT-001` | IDEMPOTENCY | Manual admin trigger is safe under concurrent calls (lock guarantees single-fire) |

(5 additional items in `specs/scheduled-task-l0.yaml`; see file for full list.)

## How to fork this template

1. **Copy the backend skeleton** (or roll your own ScheduledTask entity):
   ```bash
   cp -r templates/L4/scheduled-task/backend/* backend/src/main/java/com/<org>/scheduling/
   ```
   The shipped `ScheduledTask.java.skeleton` is a minimal JPA entity stub — rename
   to `.java`, set the package, and wire it into your Spring Boot module.

2. **Enable Spring scheduling** in your `@Configuration`:
   ```java
   @EnableScheduling
   @EnableAsync
   public class SchedulingConfig {
       // TaskScheduler bean with ThreadPoolTaskScheduler — see Spring Reference §Scheduling
   }
   ```

3. **Pick a locking strategy** (either is spec-compliant):
   - **DB row + `SELECT FOR UPDATE SKIP LOCKED`** — simplest; recommended for ≤ 5 nodes.
     Schema: `scheduled_task_lock(task_id PK, locked_at, lock_holder)`.
   - **ShedLock + JDBC backend** — battle-tested library wrapping the same pattern.
     See `blueprints/scheduled-task-manifest.yaml#lock` for advisory provider list.

4. **Wire JobHistory** — append a `JobHistory` row in a `try`/`finally` around every
   execution. `markSuccess()` or `markFailure(msg)` regardless of outcome. `lastRun`
   on the parent `ScheduledTask` updates only on success per `SCHED-EXECUTE-001`.

5. **Configuration knobs**:
   ```properties
   ax.scheduler.lock-ttl-seconds=300    # default; tune per workload
   ax.scheduler.pool-size=4
   ax.scheduler.history-retention-days=30
   ```

## Domain-specific spec requirements

| Spec ID | Requirement | Implementation hint |
|---|---|---|
| SCHED-REGISTER-001 | UUID + cron + REGISTERED status | `ScheduledTaskService.register()` factory + JPA save |
| SCHED-LOCK-001 | Acquire-or-skip distributed lock | `LockingPolicy.tryAcquire(taskId, holder)` + `SELECT FOR UPDATE SKIP LOCKED` |
| SCHED-LOCK-002 | TTL stale-lock recovery | `lockedAt + lock_ttl_seconds < now()` → reacquire allowed |
| SCHED-EXECUTE-001 | JobHistory append on every run | `JobHistory.start(...)` + `markSuccess()` / `markFailure(msg)` in `finally` |
| SCHED-IDEMPOTENT-001 | Concurrent manual trigger safe | `triggerManual()` routes through `executeWithLock()` |

## Composition

scheduled-task ships its first-consumer-arrival key in **R8 SP43** — LMS (due-date
reminders) and CMS (scheduled publish + scheduled archive) are the **first
downstream consumers**, arriving together in the same atomic commit per
`practices/DECISIONS.md` TD-2026-05-21-024 (first-consumer-arrival convention)
+ TD-2026-05-20-020 (R7 follow-up). Both recipe directories list
`scheduled-task` in their `enabled_l4_domains:` and trigger the
`applied_recipes:` key birth below.

applied_recipes:
  - api-gateway-relay
  - cms
  - internal-it
  - lms

The `applied_recipes:` key was **born** in R8 SP43 with two entries
alphabetical (`cms`, `lms`) per the first-consumer-arrival convention
(TD-2026-05-21-024 — simultaneous consumers as a single mutation). R9 SP45b
alphabetical-inserts `internal-it` between `cms` and `lms` (R6 dual-form
append-only rule). `file-storage` and `practices` L4 READMEs remain key-less
until *their* first consumers arrive (same precedent the scheduler README
itself relied on pre-R8).

## External evidence (verbatim, fetched 2026-05-20)

Two verbatim external quotes anchor this L4 domain — see
`practices/upstream/r7-sp41-scheduler-evidence.md` for the full snapshot:

- **Spring Framework Reference §Scheduling** (`https://docs.spring.io/spring-framework/reference/integration/scheduling.html`):
  > "In addition to the TaskExecutor abstraction, Spring has a TaskScheduler SPI with
  > a variety of methods for scheduling tasks to run at some point in the future."

- **Quartz Scheduler 2.3.0 Tutorial — Lesson 1** (`https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/tutorial-lesson-01.html`):
  > "Triggers do not fire (jobs do not execute) until the scheduler has been started"

Both quotes resolve the Critic L "binary verification" requirement at the catalog
level — scheduler is a real, externally-documented primitive whose semantics this
L4 catalog row faithfully reflects.

## Verification

```bash
# L4 catalog-discoverability gate (sealed sub-agent test)
bash skills/_tests/L4/scheduler-domain.test.sh

# Full guard suite (22+ guards, including recipe_governance + trio_integrity)
bash practices/evals/run-all-guards.sh

# Domain-specific trio integrity (when backend lands)
bash practices/evals/trio_integrity_guard.sh --domain scheduled-task
```

## Backend templates (skeleton)

`templates/L4/scheduled-task/backend/` currently ships only one stub:

- `ScheduledTask.java.skeleton` — minimal JPA entity stub (rename to `.java`,
  fill in `@Entity` package, repository, service)

R8 cycle (lms + cms recipes — `v1.6.0-lms-cms`) is the first-consumer event for
this L4: `applied_recipes: [cms, lms]` is now bound at the catalog level (TD-024
first-consumer convention). R8 deliberately ships recipes only — no backend
skeleton expansion in this PR per recipe-no-code principle. A fuller skeleton
(JobHistory entity, LockingPolicy interface, controller, service) is deferred to
a future scheduler backend-expansion cycle (independent of any specific recipe;
triggered by fork-receiver demand or AI-implementer needs).
