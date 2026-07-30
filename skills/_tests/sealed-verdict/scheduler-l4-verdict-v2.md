---
recipe: scheduler-l4
verdict_version: "2"
recorded_at: "2026-07-30"
agent_context: "context-0 — REAL sub-agent run (Agent tool, model=sonnet), given only templates/L4/scheduled-task/README.md + practices/AGENTS.md. v1 (verdict_version 1) was a SELF-RECORDED SIMULATION of this run (see v1 §'Sub-Agent Derived Answer (context-0 simulation ...)' at v1 line 51) — this file is the first ACTUAL execution of the sealed test."
rubric_diff_vs_v1: "Exactly 2 of the 20 rubric items (12 MUST + 8 SHOULD) were corrected before scoring; the other 18 are byte-identical to v1. M6: v1's expected lock strategy included `SELECT FOR UPDATE SKIP LOCKED` as a valid phrasing; BACKLOG P2-48 (landed after v1 was recorded) determined SKIP LOCKED is UNSAFE for this lock shape (H2 unsupported + makes a held row look absent instead of blocking), so the README now explicitly instructs forkers NOT to use SKIP LOCKED — M6 is corrected to require SELECT...FOR UPDATE + TTL stale-reclaim (or ShedLock), never SKIP LOCKED. M9: v1's criterion asked whether the agent explains why the README carries NO applied_recipes key; the README has carried that key since R8 (2026-05-21, TD-2026-05-21-024) — before v1 was even recorded (2026-05-20 recorded_at predates R8, so v1's own simulated answer was ALREADY answering a question whose premise flipped days later and was never re-checked) — so M9 is corrected to require identifying that the key IS present today and explaining when/why it was born (first-consumer-arrival convention)."
result:
  must_score: 11
  must_total: 12
  should_score: 4
  should_total: 8
  verdict: FAIL
  threshold: "≥10/12 MUST + ≥5/8 SHOULD"
---

# Sealed Verdict v2 — scheduler L4 (SP42 executed for real, BACKLOG P3-101)

## Why this file exists

`scheduler-l4-verdict.md` (v1) is SEALED and byte-identical (verified: `git diff` against it is
empty). v1's own body text discloses, at its §"Sub-Agent Derived Answer" heading (v1 line 51),
that its answer was a **"context-0 simulation"** — i.e., the person authoring v1 role-played what
a sealed sub-agent would plausibly answer, rather than actually spawning one. BACKLOG P3-101
records that no guard reads this file (so it isn't blocking), but the honest thing to do with a
self-disclosed simulation is to re-run it for real — which is what this file records.

**A lower v2 score than v1's is expected and is a FIDELITY upgrade, not a regression**: v1's
11/12 + 7/8 was produced by an author simulating agent behavior; v2's 11/12 + 4/8 (FAIL) is
produced by an actual model, under actual sealed-context instructions, actually reading the actual
current files — some SHOULD-tier details the simulation assumed an agent would surface, a real
agent given the terse 6-point (a)-(f) prompt simply didn't volunteer. That gap is real information
about the README's self-describability that the simulation could not have surfaced.

## Rubric corrections (exactly 2 — see frontmatter `rubric_diff_vs_v1` for the full rationale)

- **M6** (was: "Identifies distributed-lock strategy (`SELECT FOR UPDATE SKIP LOCKED` OR
  ShedLock)") → **now**: "Identifies distributed-lock strategy (`SELECT ... FOR UPDATE` + TTL
  stale-reclaim, explicitly NOT `SKIP LOCKED` — OR ShedLock)", per BACKLOG P2-48.
- **M9** (was: "Explains why the README carries no `applied_recipes:` key today") → **now**:
  "Correctly identifies that the README DOES carry an `applied_recipes:` key today, and explains
  when/why it was born (first-consumer-arrival convention, R8 SP43)" — README:107 has carried the
  key since R8, which predates v1's own `recorded_at` (2026-05-20 < R8's 2026-05-21).

All other 18 rubric item texts below are byte-identical to v1's MUST/SHOULD tables.

## Sealed Context (sub-agent input) — unchanged from v1

The sub-agent received **only** these two files at spawn time:

1. `templates/L4/scheduled-task/README.md`
2. `practices/AGENTS.md`

No other codebase context. No `specs/scheduled-task-l0.yaml` body, no contract, no manifest.

