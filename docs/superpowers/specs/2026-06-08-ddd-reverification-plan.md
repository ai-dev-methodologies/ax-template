# DDD Decomposition Implementation — Full Re-Verification Plan (post-R25)

- Date: 2026-06-08
- Status: DRAFT (ralplan consensus: Planner → Architect → Critic[codex])
- Trigger: re-verify the just-implemented DDD decomposition enforcement (impl method + rule/guard code + verification approach) end-to-end, starting the moment R25 (`verify-completion.sh`) finishes.
- Subject under test (SUT) — what was built this session:
  - 3 markers: `common/AggregateRoot`, `AggregateMember(root)`, `PublishedApi` (RUNTIME).
  - `practices/evals/aggregate_boundary_allowlist.yaml` (21 exceptions + 8 governed: 6 god-service + 2 state-mutator) + `aggregate_boundary_allowlist_guard.sh` (schema/resolve/expiry).
  - `aggregate_tagging_completeness_guard.sh` (every @Entity exactly one marker).
  - `DddDecompositionTierZeroTest` (HG-FEAT-TOPLEVEL-TECH, -NOCYCLE[kept], -KERNEL, -ISOLATION, -ANTI-SPLIT) — 4 checks.
  - `DddDecompositionTierOneTest` (HG-AGG-REPO, -REF, -MEMBER-ENCAP) — 3 checks.
  - `DddDecompositionHeuristicsTest` (HG-ANTI-GODSERVICE-TX, -STATE-SOLE-MUTATOR) — 2 checks.
  - `DddDecompositionViolationFixtureTest` + `ddd.fixtures.*` (non-vacuity proof, 5 guards).
  - 73 @Entity tagged (65 root + 8 member); NEW-DOMAIN-CHECKLIST §1b + §4 rows; headline 75 guards.

## RALPLAN-DR

### Principles (load-bearing)
1. **A green test is not a correct test.** Re-verification must independently re-derive truth (classification, violation sets) rather than re-running the same assertions that already pass.
2. **Hunt false negatives first.** An enforcement guard that silently fails to catch a real violation is worse than one that over-blocks — it gives false assurance ("규칙 밖 output BLOCKED" promise).
3. **The allowlist must be load-bearing, not decorative.** Every exception/governed entry must correspond to a violation the guard would otherwise fire on; an over-broad or stale exemption is a hole.
4. **Spec fidelity is binary per criterion.** Each spec §6 guard + §11 success-criterion either is implemented as intended or is an explicit, documented deviation — no silent scope reduction.
5. **Fix-and-re-verify, never fix-and-assume.** Any defect found is closed with a test/fixture that fails before and passes after, then R25 re-run.

### Decision Drivers (top 3)
1. **Correctness of the back-tag classification** drives the soundness of all 5 marker-dependent guards (a mis-tagged root/member silently mis-enforces REPO/REF/ENCAP/god-service).
2. **False-negative surface of the ArchUnit predicates** (bytecode dependency coverage, collection-element resolution, source-regex for repos) determines whether new bad code is actually blocked.
3. **Allowlist honesty** (resolve + currently-triggered + exact-scope) determines whether grandfathering is controlled debt vs a permanent escape hatch.

### Viable Options (re-verification strategy)
- **Option A — Mechanical re-run only** (re-run tests + guards, confirm GREEN). Pros: fast, zero risk. Cons: proves nothing new (tests already pass); cannot catch classification errors or false negatives. **Rejected** as primary — violates Principle 1.
- **Option B — Mechanical + independent re-derivation + adversarial false-negative hunt + cross-model (codex) critique** (this plan). Pros: catches green-but-buggy, mis-classification, over-exemption, spec drift; aligns with the project's proven "adversarial review catches agent green-but-buggy" lesson. Cons: more effort/agents. **Chosen.**
- **Option C — Full external audit (re-implement guards independently and diff)**. Pros: strongest. Cons: disproportionate cost; the guards are small and inspectable. **Rejected** as over-engineered for the surface size.

