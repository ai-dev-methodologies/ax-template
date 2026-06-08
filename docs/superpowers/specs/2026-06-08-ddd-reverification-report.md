# DDD Decomposition — Re-Verification Report (ralplan execution)

- Date: 2026-06-08
- Plan: /tmp/ddd-reverify-plan.md (Planner → Architect[AM1-4] → Critic codex[CM1-3, verdict ITERATE→addressed])
- Trigger: re-verify the DDD enforcement (impl method + guard code + verification approach) after R25 PASS.
- **Count note (point-in-time):** the "75 guards / run-all-guards 81/81" figures below are the
  BACKEND re-verification snapshot (2026-06-08, before the frontend decomposition work). The
  frontend Phase 0 later added `feature_boundary_allowlist_guard.sh`, so the CURRENT authoritative
  headline (enforced by `doc_headline_count_guard` against disk) is **76 guards** (README/CLAUDE),
  and `run-all-guards.sh` now reports **82/82**. These planning/report docs are NOT scanned by any
  guard; the enforced catalog is internally consistent at 76. (dogfood ddd-decomposition-iter1 P12.)

## Phases executed
- **F0 (CM2)** — extracted all guard predicates into shared `DddRules` (test scope), parameterized by (JavaClasses, srcRoot, allowlistPath). The production tests AND the non-vacuity fixture now call the SAME code → the proof exercises real predicates, not a copy. testPractices behavior preserved.
- **C0 (AM1)** — hardened `DddRules.repoTargetMap`: extracts the first generic type-arg of an interface's `extends … <T,…>` clause, base-interface-name-agnostic and line-break-tolerant. Added regression fixture `WidgetPartRepo extends BaseRepo<WidgetPart,\n Long>` → HG-AGG-REPO now provably fires through a custom base + line-broken type parameter (pre-AM1 regex would have missed it = false negative).
- **A** — all DDD tests GREEN; bash guards + adversarial fail-cases reproduce; run-all-guards 81/81.
- **C2 (CM1)** — `DddAllowlistBijectionTest`: runs each predicate with an EMPTY allowlist and asserts `violations == allowlist entries` EXACTLY (per-entry bijection, not sampling). Proves every exception/governed entry is load-bearing AND no violation is unguarded.
- **B (full sweep)** — adversarial refuter independently re-classified the 9 borderline entities (4 reclassified to member, 5 confirmed root). Completed B1 to ALL 73 via a mechanical "purely-parent-id-scoped repository" scan over every root: it flagged BillingEvent / PaymentEvent / Refund, each audited to a GENUINE root (own business key / own ledger-writer + nullable-FK audit rows / own service+state-machine). Final: 61 roots + 12 members, no hidden member-tagged-as-root remains.
- **C1 (false-negative fixtures, literal)** — added fixtures that PROVE each hard guard fires (CM3: hard-guard false negatives are fix-now, not documented gaps): cross-feature isolation (Outsider→WidgetPart), cross-aggregate pointer in a collection (CollectorRoot→List<WidgetRoot> — surfaced finding #6), god-service @Transactional mutating 2 roots (FixtureGodService), and the AM1 custom-base/line-broken member-repo.
- **E2 (adversarial refuter)** — allowlist-honesty refutation + non-vacuity bypass hunt (findings #2-#6).
- **D** — spec §6/§11 fidelity table (below).

## Adversarial findings & resolution
| # | Sev | Finding | Resolution |
|---|-----|---------|------------|
| 1 | HIGH | 4 entities (TransformationLeg, RegisterReading, ChangeRecord, EmailTemplateHistory) mis-tagged ROOT — they are parent-id-scoped, written only through the parent service, never referenced by own id (= member profile, matching CommentEdit/GrossObligation/NetPosition). | **FIXED (fix-now).** Reclassified all 4 to `@AggregateMember(root=…)`. Added 4 `member-repo` allowlist entries (now 9 total, consistent). Removed 2 `governed_god_service` entries (TransformationService#record, EmailTemplateService#upsertTemplate) — they mutate one root + its member now, so the grandfather became unnecessary rather than parked. |
| 2 | HIGH | No guard catches a should-be-member tagged ROOT (member discipline is opt-in by self-tagging; HG-AGG-REPO only checks members). This is why #1 was undetectable. | **Documented TIER-2 limit.** Aggregate-boundary adequacy is explicitly a human-judgment TIER-2 review item in the spec (§7: "애그리거트 경계 적정성"). A precise mechanical guard is infeasible (the 4 real cases used plain `Long` FKs, not object composition, so a structural object-FK check would not have caught them); classification correctness needs review, not a heuristic that risks false-blocking legitimate roots. Recorded as a named limitation. |
| 3 | MED | `Payment.setState` is PUBLIC (encapsulation hole on a state-machine-governed entity) grandfathered in `governed_state_mutators` rather than fixed. | **Kept governed + follow-up.** Pre-existing payment-domain code; the real fix (route PaymentService/RefundService through PaymentStateMachine) is a behavioral domain refactor with test risk, out of scope for the DDD-enforcement work (surgical-changes discipline). Documented as a remediation follow-up; the entry is honest + bijection-proven load-bearing. |
| 4 | MED | `JobHistory` Javadoc says "Immutable execution audit row" but the entity is mutated post-creation. | Pre-existing doc-vs-code error, tangential to DDD. Reported as a follow-up (not fixed — surgical scope). Classification (root) is correct. |
| 5 | LOW | god-service-via-helper and state-mutator-by-other-name are reachable bypasses. | Accepted, explicitly documented heuristic limits (spec §6 "정직한 한계"); per CM3 only the 2 HEURISTIC guards may carry such limits. |
| 6 | HIGH | HG-AGG-REF missed cross-aggregate pointers held in COLLECTIONS — `DddRules.aggRef` used `field.getRawType()` which ERASES generics, so `@OneToMany List<OtherRoot>` resolved to `List` and the element type was lost (spec §6 explicitly requires "@OneToMany/@ManyToMany 컬렉션 element 타입 해석"). Surfaced by the literal Phase-C1 collection fixture (latent on the real tree — current @OneToMany are all root→own-member). | **FIXED (fix-now).** Changed to `field.getType().getAllInvolvedRawTypes()` (keeps generic args). The C1 fixture `CollectorRoot { List<WidgetRoot> }` now makes HG-AGG-REF fire; real-tree aggRef stays GREEN (own-member collections allowed). |

## Phase D — spec §6 / §11 fidelity table
| Spec item | Implemented as | Verdict |
|---|---|---|
| HG-FEAT-TOPLEVEL-TECH (TIER-0) | DddRules.topLevelTech | faithful |
| HG-FEAT-NOCYCLE (TIER-0) | ArchitectureNoCyclicPackageTest (kept, comment fixed) | faithful |
| HG-KERNEL-NO-FEATURE-DEP (TIER-0) | DddRules.kernelFeatureDep (kernel={common,observability}) | faithful |
| HG-FEAT-ISOLATION (TIER-0) | DddRules.featIsolation — bans cross-feature @Entity/*Repository (allowlist-aware) | faithful for entity/repo; **documented deviation**: the @PublishedApi default-deny for cross-feature *services* is deferred (the @PublishedApi marker exists but no TIER-0 guard consumes it yet — reserved for the post-wave service-isolation step). |
| HG-ANTI-SPLIT-ENDPOINT (TIER-0) | DddRules.antiSplitEndpoint — verb-controller lexical ban | faithful (the spec's "축소" loose-lexical subset; cohesion = TIER-2) |
| HG-AGG-REPO (TIER-1) | DddRules.memberRepo (member has no repo, allowlist-aware) | faithful ("member no repo" form; a root may have 0..1 repos) |
| HG-AGG-REF (TIER-1) | DddRules.aggRef (getAllInvolvedRawTypes → covers @OneToMany collection element types too) | faithful |
| HG-AGG-MEMBER-ENCAP (TIER-1) | DddRules.memberEncap | faithful |
| HG-ANTI-GODSERVICE-TX (TIER-1) | DddRules.godService (roots-only, direct-save, governed) | faithful + documented helper-delegation limit |
| HG-STATE-SOLE-MUTATOR (TIER-1) | DddRules.stateMutator (entity↔<X>StateMachine name-match) | faithful + documented limits (renamed setter / differently-named machine) |
| §11.1 TIER-0 block, 52 pkgs GREEN | all GREEN; cross-feature grandfathers in allowlist | met |
| §11.2 markers + allowlist + CI guard integrated | yes (run-all-guards + verify-completion) | met |
| §11.3 wave done, TIER-1 block, existing GREEN+expiry | yes (73 tagged; 9 member-repo + grandfathers w/ 2026-12-31 expiry) | met |
| §11.4 NEW-DOMAIN-CHECKLIST + headline | §1b added + §4 rows; headline 75 guards | met |
| §11.5 reference violation fixtures prove blocking | DddDecompositionViolationFixtureTest (5 guards) + AM1 fixture + bijection | met |

## Additional fixes surfaced during re-verification
- **Shared ArchUnit import (memory + DRY).** The 4 DDD test classes each statically imported the whole authblueprint bytecode tree (4× full imports). Consolidated into one cached `DddRules.authblueprint()` shared across all four — removes the redundant imports introduced this session.
- **Test fork heap pinned (OOM fix).** The combined footprint of the PRACTICES suite (many @SpringBootTest/@DataJpaTest contexts + several whole-tree ArchUnit imports) OOMed the default test fork non-deterministically ("Java heap space" on favoriteRepository / Persistence*). Added `tasks.withType<Test> { maxHeapSize = "2g" }` to backend/build.gradle.kts (the project had NO explicit test heap). testPractices now deterministically GREEN (118 tests, incl. 14 DDD).

## Net allowlist state after re-verification
- exceptions: 25 (15 cross-feature/FEAT-ISOLATION + 1 ProviderLink AGG-REF + 9 member-repo)
- governed_god_service: 4 (was 6; -2 from reclassification)
- governed_state_mutators: 2
- All bijection-proven load-bearing.

## Verdict
The re-verification found and FIXED one HIGH defect (4 mis-classifications) and one false-negative (AM1 repo extraction), strengthened the non-vacuity proof to exercise production code (CM2), and locked allowlist honesty with a per-entry bijection (CM1). Remaining items (#2/#3/#4/#5) are documented TIER-2/heuristic limits or tangential follow-ups, consistent with the spec's explicit scope. Final gate: R25 PASS at the new state.