## Sub-Agent Prompt (SP42 — run verbatim, unmodified from v1)

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

Note: sub-question (e) is itself now stale (the README's `applied_recipes:` key has existed since
R8) — it was run **verbatim anyway**, exactly as v1 wrote it, because the point of a context-0 run
is fidelity to the real artifact, not a pre-cleaned test. The transcript shows the real agent
noticed and flagged this on its own; that is precisely the "real README self-describability
finding" this re-run exists to surface.

## Execution receipt

- **Real context-0 sub-agent run — not a simulation.** Full raw transcript committed verbatim at
  `skills/_tests/sealed-verdict/scheduler-l4-verdict-v2-transcript.md`, with an invocation header
  (model: claude-sonnet-5 / Agent tool `model: sonnet`; files provided: the same two paths above;
  date: 2026-07-30).
- The transcript is quoted/paraphrased below only as needed to justify each rubric row; the
  transcript file is the source of truth.

## MUST Rubric (12 items — M6, M9 corrected; M1-M5, M7-M8, M10-M12 byte-identical to v1)

| # | Criterion | Agent Answer (from the real v2 transcript) | Pass? |
|---|-----------|-------------|-------|
| M1 | Names domain mode `backend_only` | Transcript names `full_trio` (README §"Domain Mode": "Status: full-trio (R49 promoted, 2026-05-25)"). This is the **correct current answer** — but it does not match this criterion's literal text, because the criterion itself is now stale: the domain was promoted from `backend_only` to `full_trio` at R49 (2026-05-25), 5 days AFTER v1's `recorded_at` (2026-05-20), and this wave's rubric-correction scope was limited to M6+M9 only, so M1's text was left byte-identical per the PRD's "exactly two" mandate. **Flagged as a genuine ADDITIONAL stale-rubric finding, not authorized for correction in this wave** — recommend a future rubric-correction pass adds M1 alongside M6/M9. | ❌ (rubric-drift, not an agent defect — see note) |
| M2 | Lists `specs/scheduled-task-l0.yaml` as the backend Spec | Named explicitly in (b) | ✅ |
| M3 | Lists `contracts/scheduled-task-openapi.yaml` as the contract | Named explicitly in (b) | ✅ |
| M4 | Lists `blueprints/scheduled-task-manifest.yaml` as the policy manifest | Named explicitly in (b) | ✅ |
| M5 | Names ≥3 of REGISTER / LOCK / EXECUTE / IDEMPOTENCY families | All 4 named in (c), with per-family compliance-item citations; agent additionally (correctly, with appropriate "not derivable" caveat) surfaced a possible 5th RETENTION family found only in `practices/AGENTS.md` | ✅ |
| M6 **(corrected)** | Identifies distributed-lock strategy (`SELECT ... FOR UPDATE` + TTL stale-reclaim, explicitly NOT `SKIP LOCKED` — OR ShedLock) | (d): names DB-row `SELECT ... FOR UPDATE` via `findByIdForUpdate`, explicitly states the README's own instruction NOT to use `SKIP LOCKED` (H2 unsupported + "makes a held row look ABSENT... rather than making it wait"), names ShedLock as the alternative; TTL stale-reclaim covered via (c)'s SCHED-LOCK-002 citation | ✅ |
| M7 | Identifies a job history mechanism (JobHistory row per execution) | (c): "every run records JobHistory with start/end time, status, and error message" | ✅ |
| M8 | Identifies stale-lock TTL recovery semantics (`lock_ttl_seconds`) | (c): "stale-lock TTL expiry/re-acquisition" tied to SCHED-LOCK-002; conceptually correct, does not repeat the literal property name `lock_ttl_seconds` (same paraphrase-is-fine bar v1 applied to non-M12/S8 items) | ✅ |
| M9 **(corrected)** | Correctly identifies that the README DOES carry an `applied_recipes:` key today, and explains when/why it was born (first-consumer-arrival convention, R8 SP43) | Opens with an explicit "Contradiction flag" naming the stale premise, then under (e): confirms the key IS present with its 4 current entries, explains it was **born in R8 SP43** under the first-consumer-arrival convention (TD-2026-05-21-024 + TD-2026-05-20-020), names LMS+CMS as the simultaneous first consumers, notes the later R9 SP45b alphabetical insert of `internal-it`, and correctly reframes what the question likely intended (pre-R8 absence) with the right convention-based answer | ✅ (best-answered item in the transcript) |
| M10 | Names Spring `TaskScheduler` OR Quartz as the underlying primitive | (f): both named, each with a verbatim quote | ✅ |
| M11 | Does NOT invent additional L4 domains absent from README scope | No hallucinated domains; the one extra fact pulled from `AGENTS.md` (a RETENTION spec_ref) stays within the scheduled-task domain and is explicitly caveated as not fully confirmable | ✅ |
| M12 | Identifies manual admin trigger idempotency (concurrent trigger safe) | (c): "SCHED-IDEMPOTENT-001 (manual admin trigger is safe under concurrent calls because the lock guarantees single-fire)" — names the exact spec ID, unlike v1's own simulated answer which v1 itself docked for lacking it | ✅ (stronger than v1's simulated answer) |

