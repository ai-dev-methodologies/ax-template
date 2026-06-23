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

- perf-log schema + sharding strategy: **designed** (this doc). The schema is additive and the
  sharding maps 1:1 to the existing `test{Domain}` tasks, so adoption is mechanical when the
  single-runner wall-clock warrants it (the §2 time series is the trigger).
- Implementing the `steps[]` timing emission in `verify-completion.sh` and a reference CI
  shard matrix are follow-ups a fork-receiver does per their CI; the catalog stays CI-agnostic.
