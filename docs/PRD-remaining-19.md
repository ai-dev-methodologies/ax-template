# PRD — remaining-19 backlog convergence wave (rev 2, post-architect)

Scope: the 19 open rows of `docs/BACKLOG.md` (P1-73, P2-42, P2-44, P3-56, P3-69, P3-76,
P3-79~91 subset, P3-82~90). Executor: ralph, immediately after ralplan consensus.
Base: main @ 4bcb62e. Authoritative diagnosis + done-when per item lives in the cited
BACKLOG row; this PRD tightens each to a binary acceptance.

---

## 0. RALPLAN-DR

**Principles**
1. **Disk truth over prose** — every closure is a command + expected exit; counts
   (guards, tasks, ledger entries) are re-derived from disk at integration, never hand-copied.
2. **Honest closure classes** — code-fix vs decision-recorded vs documented-limitation vs
   **explicitly justified re-scope** are distinct; each names its exact artifact (dated note
   in the row + `practices/DECISIONS.md` entry where policy) or it is not a closure. A
   coverage-reducing shortcut dressed as a closure (e.g. deleting citations from a scanner's
   universe) is the exact green-but-hollow class this catalog exists to block.
3. **Backend shape is canonical for `/auth/me`** — `{userId,email,role,emailVerified,
   linkedProviders}` (P3-74; `contracts/auth-openapi.yaml:396-411` already declares it — the
   contract step is a verification no-op, not work). ALL frontend consumers, mock, and tests
   move to it; mocks bind to a committed golden captured from the real emission path.
4. **Lanes never run gradle**, with ONE exception: a single serialized, spinlocked
   (`/tmp/ax-gradle-lock`) pre-freeze gradle window granted to Lanes A+B for their new BE
   tests (2026-07-14 wave pattern). One central R25 on a frozen tree remains the sole
   push-qualifying verification event ([98] tree_fingerprint/tree_clean). Destructive guard
   probes only in throwaway rsync copies.
5. **Shared enforcement files have a single writer** — `run-all-guards.sh`,
   `fixture_kill_manifest.yaml`, `evidence_protected_template_anchors.txt`, headline-count
   files, BACKLOG table go through one owner; lanes submit content, not edits.

**Decision Drivers (top 3)**
1. Convergence: all 19 rows reach an **honest** closure artifact this wave — including,
   where the disk shows full closure would be dishonest or oversized, an explicitly
   justified re-scope with a numeric floor (P3-69, P2-42 second pass).
2. Push-evidence gates: fingerprint-bound recency makes the freeze/lane protocol
   load-bearing, not hygiene.
3. Collision surface: `run-all-guards.sh`, kill manifest, approval-workflow `page.tsx`,
   `PageEnvelopeCatalogSweepTest` are each touched by 2+ items — grouping is by
   file-surface, not theme.

**Viable Options**
- **A) Two sequential mega-lanes (code, then guards/docs).** Pros: near-zero collision risk.
  Cons: ~2× wall clock. (Fair steelman, per critic: a sequential lane CAN order P1-73 first,
  so priority starvation is NOT inherent — the rejection rests on wall clock alone.)
- **B) Six parallel lanes grouped by file-surface + main-loop single-writer + explicit
  serialization edges (CHOSEN).** Pros: proven pattern (2026-07-14 wave); P1-73 lands first;
  collisions engineered away by ownership map (§3). Cons: coordination overhead; the map
  must be obeyed.
- **Sub-decision, P3-69 (REVISED per architect)**: NOT "full burn-down then promote strict"
  — two of the PRD-rev1 premises were stale (`--strict-templates` already exists,
  guard `:167`; a blocking templates path is already live via
  `evidence_protected_template_anchors.txt`, `run-all-guards.sh:1199-1209`) and the
  external-conversion fallback would have DELETED ~79% of findings from the scanner's
  universe (the guard selects only `upstream_id` entries — `:291,343-345`) — a laundered
  full. **The honest full-refresh alternative (presented fairly, then deferred)**: rebuild
  the `wcag-2-2` digest (4 SC → ≥16 SC sections), expand `shadcn-ui-2026-05` to ~38
  per-component sections, author the missing `recharts-2026-05` body; benefit = the full
  ~112-quote universe becomes strict-promotable; cost = network fetch + manual HTML→snapshot
  conversion + manifest sha/bytes/fetched_at re-sync (+`time_decay` 90d clocks reset), and
  NO tooling exists (`practices/upstream/fetch.sh` enumerates none of the three ids);
  deferral reason = that work class is unavailable to this wave's offline lanes — registered
  as the residual's follow-up path with a numeric floor, not silently dropped.
  CHOSEN: normalizer-fix-first + ratchet growth + honest numeric re-scope (§2 W5a).