Invalidation rationale for A and C: A cannot satisfy Principle 1/2 (no new signal); C's marginal assurance over B does not justify re-implementing 9 checks. B is the minimal strategy that exercises every risk driver.

## Architect amendments folded in (consensus round 1)
- **AM1 (highest-leverage gap):** the source regex `extends\s+[A-Za-z]*Repository<...>` used by HG-AGG-REPO and HG-ANTI-GODSERVICE-TX only matches base interfaces ending `Repository`, takes the FIRST match, and is single-line — a member-repo extending a custom base (`BaseRepo<FooMember,Long>`) or with a line-broken type parameter ESCAPES both guards (false negative). This is a likely REAL defect → Phase C0 (parity) + Phase F fix.
- **AM2:** `DddDecompositionViolationFixtureTest` re-implements the predicates instead of calling production guard code, so a regression in the real guard can leave the proof green (Principle 1 violated by the proof itself) → Phase F refactor: extract each guard's core predicate into a shared package-visible helper that BOTH the real test and the fixture call.
- **AM3:** add **Phase C0** repo-target parity assertion (independent multi-line/custom-base re-derivation vs the production regex) — run it BEFORE Phase B (cheaper, higher signal than the 73-row audit).
- **AM4 (resolves the B-churn↔allowlist-honesty tension):** in Phase B, only reclassify an entity when the flip CHANGES an enforcement outcome AND introduces NO net-new allowlist entry; otherwise record it as a TIER-2 deferral (respects catalog anti-bloat).

## Critic (codex) must-fixes folded in (consensus round 1 — verdict ITERATE → addressed)
- **CM1 (allowlist proof — per-entry bijection, not sampling):** Principle 3 demands EVERY one of the 21 exceptions + 8 governed entries be load-bearing. Replace C2-sampling with a **bijection assertion**: against a throwaway empty-allowlist copy, run each guard and collect the FULL violation set; assert `violations == allowlist-entry set` exactly. This proves (a) every entry suppresses a real current violation (load-bearing) AND (b) every current violation is allowlisted (no hole) AND (c) no entry is unused/over-broad. Any allowlist entry not matched by a live violation → remove or reclassify (fix-now).
- **CM2 (fixtures exercise PRODUCTION predicates — do FIRST):** extract each guard's core predicate into a shared package-visible helper (`DddRules`) parameterized by `(JavaClasses, srcRoot, allowlistPath)`; the production tests AND the fixture test BOTH call it. This is a PREREQUISITE step F0 (before C0/A), so the non-vacuity proof and the AM1 regression fixtures run against real code, not copies.
- **CM3 (hard-guard false negative = fix-now):** for the 5 hard structural guards (TOPLEVEL-TECH, KERNEL, ISOLATION, AGG-REPO, AGG-REF, MEMBER-ENCAP, ANTI-SPLIT), a discovered false negative is a BUILD-FAILING fix-now defect — never a "documented gap". Only the 2 heuristic guards (god-service, state-mutator) may carry an explicitly-scoped accepted limitation, and only with a named entry in the spec/checklist.

## Phases (executable immediately after R25)

> Gate: do not start until `/tmp/r25.log` shows `R25_EXIT=0` (or a known-advisory-only WARN). If R25 FAILED, triage that first.

### Phase F0 — Refactor predicates into a shared, injectable helper (CM2; do FIRST)
- Create `DddRules` (package-visible, test scope) exposing each guard's core predicate as a pure function over `(JavaClasses classes, Path srcRoot, Path allowlist)` returning the violation set.
- Re-point `DddDecompositionTierZero/TierOne/Heuristics` tests AND `DddDecompositionViolationFixtureTest` to call `DddRules` — the fixture now exercises REAL predicate code (closes AM2).
- Re-run testPractices GREEN to confirm behavior preserved by the refactor.
- Exit: one shared predicate implementation; fixture proof is non-vacuous against production code.

