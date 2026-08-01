---
recipe: scheduler-l4
verdict_version: "3"
recorded_at: "2026-08-01"
agent_context: "context-0 — NO NEW sub-agent run. Corrects M1 only, reusing v2's already-committed real context-0 transcript verbatim, because that transcript already answers sub-question (a) correctly against the corrected criterion (see 'Why no re-run' below)."
transcript_path: "scheduler-l4-verdict-v2-transcript.md"
transcript_sha256: "10508da8224195bb42e046f9803d52b58028e19b8a5336b2245c746547662c42"
rubric_diff_vs_v2: "Exactly 1 of the 20 rubric items corrected: M1 (was: expects domain mode `backend_only`) → now: expects domain mode `full_trio`, per R49 (2026-05-25), which promoted scheduled-task from backend_only to full_trio 5 days after v1's recorded_at (2026-05-20) and predates v2's own recorded_at (2026-07-30). v2 scored M1 honestly ❌ against the STALE (pre-R49) criterion text because BACKLOG P3-105 records that correcting it was outside v2's authorized 2-item scope (M6 + M9 only). This file closes P3-105 by making that correction. All other 19 rubric item texts (M2-M5, M7-M8, M10-M12, S1-S8, plus the already-corrected M6/M9) are byte-identical to v2's."
result:
  must_score: 12
  must_total: 12
  should_score: 4
  should_total: 8
  verdict: FAIL
  threshold: "≥10/12 MUST + ≥5/8 SHOULD"
---

# Sealed Verdict v3 — scheduler L4 (BACKLOG P3-105 — M1 rubric-drift closure)

## Why this file exists

`scheduler-l4-verdict-v2.md` is SEALED and byte-identical (unedited by this wave — `git diff`
against it is empty). v2's own body text, at its M1 row and again in its closing "Additional
finding" paragraph, discloses that M1 is now ALSO stale: the criterion's literal text still says
`backend_only`, but R49 (2026-05-25) promoted scheduled-task to `full_trio` — a change that
predates v2's own `recorded_at` (2026-07-30) by more than two months. v2 explicitly recorded this
as "an ADDITIONAL stale-rubric finding, not authorized for correction in this wave" and BACKLOG
registered it as P3-105. This file is that authorized correction pass.

## Why no re-run of the sub-agent was needed

The corrected M1 asks whether the agent names `full_trio`. **v2's already-committed transcript
already does this, correctly and unprompted**, because v2's real context-0 sub-agent was reading
the CURRENT (post-R49) `templates/L4/scheduled-task/README.md` — the same sealed input file a
fresh v3 run would read today, since neither sealed input (`templates/L4/scheduled-task/README.md`
nor `practices/AGENTS.md`) has changed since v2's run (2026-07-30). Re-running SP42 against
unchanged sealed inputs cannot produce a different answer to sub-question (a) — it would
reproduce the identical transcript. Quoting the committed transcript
(`scheduler-l4-verdict-v2-transcript.md`, §"a) Domain mode"):

> `full_trio`. The README's "Domain Mode" section states: "**Status**: full-trio (R49 promoted,
> 2026-05-25)." It further notes this domain was originally `backend_only` at R7, and R49 added
> the admin Next.js surface ... closing out as "the last of the 19 L4 templates to reach
> full-trio."