- **Sub-decision, P2-42 (RIGHT-SIZED per architect)**: 17 remaining verticals ≈10,000 LOC
  aggregate vs ~430 LOC of exemplars. CHOSEN: convert the 13 verticals ≤300 LOC first;
  the big four (approval-workflow 814 / tag-categorization 543 / activity-feed 540 /
  webhook 477) are a second pass in the same lane. **Objective budget trigger (critic)**:
  "budget exhausted" ⇔ integration freeze begins (central R25 window opens) while pass-2 is
  incomplete — not a lane's own judgement call. On trigger: ratchet `min_entries` to the
  converted count and re-register the shortfall with that numeric floor (explicit honest
  re-scope, Driver 1).

---

## 0b. Acceptance matrix (binary — every code-fix item; critic C4)

| Item | Command | Expect | RED-on-revert mutation |
|---|---|---|---|
| P1-73 | `cd frontend && npx vitest run` + grep/rg locks above | 0 fail; both greps empty | FE reads `roles[]` → RED; BE DTO rename → BE leg RED |
| P2-44 | `bash practices/evals/coverage_map_guard.sh && grep -q "coverage_map" practices/evals/run-all-guards.sh` | exit 0 both | nonvacuity path → nonexistent file → exit ≠0 |
| P3-76 | `(cd backend && ./gradlew testApprovalWorkflow) && (cd frontend && npx vitest run tests/authorized-actions-parity.vitest.ts)` | exit 0 both | BE step-transition table edit → FE parity RED |
| P3-79 | `! grep -qE "\([0-9]+ tasks\)|[0-9]+ live" practices/verification-checklist.yaml` | exit 0 (no match) | — (deletion; lock = the negated grep) |
| P3-80 | `! grep -q "o.user =" practices-react/rules/js-index-maps.md` | exit 0 (no match) | reintroduce mutation line → exit 1 |
| P3-81 | `bash practices/evals/lint_own_blocks_guard.sh; bash practices/evals/lint_own_blocks_guard.sh --root practices/evals/fixtures/lint-own-blocks/fail_comment_fooled` | exit 0 then 1 | neuter comment-strip → fixture flips 1→0 ([87]) |
| P3-82 | `bash practices/evals/fixture_kill_proof_guard.sh` | PASS, item count ≥ 16 | any listed anchor neutered → KILLED proof flips |
| P3-83 | `bash practices/evals/domain_mode_consistency_guard.sh; bash practices/evals/domain_mode_consistency_guard.sh --root practices/evals/fixtures/domain-mode-consistency/fail_mismatch` | exit 0 then 1 | spec domain_mode flip w/o allowlist → exit 1 |
| P3-85 | `(cd backend && ./gradlew testPagination)` | exit 0 | contract `type: integer`→`string` → RED (type axis) |
| P3-87 | `bash practices/evals/contract_enum_parity_guard.sh; bash practices/evals/contract_enum_parity_guard.sh --root practices/evals/fixtures/contract_enum_parity_guard/fail_same_package_same_set` | exit 0 then 1 | craft same-package same-set pair → exit 1 |
| P3-88 | `W=$(mktemp -d)/c; rsync -a --exclude=.git --exclude=node_modules --exclude=build --exclude=.gradle ./ $W/; (cd $W && git init -q . && git add -A && git -c user.name=prd -c user.email=prd@local commit -qm x && printf 'version: 1\ndefaults: {working_directory: ".", timeout_seconds: 60}\nchecklist:\n  - id: p\n    title: t\n    commands: [{command: "true \|\| false", expected_exit: 0}]\n    fix_playbook: \|\n      n/a\n' > practices/verification-checklist.yaml && bash practices/scripts/verify-completion.sh); echo rc=$?; rm -rf $(dirname $W)` | prints `rc=2` (live tree untouched, guard live exit 0) | `true \|\| false` placeholder step → BLOCK exit 2 |
| P3-90 | `bash practices/evals/evidence_guard.sh` | exit 0, anchors axis active | stale `anchors_rule` reintroduced → exit 1 |
| P3-91 | `! grep -q WebhookEndpointPage contracts/webhook-openapi.yaml && (cd backend && ./gradlew testPagination)` | exit 0 both | restore envelope → sweep universe pin RED |
| P3-56(c) | `(cd backend && ./gradlew testStateMutation)` | exit 0 | remove runtime check → new unit test RED |
| P3-69 | `bash practices/evals/evidence_quote_spotcheck_guard.sh --strict --strict-templates --templates-only-protected` | exit 0 on ledger ≥ N | un-pin a fixed identity → protected path exit 1 |
| P2-42 | `bash practices/evals/l4_presentational_view_guard.sh` (min_entries=converted count) | exit 0 | ledger entry removed → exit 1 |