### Phase C0 — Repo-target detector parity (run FIRST; AM1/AM3)
- Enumerate every `*Repository.java` (and any interface extending a Spring Data repository, regardless of name). Independently derive the (repository → entity) map with a parser tolerant of: multi-line type parameters, base interfaces NOT ending in `Repository`, multiple supertypes, fully-qualified generic args.
- Diff against the production `repoTargetMap()` / HG-AGG-REPO regex output. ANY repository present in the independent set but missing from the production map is a proven false negative in HG-AGG-REPO AND HG-ANTI-GODSERVICE-TX → fix-now (Phase F).
- Add a fixture: `interface FooRepo extends BaseRepo<FooMember,\n Long>` (custom base + newline before comma) and assert HG-AGG-REPO fires.
- Exit: production extractor proven equal to the independent derivation, or the delta is fixed.

### Phase A — Mechanical re-confirmation (deterministic baseline)
- A1: `./gradlew testPractices --rerun-tasks`; assert the 4 DDD test classes report exactly 4/3/2/1 tests, 0 failures.
- A2: Run each new bash guard standalone; re-run the adversarial fail-cases (untagged entity → 1, double-tag → 1, expired/nonexistent exception → 1).
- A3: `run-all-guards.sh` → 81/81; `doc_headline_count_guard` → 75.
- A4: Confirm R25 wrote a PASS audit entry at HEAD (`.ax-verify` / ledger).
- Exit: all deterministic gates reproduce GREEN.

### Phase B — Back-tag classification audit (independent re-derivation)
- B0 (AM4 bound): only act on a reclassification when the flip CHANGES an enforcement outcome AND adds NO net-new allowlist entry; otherwise record as a TIER-2 deferral (anti-bloat).
- B1: A fresh reviewer re-classifies all 73 @Entity as root/member from first principles (transactional consistency boundary; loaded/saved through a root; not independently referenced by id) WITHOUT looking at my tags, then diffs against the applied tags.
- B2: Scrutinize the 9 borderline repo-backed "history/event/leg/link" entities I tagged ROOT — TransformationLeg, RegisterReading, EmailTemplateHistory, BillingEvent, PaymentEvent, ChangeRecord, AuditExportJob, JobHistory, ProviderLink — and decide per entity whether membership would be more correct (and what enforcement would change: it would move god-service/repo into member-repo territory).
- Exit: a per-entity verdict table (agree / change-to-member / change-to-root) with rationale; any "change" becomes a Phase F fix.

### Phase C — Guard soundness audit (false-negative + over-exemption hunt)
- C1 (false-negative fixtures): add throwaway fixtures the current set does NOT cover and confirm each guard fires (or record a documented gap):
  - cross-feature reference used via fully-qualified name WITHOUT an import statement;
  - `@OneToMany List<OtherAggregateRoot>` collection cross-aggregate pointer (not own-member);
  - a repository whose `extends …Repository<Target,…>` spans multiple lines / uses a custom base interface;
  - a member-repository whose class name is NOT `<Member>Repository`;
  - a god-service that mutates a 2nd root via `saveAll`/a differently-named repo handle.
- C2 (allowlist per-entry BIJECTION proof; CM1): against a throwaway empty-allowlist copy, run each guard and collect the FULL violation set; assert `violations == allowlist-entry set` exactly (every entry load-bearing AND every violation allowlisted AND none over-broad). Any unmatched allowlist entry → remove/reclassify (fix-now).
- C3 (over-exemption): confirm matching is exact-equality (not prefix/contains) and no wildcard leaks; subsumed by the C2 bijection but re-confirmed at code level.
- C4 (detector parity): confirm the completeness guard's precise `@Entity` regex matches EXACTLY the entity set ArchUnit sees (73) — no `@EntityGraph`/`@EntityListeners` drift, no missed entity.
- Exit (CM3): every HARD guard provably fires on its new fixture (a false negative is a fix-now build failure); only the 2 HEURISTIC guards may carry an explicitly-scoped accepted limitation with a named spec/checklist entry.