This is the correct, current answer under the corrected M1. Scoring it against the corrected
criterion text is therefore a re-scoring of existing sealed evidence, not a new derivation — the
same category of operation v2 itself performed for M6/M9 relative to v1 (those two corrections
also did not require new *sealed-context facts*, only corrected criterion text; the difference
here is that v2's transcript IS an actual sub-agent run, so there is no simulation-vs-real gap to
resolve for M1 the way there was for M6/M9's inherited v1 answers).

**No re-run also means no new transcript file.** v3 continues to point at
`scheduler-l4-verdict-v2-transcript.md` as its sealed evidence (see `transcript_path` /
`transcript_sha256` in the frontmatter above). Per BACKLOG P3-108, this file's frontmatter records
the transcript's sha256 directly (the convention P3-108 introduces going forward); the binding for
this file and for v2 is additionally registered, mechanically checked, in
`skills/_tests/sealed-verdict/TRANSCRIPT-MANIFEST.yaml` and enforced by
`practices/evals/sealed_verdict_transcript_integrity_guard.sh` — v2's own header could not be
edited to carry this field (v1/v2 are sealed), so the registry is the mechanism that applies the
sha256 binding to a transcript-carrying verdict that predates the convention.

## Rubric correction (exactly 1 — see frontmatter `rubric_diff_vs_v2`)

- **M1** (was: "Names domain mode `backend_only`") → **now**: "Names domain mode `full_trio`", per
  BACKLOG P3-105 / R49 (2026-05-25).

All other 19 rubric item texts (M2-M5, M7-M8, M10-M12, S1-S8, plus v2's own M6/M9 corrections) are
byte-identical to v2's MUST/SHOULD tables and are **not re-litigated here** — see
`scheduler-l4-verdict-v2.md` for their full agent-answer text and scoring rationale. This file
re-states them in the table below (unchanged Pass/Fail column values, unchanged agent-answer
quotes) purely so the MUST/SHOULD tables remain self-contained per file, per this catalog's sealed-
verdict convention (v1 and v2 both do the same for the rows they did not change relative to their
predecessor).

## Sealed Context (sub-agent input) — unchanged from v1/v2

The sub-agent (v2's real run, reused here) received **only** these two files at spawn time:

1. `templates/L4/scheduled-task/README.md`
2. `practices/AGENTS.md`

No other codebase context. Neither file has changed since v2's 2026-07-30 run.

## MUST Rubric (12 items — M1 corrected this wave; M6, M9 corrected in v2; M2-M5, M7-M8, M10-M12 byte-identical to v1/v2)

| # | Criterion | Agent Answer (from the v2 transcript, reused) | Pass? |
|---|-----------|-------------|-------|
| M1 **(corrected)** | Names domain mode `full_trio` | Transcript (a): `full_trio` — README §"Domain Mode": "Status: full-trio (R49 promoted, 2026-05-25)" | ✅ |
| M2 | Lists `specs/scheduled-task-l0.yaml` as the backend Spec | Named explicitly in (b) | ✅ |
| M3 | Lists `contracts/scheduled-task-openapi.yaml` as the contract | Named explicitly in (b) | ✅ |
| M4 | Lists `blueprints/scheduled-task-manifest.yaml` as the policy manifest | Named explicitly in (b) | ✅ |
| M5 | Names ≥3 of REGISTER / LOCK / EXECUTE / IDEMPOTENCY families | All 4 named in (c), with per-family compliance-item citations; agent additionally surfaced a possible 5th RETENTION family found only in `practices/AGENTS.md` | ✅ |
| M6 | Identifies distributed-lock strategy (`SELECT ... FOR UPDATE` + TTL stale-reclaim, explicitly NOT `SKIP LOCKED` — OR ShedLock) | (d): names DB-row `SELECT ... FOR UPDATE` via `findByIdForUpdate`, explicitly states the README's own instruction NOT to use `SKIP LOCKED`, names ShedLock as the alternative | ✅ |
| M7 | Identifies a job history mechanism (JobHistory row per execution) | (c): "every run records JobHistory with start/end time, status, and error message" | ✅ |
| M8 | Identifies stale-lock TTL recovery semantics (`lock_ttl_seconds`) | (c): "stale-lock TTL expiry/re-acquisition" tied to SCHED-LOCK-002 | ✅ |
| M9 | Correctly identifies that the README DOES carry an `applied_recipes:` key today, and explains when/why it was born | Opens with an explicit "Contradiction flag", then under (e) confirms the key IS present, explains R8 SP43 first-consumer-arrival convention | ✅ |
| M10 | Names Spring `TaskScheduler` OR Quartz as the underlying primitive | (f): both named, each with a verbatim quote | ✅ |
| M11 | Does NOT invent additional L4 domains absent from README scope | No hallucinated domains | ✅ |
| M12 | Identifies manual admin trigger idempotency (concurrent trigger safe) | (c): "SCHED-IDEMPOTENT-001 ... manual admin trigger is safe under concurrent calls because the lock guarantees single-fire" | ✅ |

**MUST: 12 / 12** (up from v2's 11/12 — the single change is M1 flipping ❌→✅ under the corrected
criterion; this is fidelity to the now-current criterion, not a new agent capability.)

## SHOULD Rubric (8 items — byte-identical to v1/v2)

| # | Criterion | Agent Answer (from the v2 transcript, reused) | Pass? |
|---|-----------|-------------|-------|
| S1 | Names cron expression as the registration parameter | (c): "register() persists a task with cron + status REGISTERED" | ✅ |
| S2 | Names the `REGISTERED` initial status from `SCHED-REGISTER-001` | Same citation as S1 | ✅ |
| S3 | Names UUID as the generated task id | Not mentioned anywhere in the transcript | ❌ |
| S4 | Identifies `lastRun` semantics (success-only mutation) | Not mentioned anywhere in the transcript | ❌ |
| S5 | Lists the configuration knobs (`ax.scheduler.lock-ttl-seconds`, pool-size, retention) | Not listed | ❌ |
| S6 | Identifies R8 LMS or CMS as the first downstream consumer | (e): "LMS for due-date reminders, CMS for scheduled publish/archive ... arriving together in one atomic commit," tied to R8 SP43 | ✅ |
| S7 | Names the `Spring @EnableScheduling` enablement requirement | Not mentioned | ❌ |
| S8 | Identifies the multi-node duplicate-execution prevention motive for the lock | (d): explicitly discusses the race where "both nodes acquire" absent a proper lock | ✅ |

**SHOULD: 4 / 8** (unchanged from v2 — no SHOULD-tier criterion was in this wave's authorized
scope, and none of their answers depend on the M1 domain-mode question).

## Verdict

```
MUST:   12 / 12  ✅  (threshold: ≥10)   — up from v2's 11/12; M1 corrected
SHOULD:  4 /  8  ❌  (threshold: ≥5)    — unchanged from v2
VERDICT: FAIL
```

**Still FAIL, and correctly so.** MUST tier is now fully satisfied (12/12) — the M1 correction
removed the only rubric-drift false-negative in that tier. The SHOULD tier gap is exactly what v2
recorded: `SELECT ... FOR UPDATE` between the two composite thresholds means BOTH must clear for a
PASS verdict, and SHOULD sits at 4/8 against a ≥5/8 bar — the same real, non-simulated
implementation-mechanics gap v2 found (task-id UUID generation, `lastRun` success-only semantics,
the three configuration knobs, `@EnableScheduling`), none of which this wave's single-item M1
correction touches or explains away.

**Fidelity note (per the standing "seals are history, not config" principle):** v1 and v2 remain
byte-identical and unedited. This file supersedes v2 as the current sealed-test record for
scheduler-l4; v2 stays as historical evidence of the M6/M9 correction pass and of the real
context-0 transcript it introduced (which this file continues to cite, unmodified, per its sha256
binding).

**Residual, stated honestly:** all 20 rubric items across the scheduler-l4 sealed-verdict lineage
(v1→v2→v3) have now been reviewed for staleness at least once. No further staleness is known at
this time. Should a future domain-mode/README change make any item stale again, the same
sealed-the-sealed-way procedure applies: a new versioned file, an explicit `rubric_diff_vs_v<N>`,
and — only if the sealed inputs themselves changed — a fresh context-0 sub-agent run with its own
committed transcript and sha256 binding.