**Central R25 acceptance (explicit)**: final audit line must show `exit:0, pass:10,
hard_fail:0, full_run:true, tree_clean:true, tree_clean_end:true, tree_stable:true,
tree_samples>=2` with start/end fingerprints equal — not merely "R25 green".

## 1. Closure classes

| Class | Items | Closure artifact |
|---|---|---|
| code-fix | P1-73, P2-44, P3-76, P3-79, P3-80, P3-81, P3-82, P3-83, P3-85, P3-87, P3-88, P3-90, P3-91, P3-56(c) | code/config + binary acceptance + mutation lock |
| code-fix + possible registered shortfall | P2-42, P3-69 | converted/pinned count + numeric floor for residual, recorded in the row |
| decision-recorded | P3-56(a)(b), P3-84, P3-86, P3-89 | dated decision in the BACKLOG row + `practices/DECISIONS.md` entry + the named doc artifact |

---

## 2. Workstreams

### W1 — frontend auth-shape truth (Lane A) — TOP PRIORITY

**P1-73** (BACKLOG:120) — frontend reference workload reads a response shape that does not
exist; dashboard silently renders role-less/unverified/no-providers against the real
backend; MSW mock mimics the fiction so tests are green.

- **Direction (decided)**: backend shape canonical (P3-74). **Full consumer census
  (architect-completed; all verified on disk)** — every one of these moves to the canonical
  shape:
  1. `frontend/packages/core/src/api/authClient.ts` (`UserProfile`)
  2. `frontend/packages/core/src/auth/authStore.ts`
  3. `frontend/packages/core/src/auth/store.ts:1-11` — a SECOND divergent `AuthState`,
     exported from `@ax/core` `index.ts:8`
  4. `frontend/src/features/auth/dashboard/DashboardPage.tsx` + MSW mock
  5. `templates/L4/auth/app/(authenticated)/dashboard/page.tsx:26-30,48,96-103` — its own
     local fictional `AuthState`; this is the fork-receiver deliverable AND sits outside
     lint/tsc coverage (P2-23) — only the grep lock below catches regressions here
  6. `frontend/tests/auth-state.vitest.ts`, `frontend/tests/auth-store.vitest.ts:39-40`,
     `frontend/tests/pages.vitest.tsx:92`
  7. `blueprints/auth-checklist.md:6` (prose fiction — same fix)
  8. `frontend/tests/auth/login-flow.spec.ts:42` — retains the fictional shape (critic)
  9. six `frontend/apps/*/profileClient.ts` / `operatorClient.ts` comments claiming
     `@ax/core` uses `roles[]` — false after canonicalization; fix the comments (critic)
  `contracts/auth-openapi.yaml:396-411` is already canonical — keep as a check, not work.
- **Golden binding**: commit `frontend/tests/_fixtures/auth-me.golden.json` generated from
  the production emission path (P2-36 `validation-error.golden.json` pattern: BE leg test
  serializes the real `/auth/me` response, whole-tree-compares to golden, regeneration only
  via explicit opt-in system property; FE leg feeds the same file into the MSW mock + both
  stores, so mocks cannot drift from the wire).
- **Behavioral fix proof**: a DashboardPage vitest asserting non-empty role / verified /
  providers rendered from the golden (kills the silent-empty class, not just the type).
- **Acceptance (binary)**: `cd frontend && npm run test` → 0 fail incl. new parity +
  dashboard tests; BE leg green (Lane A gradle window / central R25).
  **Grep mutation lock (covers the lint/tsc-blind L4 file)**:
  `! grep -rn "verificationState\|providerLinks" templates/ frontend/ --include='*.ts' --include='*.tsx'`
  → exit 0 (no occurrences); `rg "roles\[\]" frontend/ templates/` → exit 1 (no matches, comments
  included); `roles: string[]` shape gone from both stores.
  **RED-on-revert locks**: (1) any consumer reads `roles[]` again → FE leg RED;
  (2) BE DTO field rename → BE leg RED. Both restored green.

### W2 — contract/DTO surface (Lane B; ordered **P3-76 → P3-91 → P3-85**)

