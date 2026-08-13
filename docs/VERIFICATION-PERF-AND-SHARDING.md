# Verification cost + CI sharding design

> BACKLOG **P2-8**: as the reference workload grows (was ~30 domains, now **78
> `test{Domain}` tasks** over **136 `@SpringBootTest` classes**), the single-machine
> `verify-completion.sh` run gets slower and the per-step kill-switches need headroom.
> This document (a) defines a **perf-log schema** so verification cost is observed over
> time rather than guessed, and (b) designs a **CI-sharding** strategy for when the matrix
> outgrows one runner. It is a *design* — it does not mandate that any fork-receiver adopt
> CI (the catalog stays CI-agnostic; this is a reference shape).

## 1. Observed cost (this matrix, 2026-06)

The R25 step kill-switches in `practices/verification-checklist.yaml` already encode
observed reality:

| Step | Timeout | Observed |
|---|---|---|
| `build -x test` | 900s | fast (compile only) |
| **per-domain `test{Domain}` suite (78 tasks)** | **2700s** | 15–16 min on a warm daemon at ~30 domains; **17–23 min observed under CPU contention** when worktree builds ran concurrently (this session) |
| hard-guards (`run-all-guards.sh`, 80 guards) | 900s | ~1–2 min |
| catalog-meta-guards | 900s | seconds |
| aggregate `./gradlew test` | 900s | dominated by Spring context boots |

**Cost driver = Spring context boots, not test logic.** 136 `@SpringBootTest` classes; most
share a context config (the TestContext cache collapses identical configs to ONE cached
context), so the real cost is (a) the handful of *distinct* configs that each boot a context
and (b) any class forced to a fresh context via `@DirtiesContext` (see P2-5 — the
ContextCache-eviction fix). The matrix only grows; per-domain wall-clock grows ~linearly with
*distinct-config* count, not raw class count.

## 2. perf-log schema (observe, don't guess)

`verify-completion.sh` already appends one line per run to `.ax-verify/runs.jsonl`
(`ts / head_sha / exit / pass / warn_advisory / hard_fail / skip`). Extend it with timing so
cost is a time series, not a memory:

```jsonc
{
  "ts": "2026-06-24T...Z", "head_sha": "…", "exit": 0,
  "pass": 6, "warn_advisory": 2, "hard_fail": 0, "skip": 0,
  "total_seconds": 540,                         // wall-clock of the whole run
  "steps": [                                    // NEW — per-step durations
    {"step": "build", "seconds": 35, "exit": 0},
    {"step": "per-domain-tests", "seconds": 410, "exit": 0},
    {"step": "hard-guards", "seconds": 70, "exit": 0},
    {"step": "aggregate-regression", "seconds": 25, "exit": 1, "advisory": true}
  ],
  "domain_task_count": 78                        // matrix size at this run
}
```

Implementation: wrap each step invocation in `verify-completion.sh` with a `SECONDS=0` /
`$SECONDS` capture and emit the `steps[]` array. This is additive (existing readers ignore new
fields). With it, "per-domain-tests crossed 20 min" becomes a *detected* signal (plot
`steps[].seconds` vs `domain_task_count` over `ts`) that triggers sharding — instead of
discovering it via a timeout SIGTERM.

## 3. CI sharding (when one runner is not enough)

The per-domain step is **embarrassingly parallel** — each `test{Domain}` is independent (its
own `@Tag`, its own data). Shard by tag across N parallel CI jobs:

```
shard k of N:  ./gradlew <the test{Domain} tasks assigned to shard k>
```

