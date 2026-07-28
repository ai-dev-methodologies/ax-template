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
| `coverage_map_guard.sh` + `lib/coverage_map_guard.py` + `fixtures/coverage_map_guard/` | The MECE/schema/disk-truth **guard**: closed-enum axes, exact cardinality (107 scored + masked-with-reason), D/R drift checks against `docs/IMPLEMENTATION-STATUS.md` / `recipes/_MANIFEST.yaml`, path resolution, the honesty floor (`status: covered` with empty `nonvacuity` = FAIL), the weight-tamper guard, (P2-29/P3-58) the S3 composition-behavioral nonvacuity bar (incl. the
fs.existsSync/readFileSync-only rename-bypass content check), and (P3-60) the S1/S2 `.md`-only
nonvacuity floor — see below. |
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

## The S3 (COMPOSITION) nonvacuity bar (P2-29)

Wave-2 surfaced a composition-escape: `coverage_map_guard.sh` check 4 only asserted that a
cell's `nonvacuity` path(s) **resolve on disk** — it never required the resolved artifact to
actually be a live, behavioral, cross-domain proof. A sealed-verdict `.md` record (a static,
scored quiz that can never go RED again) and a bare Playwright compose-spec that only asserts
`RECIPE.md`/frontmatter file-existence both resolve-on-disk and therefore both silently
qualified an S3 cell for `status: covered`. 9 of the 11 `S3` recipe cells were briefly marked
`covered` this way before being disk-truth downgraded back to `partial` — only `S3.e-commerce`
survived review as genuinely covered.

An S3 cell may carry **`status: covered` only if at least one of its `nonvacuity` entries is a
proof that is**:

1. **live and re-executable** — a real test file, never a markdown verdict record and never a
   bare existence-check artifact. In file-path shape: matches `*Test.java`, `*IT.java`,
   `*.vitest.*`, or `*.spec.*` — and explicitly does **not** match a bare `*-compose.spec.*`
   file (this catalog's established naming convention for a Playwright spec that only checks
   `fs.existsSync(...)` on `RECIPE.md`/frontmatter/spec-trio files, e.g.
   `frontend/tests/recipes/booking-compose.spec.ts` — see the `notes` field on every
   non-e-commerce S3 cell for the disk-verified reasoning behind that exclusion);
2. **drives >= 2 of the recipe's enabled L4 domains through ONE runtime flow** — a single test
   run that composes multiple domain services/controllers in sequence, not N independent
   per-domain unit tests merely bundled under one file/class;
3. **asserts an invariant at the cross-domain seam** — the assertion is about the
   *interaction* between domains (e.g. "payment captured ⇒ order transitions to PAID, in the
   same transaction"), not merely that each domain's own local invariant holds in isolation;
4. **RED-able when that seam invariant is reverted** — breaking the cross-domain wiring (not a
   single domain's internal logic) must flip the test from green to red.

The model instance is
`backend/src/test/java/com/ax/template/authblueprint/ecommerce/EcommerceE2ETest.java`
(`@Tag("ECOMMERCE")`), backing `S3.e-commerce`: it composes crud + payment + notification +
audit-log + search through one HTTP-driven checkout flow, and `ECOM-INV-002` asserts
`payment captured ⇒ order.status == PAID` (with `paymentId` set) atomically, in the same
response — reverting the atomic write flips that assertion RED. Any future S3 cell claiming
`covered` should point to an equivalent capstone-style test for its own recipe.

**Mechanical enforcement vs. review**: `coverage_map_guard.sh` check 7 enforces criterion (1)
only — a path-pattern match plus the explicit `*-compose.spec.*` denylist, `.md` exclusion,
and (P3-58) a content check that rejects any file whose only assertions trace back to
`fs.existsSync`/`fs.readFileSync` regardless of filename (closes the rename-bypass: renaming
a bare compose-spec off the `-compose.spec.*` convention used to silently re-qualify it).
Criteria (2)/(3)/(4) are properties of the cited test's *content*, which a schema/path guard
cannot verify by construction; they remain a human/adversarial-review judgment call recorded in
the cell's `notes` field, the same posture the engine already takes for every other disk-truth
judgment call (see "Reading a cell" below). Check 7 is therefore a **necessary, not
sufficient**, mechanical floor: it closes the "any resolvable path passes" escape, but citing a
genuinely-live, genuinely-RED-able test that turns out to be a shallow per-fixture rule-firing
probe (not a real cross-domain runtime flow) still requires a `notes` justification and is still
subject to downgrade on review — exactly as `S3.saas-subscription`'s
`practices/consumer-proof/scenarios/S3.saas-subscription/` enforcement-probe (live, RED-able,
yet only proving guard-blocking on scenario-local fixtures, not multi-domain runtime
interaction) was rejected in wave-2 despite passing a naive liveness check.

## The S1/S2 `.md`-only nonvacuity floor (P3-60 — prophylactic, stated honestly)

Check 7 (above) bars a `covered` S3 cell from citing only a `.md` sealed-verdict record as
its nonvacuity proof. S1 (CAPABILITY) and S2 (INVARIANT) never had the equivalent floor —
check 4 only required a nonvacuity path to *resolve on disk*, so a `.md`-only nonvacuity
list would silently qualify a S1/S2 cell for `covered` the same way a sealed-verdict `.md`
briefly qualified 9/11 S3 cells before the P2-29 closure.

**Disk truth, stated honestly**: as of this closure, **0 of the 70 currently-`covered` S1/S2
cells are `.md`-only** — every one already cites at least one non-`.md` nonvacuity entry
(a real `*Test.java`/`*IT.java`/`*.vitest.*` path or guard/fixture reference). Check 8
therefore gates **zero live subjects today**. This is deliberately a *prophylactic* floor,
not a live-bug closure: it exists so that a future S1/S2 cell cannot regress into the exact
`.md`-only escape that S3 already had, without waiting for that regression to happen first.

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