Order rationale (architect A6): P3-76 shares no file with P3-91/85 (the envelope sweep never
asserts nested `ApprovalStepResponse`), and landing it first unblocks Lane F's largest
conversion (approval-workflow page, 814 LOC) at the earliest point.

**P3-76** (BACKLOG:405) — `ApprovalStepResponse` lacks step-scoped `allowedActions`.
- BE: `ApprovalActionEvaluator` computes per-step action-set (zero-local-policy as P2-38b:
  every branch is a `guards.*` call or state-machine `canTransition` probe); load onto
  `ApprovalStepResponse`; reflect in contract + blueprint + spec (WF-AUTHZ family).
- FE: `templates/L0/fork-receiver-kit/authorized-actions.ts#actionableStepFor` consumes the
  server field first (server-first), retiring the TS mirror of `ALLOWED` as decision source;
  parity vitest asserts the field.
- **Landmine (architect A8)**: if the step action-set is enum-typed in the contract,
  `contract_enum_parity` exhaustiveness applies → `contract-enum-map.yaml` entry with
  `java_enum` binding required. (Does NOT apply to P3-91 — `WebhookEndpointPage` has no enum.)
- **Acceptance**: `testApprovalWorkflow` GREEN with new parity assertions; FE parity vitest
  asserts step-scoped field. **Mutation locks**: remove step field from evaluator → BE
  parity RED; make `actionableStepFor` ignore the server field → FE parity RED.

**P3-91** (BACKLOG:422) — `/webhook-endpoints` declares `WebhookEndpointPage` but controller
returns bare `List<EndpointResponse>`.
- Fix contract to code, **P3-67-style reconciliation**: the 200 response at
  `contracts/webhook-openapi.yaml:43` `$ref`s the schema — rewrite the 200 to a bare array
  AND remove the `WebhookEndpointPage` schema in the same edit (NO guard validates `$ref`
  resolution — a dangling ref would be silent). Check `contract-enum-map.yaml` untouched.
- **Landmine (architect A5) — THREE coupled edits to `PageEnvelopeCatalogSweepTest`**, all
  in the same commit: (i) `DECLARING_CONTRACTS` 20→19; (ii) drop `"webhook"` from
  `REACHABLE_BUT_PREEXISTING_DRIFT`; (iii) `hasSize(2)`→`hasSize(1)` — both `total*` lines
  in webhook-openapi.yaml are inside `WebhookEndpointPage`.
- **Acceptance**: `grep -c WebhookEndpointPage contracts/webhook-openapi.yaml` → 0;
  `testWebhook` + envelope sweep GREEN.

**P3-85** (BACKLOG:423) — envelope sweep locks member SETS not TYPES.
- Add type axis: assert each `required:` member's declared `type` (+ `format` where present)
  against the actual JSON type of the emitted value. Lands after P3-91 (corrected universe).
- **Acceptance**: sweep GREEN. **Mutation lock (mandatory, from the row)**: flip
  `totalPages` `integer`→`string` in one contract → sweep RED; restore → GREEN (reproduction
  recorded in closure text).

### W3 — enforcement wiring (Lane C; sole writer of `run-all-guards.sh`, `fixture_kill_manifest.yaml`, `evidence_protected_template_anchors.txt`)

Rebalanced per architect A7: C keeps the items that MUST write the shared enforcement files
(P2-44, P3-83, P3-82) plus the submissions queue (Lane E anchor pins, Lane D/F fixture
descriptors, any new registrations). P3-81/P3-87/P3-88 moved to Lane D — they edit
already-registered guards + their own fixture dirs, no `run-all-guards.sh` write.

**P2-44** (BACKLOG:315) — `coverage_map_guard.sh` wired nowhere; R25 passes while the
coverage-map lies.
- Lift the wave-1 isolation posture: register in `run-all-guards.sh` (live + fixtures);
  guard must be PyYAML-fail-closed (exit 2) so the [95] disk-derived census auto-covers it.
- **Acceptance**: row's reproduction inverted — break one covered-cell nonvacuity path in
  `practices/consumer-proof/engine/coverage-map.yaml` → `verify-completion.sh` FAILS (was
  PASS); restore → PASS. `AX_PREFLIGHT_FAKE_MISSING=pyyaml` probe → exit 2, not 0.

**P3-83** (BACKLOG:424) — no cross-check among spec `domain_mode` ↔
`trio_integrity_allowlist.yaml` ↔ L4 README status.
- New guard asserting 3-way agreement wherever any of the three declarations exists;
  mismatch fail fixture + pass fixture; register in run-all-guards; [87] entry.