**MUST: 11 / 12**

## SHOULD Rubric (8 items — byte-identical to v1)

| # | Criterion | Agent Answer (from the real v2 transcript) | Pass? |
|---|-----------|-------------|-------|
| S1 | Names cron expression as the registration parameter | (c): "register() persists a task with cron + status REGISTERED" | ✅ |
| S2 | Names the `REGISTERED` initial status from `SCHED-REGISTER-001` | Same citation as S1: "...+ status REGISTERED" | ✅ |
| S3 | Names UUID as the generated task id | Not mentioned anywhere in the transcript — the agent never discussed task-id generation | ❌ |
| S4 | Identifies `lastRun` semantics (success-only mutation) | Not mentioned anywhere in the transcript | ❌ |
| S5 | Lists the configuration knobs (`ax.scheduler.lock-ttl-seconds`, pool-size, retention) | Not listed — the transcript never cites the "Configuration knobs" section of the README | ❌ |
| S6 | Identifies R8 LMS or CMS as the first downstream consumer | (e): "LMS for due-date reminders, CMS for scheduled publish/archive — arriving together in one atomic commit," explicitly tied to R8 SP43 | ✅ |
| S7 | Names the `Spring @EnableScheduling` enablement requirement | Not mentioned — (f) names the `TaskScheduler` SPI and its verbatim quote but never cites the `@EnableScheduling` annotation from the README's "How to fork" step 2 | ❌ |
| S8 | Identifies the multi-node duplicate-execution prevention motive for the lock | (d): explicitly discusses the race where "both nodes acquire" absent a proper lock, and frames `SELECT ... FOR UPDATE` / ShedLock as preventing exactly that — the multi-node duplicate-execution motive is stated, not merely implied | ✅ |

**SHOULD: 4 / 8**

## Verdict

```
MUST:   11 / 12  ✅  (threshold: ≥10)
SHOULD:  4 /  8  ❌  (threshold: ≥5)
VERDICT: FAIL
```

**This FAIL is a real, non-simulated result and is the intended outcome of running the test for
real.** The MUST tier — the primitive's discoverable *shape* (domain mode value aside, which is a
flagged rubric-drift casualty, not an agent miss) — holds comfortably. The SHOULD tier gap is
concentrated in **step-level "how to fork" mechanics that the 6-point (a)-(f) prompt never asked
for**: task-id generation (UUID), `lastRun` success-only semantics, the three named configuration
knobs, and the `@EnableScheduling` annotation. A real context-0 agent answering a terse 6-question
prompt reasonably does not volunteer implementation-mechanics details the prompt didn't request,
even though they're present in the sealed README — this is a genuine README/prompt-alignment
finding: **either the SP42 prompt should ask about these SHOULD-tier mechanics explicitly, or the
SHOULD bar should be understood as "present in the README, not necessarily volunteered under this
specific 6-point prompt."** Recorded as scored, not softened.

**Additional finding beyond the 2 authorized corrections:** M1 is now ALSO stale (domain promoted
`backend_only → full_trio` at R49, 2026-05-25, after v1's `recorded_at`). This was not corrected
in this wave because the wave's authorized scope was exactly M6 + M9; it is recorded here as a
candidate for a future rubric-correction pass rather than silently patched.

**Fidelity note (per the standing "seals are history, not config" principle):** v1 remains
byte-identical and unedited (`git diff` empty). This file supersedes v1 as the ACTUAL sealed-test
record; v1 stays as historical evidence that its own answer was self-disclosed as a simulation.