- **Partition** the 78 tasks into N roughly-equal shards. Balance by *observed* cost (from
  §2's `steps`/per-task timing), not by count — a few context-heavy domains dominate.
- **Run-once steps** (`build`, the 4 hard gates, `run-all-guards`, catalog-meta-guards) run in
  ONE shard (shard 0) — they are fast and global; do NOT replicate them per shard.
- **Aggregate `./gradlew test`** (the cross-domain regression, currently advisory) can be its
  own shard or folded into shard 0.
- **R25 gate** = all shards green (+ the recency guard on the merge commit). A fork-receiver
  wiring this into CI maps each shard to a job; the matrix is the source of truth.

### Sharding gotchas (learned this session — load-bearing)

- **NEVER `./gradlew --stop` inside a shard.** It kills the shared Gradle daemon registry,
  including sibling shards' daemons → spurious "Gradle build daemon has been stopped" failures.
  Use `--no-daemon` per shard, or give each shard an isolated `GRADLE_USER_HOME`.
- **Don't over-pack a runner.** Concurrent JVMs contend for RAM; 4+ heavy Spring-context JVMs
  on one runner caused 17–23 min crawls (and one 23-min hang) this session. One shard per
  runner core-budget; let CI fan out across runners, not threads-on-one-box.
- **Pin heap** (`-Dorg.gradle.jvmargs=-Xmx2g`) per shard so an OOM kills one task, not the box.

## 4. Status

- **Perf-log: IMPLEMENTED (2026-08, D-11 / BACKLOG:420)** — `verify-completion.sh` appends one
  line per run to a **new, independent sidecar**, `.ax-verify/perf.jsonl`, timing each step with
  bash's `$SECONDS` builtin (`record_step_perf`, called once per step-loop iteration) and writing
  after the run completes (mirroring the existing `toolpaths.json` sidecar's write-after-audit,
  non-blocking, `O_NOFOLLOW`-guarded shape). One line looks like:
  ```jsonc
  {"ts": "...", "head_sha": "...", "total_seconds": 11,
   "steps": [{"id": "catalog-meta-guards", "seconds": 8}],
   "domain_task_count": 78, "full_run": false, "exit": 0,
   "note": "sidecar only — independent of runs.jsonl; no gate reads this file"}
  ```
  This is **not** the `steps[]`-extended `runs.jsonl` shape §2 originally sketched — see below for
  why the shape changed on implementation. `.ax-verify/` is already anchored in `.gitignore`
  (`/.ax-verify/`), so no ignore-rule change was needed. Structurally enforced as observability-only
  by `practices/evals/perf_log_no_gate_input_guard.sh` (not yet wired into `run-all-guards.sh` —
  see the report for that follow-up), which BLOCKs if anything under `practices/evals/*.sh`,
  `practices/scripts/*.sh` or `.githooks/*` ever reads `perf.jsonl`.

  **Why a sidecar and not an extension of `runs.jsonl`** (a real constraint discovered during
  implementation, not a stylistic choice): `runs.jsonl`'s field set is pinned byte-for-byte by
  `completion_checklist_recency_guard.sh` (`AUDIT_WRITER_SCHEMA_DRIFT` — see the "THIS printf IS
  THE AUDIT SCHEMA" comment in `verify-completion.sh`), and `.githooks/pre-push` additionally runs
  a **PRIOR RELEASE's copy** of that guard against the log at push time. Adding a `steps[]` field
  to the pinned schema would make every push fail `AUDIT_WRITER_SCHEMA_DRIFT` against the OLD
  guard until a release ships that both writes the new field AND accepts it — a two-release
  migration for a field no gate needs to read. A wholly separate file has no such pin and needs no
  migration, at the cost of not being byte-adjacent to the audit record (both share `head_sha`/`ts`
  for correlation instead).

- **CI sharding: still a design, deliberately NOT implemented this round.** §3's partition-by-tag
  strategy is unchanged and remains adoption-ready once the §2 time series (now genuinely being
  collected via `perf.jsonl`) shows the single-runner wall-clock warrants it — that measurement
  window is the explicit trigger, and it has not started long enough yet to justify building a
  shard matrix against it. A reference CI shard matrix stays a fork-receiver follow-up; the
  catalog stays CI-agnostic.