- **Acceptance**: flip any backend_only spec to `full_trio` without touching
  allowlist/README → exit 1; live tree → exit 0. New guard file changes headline counts
  (101→102 guard files) — re-derived and synced at integration.

**P3-82** (BACKLOG:426) — wave fixtures unregistered in [87]. **NOT clerical (architect)**:
each registration = a per-fixture mutation experiment — find an allowlistable neuter anchor
(exactly-once occurrence, one of the 6 allowlisted neuter shapes), prove the 1→0 flip,
register. ×9 legacy (react-substance 4, spotcheck-templates 1, l2_frontmatter_deps 1,
l4_presentational_view 3) + ALL new fixtures this wave produces. Where no anchor shape
exists, record the justified non-registration in the manifest ((P2-33) precedent).
- **Acceptance**: `fixture_kill_proof_guard` [87] GREEN; each listed fixture has a manifest
  entry or in-manifest justification (grep-able).

**Lane C self-check (architect A8)**: no guard cross-checks guard-file existence vs
run-all registration — before handoff, C diffs `ls practices/evals/*_guard.sh` against
registered steps and accounts for every gap.

### W4 — guard precision + policy/docs (Lane D)

**P3-81** (BACKLOG:410) — `lint_own_blocks_guard` wiring detection = whole-file `grep -qF`,
foolable by a quoted rule name in a comment.
- Narrow to config-object key parsing. Fail fixture: rule name only inside a comment →
  reported NOT wired.
- **Acceptance**: fixture pair flips; live 0 regressions (341+ blocks PASS); [87] descriptor
  submitted to Lane C (anchor: neuter key-parse back to whole-file grep → 1→0).

**P3-87** (BACKLOG:411) — same-package identical-set enum swap not caught.
- Chosen arm: detect same-package identical-value-set enum pairs, require explicit
  declaration when one exists (cheaper + sound vs producer-binding rewrite).
- **Acceptance**: fixture with two same-set enums in one package → exit 1 naming both;
  live tree → exit 0 (no such pair today); [87] descriptor to Lane C.

**P3-88** (BACKLOG:419) — no-op denylist models the last fragment of `&&`/`||`, not
short-circuit.
- Implement short-circuit semantics over all-placeholder chains (decidable there, per row);
  documented boundary for wrapper/subshell/expansion unchanged. Edits `verify-completion.sh`
  (Lane D owns this file this wave; probes in throwaway rsync copy only).
- **Acceptance**: a step whose sole command is `true || false` → exit 2 BLOCK; same
  `false && true`; all existing pass fixtures unchanged.

**P3-56** (BACKLOG:384) — audit-history viewing policy + 3 warnings.
- (a) least-privilege options block + explicit rationale for current default →
  `blueprints/audit-log-manifest.yaml` RBAC table → decision-recorded.
- (b) webhook replay firstSeen: document mark-after-success / unmark-on-failure pattern in
  `blueprints/webhook-manifest.yaml` (exact file) → decision-recorded.
- (c) dead `assert` in `GovernedFormStateMachine.java:80` → explicit runtime check + unit
  test → code-fix.
- **Acceptance**: (a)(b) grep-able at named paths + DECISIONS.md entry; (c) test RED when
  check removed, GREEN restored (central R25).

**P3-79** (BACKLOG:408) — **BOTH stale counts (architect A9)** in
`practices/verification-checklist.yaml`: `:89` "(89 tasks)" is stale — the per-domain step
holds 115 gradle commands (118 = whole checklist; `register<Test>` appears 116×). `:464/:466`
"95 live" is **currently CORRECT** (disk: 101 guard files / 95 `/live` registrations, matching
CLAUDE.md:530) and becomes **102 total / 96 live** after P3-83 lands — NOT "102 live".
CHOSEN: **remove both numerals from prose titles** (stale-proof; per the row's 더 나은 방향).
If a numeral is ever kept it must be re-derived from disk AT INTEGRATION, after P3-83.
- **Acceptance**: `grep -cE "\([0-9]+ tasks\)|[0-9]+ live" practices/verification-checklist.yaml`
  → 0 (both numerals gone); `bash practices/scripts/verify-completion.sh --step backend-build`
  → exit 0 (checklist still parses/runs).

**P3-80** (BACKLOG:409) — js-index-maps.md example mutates input in-place.
- Replace with `orders.map((o) => Object.assign({}, o, { user: ... }))`.
- **Acceptance**: `grep -c "o.user =" practices-react/rules/js-index-maps.md` → 0;
  `substance_guard.sh --catalog practices-react` GREEN.

**P3-84** (BACKLOG:425) — ContextCache maxSize surveillance.
- Decision-recorded: adopt the row's own probe. **Exact probe (documented verbatim in
  CLAUDE.md Build & Test)**:
  `(cd backend && JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./gradlew test -Dspring.test.context.cache.maxSize=32)`
  → expected exit 0 when no stale-cache dependency exists; ANY new failure vs the normal
  aggregate = real inter-test state leakage (NOT flake) → fix the test, not the ceiling.
  **Trigger (explicit)**: before any major test-infra change (new @SpringBootTest batch,
  Spring/Boot upgrade, cache-size change) and at least once per quarter. Dated decision in row.

