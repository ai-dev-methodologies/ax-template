# gap-convergence engine — static artifacts (wave 1)

This directory holds the **static, disk-truth-grounded artifacts** of the
autonomous MECE gap-convergence engine: a bounded, falsifiable answer to
"how much of the ax-template catalog's promised surface is actually,
mechanically covered — and where, precisely, is it not?"

It follows the design's Part 1, Part 2, and Part 5.1 verbatim. It does **not**
include the wave orchestrator (the loop that spends iterations closing gaps) —
that is implemented separately as a Workflow. Everything here is read/report
tooling plus one MECE map.

## What's here

| File | Role |
|---|---|
| `coverage-map.yaml` | The canonical 107-cell MECE map (frozen per wave). Every cell's `status` is grounded in disk truth by census — never optimistically marked. |
| `coverage-report.sh` + `lib/coverage_report.py` | Computes the weighted coverage metric, applies **honesty downgrades** (disk truth outranks the yaml's self-report), prints per-tier scores + the top-N uncovered cells, and (with `--write`) regenerates `docs/coverage-map/COVERAGE.md` + appends to `docs/coverage-map/coverage-history.jsonl`. |
| `coverage_map_guard.sh` + `lib/coverage_map_guard.py` + `fixtures/coverage_map_guard/` | The MECE/schema/disk-truth **guard**: closed-enum axes, exact cardinality (107 scored + masked-with-reason), D/R drift checks against `docs/IMPLEMENTATION-STATUS.md` / `recipes/_MANIFEST.yaml`, path resolution, and the honesty floor (`status: covered` with empty `nonvacuity` = FAIL). |
| `canary-gaps.yaml` | ≥6 planted, **verified-absent** needs (each with a stored grep/find absence-proof run at plant time) used by the wave orchestrator to test its own gap-detection reflex — falsifiability, not aspiration. |
| `fixtures/tampered-map-for-downgrade-test.yaml` | A 4-cell tampered fixture proving `coverage-report.sh`'s honesty-downgrade logic actually fires (not just declared). |

## The MECE space (Part 1)

The target space is the disjoint union of three subspaces, each answering a
different question over the Korean-enterprise React+Spring catalog:

- **S1 CAPABILITY** (65 cells) — "can a consumer build capability D at layer L
  using only the catalog?" `D` = the 25 consumer L4 domains from
  `docs/IMPLEMENTATION-STATUS.md` (20 full-trio × {BE,FE,XB} + 5 backend-only
  × {BE}, with FE/XB masked `not-applicable` for the backend-only 5).
- **S2 INVARIANT** (31 cells) — "is cross-cutting invariant family C
  mechanically enforced at layer L, non-vacuously?" `C` = 12 concerns
  (AUTHZ, AUTHN-SESSION, MONEY-QUANTITY, IDEMPOTENCY-CONCURRENCY,
  LIFECYCLE-STATE, AUDIT-PII, ERROR-CONTRACT, QUERY-BOUNDS, INPUT-VALIDATION,
  TIME-LOCALE, OBSERVABILITY-LIMITS, TENANCY-SCOPE) × layer, per the mask
  table in `lib/coverage_map_guard.py::S2_LAYERS`.
- **S3 COMPOSITION** (11 cells) — "does recipe R compose its L4 set into a
  working consumer feature with enforcement intact?" `R` = the 11
  `status: active` recipes in `recipes/_MANIFEST.yaml`.

**Total = 107 scored cells** + 15 masked (`not-applicable`, each carrying a
required `na_reason`) = 122 rows in `coverage-map.yaml`.

The cell set is **frozen per wave** (`frozen_wave: 1`). Discovering new
signature-space is out of scope for this static build — that belongs to
`proposed-cells.yaml` + a human-gated promotion, per the design's Part 3.8
(not built in wave 1; the orchestrator that would populate it is out of scope
here too).

## The metric (Part 2)

```
score(cell): covered=1.0, partial=0.5, gap=0.0   (not-applicable excluded)
C_total = Σ w(cell)·score(cell) / Σ w(cell)
```

Weights: S3 cells w=2; S2 cells for {AUTHZ, MONEY-QUANTITY,
IDEMPOTENCY-CONCURRENCY, AUDIT-PII, TENANCY-SCOPE} w=2, other S2 w=1; S1 w=1.

**Honesty downgrade (`coverage-report.sh`)** — the yaml's self-report never
outranks disk:
- `covered` cell whose `nonvacuity` path(s) don't resolve on disk → computed `partial`.
- `covered`/`partial` cell whose `covered_by` path(s) don't resolve → computed `gap`.
- a cell citing `backlog_ref` that isn't `[x]`-checked in `docs/BACKLOG.md` → downgraded one step, with a warning.

Every downgrade is printed loudly. On the real map (as built) this currently
prints **0 downgrades** — the map was authored honestly, cell by cell, against
disk truth from the start (see `notes` field per cell for the specific
grep/find evidence and the reasoning behind every `partial`/`gap` call). The
downgrade *mechanism* itself is separately proven live by
`fixtures/tampered-map-for-downgrade-test.yaml` (3/3 expected downgrades fire).

## How to run

```bash
# PyYAML is required. This machine's default python3 (homebrew) lacks it —
# use the shim, or call /usr/bin/python3 directly:
export PATH="$HOME/.pyshim:$PATH"

# 1. MECE/schema/disk-truth guard — exit 0 = PASS, exit 1 = FAIL (findings printed)
bash practices/consumer-proof/engine/coverage_map_guard.sh
bash practices/consumer-proof/engine/coverage_map_guard.sh --fixtures   # + fixture proof

# 2. Coverage metric — prints C_total, per-tier, top-N uncovered cells
bash practices/consumer-proof/engine/coverage-report.sh
bash practices/consumer-proof/engine/coverage-report.sh --top-n 10
bash practices/consumer-proof/engine/coverage-report.sh --write   # regenerates
    # docs/coverage-map/COVERAGE.md + appends docs/coverage-map/coverage-history.jsonl

# 3. Honesty-downgrade mechanism proof (tampered fixture, not the real map)
bash practices/consumer-proof/engine/coverage-report.sh \
    --map practices/consumer-proof/engine/fixtures/tampered-map-for-downgrade-test.yaml
```

## Isolation (Part 3.7 posture, applies even without the orchestrator)

`coverage_map_guard.sh` is **intentionally NOT registered** into
`practices/evals/run-all-guards.sh` in wave 1 — the engine stays isolated.
Running `practices/evals/run-all-guards.sh --include-fixtures` must stay green
whether or not this directory exists; that isolation was verified as part of
building this artifact set (see the build report).

## What this does NOT include (out of scope for this build)

- **The wave orchestrator** (`run-engine-wave.sh` in the design's file layout)
  — the 8-step loop (SELECT → DOGFOOD → ENFORCE → GAP-SURFACE → LOG →
  CLOSE-CHEAP → RE-MEASURE) that spends iterations closing gaps. Implemented
  separately as a Workflow.
- `proposed-cells.yaml` — the human-gated new-signature staging file; only
  meaningful once the orchestrator is running.
- `docs/coverage-map/BACKLOG-CANDIDATES.md` / `docs/coverage-map/WAVE-1-REPORT.md`
  / `docs/dogfood-ledger/engine-w1-iter*.yaml` — runtime artifacts the
  orchestrator produces per iteration; nothing to build statically here.

## Reading a cell

```yaml
- id: S1.payment.BE
  tier: S1
  domain: payment          # or `concern:` for S2, `recipe:` for S3
  layer: BE                # BE | FE | XB (null for S3 — recipes have no layer axis)
  weight: 1
  status: covered           # covered | partial | gap | not-applicable
  covered_by: [...]          # real disk paths — must resolve (glob-match >= 1)
  enforced_by: [...]         # binary checks (./gradlew testX, a guard path, a shell command) — free text OK, no resolution required
  nonvacuity: [...]          # REQUIRED for status=covered — a RED-able proof path; must resolve
  backlog_ref: null          # P#-# if a gap/partial has a registered BACKLOG row
  na_reason: null            # REQUIRED when status=not-applicable
  notes: "..."               # the specific evidence/reasoning for this call — read this before trusting the status
```

If you disagree with a cell's status, the `notes` field states exactly which
grep/find/test was run and why the call landed where it did — that is the
whole point of the honesty requirement: every judgment call is falsifiable
and re-checkable, not asserted.