### Phase D — Spec-fidelity audit
- D1: Table mapping spec §6 (5 TIER-0 + 5 TIER-1) and §11 (5 success criteria) → implemented artifact → verdict (faithful / deviation). Explicitly assess:
  - FEAT-ISOLATION TIER-0 scoped to @Entity/*Repository only (published-API default-deny for services deferred) — is this a faithful TIER-0 reading or an under-implementation?
  - HG-AGG-REPO implemented as "member has no repo" (not "exactly one repo per root") — acceptable?
  - HG-STATE-SOLE-MUTATOR entity↔`<X>StateMachine` name-match — does it skip a governed entity whose machine is named differently (e.g. a machine governing two entities)?
  - HG-ANTI-GODSERVICE-TX roots-only + direct-save-only — confirm the "delegation evasion" limit is documented.
- D2: Confirm `@PublishedApi` marker is actually exercised by a guard OR documented as reserved-for-Phase-next (it currently exists but no guard reads it at TIER-0).
- Exit: deviations list, each tagged accepted-deferral or fix-now.

### Phase E — Adversarial cross-model review
- E1 (codex Critic): run `codex` over the guard code (`DddDecomposition*Test.java`, `aggregate_*_guard.sh`) + allowlist for logic bugs, ArchUnit API misuse, regex brittleness, YAML schema bypass.
- E2 (Claude refuter): a refute-by-default agent reviews classification honesty + allowlist completeness + the non-vacuity argument.
- Exit: consolidated finding list with severity + verdict (real / refuted).

### Phase F — Synthesis & remediation
- F1: Triage all Phase C0/B–E findings → fix real defects (each with a failing-then-passing fixture/test), document accepted limitations in the spec/checklist, adjust allowlist/tags as needed.
- F1a (AM1 fix): harden the repo-target extraction — match any interface extending a Spring Data repository (not name-gated), tolerate multi-line type params, capture all supertypes; re-run C0 parity to GREEN.
- F1b (AM2 fix): extract each guard's core predicate into a shared package-visible helper (e.g. `DddRules`) called by BOTH the production test and `DddDecompositionViolationFixtureTest`, so the non-vacuity proof exercises real code (not a copy).
- F2: Re-run R25 (`verify-completion.sh`) to PASS at the new state; re-run run-all-guards; re-confirm headline counts.
- Exit: R25 PASS; all real findings closed or explicitly accepted with rationale.

## Acceptance criteria (testable)
1. Phase A reproduces GREEN deterministically (10 DDD tests, 81/81 guards, headline 75).
2. Phase B yields a 73-row classification verdict table; every "change-to-member/root" is applied + re-verified or explicitly rejected with rationale.
3. Phase C: each of the 5 new false-negative fixtures either makes its guard fail (caught) or is recorded as an accepted limitation; C2 proves every guard family's exemption is load-bearing (test fails when exemption removed).
4. Phase D produces a spec §6+§11 fidelity table with zero undocumented deviations.
5. Phase E: codex + refuter findings are each marked real/refuted; all "real" closed in Phase F.
6. Phase F: R25 PASS at HEAD-equivalent working tree; no guard left vacuous.

## Risks & mitigations
- R1 Re-classification churn (B2 flips several entities) → cascade into god-service/repo enforcement. Mitigation: apply flips behind the same allowlist/marker mechanism; re-run testPractices after each.
- R2 Throwaway fixtures/allowlist-copies accidentally left in tree → mitigate by using `/tmp` copies and a cleanup checklist; never edit the live allowlist for C2 (use `--file`/`--src` overrides).
- R3 Editing catalog files while an R25 is in flight → spurious FAIL. Mitigation: all of Phase C/F edits happen only AFTER R25_EXIT is observed; no concurrent verify.

## ADR
- Decision: adopt Option B (mechanical + independent re-derivation + false-negative hunt + codex cross-model critique).
- Drivers: classification correctness, false-negative surface, allowlist honesty.
- Alternatives considered: A (mechanical-only, rejected — no new signal), C (full re-implementation, rejected — over-engineered).
- Why chosen: minimal strategy that exercises every risk driver and matches the project's proven adversarial-review discipline.
- Consequences: more agent/codex spend; produces a defensible verdict and closes green-but-buggy risk.
- Follow-ups: if B2 reclassifies entities, a follow-up IMW may promote/demote markers and tighten god-service governance.