**P3-86** (BACKLOG:412-418) — documented limitation, deliberate-evasion boundary.
- No catalog-scope actionable part: residual done-when is a fork-receiver policy seam (R26).
  Decision-recorded: document fail-closed routing as an OPTIONAL fork-receiver pattern
  (pattern note in `blueprints/payment-manifest.yaml`, referenced from
  `practices/evals/contract_enum_parity_guard.sh` header — exact files); declare the boundary text
  final at its **5th narrowing** (architect correction — vocab_scan producer-resolution was
  the 5th, not 4th); dated decision in row + DECISIONS.md. Boundary enumeration unweakened.

**P3-89** (BACKLOG:420) — intra-step tree mutation invisible between sampling points.
- Decision-recorded: keep step-boundary sampling (docs describe the limit accurately;
  intra-step periodic sampling = watcher complexity chasing a reviewer-constructed
  reachability). Record cost/benefit + revisit trigger (a demonstrated real evasion) in the
  row + `practices/evals/midrun_tree_mutation_guard.sh` header (exact file).

### W5a — templates evidence integrity (Lane E) — REVISED per architect Blocker 1

**P3-69** (BACKLOG:398) — templates/** evidence quote↔snapshot mismatch, **108** residual
(corrected count). Corrected premises: `--strict-templates` already exists (guard `:167`);
a BLOCKING templates path is already live — `evidence_protected_template_anchors.txt`
(78-line ratchet file, fatal per `run-all-guards.sh:1199-1209`). Part of the 108 is a
**guard normalizer bug**: blockquote `> ` continuation markers and backticks break the
substring match (e.g. translation-boundary.tsx's quote IS verbatim in
`react-19-error-boundary.snapshot.md:18-20`). And ~79% of findings cite digest snapshots
(wcag-2-2 and shadcn-ui-2026-05 are 48/51-line digests; recharts-2026-05 has no body) that
structurally cannot contain the quoted prose — converting those to `source_type: external`
would REMOVE them from the scanner's universe (guard selects `upstream_id` entries only,
`:291,343-345`): −76% coverage, green-but-hollow. Plan (architect synthesis, adopted):
1. **Fix the normalizer first** (blockquote continuation, backticks) in the guard;
   re-measure — the true residual is < 108. Guard edit + fixture for the normalizer class.
2. **Re-quote** citations whose snapshots genuinely contain the content (`grep -F` = 1 hit,
   old quote 0 hits — arithmetic RED-on-revert).
3. **Pin every fixed identity** into `evidence_protected_template_anchors.txt` (submitted
   to Lane C) — growth of the already-blocking ratchet IS the promotion mechanism.
4. **Convert to external ONLY where the URL is the truthful provenance** (WCAG SC permalinks
   qualify; case-by-case for digest-snapshot paraphrases of vendor pages). Record the
   converted count explicitly in the row as REDUCED SCANNER SCOPE — never as "fixed".
5. **Re-scope the row's done-when**: "protected ledger ≥ N pinned identities + residual
   registered with a numeric floor". An honest partial beats a laundered full.
- **Acceptance (binary)**: normalizer fixture flips; post-fix sweep count strictly < 108
  and every remaining finding is category-recorded (re-quoted / pinned / converted-external
  with count / registered-residual with floor N); `--templates-only-protected` run exit 0
  with the grown ratchet; no citation leaves the universe without an explicit
  converted-count entry in the row.

**P3-90** (BACKLOG:421) — `_check-anchors.sh` reports 57 violations, wired to no gate
(42 stale-rule-pointer residual after P2-43 absorbed the evidence 5). **Assigned to Lane E**
(not C): the 42 fixes live in the same templates/** frontmatter surface E already owns —
splitting them across lanes would create a same-file collision with E's re-quotes.
- Fix the 42 findings (missing `anchors_rule` 23 + dangling pointer 19). Fold the
  `anchors_rule`-resolves-to-existing-rule axis into `evidence_guard`'s templates walk (it
  already parses the 120 `@ax-template-meta` blocks — no new guard file, no run-all-guards
  write, no headline count change). Keep `_check-anchors.sh` as a dev convenience with a
  header naming `evidence_guard` as the owning gate, or delete it — recorded in the row.
- **Acceptance**: `bash templates/backend/_check-anchors.sh` → 0 violations (if kept);
  fail fixture (stale `anchors_rule`) → `evidence_guard` exit 1; [87] descriptor to Lane C.

### W5b — L4 render-testability (Lane F; sole writer of `l4_presentational_view_ledger.yaml`)

**P2-42** (BACKLOG:311) — 4/21 app-bearing verticals in the ledger; 17 remain (~10,000 LOC
aggregate vs ~430 LOC exemplars — right-sized per architect).
- **Pass 1**: the 13 verticals ≤300 LOC, per convention: extract presentational view, add
  application-level vitest, ledger entry, `min_entries` ratchet bump.
- **Pass 2 (same lane, after B lands P3-76)**: approval-workflow (814), webhook (477),
  tag-categorization (543), activity-feed (540). If wave budget exhausts: ratchet
  `min_entries` to converted count, re-register shortfall with numeric floor (honest
  re-scope, Driver 1).
- **Landmines (architect A8)**: every new `*-view.tsx` with `template_id` needs an
  `evidence:` block (evidence_guard §4.10 shape-A — 13-17 files); the ledger header names
  webhook + approval-workflow as DEFERRED — converting them must update that rationale.
- **Acceptance**: ledger entry count == `min_entries` == converted count (target 21;
  floor-registered if less); `cd frontend && npm run test` GREEN incl. new vitests; l4
  guard GREEN; new fail fixtures → P3-82 pipeline via Lane C.

---

## 3. Execution plan

### Lanes

| Lane | Items | Model | Touches |
|---|---|---|---|
| A | P1-73 | `Agent(general-purpose, model=sonnet)` impl + `Agent(general-purpose, model=opus)` review (auth surface) | frontend/packages (both stores), frontend/src, frontend/tests, MSW, templates/L4/auth dashboard page, blueprints/auth-checklist.md, 1 new BE test |
| B | P3-76 → P3-91 → P3-85 (ordered) | `Agent(general-purpose, model=opus)` (authz + contract) | approval DTOs/evaluator, contracts (approval, webhook incl. :43 $ref), PageEnvelopeCatalogSweepTest (3 coupled edits), authorized-actions.ts, contract-enum-map.yaml (if enum-typed), parity tests |
| C | P2-44, P3-83, P3-82 + submissions queue | `Agent(general-purpose, model=opus)` (enforcement) | **sole writer**: run-all-guards.sh, fixture_kill_manifest.yaml, evidence_protected_template_anchors.txt; coverage_map wiring; new 3-way consistency guard; per-fixture mutation experiments; self-check: guard files vs registrations |
| D | P3-81, P3-87, P3-88, P3-56, P3-79, P3-80, P3-84, P3-86, P3-89 | `Agent(general-purpose, model=opus)` guard precision + policy; `Agent(general-purpose, model=sonnet)` doc edits | already-registered guards + own fixture dirs, verify-completion.sh (P3-88), verification-checklist.yaml (both stale counts), blueprints, practices-react rule, 1-line BE fix + test, DECISIONS.md drafts |
| E | P3-69 (revised plan), P3-90 | `Agent(general-purpose, model=sonnet)` content + `Agent(general-purpose, model=opus)` guard edits | evidence_quote_spotcheck_guard.sh (normalizer), evidence_guard.sh (anchors_rule axis), templates/** frontmatter re-quotes + 42 anchor fixes, _check-anchors.sh, pins/descriptors submitted to C |
| F | P2-42 (pass 1: 13 small; pass 2: big 4) | `Agent(general-purpose, model=sonnet)` | templates/L4/*/app, view evidence blocks, frontend/tests vitests — **sole writer**: l4_presentational_view_ledger.yaml (incl. DEFERRED header rationale) |
| main | integration | (session) | BACKLOG rows+table (parenthesized sibling IDs), headline count re-derive/sync, DECISIONS.md final, freeze + central R25 + push + codex gate |

