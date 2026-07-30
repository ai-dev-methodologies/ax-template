# Sealed Verdict v2 — Real Context-0 Transcript (scheduler L4, SP42)

## Invocation header

- **Model:** claude-sonnet-5 (Anthropic Claude Agent SDK, general-purpose agent, `model: sonnet`)
- **Date:** 2026-07-30
- **Files provided (exactly two, absolute paths):**
  1. `templates/L4/scheduled-task/README.md`
  2. `practices/AGENTS.md`
- **Isolation mechanism:** fresh sub-agent spawn with no prior conversation memory (this
  conversation's context is not visible to it); instructed to use the Read tool ONLY on the
  two paths above and no other tool/file. This is a behavioral/instructional seal, not a
  hard technical sandbox — the agent's tool access is not mechanically restricted to those
  two paths, so the seal's integrity rests on the agent following the instruction. It reported
  using only those two Read targets and no other tool.
- **Prompt given:** the SP42 prompt from `scheduler-l4-verdict.md` §"Sub-Agent Prompt (SP42
  will execute)", reproduced verbatim (unmodified from v1, including sub-question (e) which
  is now factually stale against the current README — see below).
- **This file is the sub-agent's raw final-message output, verbatim, unedited.** Scoring
  against the corrected rubric is done separately in `scheduler-l4-verdict-v2.md`.

---

## Raw transcript (verbatim sub-agent output)

## Answer

**Contradiction flag (required by the task instructions):** Sub-question (e) presumes "the README has no `applied_recipes:` key today." That premise is false per the sealed file itself — the README (file 1) contains an `applied_recipes:` block (lines 107–119) with four entries already populated: `api-gateway-relay`, `cms`, `internal-it`, `lms`. I answer (e) honestly against what the file actually says rather than the premise.

**a) Domain mode**

`full_trio`. The README's "Domain Mode" section states: "**Status**: full-trio (R49 promoted, 2026-05-25)." It further notes this domain was originally `backend_only` at R7, and R49 added the admin Next.js surface (task list with enable/disable + manual trigger, per-task execution history with a 10s poll + Refresh), closing out as "the last of the 19 L4 templates to reach full-trio."

**b) The 3 Spec Trio file paths**

- `specs/scheduled-task-l0.yaml`
- `contracts/scheduled-task-openapi.yaml`
- `blueprints/scheduled-task-manifest.yaml`

(Listed both in the "Spec Trio anchors" bullet list and again in the "Spec Trio (backend_only)" table of the README.)

**c) At least 3 of the 4 spec families**

The README names exactly four families spanning 10 backend items: **REGISTER**, **LOCK**, **EXECUTE**, **IDEMPOTENCY**. Representative compliance items given for each: `SCHED-REGISTER-001` (register() persists a task with cron + status REGISTERED), `SCHED-LOCK-001`/`SCHED-LOCK-002` (acquire-or-skip distributed lock; stale-lock TTL expiry/re-acquisition), `SCHED-EXECUTE-001` (every run records JobHistory with start/end/status/error), `SCHED-IDEMPOTENT-001` (manual admin trigger is safe under concurrent calls because the lock guarantees single-fire).

Notably, `practices/AGENTS.md` (file 2) surfaces a rule — `retention-delete-on-high-volume-table-must-be-bounded.md` — whose `spec_ref` is `specs/scheduled-task-l0.yaml#SCHED-RETENTION-001`, i.e. a fifth family, **RETENTION**, not among the four the README names. This is corroborating (not contradicting) detail from file 2 beyond what file 1 states, and it means the README's "5 additional items in `specs/scheduled-task-l0.yaml`; see file for full list" plausibly includes at least one RETENTION item — but the full family breakdown of those 5 additional items is **not derivable from sealed context** (neither file enumerates them completely).

**d) Distributed-lock strategy**

Two spec-compliant options are named in the README's "How to fork this template" step 3:

1. **DB row + `SELECT ... FOR UPDATE`** — "simplest; recommended for ≤ 5 nodes." Schema: `scheduled_task_lock(task_id PK, locked_at, lock_holder)`. The row lock must be taken on the read (`findByIdForUpdate`) so a racer blocks and re-reads the winner's row — a plain `findById` + save is explicitly called out as a read-then-write race letting both nodes acquire (cited as `BACKLOG P2-48`). The README explicitly says **do NOT** use `SKIP LOCKED` here, because it makes a held row look ABSENT to the loser instead of making it wait, and H2 doesn't support it.
2. **ShedLock + JDBC backend** — "battle-tested library wrapping the same pattern," with the manifest (`blueprints/scheduled-task-manifest.yaml#lock`) holding an advisory provider list.

The shared constraint for either choice: the free-or-stale TEST and the claiming WRITE must be one indivisible step, and a lost race must RETURN false rather than throw.

**e) Why the README has no `applied_recipes:` key today — contradicted by the file**

As flagged above, this premise does not hold: the README **does** carry an `applied_recipes:` key, populated with `api-gateway-relay`, `cms`, `internal-it`, `lms`. Per the README's "Composition" section, the key was **born in R8 SP43** under a "first-consumer-arrival convention" (`practices/DECISIONS.md` TD-2026-05-21-024 + TD-2026-05-20-020): the key only appears once real downstream consumers arrive (LMS for due-date reminders, CMS for scheduled publish/archive — arriving together in one atomic commit), rather than being scaffolded empty in advance. `internal-it` was later alphabetically inserted between `cms` and `lms` at R9 SP45b under a "R6 dual-form append-only rule." The README adds that sibling L4s (`file-storage`, `practices`) remain **key-less** until *their* first consumers arrive — the same precedent this very README relied on before R8. So if the intended question is "why did this README **originally** ship without the key (pre-R8)," the README's own answer is: the key is deliberately withheld until first-consumer-arrival, as a matter of convention, not omission.

**f) External library or framework**

The README's "External evidence" section cites two, each with a verbatim quote sourced from `practices/upstream/r7-sp41-scheduler-evidence.md`:

- **Spring Framework's `TaskScheduler`** (Spring Framework Reference §Scheduling): "In addition to the TaskExecutor abstraction, Spring has a TaskScheduler SPI with a variety of methods for scheduling tasks to run at some point in the future."
- **Quartz Scheduler 2.3.0** (Tutorial Lesson 1): "Triggers do not fire (jobs do not execute) until the scheduler has been started."

Additionally, the "Pick a locking strategy" step names **ShedLock** as an alternative library providing the distributed-lock primitive (see d).

---

*(End of raw transcript. Reported tool usage per the agent framework: 13 tool calls, all against
the two sealed paths per the agent's own account; duration 149622 ms; no other file paths were
reported as read.)*
