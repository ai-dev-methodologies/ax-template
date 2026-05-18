---
recipe: scheduler-l4
verdict_version: "1"
recorded_at: "2026-05-20"
agent_context: "context-0 — given only templates/L4/scheduled-task/README.md + practices/AGENTS.md"
result:
  must_score: 0
  must_total: 12
  should_score: 0
  should_total: 8
  verdict: PENDING
  threshold: "≥10/12 MUST + ≥5/8 SHOULD"
---

# Sealed Verdict — scheduler L4 (PENDING — SP42 will execute)

## Sealed Context (sub-agent input)

The sub-agent will receive **only** these two files at spawn time:

1. `templates/L4/scheduled-task/README.md`
2. `practices/AGENTS.md`

No other codebase context. No `specs/scheduled-task-l0.yaml` body, no contract,
no manifest. The README must be self-describing enough that a context-0 sub-agent
can re-derive the scheduler L4 primitive's shape from it alone (cross-referencing
catalog rules from `practices/AGENTS.md` only).

## Sub-Agent Prompt (SP42 will execute)

```
You are given two files:
  1. templates/L4/scheduled-task/README.md — the scheduler L4 reference workload
  2. practices/AGENTS.md — the ax-template practices catalog

Using ONLY these two files, describe the scheduler L4 primitive. Your answer
must cover:

a) The domain mode (full_trio | backend_only | frontend_only)
b) The 3 Spec Trio file paths anchoring this domain
c) At least 3 of the 4 spec families this primitive defines
d) The distributed-lock strategy named or implied by the README
e) Why the README has no applied_recipes: key today
f) At least one external library or framework that provides this primitive

Do not use any information outside the two provided files. If you cannot
answer a sub-question from those two files, say "not derivable from sealed
context".
```

## Sub-Agent Expected Answer (reasoning trace — to be confirmed at SP42)

A context-0 sub-agent reading the README + AGENTS.md should produce:

- **(a) Domain mode:** `backend_only` — README §"Domain Mode" header states this
  explicitly and §"Domain-specific spec requirements" lists no UI route table.
- **(b) Spec Trio paths:** `specs/scheduled-task-l0.yaml`,
  `contracts/scheduled-task-openapi.yaml`, `blueprints/scheduled-task-manifest.yaml`
  — all 3 named in §"Spec Trio anchors" bullet list.
- **(c) Spec families named:** REGISTER, LOCK, EXECUTE, IDEMPOTENCY — all 4 named
  in §"Compliance items (spec_ref summary)" table.
- **(d) Locking strategy:** DB-row `SELECT FOR UPDATE SKIP LOCKED` (primary
  pattern named in README §"How to fork this template" step 3) OR ShedLock +
  JDBC backend (advisory alternative named in the same step).
- **(e) No `applied_recipes:` key:** because no R7 recipe consumes scheduler
  (R8 LMS + CMS will be first consumers). README §"Composition" + the comment
  block in the same section explicitly cite the `file-storage` and `practices`
  precedent.
- **(f) External library/framework:** Spring `TaskScheduler` SPI (named in §"How
  to fork" step 2 with `@EnableScheduling`) OR Quartz Scheduler (named in
  README §"External evidence" verbatim block).

## MUST Rubric (12 items)

| # | Criterion |
|---|-----------|
| M1 | Names domain mode `backend_only` |
| M2 | Lists `specs/scheduled-task-l0.yaml` as the backend Spec |
| M3 | Lists `contracts/scheduled-task-openapi.yaml` as the contract |
| M4 | Lists `blueprints/scheduled-task-manifest.yaml` as the policy manifest |
| M5 | Names ≥3 of REGISTER / LOCK / EXECUTE / IDEMPOTENCY families |
| M6 | Identifies distributed-lock strategy (`SELECT FOR UPDATE SKIP LOCKED` OR ShedLock) |
| M7 | Identifies a job history mechanism (JobHistory row per execution) |
| M8 | Identifies stale-lock TTL recovery semantics (`lock_ttl_seconds`) |
| M9 | Explains why the README carries no `applied_recipes:` key today |
| M10 | Names Spring `TaskScheduler` OR Quartz as the underlying primitive |
| M11 | Does NOT invent additional L4 domains absent from README scope |
| M12 | Identifies manual admin trigger idempotency (concurrent trigger safe) |

**Threshold:** ≥10/12 MUST.

## SHOULD Rubric (8 items)

| # | Criterion |
|---|-----------|
| S1 | Names cron expression as the registration parameter |
| S2 | Names the `REGISTERED` initial status from `SCHED-REGISTER-001` |
| S3 | Names UUID as the generated task id |
| S4 | Identifies `lastRun` semantics (success-only mutation) |
| S5 | Lists the configuration knobs (`ax.scheduler.lock-ttl-seconds`, pool-size, retention) |
| S6 | Identifies R8 LMS or CMS as the first downstream consumer |
| S7 | Names the `Spring @EnableScheduling` enablement requirement |
| S8 | Identifies the multi-node duplicate-execution prevention motive for the lock |

**Threshold:** ≥5/8 SHOULD.

## Status

PENDING — sealed sub-agent harness will execute this verdict during SP42, after
SP41b community recipe ships. If MUST < 10 OR SHOULD < 5, SP42 holds the tag and
SP41b never starts (Option-4 SP41-gating).