### File-collision map & serialization edges

- `run-all-guards.sh` / `fixture_kill_manifest.yaml` / `evidence_protected_template_anchors.txt`
  — Lane C only; E, D, F submit pins/descriptors/lines to C's queue.
- `templates/L4/approval-workflow/app/(approvals)/[id]/page.tsx` — **Lane B (P3-76) first,
  then Lane F pass 2**. F's pass-1 (13 small verticals) starts immediately.
- `PageEnvelopeCatalogSweepTest` — Lane B only (P3-91 3 coupled edits + P3-85 axis, ordered).
- `verification-checklist.yaml` — Lane D only (P3-79, both counts, finalized at integration
  after P3-83 changes guard-file disk truth).
- `verify-completion.sh` — Lane D only (P3-88).
- `evidence_quote_spotcheck_guard.sh` + `evidence_guard.sh` + templates/** frontmatter —
  Lane E only (normalizer, anchors_rule axis, re-quotes + 42 anchor fixes in one owner).
- `BACKLOG.md`, CLAUDE.md/SKILL.md/README counts, `practices/DECISIONS.md` — main loop only,
  at integration.
- Gradle: forbidden in lanes, EXCEPT the pre-freeze serialized window (below).

### Central R25 + push protocol (revised per architect A11)

1. All lanes land; main loop applies queued shared-file edits + BACKLOG row closures
   (checkbox + table together — `backlog_convergence_integrity_guard`; sibling IDs in prose
   parenthesized `(P3-xx)`).
2. Re-derive headline counts from disk and sync CLAUDE.md / SKILL.md / README
   (`doc_headline_count_guard`; P3-83 makes guard files 102).
3. **Pre-freeze gradle window (CHOSEN over ≥3 central iterations)**: Lanes A then B, one at
   a time under `/tmp/ax-gradle-lock` spinlock, run their targeted `./gradlew test{Domain}`
   (auth/asvs; approvalworkflow + webhook + envelope sweep) and fix locally. This is the
   2026-07-14 wave pattern and moves first-failure debugging out of the frozen phase.
4. **Freeze the tree.** `JAVA_HOME=/opt/homebrew/opt/openjdk@21/...` →
   `bash practices/scripts/verify-completion.sh` (full). Budget 2 fix iterations (each
   followed by a FULL re-run — partial runs don't qualify for push).
5. PASS → `git push origin HEAD:main` (solo direct-push policy; pre-push per-ref recency
   guard validates fingerprint-bound audit).
6. **Codex gate last**: `codex exec -c model="gpt-5.6-sol"` xhigh, `< /dev/null` (stdin
   hang), on the pushed head. P0/P1: fix now → full R25 → push again. P2/P3: register in
   BACKLOG (user standing policy).

### Known landmines (encode, don't rediscover)

1. `PageEnvelopeCatalogSweepTest` — P3-91 needs THREE coupled edits (§2 W2) in the same
   commit as the contract edit.
2. `contracts/webhook-openapi.yaml:43` `$ref`s `WebhookEndpointPage`; NO guard validates
   `$ref` resolution — rewrite the 200 response and remove the schema together.
3. `contract-enum-map.yaml` — sync required if P3-76's action-set is enum-typed (java_enum
   binding); NOT triggered by P3-91.
4. `test_tag_task_coverage_guard` — every new BE `@Test` class needs `@Tag` + a task's
   `includeTags`; `verification_checklist_task_coverage_guard` — any new `register<Test>`
   needs a checklist entry (Lane A/B's new BE tests).
5. `evidence_guard` §4.10 shape-A — every new L4 `*-view.tsx` with `template_id` needs an
   `evidence:` block (Lane F, 13-17 files).
6. `doc_headline_count_guard` — P3-83's new guard file changes disk truth; counts re-derived
   at integration, never incremented in prose. `verification-checklist.yaml:464/:466` "95
   live" is part of the same sync (P3-79).
7. `backlog_convergence_integrity_guard` — table == checkbox truth; parenthesize sibling IDs.
8. Guards touching YAML must be PyYAML-fail-closed (exit 2) or [95] census flags them.
9. No guard cross-checks guard-file existence vs run-all registration — Lane C self-check.
10. `l4_presentational_view_ledger.yaml` header names webhook + approval-workflow DEFERRED —
    converting them must update that rationale (Lane F).
11. Recency gate records tree_fingerprint + tree_clean — any edit during central R25 voids
    push eligibility ([98]); re-run full, don't patch the audit.
12. `evidence_quote_spotcheck_guard` universe = `upstream_id` entries only — external
    conversion shrinks scope and MUST be counted in the row (Lane E rule 4).

## 4. Out of scope

- Backend `/auth/me` shape change (frontend moves instead — P3-74 canonical).
- P3-86 fail-closed routing implementation (fork-receiver policy; documented as optional
  pattern only).
- Intra-step tree sampling implementation (P3-89 decision keeps boundary).
- New industry dogfood or new rule mining — this wave only converges existing rows.
