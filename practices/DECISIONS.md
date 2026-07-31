# DECISIONS — rule provenance + acceptance trail

> Every rule in `practices/rules/*.md` and every rejected candidate is recorded here. The
> file is the audit trail for "why was this rule added / refused / modified?". Without
> this trail the catalog is just opinion.

## How to read an entry

```
## <RULE_ID or candidate name>
- Status: ACCEPT | REJECT | DEFERRED | SUPERSEDED
- Date: YYYY-MM-DD
- Maintainer: <name>
- Evidence: see rule frontmatter `evidence:` (or "none" for REJECT entries)
- Rationale: 1-3 sentences on *why this passes provenance* (or *why not*)
- Alternatives considered: short list + why each lost
- Re-evaluation trigger: concrete event that would force a revisit
```

## Acceptance criteria (any maintainer applying ACCEPT must verify)

1. Rule has `evidence:` with ≥1 entry that either:
   - cites a `_MANIFEST.yaml` snapshot section + quote, OR
   - is an external citation (RFC / JEP / vendor reference / peer-reviewed paper)
2. The quote / citation is reachable today (not stale or 404).
3. The rule is not pure stylistic preference dressed as "best practice".
4. At least one alternative was considered and rejected with rationale.

If any criterion fails, mark `Status: DEFERRED` (or REJECT with rationale) — do not ACCEPT.

---

# Rules — ACCEPTED

> Entries below are placeholders to be filled out during the upcoming retrofit pass.
> Each rule listed here exists in `practices/rules/*.md`; the retrofit will populate the
> `Rationale / Alternatives / Re-evaluation trigger` fields and ensure the rule's own
> `evidence:` block matches what's recorded here.

## PRACTICES-PERS-001 — persistence-no-n-plus-1
- Status: ACCEPT (provisional — pending evidence retrofit)
- Date: 2026-05-15
- Evidence: spring-boot-3.5 snapshot + Spring Data JPA reference
- Rationale: (to be filled — JPA fetching docs explicitly identify N+1 + JOIN FETCH remedy)
- Alternatives considered: @EntityGraph (annotation-driven); pagination-only (does not solve depth)
- Re-evaluation trigger: Spring Data JPA major release changing default fetch semantics

## PRACTICES-TX-001 — transaction-no-self-invocation
- Status: ACCEPT (provisional)
- Evidence: Spring Framework reference §Declarative transactions
- Alternatives considered: AspectJ load-time weaving (heavier; project-wide); manual `TransactionTemplate` (verbose)
- Re-evaluation trigger: Spring AOP moving away from proxy-based advice

## PRACTICES-VAL-001 — validation-mass-assignment-guard
- Status: ACCEPT (provisional)
- Evidence: OWASP Mass Assignment Cheat Sheet + Spring MVC `@ModelAttribute` docs
- Alternatives considered: @JsonView (partial, fragile); allow-list serializers (heavier)
- Re-evaluation trigger: Spring Boot 4 binding model change

## PRACTICES-TEST-001 — testing-restassured-blackbox
- Status: ACCEPT (provisional)
- Evidence: rest-assured.io + Spring Boot Testing reference
- Alternatives considered: MockMvc (tight coupling); WebTestClient (good but reactive-leaning)
- Re-evaluation trigger: rest-assured archived OR Spring Boot deprecates @LocalServerPort

## PRACTICES-CORE-001 — core-constructor-injection
- Status: ACCEPT (provisional)
- Evidence: Spring Framework reference §Constructor-based dependency injection
- Alternatives considered: setter injection (mutability); field injection (reflection-only mocks)
- Re-evaluation trigger: Spring Framework 7 introducing first-class field injection ergonomics

## PRACTICES-CORE-002 — core-aop-proxy-no-final
- Status: ACCEPT (provisional)
- Evidence: Spring Framework reference §Proxying mechanisms (CGLIB constraints)
- Alternatives considered: AspectJ LTW (avoids the restriction at the cost of complexity)
- Re-evaluation trigger: Spring's default proxy backend changing (e.g. native image moving to LTW)

## PRACTICES-CORE-003 — core-singleton-no-mutable-state
- Status: ACCEPT (provisional)
- Evidence: Spring Framework reference §Bean scopes + JCIP "Java Concurrency in Practice"
- Alternatives considered: @Scope("prototype") (heavier, hides shared state); explicit synchronization (verbose)
- Re-evaluation trigger: Project Loom virtual-thread idioms shifting the default reasoning

---

# Rules — REJECTED / DEFERRED candidates (P2-D)

> Every controversial-rule candidate has a record here so the same debate doesn't restart
> from scratch in six months.

## quality-records-over-lombok — REJECTED
- Date: 2026-05-16 (was DEFERRED 2026-05-15)
- Decision: REJECT (closes the debate; not on the candidate list)
- Evidence considered: none authoritative. JEP 395 (records) and Lombok project docs cover overlapping but distinct surface; neither claims the other should be abandoned.
- Rationale: Style preference, not a falsifiable best practice. Records cover the immutable-value-object case but not `@Builder`, `@With`, or `@SneakyThrows`-style sugar. Without a clear external "사실상의 표준" deprecating one in favor of the other, accepting this rule violates the provenance criterion. Additionally, mandating it would cause portability fixtures that legitimately use Lombok (e.g. spring-petclinic forks) to false-fail.
- Alternatives considered: blanket rule (rejected — taste dressed as standard); ACCEPT with allow-list of safe Lombok annotations (rejected — same provenance gap); ACCEPT only in `ax-template` reference impl (rejected — ax-template already uses records by convention so the rule would be vacuous).
- Re-evaluation trigger: a JEP or OpenJDK communication that explicitly recommends migration, OR a documented community-wide consensus (e.g. Spring style guide adopting it). Until that exists, do not relitigate.

## arch-hexagonal-mandatory — REJECTED (new arch rules); arch category retained at current 3 rules
- Date: 2026-05-16 (was DEFERRED 2026-05-15)
- Decision: REJECT *new* hexagonal/modulith-style rules. The existing `arch-no-cyclic-package`, `arch-layer-boundary`, and `arch-repositories-extend-jpa` rules are universal and stay.
- Evidence considered: Alistair Cockburn's hexagonal architecture paper; Spring Modulith reference; the universal-vs-style-specific distinction.
- Rationale: The currently shipped arch rules (cyclic package, layer boundary, JPA repository convention) are universal — they hold under layered, hexagonal, Modulith, and clean architecture alike. Mandating a *specific* architectural style (Hexagonal port/adapter package layout, or Modulith module boundaries) would impose ax-template's taste on consumers of the template and would cause portability fixtures using traditional layered structure to false-fail. The arch category is therefore **closed at 3 rules** — no new style-specific rules accepted.
- Alternatives considered: Hexagonal-mandatory (rejected — imposes taste, breaks portability); Spring Modulith adoption (rejected — needs new dependency + vacuous on non-Modulith fixtures); leave the category open-ended (rejected — invites style debates per-PR).
- Re-evaluation trigger: a Spring-blessed architecture style guide that picks one and only one architecture for new projects.

## native-graalvm-required — REJECTED (category-level rejection)
- Date: 2026-05-16 (was DEFERRED 2026-05-15)
- Decision: REJECT the entire `native-` category. Catalog narrows from 22 → **21 categories**.
- Evidence considered: Spring Boot Native Image reference; GraalVM project documentation; the build-time cost of GraalVM SDK in maintainer feedback loops.
- Rationale: GraalVM Native Image is a niche, opt-in deployment target — most Spring Boot backends ship on the JVM. Adding the category would force GraalVM SDK into the maintainer build (build time 30s → 5–10 min, which destroys the fast-feedback property the methodology depends on), and external portability fixtures rarely declare native compatibility, so the rules would be vacuous (no fixture to verify against). When the team needs Native Image, the rules belong in a separate template (e.g. `ax-template-native`), not this one.
- Alternatives considered: ACCEPT category with 3 rules (rejected — build time regression unacceptable); ACCEPT as opt-in profile only (rejected — runs as vacuous in the default profile); separate template (deferred, not blocking this decision).
- Re-evaluation trigger: Spring Boot defaulting to native image in a future major release, OR ax-template being explicitly forked for serverless/Lambda deployment.

## balance-strict-25-percent — DEFERRED (advisory retained)
- Date: 2026-05-16 (re-affirmed)
- Decision: DEFER — balance stays **advisory only** (`gating.binary_only: true` in `rubric.yaml`).
- Evidence considered: none — the 25% threshold itself was a maintainer choice during ralplan, not externally anchored. Current catalog is 64 rules across 21 categories — well below 25% in any single category.
- Rationale: The 25% balance threshold is a **maintainer-set axiom**. Promoting it to a hard gate at 64 rules would lock in an opinion without external evidence; one of the categories could legitimately spike to 26% during natural growth (e.g. concentrated work on `persistence-` items) and trigger a meaningless merge block.
- Alternatives considered: ACCEPT 25% as hard gate (rejected — no data); RAISE to 40% then hard-gate (rejected — still arbitrary, same provenance gap); KEEP advisory (chosen).
- Re-evaluation trigger: empirical data showing that category-prefix concentration above some threshold N% causes measurable rule-quality regression — requires ≥ 30 rules of history *plus* a recorded incident where balance imbalance led to false confidence (e.g. security gap covered by overweight `error-` rules).

---

# Known limitations history

## ~~Snapshot bodies are navigation indexes~~ — RESOLVED 2026-05-15

- First noted: 2026-05-15
- Symptom (historical): the initial snapshots (`spring-boot-3.5`, `spring-security-6.x`)
  were fetched from `https://docs.spring.io/.../htmlsingle/index.html` landing pages —
  navigation indexes without topic prose. Keyword greps for `JOIN FETCH`, `Transactional`,
  `CGLIB`, `mass assignment` all returned MISS.
- Resolution (same day): rewrote `practices/upstream/fetch.sh` `SNAPSHOTS` array to 10
  topic-level deep references — Spring Framework reference sections, Spring Boot testing,
  Spring Data JPA query-methods, OWASP Mass Assignment Cheat Sheet, REST-assured Usage
  wiki, CWE-915. Confirmed 9/10 snapshots return ≥ 90 KB of real prose with keyword hits;
  `spring-jpa-fetching` URL corrected after WebSearch (the old `/jpa/fetching.html` path
  is a 404 — switched to `/jpa/query-methods.html`).
- All 7 existing rules now carry both `upstream_id`-shape evidence (with quoted
  substring extracted directly from the relevant snapshot) and the original
  `source_type: external` citations.
- Next refinement (still backlog): add an advisory `quote-match-check.sh` that asserts
  each `quote` field is a substring of the referenced snapshot's stripped text. Until
  then, drift between rule and source is caught only by humans during PR review.

---

# P2-D — final disposition 2026-05-16 (supersedes 2026-05-15)

The 2026-05-15 disposition kept all four candidates as DEFERRED. The 2026-05-16 user
sign-off resolves three of them to REJECT and leaves the fourth at DEFER:

| Candidate | 2026-05-15 | 2026-05-16 | Effect |
|-----------|-----------|-----------|--------|
| `quality-records-over-lombok` | DEFER | **REJECT** | Closed permanently. Records-vs-Lombok is style preference; no provenance available. |
| `arch-hexagonal-mandatory` (and other style-specific arch rules) | DEFER | **REJECT (new rules)** | The arch category is closed at the current 3 universal rules (cyclic / layer / jpa). No new style-specific arch rules are accepted. |
| `native-graalvm-required` (and the `native-` category itself) | DEFER | **REJECT (category)** | The `native-` category is removed from `rubric.yaml`. Catalog narrows **22 → 21**. |
| `balance-strict-25-percent` | DEFER | **DEFER** | Re-affirmed. Balance stays advisory. |

The catalog is now 21 advisory categories (was 22). 64 rules are distributed across 21
of those categories — every category currently shipped has ≥ 3 rules except where the
category itself is the universal-only kind (arch). No category exceeds the 25% balance
threshold; balance remains advisory.

The provenance trail behind each disposition is the corresponding entry above
(quality-records-over-lombok, arch-hexagonal-mandatory, native-graalvm-required,
balance-strict-25-percent). Re-evaluation triggers are documented per-candidate; without
the trigger event, do not relitigate.

## lang-records-for-dtos-widen — DEFERRED
- Date: 2026-05-16
- Decision: DEFER (keep rule's detection surface narrow at `*Request` / `*Response`)
- Evidence considered: 2026-05-16 portability measurement — both spring-petclinic and spring-realworld passed vacuously (neither uses the `*Request` / `*Response` suffix convention).
- Rationale: Widening to `Dto` / `Form` / `Payload` would expand the rule's reach across external fixtures but introduce false positives on non-DTO classes whose names happen to end in those tokens (e.g. `WebForm`, `JsonPayload`, `FormBuilder`). False positives damage catalog trust more than vacuous PASS damages coverage signal — and the four other portability-tested rules (cyclic-package, layer-boundary, no-system-streams, no-public-mutable-fields) already provide external validation of the catalog's portability claim.
- Alternatives considered: ACCEPT broad suffix set (rejected — false-positive risk on generic naming); annotation-based detection like `@RecordDto` (rejected — fixture authors won't opt in, so still vacuous); leave at `*Request` / `*Response` (chosen — honest scope, documented limitation).
- Re-evaluation trigger: a Spring-blessed style guide that endorses a specific DTO naming convention beyond `*Request` / `*Response`, OR portability measurement on a third fixture that demonstrates the rule failing to catch a real anti-pattern that the wider suffix set would have caught.

---

# Technical-debt entries

> Cycle-level architectural decisions and follow-ups. Rule provenance lives above; this
> section records changes to generators, guards, evidence policy, and the catalog's I/O
> surface — anything that affects how the catalog is built, verified, or interpreted.

## TD-2026-05-25-033 — AGENTS.md TOC + `generate_agents.sh` extension + 25th hard guard

- Status: ACCEPT
- Date: 2026-05-25 (R13 SP51)
- Predecessors: TD-024 (sha-input clause, R7 TD-020 idempotency); TD-032 (R12 PRD deferred-to-R13 standalone ADR)
- Owning PRD: `docs/superpowers/specs/2026-05-25-r13-toc-brn-checksum-prd.md`
- Decision: Extend `practices/generate_agents.sh` (+50 LOC) to append a `# Catalog TOC` section AFTER the rule-concat body. Sub-sections: L4 domains (with `applied_recipes:` cross-link), active recipes (with `enabled_l4_domains:` cross-link), sealed verdicts. Single-pass `MANIFEST_ROWS` cache parses `recipes/_MANIFEST.yaml` once. Cross-link rows comma-space join via awk helper `join_cs()` — NOT `paste -sd ', '` (cycles delimiter chars, producing malformed tokens `a,b c,d`). New 25th hard guard `practices/evals/agents_md_toc_disk_truth_guard.sh` (≤50 LOC) binary-verifies sha-asymmetry by re-running the generator and diffing the committed AGENTS.md (whole file + defensive TOC slice). Pass + fail fixtures under `practices/evals/fixtures/agents_md_toc_disk_truth/` prove the guard detects hand-edited TOC bodies.
- TD-024 amendment (Architect M1 / Codex hard #4 wording precision):
  - **sha-input clause UNCHANGED.** The sentinel `source_concat_sha256:` still covers `practices/rules/*.md` concatenation ONLY. Rule add/remove/modify triggers a genuine sha refresh; TOC-only mutations (L4/recipe/verdict adds) do NOT.
  - **I/O surface clause AMENDED.** `generate_agents.sh` now reads 3 additional disk surfaces — `templates/L4/*/README.md` (`applied_recipes:` block), `recipes/_MANIFEST.yaml` (`recipes:` / `- pattern:` / `enabled_l4_domains:`), `skills/_tests/sealed-verdict/*.md` listing — to emit observability TOC outside the fingerprint. Explicit documented expansion, not a side effect.
- Drivers: (i) R12 §8 mandate to specify whether the sentinel sha covers rule-concat only or full AGENTS.md content, and provide post-extension script shape before merging the cycle; (ii) Architect H2 / Codex hard #2 — sha-asymmetry deserves a binary guard, not documented-only handling (fork-receiver seeing sentinel unchanged might assume whole-file consistency).
- Alternatives considered:
  - Option (b) full-AGENTS.md sha — REJECTED. Amends TD-024 sha-input clause; rule-only edits would now ALSO refresh on cosmetic TOC changes, conflating two failure surfaces.
  - Defer R13 — REJECTED. R12 TD-032 stale debt; observability gap persists across cycles.
  - Bundle Axis B (BRN checksum) — REJECTED. R13 evidence-floor gate UNMET on 2026-05-25 (0 verbatim Korean authoritative primary + 0 international; 9 downgrades; 2 OSS-comment below R8-R10 rigor floor). Shipping low-rigor Axis B re-opens R12 Architect H1 BLOCKING.
  - Documented-only sha-asymmetry (iter 1 plan) — REJECTED iter 2. Fork-receiver semantic risk: prose alone does not surface drift on push.
  - Hand-edited TOC — REJECTED. Round-trip violation; re-opens R12 Architect H2.
  - `paste -sd ', '` join — REJECTED. Cycles delimiter chars across stdin lines; emits `a,b c,d` instead of `a, b, c, d` (Codex BLOCKING L).
- Why chosen: sha-input clause preserved (TD-024 invariant intact); I/O expansion explicit and bounded to 3 disk surfaces; ≤50 LOC generator add + ≤50 LOC guard; sha-asymmetry binary-verified on every push.
- Consequences:
  - Rule add → sha refresh (R12 behaviour preserved).
  - L4 / recipe / verdict add → TOC drift WITHOUT sha refresh, surfaced by 25th guard (NEW R13+).
  - Hand-edited TOC → guard restores committed AGENTS.md and exits non-zero (NEW R13+).
  - Schema drift in `_MANIFEST.yaml` (e.g. `- id:` instead of `- pattern:`) → inline `REC_COUNT == 11` assertion fails fast (NEW R13+).
  - macOS bash 3.2 + Linux bash 5.x both PASS — `cross_host_policy:` dual-host gate; single-host acceptance NOT permitted (Architect M2).
- Verification:
  - `bash practices/generate_agents.sh && bash practices/generate_agents.sh && git diff --exit-code practices/AGENTS.md` — idempotent.
  - `head -5 practices/AGENTS.md | grep source_concat_sha256:` → `d367ba2fdbee00f71c4ba0098c60e645192dd7076e3bff7c79b85ce4f7c102e4` (UNCHANGED from R12 baseline).
  - `bash practices/evals/agents_md_toc_disk_truth_guard.sh` → exit 0.
  - `bash practices/evals/run-all-guards.sh` → all guards PASS (24 → 25).
  - `bash practices/evals/run-all-guards.sh --include-fixtures` → all guards PASS (pass-fixture exit 0; fail-fixture exit 1 as expected).
- Re-evaluation trigger:
  - Catalog grows past current 12 L4 / 11 active recipes / 13 sealed verdicts (inline 12/11/13 assertion intentionally fails — update assertion + regenerate AGENTS.md in the same atomic SP).
  - AGENTS.md size exceeds ~10K lines (consider TD-034 split per R14+).
  - `## Hard guards` TOC sub-section requested (deferred to R14 TD-034 per Architect M4 / Codex soft #3).
- Follow-ups (deferred to R14+):
  - TD-034 `korean-brn-checksum` rule (R14/R15 retry on a bounded 9-source list; R16 UNMET → escalate to Architect rigor-floor downgrade vote).
  - TD-034 `## Hard guards` TOC sub-section.

# Audit

- Last reviewed: 2026-05-16
- Next scheduled review: when rule count crosses 100, or when a snapshot in `_MANIFEST.yaml` becomes stale (> 90 days, caught by `time_decay_guard`).
- 2026-05-15 — DECISIONS-P3.md signed: multi-layer hard enforcement activated (`.githooks/pre-commit` + `.claude/settings.local.json` PreToolUse + `practices-sentinel.yml`).
- 2026-05-16 — P2-D resolved: D1/D2/D3 REJECTED, D4 DEFERRED. Catalog narrows 22 → 21 categories (native removed). `rubric.yaml.advisory_metrics.balance.categories` updated. `MAINTAINER.md §4` updated.
- 2026-05-16 — N1 codified: branch protection policy moved from click-ops to source-of-truth (`.github/rulesets/main-protection.json` + `practices/scripts/setup-branch-protection.sh`). DECISIONS-P3.md §Activation step 3 updated.
- 2026-05-16 (afternoon) — N1 REVERSED: branch protection codification REMOVED. ax-template was scope-corrected to `/ax-transform` skill source — git workflow / branch protection / PR policy is fork-받은 팀의 영역, not the skill's. Files deleted: `.github/rulesets/main-protection.json`, `practices/scripts/setup-branch-protection.sh`. `.githooks/pre-push` Stage 0 (block direct push to main) removed. DECISIONS-P3.md §Activation reduced to catalog-quality-only. break-glass procedure + `practices/break-glass-log.md` deleted (procedure assumed PR-based workflow which the skill must not impose).
- 2026-05-16 — N2 resolved: lang-records-for-dtos-widen DEFERRED. Rule's detection surface stays at `*Request` / `*Response`. Re-evaluation trigger: Spring-blessed style guide endorsement OR portability measurement on a 3rd fixture revealing a missed anti-pattern.
- 2026-05-16 — N3 resolved: spring-modulith-example added as 3rd portability fixture (frademacher/spring-modulith-example, MIT-licensed, JDK 21, ~10s build). Result: all 5 rules PASS on modulith; the realworld cycle finding from the 1st run is now corroborated rather than tied — petclinic+modulith both cycle-free, realworld alone has the cycle. portability/run.sh auto-detects JDK 21 on macOS.
- 2026-05-25 — TD-2026-05-25-033 (R13 SP51 atomic-4) ACCEPTED: AGENTS.md TOC + `generate_agents.sh` extension (42 → 92 LOC, +50) + 25th hard guard `practices/evals/agents_md_toc_disk_truth_guard.sh` (39 LOC) + pass/fail fixtures. TD-024 sha-input clause UNCHANGED; I/O surface clause AMENDED for observability TOC emission outside sha scope. Sentinel `d367ba2f...` UNCHANGED across R13 (proof of TD-024 sha-input honored). Hard guards 24 → 25. R12 TD-032 closed. Axis B (BRN checksum) deferred R14 per §10 bounded 9-source retry — evidence floor UNMET 2026-05-25 (0 verbatim primary; 9 downgrades; 2 OSS-comment below floor).
- 2026-05-20 — R14 dogfood-14 — GAP-A + G11 closure (P1 loop closure verdict candidate):
  - **GAP-A (qualifier consistency):** `SlowProviderLatencyDecorator` constructor parameter changed from concrete `MockProvider delegate` to interface `PaymentProvider delegate` + `@Qualifier("rawPaymentProvider")`. `MockProvider` annotated `@Component("rawPaymentProvider")`. Bean-name constant `RAW_PROVIDER_BEAN_NAME = "rawPaymentProvider"` declared. 36th hard guard `practices/evals/payment_provider_qualifier_consistency_guard.sh` locks all three sides (constructor signature + MockProvider annotation + constant string). Verified bidirectionally: bare `@Component` → guard FAIL; restored → guard PASS. Catalog vision: fork-receiver adding real PG provider (Stripe / Toss / KG Inicis / NICE / KCP) registers their adapter under the same bean name + disables mock via profile gating, decorator wires transparently in both cases. iter-2 stash-drop scenario (interface-only fix without qualifier) no longer recurs because the guard rejects it.
  - **G11 (Korean PG signature evidence anchoring):** `practices/upstream/_MANIFEST.yaml` extended with three new snapshot entries — `kg-inicis-signature-2026-05`, `nice-payments-signature-2026-05`, `kcp-payments-signature-2026-05` — plus three corresponding `practices/upstream/*.snapshot.md` files recording algorithm + canonical signing string + header/param name + key env-var + replay window per vendor. Source URLs documented as `via: "fork-receiver-provided"` because the canonical vendor PDFs are partner-portal-gated; the snapshot bodies mirror the publicly downloadable sample-code READMEs so a fork-receiver authoring a verifier rule has a stable `upstream_id` anchor that passes `evidence_guard.sh`. Snapshot count 58 → 61. AGENTS.md sentinel UNCHANGED (TD-024 sha-input clause preserved — manifest-only edits do not refresh rule-concat sha).
  - **Verification:** `bash practices/evals/run-all-guards.sh` → 23 → 24 PASS (the new 36th guard joins; all 23 previous guards still PASS). Full guard sweep including all previous payment guards: GREEN. `./gradlew test --tests "PaymentProvider007Test" --tests "PaymentProviderMatrixTest" --tests "com.ax.template.authblueprint.payment.*"` — payment package fully GREEN. Full backend `./gradlew test` baseline shows 10–14 pre-existing flaky failures (BillingFlowIT / IdentityVerificationFlowIT / PortabilityCyclicPackageTest / PaymentAuthzTest under shared-context pollution); main HEAD exhibits the same flakiness — R14 changes introduce ZERO new regressions on the payment surface.
  - **Decision:** R14 closes the last two known R10/R13-deferred catalog gaps. If a subsequent sub-agent / dogfood loop discovers no further unclassified gap, this round qualifies as **P1 loop closure** — mirroring the P2 R12 verdict so the full dogfood loop terminates.

# 2026-05-26 Session — R50 through R75 catalog growth (ADRs)

## R50 — 5 frontend rules captured from R47 audit-feed dogfood
- Status: ACCEPT
- Date: 2026-05-26
- Drivers: R47 dogfood surfaced 5 frontend patterns that were repeatedly broken in PRs (destructive action no confirm, dashboard polls without refresh signal, server errors rendered raw, mutation outcomes invisible, secret-show-once with refresh leak).
- Alternatives considered:
  - Single mega-rule "frontend safety" — rejected, too vague to enforce, no clear remediation path
  - Per-rule ESLint plugin extension — deferred to R90 (out of this session's scope)
  - Documentation-only — rejected, R47 cycle proved discipline drifts without enforceable rule
- Why chosen: 5 specific rules, each with anchored OWASP/WCAG/React-docs evidence + Incorrect/Correct examples. Each rule's verification pattern is grep-able for a future mechanical guard.
- Consequences: 99 → 104 rules. Adoption tracked across R51-R55 (preempted day-one in new L4s).
- Follow-ups: ESLint plugin rules for each (R90, deferred).
- Commits: d55b96c5d819

## R51 — email-outbox L4 promotion (future_add → selectable, 20th L4)
- Status: ACCEPT
- Date: 2026-05-26
- Drivers: Outbox pattern is standard transactional-email infrastructure; previously a future_add tier reservation. Composition-kit fork-receivers consistently asked for the pattern.
- Alternatives considered:
  - Stay as future_add — rejected, deferring forever loses the catalog improvement opportunity
  - Backend-only (no frontend trio) — rejected, ops monitor is a real surface; admin page is needed
  - Use existing email modules (NotificationService) — rejected, NotificationService is content-routing; outbox is retry-state machine, different concern
- Why chosen: Spec Trio (8 spec items / 5 families) + full backend impl + full-trio frontend + R50/R52 lessons preempted day-one (sanitize, hash, confirm, dataUpdatedAt).
- Consequences: 19 → 20 L4. 1st L4 to ship with R50 rules applied from commit zero.
- Follow-ups: R60 dogfood iter1; R71 ledger entry; F4/F5/F10 deferreds.
- Commits: 15a9bf3 (R51), 33ada1e (R86 sanitize tests), 3cbbbff (F4)

## R53 — L0 fork-receiver-kit (frontend cross-cutting layer)
- Status: ACCEPT
- Date: 2026-05-26
- Drivers: Seven L4 trios had drift-prone inline copies of use-caller-id.ts / parse-error.ts / entity-key.ts (R55 was 7th identical copy). Each fix to one had to be hand-mirrored.
- Alternatives considered:
  - Keep inline copies + document discipline — rejected, R55 showed copies were already drifting (some had FavoritesError; others didn't)
  - Lift to a published npm package — rejected, fork-receivers fork source, not consume packages
  - Lift to L1 primitives layer — rejected, L1 is render primitives; helpers without JSX need a layer below
- Why chosen: New L0 layer below L1 (`templates/L0/fork-receiver-kit/`) hosts pure-TS helpers. L4 imports via `templates/L0/fork-receiver-kit/<helper>`. R53 deleted all 15 inline copies in one commit (no drift window).
- Consequences: 15 inline → 3 canonical files. L0 added as new layer. Fork-receiver bundle now includes L0 + per-L4 (small added burden, documented in IMPLEMENTATION-STATUS).
- Follow-ups: backend `common` package added by R67 mirror.
- Commits: 1635e5c

## R54 — identity-verification residual closure (8/8 envelope → 19/19 full)
- Status: ACCEPT
- Date: 2026-05-26
- Drivers: ralplan master plan listed "R54: identity-verification frontend full-trio". IDV spec declares `domain_mode: backend_only   # no frontend UI in scope; CI/DI callback is server-to-server`. Strict ralplan interpretation would have created PII-exposing admin pages.
- Alternatives considered:
  - Execute ralplan literally (create frontend) — rejected, violates spec's explicit declaration; reopens R2 closure
  - Skip R54 entirely — rejected, R2 left real backend gaps (VerifiedIdentity persistence, admin GET, audit publish)
  - Amend spec to remove backend_only declaration — rejected, would override 개인정보보호법 §24 reasoning that closed the spec at backend_only
- Why chosen: Re-scoped to backend residual closure. Added VerifiedIdentity entity + repository + adapters + service + admin GET + audit. NO frontend trio. Spec.domain_mode preserved.
- Consequences: 8/8 → 19/19 testIdentityVerification GREEN. Triggered R58 rule + R59 guard to mechanise the lesson.
- Follow-ups: R58 (rule), R59 (guard).
- Commits: 48f16bd

## R58 — spec-domain-mode-gates-frontend-trio rule
- Status: ACCEPT
- Date: 2026-05-26
- Drivers: R54 was caught by manual spec inspection. Future AI agents would re-make the same mistake without a rule + guard.
- Alternatives considered:
  - Documentation-only addition to METHODOLOGY — rejected, AI agents skip docs under pressure
  - Manual review checklist — rejected, doesn't scale across L4 count
  - Inline comment in each spec — rejected, doesn't gate creation of templates/L4/<domain>/app/
- Why chosen: Catalog rule with evidence (OWASP ASVS V1.2 + RFC 2119 MUST semantics). Documents the pattern + Incorrect/Correct shell examples. Sets up the mechanical companion (R59).
- Consequences: 104 → 105 rules. Forces every future "frontend trio" decision through spec.domain_mode check.
- Follow-ups: R59 mechanical guard.
- Commits: 405d453

## R59 — l4_frontend_domain_mode_guard (R58 mechanical companion, 41st guard)
- Status: ACCEPT
- Date: 2026-05-26
- Drivers: R58 was text discipline; needed mechanical enforcement so it doesn't drift.
- Alternatives considered:
  - Single-file guard (current shape) — chosen
  - CI-only enforcement — rejected, would not gate local development
  - Hook-based on git commit-msg — rejected, doesn't cover post-commit refactors
- Why chosen: Standalone bash guard scanning templates/L4/<domain>/app/ vs specs/<domain>-l0.yaml#domain_mode (with fallback to <domain>-frontend-l0.yaml for auth/crud). Refuses backend_only / absent / unknown.
- Consequences: 40 → 41 hard guards. Also retroactively added `domain_mode: full_trio` to 17 specs that lacked the field (silent grandfather closed).
- Follow-ups: none; guard is self-enforcing.
- Commits: 1c717e2

## R60 — email-outbox 2-persona dogfood iter1 (8 closures + EmailPiiHelper)
- Status: ACCEPT
- Date: 2026-05-26
- Drivers: R51 shipped fast; needed independent persona review before declaring catalog-ready. 11 findings (P1 ops: 5; P2 security: 6).
- Alternatives considered:
  - Skip dogfood (rely on R47 closure) — rejected, R47 was a different domain; new code surface needs fresh eyes
  - Single-persona review — rejected, different personas catch different gaps (ops vs security)
  - Run iter1 with 3+ personas — rejected, marginal gain over 2 for the first iteration
- Why chosen: Standard 2-persona protocol per CLAUDE.md. iter1 closed 7 real_bug (HIGH/MEDIUM); 4 scope_deferral (LOW); 0 methodology_gap.
- Consequences: PII discipline tightened (recipient hash + lastError storage scrub). EmailPiiHelper born — later promoted (R67).
- Follow-ups: R71 ledger; R67 lift trigger.
- Commits: ab4378e

## R67 — AuditPiiHelper lift to backend common package
- Status: ACCEPT
- Date: 2026-05-26
- Drivers: Seven backend modules (R62 ActivityFeed, R63 ScheduledTask/Webhook/ReportExport/AuditLog, R65 Notification, R72 NotificationDispatcher, plus origin emailoutbox) had adopted EmailPiiHelper. Helper name was email-specific; class was package-private to emailoutbox.
- Alternatives considered:
  - Keep package-private + duplicate inline in each module — rejected, defeats DRY; 7 sites would drift
  - Make EmailPiiHelper public but keep in emailoutbox — rejected, package coupling smell (why does ActivityService import from emailoutbox?)
  - Lift to a third-party shared lib — rejected, ax-template is monorepo
- Why chosen: New `com.ax.template.authblueprint.common` package, rename to `AuditPiiHelper` (semantically broader). 9 file touches in one commit (no drift window). l4_domain_reachability_guard updated to skip the new `common` package.
- Consequences: 1 canonical helper, 7+ adopters import from common. Triggered R80 rule capture.
- Follow-ups: R80 rule. R83 mechanical detector (deferred — see R88 "How to detect a missed lift").
- Commits: 7aea95e

## #39 — Money representation: layered boundary (long minor-units storage ↔ BigDecimal major PG-edge)
- Status: ACCEPT
- Date: 2026-05-31
- Drivers: IDW2 dogfood (all 3 personas flagged it) + the consistency audit: the catalog shipped two monetary representations with two contradictory-looking rules. `currency-amount-precision-explicit` (CRITICAL, billing-l0) mandates `long` minor-units in `..billing..` and prohibits `BigDecimal`; `lang-bigdecimal-for-money` (HIGH, payment-l0) mandates `BigDecimal` and its prose "rejects the mixed form". The reference workload IS the mixed form (billing/ecommerce/frontend = long minor; payment = BigDecimal major). Worse, the ecommerce→payment seam did `BigDecimal.valueOf(order.getTotalAmount())`, passing minor-units `long` into payment's major-units `BigDecimal` API — a silent 100x over-charge for every 2-decimal currency (KRW/JPY hid it because they are 0-decimal). Flagged SENSITIVE (relates to the R108 money-currency spec exclusion).
- Alternatives considered:
  - **New `specs/money-l0.yaml`** — REJECTED. The IDW2 ledger is explicit: "RECONCILES the existing contradiction, not a new competing spec." Avoids resurrecting the excluded R108 money-currency spec.
  - **Full migration to `long` minor-units everywhere** (rewrite payment BigDecimal→long: PAYMENT-MONEY-001, Refund arithmetic, MoneyDeserializer, ArchUnit no-BigDecimal) — REJECTED. Large + risky; rips up payment's decimal-string PG inputs + ISO-4217 multi-currency precision + partial-refund arithmetic. The ledger says "reconcile over time", not rip-and-replace.
  - **Doc-only reconcile** (ADR + rule-scope only, no code/guard) — REJECTED. Leaves the 100x seam bug merely documented, not mechanically blocked — violates "spec-first, guard-second" + north-star zero-tolerance.
- Why chosen: **Layered boundary.** `long` minor-units is the canonical INTERNAL/storage representation (billing/ecommerce/frontend); `BigDecimal` major-units is a payment/PG-EDGE transport reached ONLY through `common/Money.toMajorUnits` / `toMinorUnits` (decimal point placed at the ISO-4217 `Currency.getDefaultFractionDigits()` scale; strict — throws on sub-minor-unit precision). This satisfies `lang-bigdecimal-for-money`'s own escape clauses: (a) THIS ADR documents it; (b) the billing ArchUnit rule asserts `..billing..` monetary fields are `long`; (c) `payment-iso-4217-currency` asserts the scale. Both money rules updated to scope-and-cross-reference each other (no longer contradictory). The ecommerce seam now calls `Money.toMajorUnits(order.getTotalAmount(), order.getCurrency())`. `money_boundary_seam_guard.sh` (64th hard guard) mechanically blocks the raw `BigDecimal.valueOf(<minor getter>)` anti-pattern; `MoneyTest` (@Tag COMMON_ADVICE) pins per-currency round-trips (USD 1099↔10.99, KRW 1000, BHD 3-decimal) + the strict-precision contract + a regression assertion that the seam differs from the raw bug.
- Consequences: One canonical conversion seam (`common/Money`), guarded; the 100x multi-currency over-charge is fixed + cannot recur. `BigDecimal` confined to the PG edge by convention + guard. Frontend L0 `money.ts` mirrors the same minor↔major contract on the client.
- Follow-ups: if a future fork wants single-representation, the (a)(b)(c) escape in `lang-bigdecimal-for-money` is the documented path. payment's hardcoded `CURRENCY_SCALES` map could later defer to `Money.fractionDigits` (cosmetic, not a contradiction).
- Commits: (this commit)

## G006 — Forcing wire: scaffold→/ax-plan→impl gated by a scaffold-marker guard
- Status: ACCEPT
- Date: 2026-06-07
- Drivers: ultragoal G006 ("Gate ax-scaffold/dev entry on a filled+traced Spec Trio; route to /ax-plan if absent; bind to NEW-DOMAIN-CHECKLIST + METHODOLOGY"). After G005 built /ax-plan, nothing FORCED its use: a domain could be scaffolded (or even implemented) without a filled Spec Trio. Two latent gaps surfaced: (1) the two promoted hard gates (domain_spec_trio + spec_item_verification_binding) pass an EMPTY skeleton vacuously — 0 items → nothing unbound, files present per mode — so an unplanned scaffold was GREEN; (2) ax-scaffold's new-domain.sh emitted the backend spec as `<domain>-asvs-l0.yaml` while the entire catalog + /ax-plan + the binding guard use the canonical `<domain>-l0.yaml`, so the scaffold→plan chain did not actually connect.
- Alternatives considered:
  - **Gate by mapping impl-package → spec** (block if backend code exists without a filled spec) — REJECTED. The package↔spec name mapping is fragile (apiversioning↔api-versioning, identityverification↔identity-verification); l4_domain_reachability already carries a hardcoded camel→kebab table for exactly this pain. A second such map is a maintenance trap.
  - **Make domain_spec_trio_guard reject empty `items: []`** — REJECTED as the primary wire: some legitimately-staged specs may transit an empty state, and it conflates "files present" (that guard's job) with "filled".
  - **Doc-only routing** (just tell people to run /ax-plan) — REJECTED. Non-mechanical; violates north-star zero-tolerance.
- Why chosen: **A marker-based forcing guard + chain rename.** (a) `spec_scaffold_unfilled_guard.sh` (73rd hard guard, BLOCKING [70] in run-all-guards) fails the build while ANY `specs/*-l0.yaml`/`*-frontend-l0.yaml` still carries the literal `# TODO: Add` marker that new-domain.sh writes — so a scaffolded-but-unplanned domain keeps the catalog RED until /ax-plan fills it (which removes the marker by writing real items). The marker is matched precisely (`^\s*#?\s*TODO:\s*Add`) so it does NOT false-fire on bare `TBD`/`FIXME` that appear in complete specs' `introduced_at:` provenance prose. (b) new-domain.sh now emits the canonical `<domain>-l0.yaml` (not `-asvs-l0.yaml`) with a `domain_mode` placeholder, and its "Next steps" routes to `/ax-plan <domain>` as the required next action — connecting the chain ax-scaffold → /ax-plan → dev. No fragile mapping; the guard needs only the marker. Fixture pair (fail_unplanned/pass_filled) + live check wired. Passes on all 87 current specs (none carry the marker).
- Consequences: /ax-plan is now mechanically unskippable — you cannot land a scaffolded domain without planning it. The scaffold→plan chain is name-aligned. NEW-DOMAIN-CHECKLIST gains an explicit "STEP 0 — Plan first" gate; METHODOLOGY notes it. Only ax-scaffold's SKILL.md + new-domain.sh referenced the legacy `-asvs-l0` name (no guard/test depended on it), so the rename is safe.
- Follow-ups: G007 dogfood adds a real new domain end-to-end via /ax-plan and proves the gate forces the plan, then adversarial re-verify to convergence + final R25 green.
- Commits: (this commit)

## AX-DDD-AUDITLOG-ENTITY — retire 4 grandfather exceptions via a published AuditLogDto port
- Status: ACCEPT
- Date: 2026-06-08
- Drivers: The DDD decomposition wave shipped 4 `cross-feature-entity` grandfather exceptions in `aggregate_boundary_allowlist.yaml` (DsrService / IdentityVerificationService / SecretService / CircuitBreakerPolicy → `auditlog.AuditLog`, all expiry 2026-12-31, ticket AX-DDD-AUDITLOG-ENTITY). A design workflow ranked this the safe do-now remediation (vs AX-DDD-AUTH-USER = defer_imw; AX-DDD-MEMBER-REPO = Pattern B retires nothing because HG-AGG-REPO is visibility-agnostic; ECOM = keep_as_is composition). Verified against code: all 4 callers couple to the entity ONLY on the WRITE path (`auditLogService.record(AuditLog.builder()…)`) — the allowlist rationale "DsrService reads AuditLog entity for the access manifest" was inaccurate (no read path exists). So a single published write-port retires all 4.
- Alternatives considered:
  - **Keep the exceptions until 2026-12-31** — REJECTED. They are governed but real debt; the do-now closure is low-risk (1 test touched) and removes them before expiry.
  - **Add a read/query DTO port too** — UNNEEDED. No caller reads the entity; only the write coupling exists.
  - **Have `record(AuditLogDto)` delegate to `record(AuditLog)`** — REJECTED. That is a Spring self-invocation: it bypasses the proxy and loses the `REQUIRES_NEW` isolation. The DTO overload is independently `@Transactional(REQUIRES_NEW)` and calls `repository.save` directly.
- Why chosen: **A published `AuditLogDto` port (record + builder mirroring the entity's 11-field fluent API), marked `@PublishedApi`.** `AuditLogService.record(AuditLogDto)` maps DTO→entity (unset id/outcome/timestamp fall through to the entity builder's defaults, preserving prior behaviour exactly) and persists in its own `REQUIRES_NEW` transaction. The 4 callers now `import AuditLogDto` and call `AuditLogDto.builder()` — none import the `AuditLog` entity, so HG-FEAT-ISOLATION passes with no exception. The entity `record(AuditLog)` overload stays for the internal `AuditLoggingAspect` (same feature) and the test fixture. The 4 exceptions are removed from the allowlist; `DddAllowlistBijectionTest` enforces the bijection (4 fewer exceptions ↔ 4 fewer FEAT-ISOLATION violations). Note: `@PublishedApi` is currently documentary — the live `featIsolation` guard flags only cross-feature `@Entity`/`*Repository` refs, so a plain DTO is already legal; the marker-as-default-deny flip remains a separate deferred follow-up.
- Consequences: AX-DDD-AUDITLOG-ENTITY closed (allowlist 25→21 exceptions). Cross-feature audit writes go through a real published contract, not another aggregate's JPA root. One test migrated (`WebhookCircuitBreakerTest`: captor + 3 accessors entity→DTO; `record(any())`→`record(any(AuditLogDto.class))` to resolve the new overload's ambiguity). `IdentityVerificationFlowIT` (reads persisted `AuditLog` rows) and `AuditLogRecordNonBlockingTest` (aspect path on the entity overload) are unaffected — verified.
- Follow-ups: AX-DDD-AUTH-USER (defer_imw) and the `@PublishedApi` default-deny flip remain open.
- Commits: (this commit)

## AX-DDD-AUTH-USER — retire 11 grandfather exceptions via UserAccountService + reference-by-id
- Status: ACCEPT
- Date: 2026-06-10
- Drivers: The DDD decomposition wave grandfathered the deepest coupling in the tree — 11 allowlist
  exceptions (auth↔user, expiry 2026-12-31): 2 token entities + ProviderLink held `@ManyToOne
  UserEntity` object pointers, their repositories took `UserEntity` parameters, and
  AuthServiceImpl/OAuthService imported `UserEntity`/`UserRepository` directly (19+20 touchpoints).
  BACKLOG P0-1~11; the 2026-06-08 design workflow judged it defer_imw. This IMW closes it.
- Alternatives considered:
  - **UserLookupPort (read-only SPI)** — REJECTED. Signup/login/verify/reset MUTATE the User
    aggregate; a lookup-only port covers none of the load-bearing flows.
  - **Repository-mirror port** (port methods = UserRepository methods) — REJECTED. A port that
    mirrors a repository decouples nothing (god-port smell); the entity still leaks via returns.
  - **Feature merge (auth+user → identity)** — REJECTED for now. A package-physical merge is a
    repo-wide rename; the port approach achieves the same boundary without the churn.
- Why chosen: **3-wave mechanical retire.** (A) RefreshToken/VerificationToken reference the User
  aggregate BY ID — `@ManyToOne UserEntity` becomes `@Column(name="user_id") UUID userId`, which is
  SCHEMA-IDENTICAL (the JoinColumn name is preserved, so the V028 `REFERENCES users(id)` FK still
  applies); repositories take `UUID userId`. (B) ProviderLink + ProviderLinkRepository +
  OAuthProvider relocate user→auth (the OAuth identity-link is auth-domain data) and go by-id the
  same way. (C) `user.UserAccountService` (@PublishedApi) is the use-case port — authenticate /
  register / registerOAuthAccount / markEmailVerified / resetPassword / changePassword / findBy* —
  so hashing AND credential verification live next to the aggregate that stores the hash;
  `UserAccountDto.hasPassword` replaces the raw `hashedPassword` the old coupling exposed (the only
  cross-feature question ever asked of the hash outside a credential check is the OAuth unlink
  guard's "does one exist?"). FEAT-ISOLATION flags only `@Entity`/`*Repository` targets, so the
  service+DTO dependency is legal with ZERO exceptions.
- Consequences: allowlist exceptions 21→10 (AX-DDD-AUTH-USER fully retired); `DddAllowlistBijectionTest`
  then FORCED a second cleanup — AuthServiceImpl#verifyEmail/#resetPassword stopped being
  god-service-tx (they now touch one aggregate's repo + the port), so governed_god_service shrank
  4→2. The bijection working exactly as designed: a refactor that removes the smell must also remove
  its grandfather. 4 test files migrated (setUser→setUserId, findByUserAndTokenType→ById). Behaviour
  preserved: testAsvs 26/26, testPractices, E2E + OAuth smoke GREEN.
- Follow-ups: AX-DDD-MEMBER-REPO ×9 and AX-DDD-ECOM-COMPOSITION ×1 remain (BACKLOG P0-12~20);
  the auth↔user boundary question "merge into identity?" stays open as a future maintainer call.
- Commits: (this commit)

## AX-DDD-MEMBER-REPO — retire all 9 member-repository exceptions via root-repo JPQL + MemberWriter
- Status: ACCEPT
- Date: 2026-06-10
- Drivers: The DDD wave grandfathered 9 `member-repo` exceptions (an @AggregateMember entity owning
  its own Spring Data repository — HG-AGG-REPO): ApprovalStep, CommentEdit, SubjectMember,
  GrossObligation, NetPosition, TransformationLeg, RegisterReading, ChangeRecord,
  EmailTemplateHistory. BACKLOG P0-12~20, expiry 2026-12-31. Audit found every repo had exactly ONE
  service consumer (and ApprovalStepRepository was fully dead — 0 methods, 0 callers).
- Alternatives considered:
  - **Per-domain custom repository fragments** (interface + impl ×8) — REJECTED: 16 boilerplate
    files for what one shared seam expresses.
  - **Cascade via root @OneToMany collections** — REJECTED: changes entity load semantics
    (a register's thousands of readings cannot ride on the root), touches mappings catalog-wide.
  - **Reclassify members back to roots** — REJECTED: the 2026-06-08 adversarial re-verification
    settled the classifications; reversing them to dodge the guard would be gaming it.
- Why chosen: **load/mutate-through-root, mechanically.** Member READS become explicit JPQL
  `@Query` methods on the ROOT's repository (a root-typed interface may query any entity in JPQL;
  derived method names cannot — they parse against the root type). Member WRITES go through ONE
  shared seam, `common/MemberWriter` (@Component wrapping EntityManager persist/persistAndFlush/find;
  precedent: `common/OptimisticLockingSupport` is the same EM-backed kind of helper), called only
  from the owning root's service inside the root's transaction/lock. The netting INDEPENDENT
  per-node cross-check (repo-SUM vs in-memory reduction — IDW15's false-backstop lesson) is
  preserved verbatim as root-repo JPQL. Dead code was deleted, not migrated
  (ApprovalStepRepository; EmailTemplateHistory's caller-less listing query).
- Consequences: NO @AggregateMember owns a repository; `DddAllowlistBijectionTest` then FORCED the
  allowlist cleanup (exceptions 10 → 1 — only the deliberate ecommerce composition remains; the
  member-repo escape-hatch class is EMPTY). 3 emailoutbox tests migrated to mock the MemberWriter
  seam. 8 per-domain tasks + testPractices GREEN.
- Follow-ups: none for P0 — this closes the LAST P0 item (P0 26/26, 100%).
- Commits: (this commit)

## no-caller-identity-from-props — 15th ax/* ESLint rule (practices-react/eslint-plugin-ax)
- Status: ACCEPT
- Date: 2026-07-20
- Drivers: consumer-proof gap-convergence engine wave-2, Cell 4 (coverage-map.yaml `S2.AUTHZ.FE`).
  Three practices-react rule docs (`audit-log-frontend-viewer-rbac-virtualized.md`,
  `impersonation-banner-required-when-acting-as-other-user.md`,
  `no-impersonation-bypass-via-helper-rename.md`) already documented the caller-identity/impersonation
  concern, but all three are `verification.type: review` (human-only) — none of the 14 shipped ax/*
  ESLint rules mechanized it. Planted + closed as `canary-gaps.yaml` CANARY-010 this wave (the
  absence_proof grep against `practices-react/eslint-plugin-ax/rules/*.js` returned 0 hits at plant
  time; now matches this rule).
- Evidence: CWE-639 (Authorization Bypass Through User-Controlled Key) + OWASP API Security Top 10
  (2023) API1:2023 Broken Object Level Authorization — both quoted verbatim in the rule's `evidence:`
  block (`practices-react/rules/no-caller-identity-from-props.md`). This is the FE mirror of the
  already-accepted backend rule `caller-authentication-only-no-userid-param` (never accept `userId`
  via path/query server-side; derive from `Authentication`) — same structural fix, one layer up the
  call stack: derive identity from `useCallerId()` inside the function that makes the call, never from
  a prop/param/searchParams value a caller handed in.
- Alternatives considered:
  - **Leave it review-tier (do nothing)** — REJECTED: this is precisely the "verification escape" the
    gap-convergence engine exists to surface and close (CANARY-010's whole purpose); a mechanically
    enforceable shape with no mechanical enforcement is an honest gap, not an acceptable resting state.
  - **A generic "no-prop-named-userId" rule** — REJECTED: too broad (a display-only `userId` prop used
    only for a label, never fed into a data call, is not the vulnerability) — would false-positive on
    harmless display props and erode trust in the rule.
  - **Snake_case / arbitrary-identity-name coverage in v1** — DEFERRED, not rejected: scoped this
    rule to 4 identity names (`userId`/`currentUserId`/`actorId`/`memberId`) and 3 source names
    (`props`/`params`/`searchParams`) matching the originating task; recorded as an explicit
    `audit.completeness.amendments` entry in the rule's own frontmatter rather than silently narrowing
    scope.
- Why chosen: flags the 4 identity names sourced from (a) a function's own parameter destructuring,
  (b) destructuring FROM an identifier literally named `props`/`params`/`searchParams`, or (c) member
  access (including a computed key) or `searchParams.get(...)` on those same three names — when that
  value flows into an authz-relevant data call (fetch/get/load/query/find/list/search/filter/where, or
  a known query hook: useSWR family/useQuery/useMutation/useInfiniteQuery/useSuspenseQuery). Shipped at
  `error` directly (not `warn`→promote): a standalone Linter-API sweep across all 6 reference apps +
  `frontend/src` + `frontend/packages` + `templates/L1` + `templates/L4` (572 files) found 0 real
  violations before shipping. Wired into both `practices-react/eslint-plugin-ax/index.js`
  `configs.recommended` and `frontend/eslint.config.mjs` `sharedRules`.
- Consequences: `practices-react/eslint-plugin-ax` rule count 14 → 15; `frontend/eslint.config.mjs`
  gains one more error-tier rule with 0 violations across all reference apps (verified:
  `cd frontend && npm run lint` green). Closes `canary-gaps.yaml` CANARY-010 and flips
  `coverage-map.yaml` `S2.AUTHZ.FE` partial → covered.
- Follow-ups: snake_case / broader identity-name conventions (BACKLOG candidate, not yet registered —
  low priority given 0 live violations found).
- Commits: (this commit — not yet pushed; wave-2 INTEGRATION lane)

## TD-2026-07-29-P3-56a — audit-log LIST/GET stays any-authenticated
- Verified ALREADY ON DISK (blueprints/audit-log-manifest.yaml `rbac.read_policy`, landed 2026-07-28) — deliberately NOT re-authored. Default `any-authenticated` with stated rationale (masked actorIp, metadataJson never exposed, OWASP ASVS V7) plus two least-privilege options a fork MUST choose between: `self-only`, `auditor-role-only`. Closure class: decision-recorded.

## TD-2026-07-29-P3-56b — webhook inbound replay claim point
- blueprints/webhook-manifest.yaml#inbound-replay-dedup. The reference marks event_id seen at VERIFICATION time (InboundSignatureVerifier step 4 → ReplayDedupStore.firstSeen) with no rollback. Safe here ONLY because nothing processes the body after verify(); once a fork adds processing, a downstream failure turns the sender's legitimate retry into a 409 for the whole 300s window — at-least-once silently traded for at-most-once. Three patterns documented with guarantees AND costs; default_for_forks: mark-after-success (its concurrent-delivery cost is discharged by binding event_id to idempotency-l0). Catalog documents, does not gate: R26.

## TD-2026-07-29-P3-84 — ContextCache ceiling surveillance
- Adopt the row's own probe: re-run the aggregate at the OLD ceiling; any NEW failure is real inter-test state leakage, not flake — fix the test, never the ceiling. Trigger: before any major test-infra change and at least quarterly. The ceiling was made overridable (-PcontextCacheMaxSize, default 128) because the originally-specified -Dspring.test.context.cache.maxSize=32 does not reach the forked test JVM — the probe as first written would have proven nothing. -P over -D: a daemon -D persists into later runs reusing that daemon.

## TD-2026-07-29-P3-86 — contract-enum enforcement boundary, FINAL at the 5th narrowing
- Residual is deliberate evasion, not laziness: (a) a property-conditional @Bean implementing the SPI via a JDK Proxy under a configuration no test sets; (b) a wire literal injected via @Value/System.getProperty. No absence proof over an UNEXERCISED configuration can exist; claiming coverage would be the broad-but-false guarantee this catalog exists to block. Boundary stated by enumeration in the guard header, UNWEAKENED. The sound alternative is fail-closed ROUTING, shipped as an OPTIONAL fork-receiver pattern (blueprints/payment-manifest.yaml#callback.fail_closed_provider_routing, referenced from the guard header) — closing the provider set would force every fork to edit the catalog contract to register its own PG, a deployment-policy decision colliding with R26 autonomy.

## TD-2026-07-29-P3-89 — keep step-boundary tree sampling
- Recorded in practices/evals/midrun_tree_mutation_guard.sh header. Intra-step periodic sampling shrinks the window by a constant factor and never closes the class — only an immutable tree does. Cost: a ~2,200s background sampler needing reaping on every exit path incl. SIGTERM, whose own failure modes (orphaned process, false "mutated" from reading a file mid-write) land on the PUSH path. Exposure is already bounded by the sibling links (both endpoints clean and equal to the audited fingerprint). REVISIT TRIGGER: a demonstrated real evasion not constructed to demonstrate one; on that evidence prefer an immutable-tree run (read-only snapshot / container) over a sampler.

## TD-2026-07-30-(P3-93) — snapshot refresh is a fetch transaction, and the quote ratchet moves in five halves
- Status: ACCEPT
- Date: 2026-07-30
- Maintainer: ax-template (PRD-final-4 W1, Lanes A + B)
- **Refresh path.** Snapshot bodies were rebuilt with `curl` piped through the COMMITTED
  deterministic extractor `practices/scripts/snapshot-extract.sh` (strip script/style → strip tags
  → HTML-unescape → collapse whitespace; `--self-test` replays committed fixture pairs). No model
  in the loop, by construction. `practices/upstream/_FETCH-RECEIPTS.yaml` is established as the ONLY
  LEGAL REFRESH PATH: every attempted URL gets a `kind: fetch` row (url, curl exit, HTTP status,
  extracted byte count, sha256, fetched_at) and every touched identity a `kind: assembly` row
  binding its body digest to the fetch ids it was built from. A snapshot edited without a matching
  receipt is `RECEIPT_MISSING` (exit 2) — see the sibling entry (P2-57).
- Alternatives considered:
  - **WebFetch / WebSearch as the fetch mechanism** — REJECTED. A WebFetch result is a model's
    rendering of a page, not source bytes. A snapshot authored from one is a PARAPHRASE LABELLED
    VERIFIED, which is strictly worse than today's honest "unverifiable": it converts an admitted
    gap into a false provenance claim. Those tools were used for triage only (is the URL alive,
    where does the content live).
  - **Ledger-only ratchet, no fetch (the row's own fallback wording, "or ledger 추가 래칫으로
    플로어 상승")** — REJECTED as legitimate-but-weaker. It raises the floor without checking whether
    the citations are true; the P3-69 precedent showed fetch-verification finds real citation
    defects, and it did again here (19 of them).
  - **Doctoring a snapshot so a cited quote resolves** — REJECTED absolutely. Where citation and
    fetched page disagreed, the CITATION moved. 19 quotes across 18 files were re-anchored to genuine
    page text, including three that were never quotes of anything: a stale `sonner` description that
    actually belonged to shadcn's retired `toast` component, two invented Stripe webhook-event
    summaries, and a `next-themes` "cookie storage" premise the library does not implement
    (it uses localStorage + a blocking script). The remaining 27 of 37 shadcn-ui quotes matched the
    fresh extraction verbatim on first fetch.
- **Ratchet.** Protected-anchor floor 18 → **64** (= 18 + 46; the 46 newly-clean identities are
  disjoint from the existing 18 — the overlapping FILES carry a different `upstream_id` in each set,
  and identity is the `(path, upstream_id)` pair). All FIVE surfaces move together and are
  census-compared for EQUALITY *inside the guard on every live run*
  (`PROTECTED_LEDGER_CENSUS_UNEQUAL`, exit 3 — distinct from findings and from other structural
  defects): ledger rows == distinct `# require:` directives == `# min_entries:` ==
  `len(LIVE_REQUIRED_PROTECTED_IDENTITIES)` == `LIVE_MIN_PROTECTED_ENTRIES`. The prior `<=`/`>=`
  pair permitted the UPWARD HALF-MOVE — raise both numbers, add only some tuples and only some
  directives, stay green. Demonstrated: deleting ONE `# require:` line with all four other surfaces
  at 64 now exits 3; under the old asserts it exited 0.
- Also closed: a protected quote must now match at least one NON-heading line of the body
  (`TEMPLATE_QUOTE_ONLY_IN_HEADING`, exit 1). Snapshot HEADINGS are deliberately authored to carry
  cited `section` names because `section` is checked fatally — so heading text is the one part of a
  snapshot the citer writes, and a quote allowed to resolve there would be verified against the
  citer's own table of contents. Prose may not be authored; headings may.
- Consequences: 64 files / 68 anchors / 0 findings at exit 0. The advisory `--include-templates`
  sweep drops 53 findings → **7**, all `TEMPLATE_SNAPSHOT_FILE_MISSING` for `recharts-2026-05`.
- Residual, honestly bounded → **(P3-103)**: all 5 canonical `recharts.org/en-US/api` URLs return
  HTTP 404 with a byte-identical 1938-byte SPA shell, so no body can be authored from a static
  fetch. 6 identities / 7 findings, probe receipts committed (r049–r055), floor 7. NOT closable by
  this method and not papered over as one.
- Re-evaluation trigger: recharts publishes statically-fetchable API docs; or `time_decay_guard`
  approaches expiry (`practices-react` oldest `fetched_at` 2026-05-13) — at which point `fetched_at`
  may NOT be bulk-touched without re-fetching, which would be doctoring one level up.
- Commits: (this commit — PRD-final-4 wave, Lanes A + B)

## TD-2026-07-30-(P2-57) — snapshot bodies get a three-domain integrity chain and a shrink-only residual
- Status: ACCEPT
- Date: 2026-07-30
- Maintainer: ax-template (PRD-final-4 W1b, Lane B)
- **Problem.** The protected-anchor ratchet pins template citations to the TEXT OF SNAPSHOT BODIES,
  and nothing checksummed those bodies. Measured at wave start: of the 91 manifest ids with a
  committed `.snapshot.md`, **71 recorded a `sha`/`bytes` pair that did not describe the file it
  claims to describe**, and no guard looked. stripe-billing recorded 1657 bytes for a 2089-byte file.
  recharts-2026-05, next-intl-2026-05 and kakao-postcode-2026-05 shared ONE sha across THREE
  different byte counts — a sha256 is a function of the bytes, so at least two of those records were
  never computed from any file. Ratcheting 46 further identities onto bodies in that state would have
  been a PAPER ratchet: quote locked, quoted thing freely editable. Hence W1b landed IN-WAVE, before
  the ratchet was declared done.
- **`practices/evals/manifest_snapshot_integrity_guard.sh`** — a chain of three checks in three
  DISTINCT digest domains, deliberately never collapsed into one comparison (a single "does
  everything agree" digest is satisfiable by recomputing it, which is exactly what a doctored
  refresh does):
  - (a) FILE — every manifest id with a body: `sha`/`bytes` == `shasum -a 256`/`wc -c` of the WHOLE
    FILE (`MANIFEST_FILE_DIVERGED`, exit 1);
  - (b) BODY — every W1-touched id: the header's recorded body-sha == a recompute with the header
    stripped at the first literal `---` + blank line. The header records the BODY's digest, never its
    own file's: a self-referential sha inside the file it hashes is unverifiable by construction;
  - (c) RECEIPT — that body-sha == the id's assembly receipt (or single-URL fetch receipt), with every
    referenced per-URL row present (`RECEIPT_MISSING`, exit 2). This is the link that makes a fetch
    the only legal refresh: editing a body and its manifest entry together — the natural way to
    launder a doctored snapshot past (a) — leaves no receipt describing the new bytes, and writing one
    means recording a URL, an HTTP status and a fetched_at.
  The touched set is DERIVED from the assembly rows, not hardcoded: it is the committed ledger, not
  the guard's author, that says which ids are covered.
- **Allowlist.** `71` is the FROZEN baseline universe recorded in the allowlist header; the post-W1
  residual is **63** (71 − the 8 ids W1 synchronized), re-censused from disk at integration rather
  than carried over from the plan. Four mechanics are enforced by the guard, not by convention:
  subset-only against the frozen baseline (additions exit 2, so post-freeze divergence can be FIXED
  but never suppressed) · unique `(catalog, id)` keys · NON-REDUNDANCY (an entry whose manifest now
  matches disk is stale and FAILS, so the list length always IS the residual and burn-down cannot be
  absorbed by padding) · a non-empty per-entry `reason:`. A W1-touched id may not appear at all.
- Alternatives considered:
  - **Correct the 63 records by recomputing sha/bytes from the present file** — REJECTED, and this is
    the load-bearing judgment. The originally-fetched bytes are gone; recomputing would MANUFACTURE
    the provenance claim ("this is what the fetch produced") about bytes nobody can attest to — the
    precise defect this guard exists to make impossible. Honest suppression with a stated reason beats
    a fabricated green.
  - **Register the guard as advisory until the residual is zero** — REJECTED: an advisory guard is one
    nobody promotes. Blocking-with-a-shrink-only-allowlist blocks all NEW divergence today.
  - **Fold (a)+(b)+(c) into one digest** — REJECTED as above (cross-domain equality is self-certifying).
- Consequences: guard files 102 → 103 (three enforced headline counts updated: `README.md`,
  `CLAUDE.md`, `skills/ax-transform/SKILL.md`); `[87]` double floor 62 → 64 for two new kill-proofs
  (`fail_diverged`, `fail_stale_allowlist`); 4 fixtures. RED-verified: a ONE-BYTE edit to a
  Lane-A-fetched body → `RECEIPT_MISSING` exit 2, restored byte-identical → exit 0.
- Re-evaluation trigger: the 63-entry residual reaches 0 (delete the allowlist and the subset
  machinery with it); or an id needs refreshing WITHOUT an assembly row, which would reveal the
  derived-touched-set convention as too narrow.
- Commits: (this commit — PRD-final-4 wave, Lane B)

## TD-2026-07-30-(P1-anchor-ratchet) — every floor/baseline moves out of the working tree and into the previous release
- Status: ACCEPT
- Date: 2026-07-30
- Maintainer: PRD-final-4 wave, P1-seal lane
- Evidence: three cross-family-reviewer reproductions, each re-run here against the RELEASED copy
  of the guard (`git show origin/main:<guard>`, exit 0 = the bypass was real) and against the fixed
  copy (non-zero), then restored byte-identically with sha verification. Guard headers carry the
  mechanics; this entry carries the reasoning.
- Rationale: **the three bypasses had ONE root cause — every floor, pin set, frozen baseline and
  provenance record lived ONLY in mutable working-tree surfaces that a single commit can edit
  coherently together.** Duplicating a number in two in-tree places (the posture P2-51/P3-104
  established) raises the cost of a downgrade from one edit to five; it never makes it impossible,
  because all five are in the same tree as the change being reviewed. A "frozen" list that is
  re-read from the file being edited is not frozen — it is self-certifying. The fix is to move the
  reference OUT of the tree: `git show <ANCHOR>:<path>`, ANCHOR resolved at guard runtime as
  origin/main → HEAD.
  Why that is sound rather than circular: **R25 runs with the working tree at HEAD, and HEAD is
  AHEAD of origin/main — it is the commit being released.** So origin/main is genuinely the prior
  released state and is not editable by the commit under verification; the pre-push recency guard
  then binds every push to an R25 run at that HEAD, so nothing reaches origin/main without having
  satisfied the ratchet against the origin/main before it. The ratchet is transitive across
  releases: to lower a floor you would first have to release a commit that lowers it, which the
  gate at that commit refuses.
  The three reproductions, and what each proved was NOT covered by the in-tree pins:
  1. **P1-1, protected-ledger ratchet coherently downgradeable.** The five-surface census
     (`evidence_quote_spotcheck_guard.sh`) compares five CURRENT-TREE values *to each other*, so
     lowering all five at once — `LIVE_MIN_PROTECTED_ENTRIES` 64→63, drop the accordion tuple from
     `LIVE_REQUIRED_PROTECTED_IDENTITIES`, `# min_entries:` 64→63, drop the accordion `# require:`
     directive, delete the accordion ledger row — left the census EQUAL and the gate GREEN while
     `templates/L1/components/accordion.tsx::shadcn-ui-2026-05` silently left the fatal set.
     Reproduced: released copy exit 0 (census "all five surfaces == 63"), fixed copy
     `MONOTONIC_FLOOR_REGRESSION` exit **4**. A second reproduction covered SUBSTITUTION at a
     constant count (swap accordion for another identity, all five still 64):
     `PROTECTED_IDENTITY_REMOVED` exit **5** — which is why the identity comparison is a SUBSET
     check and not an equality or a count.
  2. **P1-2, refresh without receipt.** Chain scope was derived from the `kind: assembly` receipt
     rows, i.e. the guard asked the *ledger* which ids to verify and the ledger is editable. So
     refreshing an allowlisted NON-assembly id (`practices-react::cwv-2026`: doctor the body, sync
     the manifest sha/bytes to the new file, delete its allowlist row) passed domain (a), was a
     legal shrink of the allowlist, and the chain never looked. Reproduced: released copy exit 0
     (PASS, 62 entries), fixed copy `RECEIPT_MISSING` exit 2. Separately, REWRITING an existing
     assembly row's `body_sha256`/`body_bytes` to a doctored body's digest closed domain (c) on a
     receipt edited to fit — released copy exit 0 with all three in-tree domains agreeing on the
     doctored body; fixed copy `RECEIPT_LEDGER_MUTATED` exit **4**, as does a ONE-BYTE edit to an
     existing fetch row (`r002 bytes: 5431 → 5432`).
  3. **P1-3, "frozen" baseline mutable.** `baseline_universe` was enforced only against ITSELF, so
     diverging a clean id (`practices-react::mdn-promise-all`) and then adding it to
     `baseline_universe` + `entries` + bumping `baseline_count` satisfied subset-only. Reproduced:
     released copy exit 0 (PASS, "baseline universe 72"), fixed copy `BASELINE_MUTATED` exit **4**.
     `ALLOWLIST_GREW` exit 4 covers the second half (a residual entry that IS in the frozen
     baseline but was not suppressed by the previous release).
- Interaction resolved (reviewer's register note): **multiple `kind: assembly` rows per identity are
  now LEGAL.** Append-only and "one digest per body" are only compatible if a repeat refresh may
  APPEND — the old `RECEIPTS_DUPLICATE_ASSEMBLY` forced every repeat refresh to MUTATE history,
  which is precisely what the append-only ratchet must forbid. The chain therefore binds to the
  LATEST assembly row per identity (last in file order = last appended), and append-only keeps every
  earlier row byte-intact, so supersession is auditable rather than a silent overwrite. Two fixtures
  pin both halves: `pass_repeat_refresh_latest_assembly` (exit 0 here; exit 2 under the released
  guard, which is the proof the relaxation was required) and `fail_latest_assembly_mismatch`
  (latest row wrong while an EARLIER row matches → exit 2, so "bind to latest" cannot degrade into
  any-row-matches or first-row-wins).
- Alternatives considered:
  - **Add a sixth and seventh in-tree pin** — REJECTED: the reviewer's three reproductions are all
    the same shape, and N in-tree pins are defeated by N coordinated edits. Adding pins raises cost
    linearly and closes nothing.
  - **Store the floors in a signed/immutable sidecar file in the tree** — REJECTED: a sidecar is
    still in the tree, and a signature the tree can also produce verifies nothing. git history is
    already the immutable-relative-to-the-working-tree artifact, and the gate ordering above makes
    it the *released* state rather than merely an older one.
  - **Compare `baseline_universe` byte-for-byte instead of as a set** — REJECTED as implemented:
    the freeze's MEANING is the set of `(catalog, id)` identities, and set equality catches every
    addition and removal in both directions while tolerating reordering or a comment edit inside
    the list, neither of which weakens anything. Stated as a deviation from the literal
    "byte-equal" instruction rather than left implicit. `baseline_count` is still checked against
    the list length by the pre-existing in-tree check.
  - **Fail closed when no git anchor resolves** — REJECTED: it would break tarball exports and
    fresh forks with no history. Instead the anchor checks print a loud WARN and skip, and the
    honest consequence is stated in the guard headers: in a tree with no git history the ratchet is
    inert — but such a tree is also unpushable, because the pre-push recency guard is a git hook.
- Residual, stated rather than papered over: **a receipt is a SELF-REPORTED record and no offline
  gate can prove a curl happened.** Append-only closes REWRITING a released receipt; it does not
  close APPENDING a fabricated one (doctor the body, sync manifest + header, append a new fetch row
  and a new assembly row whose digest matches). What the chain guarantees is not "a doctored refresh
  is impossible" but "a doctored refresh leaves a permanent, immutable, reviewable claim naming a
  URL, an HTTP status and a fetched_at". Closing the remainder needs evidence the tree cannot author
  (an independent fetch at review time — cf. the periodic network `external_url_spot_audit.sh` — or
  a signed transparency log). Two candidate tightenings were considered and deliberately NOT added,
  because both would be paid for by future legitimate waves in false positives: requiring a
  newly-appended assembly row to cite at least one newly-appended fetch row (breaks registering an
  existing body under a second catalog) and freezing the ledger's `notes:` key (breaks documenting a
  later refresh).
- Coverage honesty: the anchor checks are gated on LIVE_ROOT — a fixture root has no release history
  of its own, so **these codes are not fixture-coverable**, and their non-vacuity evidence is the
  live pre-fix/post-fix reproductions above rather than a fixture pair. `[87]`'s double floor stays
  64/64: the two new fixtures are a PASS fixture and an exit-2 fail fixture, and `[87]` registers
  exit-1 fail fixtures.
- New failure codes: `evidence_quote_spotcheck_guard.sh` — `MONOTONIC_FLOOR_REGRESSION` (exit 4),
  `PROTECTED_IDENTITY_REMOVED` (exit 5); `manifest_snapshot_integrity_guard.sh` —
  `RECEIPT_LEDGER_MUTATED`, `BASELINE_MUTATED`, `ALLOWLIST_GREW` (all exit 4, deliberately distinct
  from 1 findings / 2 structural so "the ratchet was rolled back across releases" is never readable
  as "a body diverged" or "a shape is wrong"). Advisory WARN codes (never fatal):
  `ANCHOR_UNAVAILABLE`, `ANCHOR_FALLBACK`, `ANCHOR_GUARD_ABSENT`, `ANCHOR_ALLOWLIST_ABSENT`,
  `ANCHOR_RECEIPTS_ABSENT`, `ANCHOR_UNPARSEABLE`, `ANCHOR_BASELINE_MALFORMED`,
  `ANCHOR_DIFF_UNAVAILABLE`, `ANCHOR_RECEIPTS_UNCHUNKABLE`, `ANCHOR_NO_FLOOR`,
  `ANCHOR_NO_PIN_SET`.
- Re-evaluation trigger: the release branch stops being `origin/main` (the anchor name is pinned in
  both guards); or a legitimate wave needs to LOWER a protected floor / REMOVE a baseline entry, at
  which point the deliberate answer is a reviewed release that raises the floor differently, not a
  relaxation of the ratchet.
- Commits: (this commit — PRD-final-4 wave, P1-seal lane)

## TD-2026-07-30-(P1-anchor-ratchet-r2) — the anchor parser and the receipts sequence become fail-closed surfaces themselves
- Status: ACCEPT
- Date: 2026-07-30
- Maintainer: PRD-final-4 wave, P1-seal round-2 lane (report finalized by the main loop after the
  implementing agent was cut twice by transient API 529s; implementation was already on disk and
  is verified by the sweeps + an independent RED re-verification pass recorded in the wave report)
- Evidence: two round-2 cross-family reproductions, both attacking the ROUND-1 FIX itself.
- Rationale: round 1 moved every floor out of the tree and into `git show ANCHOR:`. Round 2 proved
  the two remaining trust points were still in the tree: (A) the anchor is only useful if it can be
  PARSED, and the parser's failure mode was WARN-and-skip — so release N could change nothing but
  REPRESENTATION (`64` → `int("64")`, alias the frozenset, or even reformat the heredoc fence) while
  runtime stayed 64/64 and its own gate stayed green, and release N+1 could then downgrade freely
  against an unparseable anchor; (B) receipts append-only compared rows/chunks keyed BY ID,
  discarding ORDER, while latest-assembly binding used FILE ORDER — so reordering intact chunks let
  an OLD assembly row become "latest" and a reverted snapshot pass with no new receipt, and
  indenting the whole list (identical YAML semantics) tripped the ADVISORY unchunkable path and
  skipped byte identity entirely.
  The repairs close the generation-N hole, not just the N+1 one:
  1. **SELF-PARSE (exit 6 SELF_UNPARSEABLE)** — on every live-root run the guard parses its OWN
     current file with the SAME extractor the anchor side uses. The laundered representation can
     never ship, because the release that introduces it fails its own gate. Belt-and-braces:
     anchor state (iii) "file present but unparseable" is now BLOCKING exit 7 ANCHOR_UNPARSEABLE
     (for histories predating the self-check); states (i) no-git and (ii) file-absent-in-anchor
     remain skip — (i) is unpushable anyway, (ii) is not attacker-creatable (git history is
     immutable to the working tree).
  2. **PREFIX RULE + SELF-CHUNK (RECEIPT_LEDGER_MUTATED exit 4 / RECEIPTS_SELF_UNCHUNKABLE
     exit 2)** — the anchor's row-ID and byte-chunk sequences must be an EXACT PREFIX of the
     current sequences; only suffix appends are legal (subsumes deletion/reorder/insertion-before-
     end; per-row field diffs are kept only for sharper diagnostics). The unchunkable path is no
     longer advisory anywhere on an anchored live root, and the CURRENT ledger failing to chunk is
     itself blocking — the ledger is machine-owned, its format is ours to keep chunkable, so the
     generation-N indent laundering also dies at its own gate.
- Commits: (this commit — PRD-final-4 wave, P1-seal round-2)

## TD-2026-07-30-(P1-anchor-authenticity) — the anchor itself is authenticated: it must be an ancestor, it must be a regular file, and the push must agree with the remote
- Status: ACCEPT
- Date: 2026-07-30
- Maintainer: PRD-final-4 wave, P1-seal round-3 lane
- Evidence: two round-3 cross-family reproductions, both attacking the ROUND-2 FIX itself. Every
  claim below was reproduced in throwaway clones under the session scratchpad; the working tree
  was verified byte-identical afterwards.
- Rationale: rounds 1-2 hardened WHAT the anchor says (its constants must parse, its ledger must
  chunk, its sequences must be prefixes). Round 3 attacked WHICH COMMIT the anchor IS, and HOW ITS
  BYTES ARE READ. Both were still fully in the attacker's hands.

  **P1-X — the local anchor ref is attacker-mutable, so bootstrap state (ii) is reachable after
  all.** Round 2 explicitly kept "this file is ABSENT at the anchor ⇒ first-release bootstrap ⇒
  skip" on the reasoning that "git history is not attacker-creatable". That reasoning was wrong,
  because the guards never verified WHICH history they were reading. `refs/remotes/origin/main` is
  an ORDINARY LOCAL REF and `git update-ref` writes it. Reproduction, verbatim: commit the
  malicious downgrade as M; build FAKE = M's tree MINUS the two anchor paths, parented on the REAL
  origin/main; `git update-ref refs/remotes/origin/main FAKE`; run the guards. Both ratchets found
  their own file absent, took the bootstrap skip and exited 0 — measured: with an honest anchor the
  coherent five-surface 64→63 downgrade exits 4 MONOTONIC_FLOOR_REGRESSION, with FAKE it exits 0.
  The audit line could not help: it fingerprints the working TREE, and a ref is not part of the
  tree. Pre-push received the authoritative remote sha but used it only to pick a regression diff
  base.
  THREE INDEPENDENT LAYERS, all landed:
  1. **ANCESTRY — ANCHOR_NOT_ANCESTOR** (evidence exit 8 / manifest exit 5). The resolved anchor
     must satisfy `git merge-base --is-ancestor <anchor> HEAD`. FAKE is parented on REAL and is
     therefore not an ancestor of M. RED: exit 8 / exit 5, this layer fires FIRST on the reviewer's
     exact reproduction. The legitimate exception — a branch that has genuinely diverged from
     origin/main — BLOCKS with the remedy printed (`git fetch origin && git rebase origin/main`),
     because a release that does not descend from the release it claims to ratchet against has not
     been ratcheted at all.
  2. **BOOTSTRAP IS NOT FILE-ABSENCE ALONE — ANCHOR_BOOTSTRAP_IMPLAUSIBLE.** Stated plainly in
     both guard headers: A FILE THAT HAS HISTORY IN THIS REPO CAN NEVER LEGITIMATELY BE "ABSENT IN
     THE PREVIOUS RELEASE". Absence is honored only when `git rev-list -1 <anchor> -- <path>` is
     EMPTY, plus a redundant probe from the other end (the last HEAD-reachable commit touching the
     path must not be an ancestor of the anchor). RED, with layer 1 deliberately SATISFIED (the
     downgrade re-parented onto FAKE so the anchor IS an ancestor): both guards still block —
     evidence exit 8, manifest exit 5, naming FAKE as the commit that "touched" the path by
     deleting it. So layer 2 kills the reproduction independently of layer 1.
     DELIBERATE DEVIATION FROM THE BRIEF, stated: the brief's literal predicate ("`rev-list -1
     HEAD~1 -- <path>` non-empty ⇒ manufactured") BLOCKS A LEGITIMATE CASE — a ratcheting file
     added one or two commits ago, while origin/main still predates it, has real HEAD~1 history and
     an honest absence at the anchor. The shipped predicate asks whether the ANCHOR's history had
     the file, which is the false-positive-free form of the same idea.
  3. **AUDIT-BOUND ANCHOR + PRE-PUSH AUTHENTICATION — AUDIT_ANCHOR_UNBOUND / AUDIT_ANCHOR_FORGED.**
     `verify-completion.sh` now records `anchor_sha`/`anchor_kind` in the audit line, resolved by
     the SAME helper the guards source, so runner and guards cannot disagree. `.githooks/pre-push`
     passes the sha GIT HANDS IT for the ref being pushed — taken from the remote's own
     advertisement, the one copy no local `git update-ref` can rewrite — and
     `completion_checklist_recency_guard.sh` requires the recorded value to EQUAL it. A missing
     field is refused, not assumed honest. This layer holds even if 1 and 2 were both bypassed, and
     it also closes the "delete the tracking ref so the anchor falls back to HEAD" variant for
     free: HEAD's sha is never the remote's current tip on a push that ships work.
     RED: fixtures `fail_anchor_forged` (recorded FAKE, remote advertises REAL) and
     `fail_anchor_unbound` (no anchor_sha at all) exit 1; `pass_anchor_bound` exits 0; the
     PRE-round-3 recency guard exits 0 on `fail_anchor_forged`, so the fixtures are non-vacuous.
     Hook wiring measured against the real `.githooks/pre-push`: `--expect-anchor-sha` IS passed
     for `refs/heads/main`, is NOT passed for a feature ref or a ZERO remote sha, and a delete-only
     push still skips the stage entirely. Legitimate exception, printed in the failure text: the
     remote MOVED since the run — `git fetch` + re-run R25, which is the honest resolution because
     the ratchet was measured against a release that is no longer being extended.

  **P1-Y — symlink representation laundering. THE CLASS, stated once because it generalizes: THE
  ANCHOR SIDE READS GIT OBJECTS AND THE SELF SIDE READS THE FILESYSTEM, AND ANY PLACE THOSE TWO
  DISAGREE IS A LAUNDERING CHANNEL.** `open()`/`os.path.isfile` follow symlinks; `git show` /
  `git ls-tree` return the LINK BLOB, which is just the target pathname. So a path replaced by a
  symlink is read as real content by the generation that ships it and as an attacker-chosen
  pathname by the generation that anchors on it.
  Reproduction A (measured): at generation N replace the evidence guard with a symlink whose TARGET
  PATHNAME is itself valid, weakened python. Path components may contain any byte but `/` and NUL,
  so the `/` inside a quoted identity doubles as a directory separator and the pathname
  `LIVE_MIN_PROTECTED_ENTRIES=0;LIVE_REQUIRED_PROTECTED_IDENTITIES={("templates/L1/components/currency-input.tsx","stripe-billing-2026-05")}`
  both parses as python AND resolves on disk. Round-2 at gen-N: exit 0 (bash and the self-parse
  check follow the link and see the real 64/64). Round-2 at gen-N+1 with that commit as anchor:
  `anchor ratchet OK — floor 0 → 63, pin set 1 → 63 (superset)`, exit 0 — the 64→63 downgrade
  ships. (Worth recording: a SINGLE-COMPONENT target as literally sketched in the brief does not
  complete the bypass — it yields floor 0 but a pin set that is not a subset, so round-2 exits 5.
  The multi-component form above is what actually works.)
  Reproduction B (measured): make `_FETCH-RECEIPTS.yaml` a symlink at generation N (round-2: exit
  0). At N+1 `git show <anchor>:…` returns a scalar STRING, and every append-only layer was nested
  under `if isinstance(prior_doc, dict)` with NO blocking else — so rewriting a released row's
  provenance field exits 4 RECEIPT_LEDGER_MUTATED against an honest anchor and exits 0 against the
  symlink anchor. The entire ratchet retired itself in silence.
  REPAIRS:
  1. **MODE CHECK ON BOTH SIDES.** `ANCHOR_PATH_NOT_REGULAR` — `git ls-tree <anchor> -- <path>`
     mode must be 100644/100755 (120000 blocks). `SELF_PATH_NOT_REGULAR` — lstat (`[ -L ]`, not
     stat) on the leaf AND on every path component below the base, so a symlinked DIRECTORY cannot
     launder a regular leaf either. Applied to: both guard files, the protected-anchor ledger,
     `_FETCH-RECEIPTS.yaml`, both `_MANIFEST.yaml` files, the allowlist, and (in python, via
     `os.path.islink`) every `*.snapshot.md` body. RED: repro A gen-N → exit 2, gen-N+1 → exit 8;
     repro B gen-N → exit 2, gen-N+1 → exit 5.
  2. **TYPE-CHECK ANCHOR PARSE RESULTS — ANCHOR_MALFORMED (blocking), never a silent skip.** The
     `isinstance(prior_doc, dict)` nesting is gone (the block is dedented under two preceding
     fail-closed checks). RED for the belt-and-braces path, with the mode check deliberately
     satisfied (an anchor whose ledger is a REGULAR FILE containing a YAML scalar): round-2 exit 0,
     round-3 exit 5 ANCHOR_MALFORMED.
     SWEEP OF THE SAME SHAPE, every instance found and closed, all previously WARN+skip:
     · receipts anchor unparseable → ANCHOR_MALFORMED (was: WARN, then the isinstance nesting)
     · receipts anchor not a mapping → ANCHOR_MALFORMED (was: silent skip via the nesting)
     · allowlist anchor unparseable → ANCHOR_MALFORMED (was: collapsed into "absent ⇒ bootstrap")
     · allowlist anchor not a mapping (`ANCHOR_ALLOWLIST_SHAPE`) → ANCHOR_MALFORMED
     · allowlist anchor with no well-formed `baseline_universe` (`ANCHOR_BASELINE_MALFORMED`) →
       blocking; the freeze reference cannot be absent for the release that needs it
     · `git diff` against the anchor failing (`ANCHOR_DIFF_UNAVAILABLE`) → blocking on a LIVE root;
       change-driven scope is the layer that makes an edited-but-unreceipted body fatal
     `anchor_yaml` now returns a distinct `ANCHOR_ABSENT` sentinel, so "absent" and "unreadable"
     can never be collapsed again.
- Alternatives considered:
  · *Three copies of the anchor resolver (status quo).* Rejected: layer 3 requires the runner's
    recorded sha and the guards' resolved sha to be the SAME object. One helper
    (`practices/scripts/lib/release_anchor.sh`) makes that true by construction rather than by
    convention. The two end-to-end harnesses [97][98] now ship the helper into their sandboxes.
  · *Skip layer 3 when `anchor_kind != "origin/main"`.* Rejected — that is the bypass: delete the
    tracking ref, fall back to HEAD, self-compare, ship. Plain equality against the remote's sha
    covers the fallback case for free and needs no extra rule.
  · *Resolve symlinks and compare targets instead of refusing them.* Rejected: it keeps the
    asymmetry alive (git would still compare the link blob) and buys nothing — no anchor-critical
    path has a legitimate reason to be a link.
  · *Treat symlinked anchor paths as advisory.* Rejected for the round-2 reason: an advisory that
    silently retires a ratchet is the payload, not a degradation.
- Residuals, stated rather than papered over:
  · Layer 3 binds only the push whose REMOTE ref is the anchor branch (`refs/heads/main`). A
    feature-branch push does not advance origin/main and supplies nothing to authenticate. Correct
    by design, but it means the authentication fires at publish time, not at every push.
  · `ANCHOR_PATH_NOT_REGULAR`, `ANCHOR_NOT_ANCESTOR`, `ANCHOR_BOOTSTRAP_IMPLAUSIBLE` and
    `ANCHOR_MALFORMED` are anchor-bound and therefore live-root only — NOT fixture-coverable.
    Their evidence is the RED reproductions above. Only the self-side
    (`SELF_PATH_NOT_REGULAR`) is fixture-provable, and it now is:
    `manifest-snapshot-integrity/fail_receipts_symlink` and
    `evidence-quote-spotcheck/fail_protected_ledger_symlink`, both byte-copies of their PASS
    siblings differing only by the link, both exit 2, both exit 0 under the pre-round-3 guards.
    Exit 2 (structural), so the [87] exit-1 kill-proof floor stays at 64.
  · The pre-push hook remains opt-in per clone (`install-hooks.sh`). Layer 3 is only as strong as
    that installation — unchanged from every other push-time gate here, and stated so nobody reads
    "unpushable" as stronger than it is.
  · Symlink checking is lstat-based on the paths this repo names. A fork-receiver that adds new
    anchor-critical paths must add them to the two lists in the guards' shell preambles; nothing
    discovers them automatically.
  · The shared helper is a NEW dependency of both guards. It is resolved from the committed
    repo-relative path and, when (and only when) that path is absent, from `AX_RELEASE_ANCHOR_LIB`
    — an affordance for `fixture_kill_proof_guard` [87], which proves fixture non-vacuity by
    running a MUTATED COPY of each guard from a bare temp path. Because the override is consulted
    only on the absence of the committed file, it is inert on every real tree (verified: a bogus
    override on the live repo still exits 0) and cannot substitute a weakened helper into a live
    run. A helper that resolves nowhere is `RELEASE_ANCHOR_LIB_MISSING`, exit 2 — fail-closed,
    because without it neither the symlink check nor the anchor authentication runs at all.
- Commits: (this commit — PRD-final-4 wave, P1-seal round-3)

## TD-2026-07-30-(P1-anchor-runtime) — every input the ratchet trusts is authenticated at the moment of use
- Status: ACCEPT
- Date: 2026-07-30
- Maintainer: PRD-final-4 wave, P1-seal round-4 lane
- Evidence: four round-4 cross-family reproductions, all attacking the ROUND-3 FIX. Every claim
  below was reproduced in a throwaway clone under the session scratchpad (`git clone --no-hardlinks`
  of the worktree); the live tree was only ever read, and its `git status` was verified unchanged
  by the round-4 sweep afterwards.
- Rationale, and it is one sentence because the four findings are one finding:
  **EVERY INPUT THE RATCHET TRUSTS — how git interprets objects, the anchor ref at each read, the
  helper code itself, the audit record — MUST BE AUTHENTICATED AT THE MOMENT OF USE, NOT ASSUMED.**
  Rounds 1-3 authenticated the anchor's CONTENT (its constants must parse), its IDENTITY (which
  commit it is) and its REPRESENTATION (regular file, not symlink). Round 4 found that the
  ratchet's own RUNTIME was still unauthenticated in four places, and each of those places is an
  input exactly like the anchor is.

  **P1-1 — GIT REPLACEMENT REFS: the sha is stable, the object is not.**
  `git replace <real> <fabricated>` installs `refs/replace/<sha>` and every ordinary git command —
  rev-list, show, ls-tree, merge-base, log, diff — then reads the FABRICATED object while every sha
  printed anywhere stays authentic. Layers (1) ancestry and (2) bootstrap-plausibility ARE those
  commands, so they can be answered out of a history the attacker wrote; the audit line records the
  honest sha, and the pre-push binding against the remote's advertisement matches perfectly. The
  tree fingerprint cannot see it either — a replacement ref is not part of the working tree.
  RED, measured in the clone (`refs/replace/5e1f5a9…` → a commit whose tree carries an
  `evidence_quote_spotcheck_guard.sh` with `LIVE_MIN_PROTECTED_ENTRIES = 0`):
      git rev-parse origin/main                       → 5e1f5a9…  (UNCHANGED, before and after)
      git show origin/main:…evidence_quote…guard.sh   → LIVE_MIN_PROTECTED_ENTRIES = 0
      git --no-replace-objects show <same>            → LIVE_MIN_PROTECTED_ENTRIES = 64
      PRE-FIX guard (round-3 copy, in situ)           → exit 0     ← the ratchet's floor is now
                                                                     whatever the attacker chose
      POST-FIX guard                                  → exit 8  ANCHOR_REPLACE_REFS_PRESENT
      NEGATIVE CONTROL (replace ref deleted, post-fix)→ exit 0
  Closed twice over, deliberately: `GIT_NO_REPLACE_OBJECTS=1` is EXPORTED by the shared helper (so
  it reaches the python subprocesses inside the guards — the class of call site a wrapper function
  cannot reach) AND `ax_git` passes `--no-replace-objects` explicitly (so a call that loses the
  environment still reads real objects); on top of that, a tree carrying `refs/replace/*` is
  REFUSED outright by both guards, the runner, the recency guard and the hook. The refusal is the
  load-bearing half: "we read past them at every call site" is a claim about every present and
  future call site, which is not a property this catalog can keep true by inspection, and a
  released tree has no reason to carry them.

  **P1-2 — TOCTOU ON THE ANCHOR REF: a run is not an instant, and neither is a ref.**
  verify-completion captured the anchor sha for the audit line while each guard re-resolved
  origin/main INDEPENDENTLY, minutes later. Flip the ref to an ancient root commit for exactly the
  window the guards run in: they find their own files absent, take the first-release bootstrap
  skip, and the audit records the honest sha.
  RED, measured (origin/main → the repo's root commit 00c9e2c…):
      PRE-FIX guard, ref flipped                      → exit 0  (bootstrap-skips, silently)
      POST-FIX guard with the runner's pin exported   → exit 8  ANCHOR_REF_MOVED_MIDRUN
      POST-FIX guard STANDALONE, ref flipped 0.15s
        into its own 0.55s run (no pin at all)        → exit 8  ANCHOR_REF_MOVED_MIDRUN
      PRE-FIX runner, ref flipped 2s into the run     → exit 0, audit {"anchor_sha":"5e1f5a9…",
                                                        anchor_sha_end: absent, anchor_stable: absent}
      POST-FIX runner, same flip                      → exit 1, hard_fail 1, audit
                                                        {"anchor_sha":"5e1f5a9…",
                                                         "anchor_sha_end":"00c9e2c…",
                                                         "anchor_stable":false}
      NEGATIVE CONTROL, post-fix runner, ref untouched→ exit 0, anchor_stable true, endpoints equal
  SINGLE-RESOLUTION BINDING: the runner resolves ONCE and exports
  `AX_RELEASE_ANCHOR_SHA/_KIND/_REF/_ROOT` (the ROOT is part of the pin so a nested sandbox run
  cannot inherit an unrelated repo's pin); every guard re-reads the ref at ITS OWN moment of use
  and requires equality — the pin is used, and it is used only after being re-authenticated
  against the ref as it stands at that instant; a guard with no pin re-reads the ref after its own
  work; and the runner re-verifies at the END and writes the run AS A FAILURE on drift
  (`anchor_sha_end`/`anchor_stable`, mirroring `tree_stable`/`tree_samples` — written to the audit
  log, not suppressed, because `fail_fast_blocking_audit_guard` requires every blocking path to
  leave a trail). The consumer half is fixture-proven:
  `completion_checklist_recency/fail_anchor_moved_midrun` (pre-round-4 guard exits 0 on it),
  registered in the [87] kill-proof manifest.

  **P1-3 — THE SHARED HELPER IS AN INJECTABLE POLICY SURFACE.**
  Round 3 made `practices/scripts/lib/release_anchor.sh` the single decider, which also made it
  the single thing worth attacking. Three sub-attacks, three answers, all proven by
  `practices/scripts/ax-prove-helper-injection-blocked.sh` (registered in run-all-guards, with its
  own negative control):
  (a) INHERITED FUNCTIONS. The idempotence guard `return 0`-ed on `_AX_RELEASE_ANCHOR_LIB` BEFORE
      defining anything, so `export -f ax_anchor_*` plus a forged `_AX_RELEASE_ANCHOR_LIB=1` left
      the ATTACKER's policy authoritative. RED: the round-3 helper, sourced with those exports,
      reports `anchor=ATTACKER` and exits 0 — measured, and re-measured on every run of the proof
      script so it can never become a claim about the past. FIX: **the early return is gone.** The
      definitions are re-established on EVERY source; the marker guards only the injection REPORT.
      Every reserved name is `unset -f`'d before definition, and a name that ARRIVES defined with
      the marker unset — or a marker that arrives EXPORTED, which this file never does — is
      `HELPER_FUNCTION_INJECTED`, exit 2. POST-FIX: exit 2 on both signatures. The same treatment
      is applied verbatim to `.githooks/pre-push-lib.sh` and its `pp_*` namespace, which owns the
      anchor binding the push gate hands the recency guard.
  (b) SYMLINKED HELPER. Both loaders used `[ -f ]`, which FOLLOWS SYMLINKS, before any mode check
      existed. FIX: a minimal lstat walk + `git ls-tree` mode check runs INLINE in each consumer
      immediately before the source. BOOTSTRAP PROBLEM, and it dictates the shape: the check cannot
      live in the file it is checking, so those ~15 lines are **deliberately duplicated** in both
      ratcheting guards, verify-completion.sh and .githooks/pre-push. Factoring them out would
      undo the fix. POST-FIX: `HELPER_PATH_NOT_REGULAR`, exit 2.
  (c) DELETED HELPER. The helper was on nobody's anchor-critical list, so deleting it activated the
      `AX_RELEASE_ANCHOR_LIB` env fallback — an attacker-chosen policy file on a live tree. FIX:
      the fallback is honored only when the committed path is absent AND the root is NOT a git work
      tree (exactly the relocated-copy sandbox [87] needs; [97]/[98]/[the pyyaml and resume
      harnesses] copy the committed file instead, which is the pattern that should have been used
      all along). On a git root a missing helper is `RELEASE_ANCHOR_LIB_MISSING`, exit 2. AND the
      five ratchet-toolchain paths — the helper, `verify-completion.sh`,
      `completion_checklist_recency_guard.sh`, `.githooks/pre-push`, `.githooks/pre-push-lib.sh` —
      are now on BOTH guards' anchor-critical lists, so deleting one is
      `ANCHOR_BOOTSTRAP_IMPLAUSIBLE` and symlinking one is `ANCHOR_PATH_NOT_REGULAR` /
      `SELF_PATH_NOT_REGULAR`.

  **P1-4 — A HAND-AUTHORED AUDIT LINE WAS ACCEPTED AS PROOF.**
  `.ax-verify/runs.jsonl` is an ordinary append-only text file. `echo '{…}' >> runs.jsonl` with the
  pushed sha and the remote's advertised anchor satisfied every check the recency guard had,
  because every value those checks compare was supplied by the same author. Five answers, and the
  honest accounting of what each is worth:
  · **RECOMPUTE what is recomputable** (check 12). The tree fingerprint is recomputed from the
    working tree; a forged `"x"` — which used to satisfy "the tree is identified" — now fails.
    The algorithm moved to `practices/scripts/lib/tree_fingerprint.py` so writer and verifier
    cannot drift. HONEST LIMIT, stated because it changes what this is worth: ON A CLEAN TREE both
    of the fingerprint's inputs are EMPTY, so the digest is a CONSTANT shared by every clean tree
    of every commit (measured: `0a815065…` in three unrelated repositories). Since push
    eligibility already requires a clean tree, the recompute proves the value was produced by the
    algorithm and that the tree is as clean as claimed — it does NOT independently identify the
    code. `head_sha`, re-read from git, carries that.
    A MEASUREMENT MUST NOT DISTURB WHAT IT MEASURES: the helper is executed as a SUBPROCESS, never
    imported. `import tree_fingerprint` wrote `__pycache__/*.pyc` into the very tree being
    fingerprinted, and the first run of this check refused every honest push for a mismatch it had
    caused itself. Found by [97], fixed, and recorded here because it is the kind of self-inflicted
    wound a gate that writes is always one import away from.
  · **BIND to run-local evidence** (check 13). `.ax-verify/last_run.jsonl` must exist, must be
    about the same head and the same tree, and must contain no non-PASS step. A forger must now
    fabricate a consistent SET of artifacts rather than append one line.
  · **PIN THE SHAPE to the writer** (check 11). The line's field set must be exactly what
    verify-completion.sh emits, and the pin is cross-checked against that printf itself
    (`AUDIT_WRITER_SCHEMA_DRIFT`), so the two cannot drift apart silently and every field added
    later becomes a new forgery detector.
  · **REFUSE DUPLICATE KEYS** (check 2) — the reviewer's specific note. `json.loads` keeps the
    LAST occurrence, so a duplicated `*_end` field lets a placeholder pass while a human reading
    the line sees the honest value first. A record that says two things lets its writer choose
    which one is audited.
  · **NO HMAC, and this is a decision rather than an oversight.** A signature would make forgery
    infeasible instead of merely inconvenient. There is nowhere to keep a key in a PUBLIC
    fork-base catalog, and a key committed beside the data it authenticates authenticates nothing.
    Shipping the ceremony without the secret would be theater, so it is not shipped.
  RED, measured (each fixture is green on EVERY pre-round-4 axis; pre-round-4 guard → exit 0,
  post-fix → exit 1):
      fail_audit_line_forged_shape       0 → 1  AUDIT_LINE_SCHEMA_MISMATCH
      fail_audit_line_duplicate_key      0 → 1  AUDIT_LINE_DUPLICATE_KEY
      fail_anchor_moved_midrun           0 → 1  AUDIT_ANCHOR_MOVED_MIDRUN
      fail_run_ledger_missing            0 → 1  AUDIT_RUN_LEDGER_MISSING
      fail_run_ledger_head_mismatch      0 → 1  AUDIT_RUN_LEDGER_HEAD_MISMATCH
      fail_run_ledger_status_conflict    0 → 1  AUDIT_RUN_LEDGER_STATUS_CONFLICT
  Five of the six are registered in the [87] kill-proof manifest and the floor moves 64 → 69.
  `fail_run_ledger_missing` is deliberately NOT registered: neutering its condition lands on the
  empty-ledger check, so it cannot flip to exit 0 by a single-anchor mutation and would be reported
  vacuous for a structural reason rather than a coverage one. Saying so is cheaper than pretending.
- Alternatives considered and rejected:
  · *Trust `GIT_NO_REPLACE_OBJECTS` alone and skip the refusal.* Rejected: it is a claim about
    every call site forever, including ones nobody has written yet.
  · *Keep the helper's early return and simply not export functions.* Rejected: a gate cannot
    depend on its attacker's restraint. The definitions are unconditional now.
  · *Factor the inline helper preflight into the helper.* Rejected — that is the bootstrap error
    itself: the object under test would certify itself. The duplication is the fix.
  · *Sign the audit line with a repo-committed key.* Rejected as theater; see above.
  · *Have the recency guard re-run the checklist itself.* Rejected: the cycle the guard's own
    header has documented since it was written.
- Residuals, stated rather than papered over:
  · **Forgery is not impossible, only expensive.** Everything recomputable is reproducible by an
    attacker willing to run the same commands the runner runs. What round 4 closes is the cheap,
    observed shape — the appended hand-authored line — plus the drift of anyone who copies an old
    line. A determined party with write access to the repository can still fabricate a passing
    record, and no check in this catalog changes that without a secret it cannot hold.
  · **Two observations bound a window; they do not eliminate it.** The anchor is read at the
    runner's start, at each guard's own read, after each guard's work, and at the runner's end. A
    ref moved and restored entirely between two adjacent reads is unobserved — the same shape as
    the tree-sampling limit inherited from round 2 of the push-evidence work.
  · **`ANCHOR_REPLACE_REFS_PRESENT`, `ANCHOR_REF_MOVED_MIDRUN` (guard side) and
    `HELPER_PATH_NOT_REGULAR` are live-root / environment-bound and NOT `--root`-fixture-coverable.**
    Their evidence is the RED reproductions above plus the scripted proof
    `ax-prove-helper-injection-blocked.sh`, which carries its own negative control (the unattacked
    sandbox exits 0, so the attacks' exit 2 is attributable to them).
  · **Defence in depth and single-layer mutation proofs pull against each other, and round 4 paid
    that bill twice.** The new consumer-side layers independently block the attacks that [97] and
    [98] neuter one layer at a time to prove load-bearing, so with P+C (resp. S+T) neutered the
    push was still refused — measured, and that measurement IS the evidence that the recompute and
    the ledger cross-check are load-bearing. Both harnesses now neuter the new layer alongside the
    old ones, and the honest reading is written into them: "each of P and C is load-bearing GIVEN
    the recompute is off". Left alone, those harnesses would report themselves broken every time a
    layer is ADDED, which penalises exactly the thing they exist to encourage.
  · **The recency guard's schema pin couples two files.** Adding a field to
    verify-completion.sh's audit printf without updating `AUDIT_SCHEMA_KEYS` is
    `AUDIT_WRITER_SCHEMA_DRIFT` and blocks every push. That coupling is intentional and it is a
    real maintenance cost; it is stated in both files.
  · **Existing audit lines are now invalid by construction.** The two new fields mean every
    pre-round-4 line fails check 9. Re-running R25 was already required for any push (recency), so
    this costs nothing beyond saying it out loud.
  · The pre-push hook remains opt-in per clone (`install-hooks.sh`), unchanged from every other
    push-time gate here.
- Commits: (this commit — PRD-final-4 wave, P1-seal round-4)

## TD-2026-07-30-(P1-hermetic-runtime) — the ratchet stops inheriting its own runtime, and every "unknown" becomes a BLOCK

- Date: 2026-07-30
- Status: accepted
- Trigger: cross-family reviewer, ROUND 5. Round 4's seals were verified dead IN AN UNCONTAMINATED
  RUNTIME. The round-5 finding is that the runtime itself is contaminable: the gate is made of
  `git`, `python3`, `cd`, `pwd` and a handful of its own files, and ALL of those are supplied from
  outside. **UNIFYING PRINCIPLE, stated once and applied everywhere: the ratchet must execute
  HERMETICALLY — it may not inherit git context, shell functions, PATH, or a mutable
  implementation of its own evidence — and every verification must FAIL CLOSED, never fall open on
  an empty or unknown result.**

  **P1-1 — THE GIT REPOSITORY IDENTITY WAS NOT BOUND TO REPO_ROOT.**
  `git -C <path>` says where to start LOOKING; it does not say which repository to USE.
  `GIT_DIR`, `GIT_WORK_TREE`, `GIT_COMMON_DIR`, `GIT_OBJECT_DIRECTORY`,
  `GIT_ALTERNATE_OBJECT_DIRECTORIES`, `GIT_INDEX_FILE`, `GIT_NAMESPACE`, `GIT_CEILING_DIRECTORIES`
  and the `GIT_CONFIG*` family all override discovery and configuration from the environment, and
  `AX_RELEASE_ANCHOR_ROOT` scoped only the PATHNAME. RED, measured in a throwaway clone with a
  DIRTY tree and a CLEAN shadow checkout of the same commit:

      honest_fp     = c764531c8f18…      honest toplevel = …/probe/S   honest clean = false
      GIT_DIR=<shadow>/.git GIT_WORK_TREE=<shadow>:
      redirected_fp = 0a815065ebf5…      toplevel        = …/probe/C   clean        = true
      clean-tree constant of the shadow = 0a815065ebf5…   (identical — the dirty tree vanished)

  R25 would run its tests against the dirty tree while every head / status / fingerprint / anchor /
  cleanliness read describes the clean one; revert the dirty fix afterwards and the pushed commit
  keeps valid-looking clean evidence. FIX: a HERMETIC BOOTSTRAP, duplicated verbatim into every
  entry (`release_anchor.sh`, `verify-completion.sh`, both ratcheting guards, the recency guard,
  `run-all-guards.sh`, `.githooks/pre-push`, `.githooks/pre-push-lib.sh`) because it must precede
  the first git call, any `source`, and even the `cd`/`pwd` that computes the script's own
  directory: the WHOLE `GIT_*` family is unset (`GIT_NO_REPLACE_OBJECTS` included — inherited as
  `0` it RE-ENABLES replacement refs — and re-established as `1` by us), then the real toplevel is
  derived from the trusted root and REQUIRED to equal it (**GIT_CONTEXT_REDIRECTED**, blocking),
  and the derived gitdir/worktree are passed EXPLICITLY on every call for that root (`ax_git`,
  `pp_git`) so nothing downstream depends on discovery. `tree_fingerprint.py` scrubs the same
  family from its own subprocess environment and raises rather than returning a value when the
  work tree answering it is not the repo it was asked about (exit 3, never "nogit").
  POST-FIX: the same redirection now exits 1 at the push gate; `tree_fingerprint.py` on a
  subdirectory exits 3 with GIT_CONTEXT_REDIRECTED. Fixture: `fail_git_context_redirected`.

  **P1-2 — EXPORTED-FUNCTION INJECTION THROUGH COMMAND DEPENDENCIES.**
  Round 4 reserved this catalog's own names (`ax_anchor_*`, `ax_git`, `pp_*`). It did not reserve
  the COMMANDS those functions call, and bash imports exported functions across `bash script.sh`.
  RED, all measured:

      exported `git`      → PIN_BEFORE 5e1f5a9c8c4d → PIN_AFTER 00c9e2c000…, every rc 0
      exported `pwd`      → ax_anchor_export_pin records PIN_ROOT=/foreign/repo (the pin is keyed
                            on that value, so the root scoping is defeated at its own key)
      exported `python3`  → recency guard on a root with NO audit log: baseline exit 1
                            (AUDIT_LOG_MISSING) → exit 0. An entire python gate skipped.
      exported `bash`     → same shape one level up, at the run-all-guards dispatch: exit 0.

  FIX, in the same bootstrap: every command name the ratchet actually invokes is enumerated
  (git, bash, sh, python/python3, env, cd, pwd, command, builtin, printf, echo, eval, exec, read,
  test, declare, unset, export, local, source, grep, sed, awk, cut, tr, sort, uniq, head, tail, wc,
  find, ls, cat, cp, mv, rm, mkdir, mktemp, dirname, basename, date, shasum, sha256sum, xargs, tee,
  true, false); an inherited definition of any of them is **HELPER_FUNCTION_INJECTED** (blocking)
  and they are `unset -f`'d anyway (which also strips them from every child's environment);
  `BASH_ENV`/`ENV`/any surviving `BASH_FUNC_*` is **HERMETIC_ENV_HOSTILE** (blocking); `git` and
  `python3` are resolved ONCE to absolute paths from a PATH stripped of relative entries and
  validated to be executable regular files outside the repository
  (**HERMETIC_TOOL_UNUSABLE**), after which every call site uses `"$AX_GIT_BIN"` / `"$AX_PY_BIN"`,
  `builtin cd` and `builtin pwd`. POST-FIX: each of the four exports above exits non-zero with
  HELPER_FUNCTION_INJECTED before any check runs.
  DELIBERATE NON-CHOICE, stated because it is a real trade: the PATH is NOT replaced with system
  directories. On this maintainer's machine that would swap `/opt/homebrew/bin/python3` for
  `/usr/bin/python3` and silently change which interpreter carries PyYAML — hardening that turns
  guards into skips is a fail-open wearing a helmet.

  **P1-3 — THE FINGERPRINT RECOMPUTE FAILED OPEN AND AUTHENTICATED ITS OWN MUTABLE
  IMPLEMENTATION.** Three defects, one shape: the gate trusted a file it had only checked the MODE
  of, and treated "no answer" as "no problem".
  RED, measured with a hand-authored audit line whose `tree_fingerprint` is a bogus constant:

      honest helper                          → exit 1  AUDIT_FINGERPRINT_MISMATCH
      helper rewritten to `print("nogit")`   → exit 0   (recompute disabled: the comparison was
                                                        `if recomputed and recomputed != "nogit"…`)
      helper rewritten to print the recorded constant → exit 0  (writer and verifier are the same
                                                        compromised implementation)

  and in the second case the tree was DIRTY at the time, so the clean-tree binding died with it.
  FIX, three parts:
  (a) FAIL CLOSED — the recorded value must BE a 64-hex digest (`"x"` satisfied the old
      "non-empty, not nogit" test — fixture `fail_tree_fingerprint_not_a_digest`), the recomputed
      value likewise, and any other outcome is **AUDIT_FINGERPRINT_UNVERIFIABLE**. On a live root
      an unusable git context is itself blocking (**GIT_CONTEXT_UNUSABLE**) instead of the old
      "not a git repo and no fixture marker ⇒ PASS". The runner blocks the same way
      (FINGERPRINT_UNVERIFIABLE) rather than degrading to an `unverifiable-…` placeholder.
  (b) AUTHENTICATE THE TOOLCHAIN BLOB BEFORE FIRST USE — the six files that ARE the gate
      (`release_anchor.sh`, `tree_fingerprint.py`, `verify-completion.sh`,
      `completion_checklist_recency_guard.sh`, `.githooks/pre-push`, `.githooks/pre-push-lib.sh`)
      must be byte-identical to what git records for them at the sha under audit
      (**RATCHET_TOOLCHAIN_MODIFIED**, blocking in the runner, both ratcheting guards and the push
      gate), and the push gate performs the recompute with the PREVIOUS RELEASE'S copy of
      `tree_fingerprint.py`, read out of git into a temp file OUTSIDE the tree — so the verifier no
      longer shares the runner's implementation.
      **HONEST CONSEQUENCE, and it is a real cost, so it is stated rather than buried:** this binds
      the toolchain to the previous release. An uncommitted edit to any of those six files now
      BLOCKS R25 (toolchain work becomes "commit, then verify" — there is no `--dry-run` exemption,
      because an exemption keyed on a CLI flag is an exemption an attacker also passes). A REFACTOR
      ships freely (the prior implementation computes the same digest). A change to what the
      fingerprint algorithm OUTPUTS cannot pass this gate at all: shipping one is a deliberate
      human decision requiring the maintainer to land it with the hook uninstalled and to record
      that here. No override flag is shipped for it.
      Anchor strength, disclosed: the anchor is the sha the REMOTE advertises when the hook
      supplies it (unforgeable locally), else `origin/main`, else HEAD — and with HEAD the
      implementation is the one being audited, so the binding degrades to (a) alone. The push path,
      which is the one that matters, always supplies the remote's sha.
  (c) DIRTY SAMPLES ARE PERMANENT — `tree_clean_end` is now the ACCUMULATED value across every
      step-boundary sample, not the last reading, and the anchor is sampled at every boundary too
      (`anchor_stable` is an accumulator that the closing read can no longer reset). Crucially both
      accumulators read `git status` / `rev-parse` DIRECTLY, so they survive a compromised
      fingerprint helper. The push gate additionally reads `git status` itself
      (**AUDIT_TREE_DIRTY_NOW**) instead of believing the record's `tree_clean`.

  **FAIL-OPEN SWEEP (the same two shapes, everywhere they were touched).** Closed additionally:
  `ax_anchor_check_replace_refs` treated an enumeration FAILURE as "no replacement refs";
  `ax_anchor_check_ancestry` treated an unreadable HEAD as "nothing to compare";
  `_ax_anchor_bootstrap_implausible` treated a failed `rev-list` (either of the two probes) as
  "the path was never here" — i.e. the one branch that switches a ratchet OFF was reachable by
  making a git command fail; the recency guard treated a failed `for-each-ref refs/replace/` and a
  failed `git status` as clean; the pre-push hook skipped its helper-mode and replacement-ref
  probes entirely when `git rev-parse` failed. All now block on a live root.
  Also closed while in the same code (registered P2): `AX_RELEASE_ANCHOR_REF` was exported as part
  of the pin and never validated — it is now one clause of the pin comparison.

- Evidence: `practices/scripts/ax-prove-hermetic-runtime.sh` (registered in run-all-guards) runs
  every attack above against the LIVE push gate in a throwaway sandbox: (A) git-context
  redirection, (B) exported `git`, (C) exported `cd`+`pwd`, (D) exported `python3`, (E1) an
  UNCOMMITTED tampered fingerprint helper, (E2) the same tamper COMMITTED — plus (F) a NEGATIVE
  CONTROL that must PASS, and (A′) the identical attack in a sandbox built with the round-5
  additions removed, which must REPRODUCE (measured: A′ exit 0, A exit 1). Both halves are
  therefore non-vacuous by construction, and the neuter anchors are asserted unique so the harness
  goes stale LOUDLY. The two mutation matrices ([97] push-evidence, [98] mid-run) were extended
  with the new independent layers for the reason already recorded there: a matrix that reports
  itself broken whenever a layer is ADDED would penalise defence in depth.
- Residual, stated plainly: the bootstrap is written in bash, so a sufficiently exotic inherited
  definition of the very builtins it uses to detect definitions (`declare`, `unset`) is not
  something bash lets any script fully escape — the enumeration + the `BASH_FUNC_*` refusal is the
  practical bound, not a proof. Sampling remains at step boundaries (one step wide, not zero).
  And nothing here makes forgery IMPOSSIBLE: a party with write access who runs the same commands
  the runner runs can still fabricate a record. Without a key there is no ceremony that changes
  that, and a key committed beside the data authenticates nothing.
- Commits: (this commit — PRD-final-4 wave, P1-seal round-5)

### TD-2026-07-30-P1-preflight-and-raw-bytes — 게이트가 실행되기 전과, 게이트가 "바이트"라고 부르는 것 (round 6)

- Context: cross-family reviewer, P1-seal round 6 (2026-07-30). Round 5's seals were verified dead
  for the exact inputs they named. Four new P1s landed one level below them. Both are instances
  of two invariants, stated once so the next round can be checked against them rather than against
  a list of patches:
  - **(α) NOTHING OVERRIDABLE MAY EXECUTE BEFORE THE SCRUB THAT DETECTS OVERRIDES.**
  - **(β) ANY CLAIM ABOUT BYTES MUST BE MADE ON RAW BYTES, NEVER THROUGH A FILTERED/NORMALIZING
    VIEW.**

- **P1-1 — overridable commands ran BEFORE the scrub (α).** `.githooks/pre-push:36` executed
  `set -uo pipefail` ahead of its own hermetic bootstrap, and every bootstrap used `[ … ]` before
  the surviving-function sweep; neither `set` nor `[` was on the dependency list, and the
  evidence guard parsed ALL of its arguments before its bootstrap. MEASURED, honest baselines in
  parentheses: `BASH_FUNC_set%%='() { exit 0; }'` → pre-push **exit 0** (baseline 1);
  `BASH_FUNC_[%%='() { exit 0; }'` → the recency guard on an invalid root **exit 0** (baseline 2).
  A dependency LIST cannot close this: the list is consulted by code that has already run.
  FIX — every entry now opens with a **pure-keyword preflight** whose only tools are (i) shell
  keywords (`if`/`case`/`for`/`[[ ]]`), (ii) variable assignment, (iii) ONE command invoked by
  ABSOLUTE PATH (`/usr/bin/env` — bash never resolves a word containing a slash through the
  function table), and (iv) abort via `${x:?msg}`, which is a parameter expansion, not a command:
  a non-interactive shell prints to stderr and exits non-zero. `exit`, `echo`, `printf`, `[` and
  `set` are all shadowable and are therefore unusable at that point. `${!BASH_FUNC_@}` is NOT the
  sensor: bash imports the function and DELETES the shell variable, so the prefix expansion is
  empty in the child while the environ still carries the entry (measured, bash 3.2.57 / Apple);
  `/usr/bin/env` is the only unhijackable way to see the channel. The dependency list was
  additionally extended to every name any entry invokes (`set`, `[`, `test`, `printf`, `exit`,
  `exec`, `read`, `trap`, `shift`, `local`, `eval`, …), and the evidence guard's argument parsing
  was moved BELOW its bootstrap. New codes: **HERMETIC_PREFLIGHT_HOSTILE**,
  **HERMETIC_PREFLIGHT_UNVERIFIABLE** (an `/usr/bin/env` that produces nothing is unknown, and
  unknown never passes).

- **P1-2 — an absolute path does not even smoke-test the binary, nor python's startup.**
  (HEADLINE CORRECTED, reviewer ROUND 8 / P1-B: it read "authenticates neither…", which implies
  that what replaced it DOES authenticate. It does not — see the scope note at the end of this
  bullet.) The round-5
  validator accepted anything lexically absolute that passed symlink-FOLLOWING `-f`/`-x`.
  MEASURED: a symlink named `python3` → `/usr/bin/true` passed every test and turned the recency
  guard's entire python body into **exit 0** (honest baseline 1); a `sitecustomize.py` calling
  `os._exit(0)`, reached through an inherited `PYTHONPATH`, did the same. FIX — (a) the tool's
  DIRECTORY is canonicalised with `builtin pwd -P` and the program is then RUN ONCE AGAINST A
  FIXED CHALLENGE (`git --version` must produce a git banner; python3 must print a self-report
  under `-I -S`), new code **HERMETIC_TOOL_UNAUTHENTIC**; (b) the whole `PYTHON*` family is scrubbed in
  the bootstrap and every ratchet-internal python call site runs `-I -S` (isolated, no `site`,
  which is what a `sitecustomize.py` rides in on); (c) the PyYAML concern is real and is answered
  rather than hidden — see the honest limit below.
  HONEST LIMIT, stated because the alternative is theatre: **`-I -S` is NOT applied to the two
  call sites that need PyYAML** (verify-completion's checklist parser and the manifest guard's
  body). On this maintainer machine PyYAML lives in the USER site directory, so `python3 -I -c
  'import yaml'` already fails; running the checklist parser isolated would block every honest run.
  Those two sites use `-E`, which still refuses `PYTHONPATH`/`PYTHONHOME`/`PYTHONSTARTUP` — the
  reviewer's actual injection vector — and they are preceded by an `import yaml` capability probe
  **run with the same interpreter and the same flags**, which BLOCKS
  (**HERMETIC_PY_YAML_UNAVAILABLE**) and never degrades to a skip.
  SCOPE OF (a), stated (reviewer ROUND 8 / P1-B): the challenge is **public and fixed**, so a
  hostile PATH wrapper forwards it to the real binary and answers correctly — the reviewer
  demonstrated exactly that in round 7 (P1-2), flipping `fail_audit_log_missing` from exit 1 to
  exit 0. (a) is therefore a **SMOKE TEST for a MIS-RESOLVED tool**, not identity and not
  authentication; PATH executables are declared trusted by
  TD-2026-07-30-(ratchet-threat-model), with the external-trust-root work registered as
  docs/BACKLOG.md P2-68. The code name `HERMETIC_TOOL_UNAUTHENTIC` is retained only because
  guards, fixtures and entries in this file reference it; it means "did not answer the smoke
  test". The residual is a
  `sitecustomize.py` already installed inside site-packages, which is the same trust domain as
  PyYAML itself.

- **P1-3 — six-file authentication was circular and incomplete.** The runner and both ratcheting
  guards authenticated the toolchain against **HEAD** — which is the thing being shipped, so a
  COMMITTED backdoor equals HEAD and certifies itself. MEASURED: inserting
  `if [ -n "$EXPECT_SHA" ]; then exit 0; fi` after the recency guard's argument parsing and
  COMMITTING it made `.githooks/pre-push` **exit 0** on a tree with no audit log at all (honest
  baseline 1) — invisible to run-all-guards (which drives that guard with `--fixtures`), to the
  falsification proofs (which omit `--expect-sha`), and to the guard's own self-check (which the
  early exit precedes). The six-path list also EXCLUDED both ratcheting guards and
  `run-all-guards.sh`. FIX — (a) the path list is **nine**; (b) the PUSH-TIME consumer extracts the
  **REMOTE-ADVERTISED previous release's** copy of the recency guard (plus the runner, the anchor
  library and the fingerprint helper) into a directory outside the tree and **RUNS it** against the
  live root, BEFORE the tree's own copy is invoked at all — an argument-triggered early exit cannot
  short-circuit a check that happens in the hook, not in the guard. Byte-equality with the anchor
  is deliberately NOT required; that would make every legitimate change to these files unpushable
  forever. New code **RATCHET_PRIOR_RELEASE_GUARD_FAILED** (+ **RATCHET_ANCHOR_COPY_UNVERIFIABLE**
  when no prior release can be resolved — fail-closed).
  MIGRATION REALITY, disclosed: a legitimate change to the toolchain ships in a release whose gate
  ran with the PRIOR copy. Tightenings are free. A change to what the prior copy MEASURES (the
  audit line's field set, the fingerprint algorithm's output) cannot pass this gate and is a
  deliberate human decision, exactly as the round-5 fingerprint binding already recorded.
  RESIDUAL, unprovable here and said plainly: **this hook cannot authenticate ITSELF.** A committed
  backdoor in `.githooks/pre-push` or `pre-push-lib.sh` is executed by git from the working tree,
  and the only thing that could catch it is the thing being subverted. Off-box review of the diff
  is what remains.

- **P1-4 — byte claims honoured git's clean filters (β).** `ax_ratchet_toolchain_authentic` used
  `git diff --quiet <rev> -- <path>` *specifically* so that "git's own filters/eol handling are
  applied on both sides" — which is the defect. MEASURED (the reviewer's axmask reproduction,
  re-run and recorded): with `filter.axmask.clean = cat <the committed copy>` and
  `$GIT_DIR/info/attributes: practices/scripts/lib/tree_fingerprint.py filter=axmask`, a working
  file carrying appended attacker bytes gave `git diff --quiet HEAD -- <path>` **rc 0**,
  `git hash-object -- <path>` = the HEAD blob `79e18672…`, and `git hash-object --no-filters`
  = the truth `3627bc8c…`; `git diff HEAD --binary` was **0 bytes**. FIX — (a) the comparison is
  `git rev-parse <rev>:<rel>` vs `git hash-object --no-filters`, two raw ids, no filtered view;
  (b) a live root that DECLARES any `filter.*.clean|smudge`, carries a non-empty
  `$GIT_DIR/info/attributes`, or attaches a filter attribute to a ratchet-critical path is refused
  outright — new code **GIT_FILTERS_PRESENT** (`ax_ratchet_filters_absent`, plus the same three
  probes inside the recency guard and inside `tree_fingerprint.py`, which now exits **5**);
  (c) the dirty accumulator's sensor no longer uses `git diff HEAD --binary` — the content of every
  modified path is read with `open()`, which no git configuration can interpose on.
  **The clean-tree constant is preserved BY CONSTRUCTION** (`0a815065ebf5…`, verified before and
  after): on a clean tree nothing is appended after the `\0diff\0` separator. That is load-bearing —
  the recency guard recomputes with the PREVIOUS RELEASE's copy of the helper, and a changed
  clean-tree constant would brick every honest push.

- Registered P2s (reviewer), closed in the same code: the previous-release extraction honoured
  `TMPDIR` (now pinned to `/tmp`, refused if the result is a symlink or sits inside a work tree)
  and mutated the blob with `.strip()` + an added newline (now read as BYTES, written verbatim, and
  required to hash back to the blob id it came from); several post-bootstrap sites still spelled
  the tool as the bare word `git`/`python3` despite the round-5 absolute-call-site claim (closed —
  it is part of P1-2's fix). STILL OPEN, registered not closed: **no parity guard prevents the
  eight duplicated bootstraps from drifting.** They are duplicated on purpose (the scrub cannot
  live in a file it must precede the sourcing of), which is exactly why drift is the risk.

- Evidence: `practices/scripts/ax-prove-hermetic-runtime.sh` (registered in run-all-guards) was
  extended from six cases to thirteen. New: **(G)** exported `set` and exported `[` injected
  through the environment → HERMETIC_PREFLIGHT_HOSTILE, with **(G′)** the same attack in a sandbox
  whose preflight is removed → exit 0, reproduces; **(H)** python3 → `/usr/bin/true` →
  HERMETIC_TOOL_UNAUTHENTIC, **(H′)** reproduces with the self-report removed; **(I)** a
  `PYTHONPATH` sitecustomize `os._exit(0)` → the gate still reaches its honest non-zero verdict;
  **(J)** the clean-filter byte mask → GIT_FILTERS_PRESENT / RATCHET_TOOLCHAIN_MODIFIED, **(J′)**
  reproduces (rc 0) with the raw comparison reverted to `git diff --quiet`; **(K)** the COMMITTED
  push-only bypass through the LIVE hook → RATCHET_PRIOR_RELEASE_GUARD_FAILED, **(K′)** reproduces
  (exit 0) with the prior-release re-run removed; **(L)** a POSITIVE control — an honest push
  through the FULL hook exits 0 and reports that the previous release's guard also passes, without
  which "(K) blocks" would be compatible with "it always blocks". Every neuter anchor is asserted
  to occur exactly once, so the harness goes stale LOUDLY rather than silently proving nothing.
  NOT fixture-covered, and why: these four attack surfaces are the ENVIRONMENT and the git CONFIG,
  neither of which a `fixtures/<guard>/fail_*` DIRECTORY can express — the proof harness is the
  registered mechanism, and it runs inside `run-all-guards.sh`.
- Harness reconciliations forced by the fix, recorded because each is a real weakening or a real
  cost: (i) the pure-keyword preflight now refuses exported functions BEFORE the round-4 namespace
  check can name them, so `ax-prove-helper-injection-blocked.sh` case (a) reports
  HERMETIC_PREFLIGHT_HOSTILE — a new case **(a2)** runs the same attack with the preflight stripped
  from the sandbox copies, so the round-4 refusal stays non-vacuous instead of becoming dead code
  nobody notices; (ii) `pre_push_decision_guard.sh` now expects the hook to consult the recency
  guard TWICE per shipping ref (prior release, then tree) and accepts either R25 block message;
  (iii) `pyyaml_preflight_coverage_guard.sh` simulates PyYAML absence with a PYTHONPATH shim, which
  `-E` ignores — measured, both ratcheting guards exited 0 with the shim in place having verified
  everything, i.e. the probe went INERT rather than lenient. `-E` is not negotiable (it is what
  refuses the sitecustomize injection), so those two are now PROBE_EXEMPT with the same static
  assertion `adversarial/run.sh` already gets, reported in the output rather than skipped;
  (iv) the toolchain list is enforced with an ABSENT-ON-BOTH-SIDES exemption — a tree that never
  carried part of the toolchain (a fork that took the hook but not the runner, the mutation-matrix
  sandboxes) is a legitimate shape, while absent on exactly ONE side blocks.
- One regression was introduced and caught by this sweep, recorded because it is the exact class
  this catalog exists to catch: a comment placed BETWEEN backslash continuations in
  `evidence_quote_spotcheck_guard.sh` swallowed the entire `STRICT=… LIVE_ROOT=… GIT_ANCHOR=…`
  environment prefix, so the guard ran its body without them and still printed a green summary.
  `bash -n` passed. Only the live sweep saw it.
- Commits: (this commit — PRD-final-4 wave, P1-seal round-6)

---

## TD-2026-07-30-(ratchet-threat-model) — what the ratchet defends against, and what it does not

- Date: 2026-07-30
- Status: accepted
- Trigger: cross-family reviewer, ROUND 7, P1-2. A wrapper placed on an absolute `PATH` entry
  prints the accepted banner for the gate's fixed self-identification challenge and exits 0 for
  everything else. MEASURED: it flipped `fail_audit_log_missing` from exit 1 to exit 0, including
  for the EXTRACTED PRIOR-RELEASE guard — the layer whose whole purpose is to be code the current
  tree did not write. The reviewer states the general result plainly, and it is correct: **no
  executable can authenticate itself against a malicious wrapper using a public fixed challenge.**
  The options are to declare PATH executables trusted, or to bind the toolchain to an external
  trust root (signed digests, a package boundary). This entry declares the first, and registers
  the second as future work rather than pretending the first is the second.

### The boundary, stated

**The ratchet defends against COMMITTED SUBVERSION OF THE CATALOG** — what a fork, a pull request
or an AI agent ships *in the tree*: a lowered floor, a removed anchor, a rewritten fingerprint
helper, a backdoored guard, a forged audit line, a filtered view of the bytes, a laundering
symlink, an evidence quote that no snapshot contains. It evaluates that on a developer machine
**whose interpreter and PATH are trusted**.

**It does NOT defend against an adversary who already controls the execution environment** —
`PATH` wrappers, shell startup files, the user account, the kernel. This is not a gap discovered
late; it is the DEFINITION OF THE ARTIFACT. Such an adversary does not need to defeat a check:
they can simply **not install the opt-in hooks** (`practices/scripts/install-hooks.sh`), or push
with the gate disabled entirely. A catalog gate is a **correctness ratchet, not a sandbox**. A
ratchet raises the floor of what can be shipped by accident, by drift, or by an agent optimising
for a green result; it does not contain a hostile operator, and any claim that it does would be
false in a way that matters more than the checks it would decorate.

Two consequences follow, and both are load-bearing:

1. **Anything that is measurable ON THE TREE is in scope and is ratcheted** — bytes, blobs,
   ancestry, schema, the previous release's copy of the gate. These are the checks that earn
   their keep, because "the tree" is exactly what a fork/PR/agent controls.
2. **Anything whose only defence would be authenticating the environment is OUT of scope and is
   made VISIBLE instead of guarded.** `verify-completion.sh` prints the absolute tool paths it
   resolved and writes them to `.ax-verify/toolpaths.json`, labelled *transparency, not
   authentication*. A reviewer can SEE a wrapper path. Nothing verifies it, and nothing claims to.

### What was deliberately NOT built (and why that is the honest answer)

- **A harder self-identification challenge** (nonce, hash of a random string, timing). A wrapper
  can forward the challenge to the real binary and answer correctly. Raising the cost of a wrapper
  is not a boundary; it is theatre with a maintenance bill.
- **Refusing non-root-owned tool paths.** Tempting and cheap-looking, but it would refuse every
  Homebrew/pyenv/nvm toolchain — i.e. this maintainer's own machine, where `python3` carrying
  PyYAML is not `/usr/bin/python3` — and the fallback would be to skip the python guards, which is
  a fail-OPEN dressed as hardening. Where a root-owned absolute path already satisfies the
  requirement it is preferred (`/usr/bin/env` is invoked by absolute path in every entry's
  privileged re-exec, and `/bin/bash` is what `$BASH` resolves to here); where it does not, the
  resolved path is RECORDED rather than swapped.
- **Binding tools to an external trust root.** Registered as `docs/BACKLOG.md` **P2-68**, gated on
  ax-template ever running in an untrusted-environment context (hosted CI for third-party forks,
  an agent sandbox with an attacker-writable PATH). Until then it would be cost without a threat.

### AMENDMENT (2026-07-31, reviewer ROUND 8 / P1-B) — the startup file is environment control, and `$-` does not attest startup history

The round-7 entry (TD-2026-07-30-(P1-privileged-startup)) is **not withdrawn, and it is not a
seal.** Round 8 showed four ways an adversary who controls the startup environment defeats the
privileged re-exec. Every one of them requires exactly that control, so all four are **out of
scope by the boundary above** — the same adversary can simply not install the opt-in hooks. What
must change is the CLAIM, not the code:

**Bash startup-file control (`BASH_ENV` / `ENV`, and by extension `~/.bashrc` for an interactive
parent, `shopt -s expand_aliases` payloads, and anything else bash executes before a script's
first line) is ENVIRONMENT CONTROL and is therefore OUT OF SCOPE.** The round-7 privileged-mode
work is **DEFENSE IN DEPTH against accidental and inherited contamination** — a `BASH_ENV` left
in a profile, a function exported by a tool wrapper, a stale `ENV` in a CI image — **not a seal
against a hostile startup file.** `$-` attests the shell's **CURRENT mode**; it attests nothing
about the shell's **startup history**. Inside bash, any detector that could be written is itself
reachable from the same startup file, so there is no in-script construct that attests what
already ran.

The reviewer's four reproductions, recorded verbatim as the evidence for that boundary:

> - `BASH_ENV` can define functions and THEN `builtin set -p`
> - `$BASH` is assignable after startup
> - an attacker `exec` that does `builtin exec /usr/bin/true` never returns so the second `case`
>   never fires
> - a sourced-lib probe with a late `set -p` + `declare(){ exit 0; }` gave rc 0 with no
>   `SOURCED_OK`

Read together: the payload runs first and then MANUFACTURES the state the re-exec block tests
for, so the block's own premise (`$- contains p` ⇒ nothing hostile ran) is false under startup
control. That is a correction to the round-7 prose, not a new attack surface — round 7 already
recorded the residual "`$BASH_ENV` whose first line is `exit 0`", and these four are the same
class taken further.

**The cheap strengthening was evaluated and DECLINED, with a reason rather than a shrug.**
> **SUPERSEDED (2026-07-30, round 9 — read the round-9 entry before trusting the paragraph
> below).** A cross-family reviewer refuted this rationale point by point and the refutation was
> measured and accepted: an ALLOWLISTED `env -i` does preserve all four `AX_RELEASE_ANCHOR_*`
> fields, `STRICT`/`LIVE_ROOT`/`GIT_ANCHOR` are established later and need not be lost, and
> `BASH_ENV`/`ENV` can be passed through VISIBLY so the loud refusal still fires without ever
> being sourced. The decision not to implement it stands on a DIFFERENT and narrower ground
> recorded in `TD-2026-07-30-(P1-representation-parity)` and tracked as BACKLOG P2-70 — not on
> the three points below, which are wrong. The text is kept unedited because this trail is
> append-only: a refuted argument that is silently deleted teaches nothing.
Re-executing through `/usr/bin/env -i` with a minimal allowlist would keep `BASH_ENV`/`ENV` out
of the child's environ entirely. It is refused because it BREAKS TWO THINGS THIS CATALOG
DEPENDS ON, and buys nothing against the class it would be sold as closing:

1. **It would silently drop `AX_RELEASE_ANCHOR_SHA`/`_KIND`/`_REF`/`_ROOT`.** Those are exported
   ONCE by the runner and re-read by every guard, and that single-resolution pin is precisely
   what closes the round-4 TOCTOU on the anchor ref (TD-2026-07-30-(P1-anchor-runtime), defect 2).
   With them stripped, each guard would fall back to re-resolving `origin/main` itself — i.e. the
   round-4 defect, restored by a "hardening" change. Guards also take env-borne configuration
   (`STRICT=` / `LIVE_ROOT=` / `GIT_ANCHOR=`), and a swallowed env prefix producing a green
   summary is the exact failure recorded two entries above in this file.
2. **It would convert a LOUD refusal into SILENT tolerance.** Today a contaminated environment is
   detected and named: the pure-keyword preflight refuses on `BASH_ENV`/`ENV` being set at all
   (`HERMETIC_PREFLIGHT_HOSTILE`). Stripping the variable before the preflight sees it would make
   the accidental case — the case actually in scope — pass quietly instead of telling the
   operator to fix their shell. For the hostile case it changes nothing, because a startup file
   that runs `exit 0`, or that fakes `$-`, never reaches the re-exec at all.

So nothing was kept from that suggestion, and nothing was faked. The honest summary is: **against
accidental/inherited contamination the environment is refused loudly, and that is what the
round-7 work buys. Against a hostile startup file there is no in-bash defence, and this entry
says so instead of decorating it.**

### Where the boundary bites, concretely

- `$BASH_ENV` whose FIRST line is `exit 0` ends the shell before any entry's first line exists
  (measured: exit 0, nothing printed). The round-7 privileged re-exec kills the *stealth* class —
  a payload that lets the gate appear to run and pass — but a payload that simply prevents the
  gate from running at all is environment control, and is out of scope by the paragraph above.
- A PATH wrapper on `git` or `python3`, per the reviewer's P1-2 reproduction.
- Anything a fork-receiver chooses not to install. The hooks are opt-in by design (see CLAUDE.md,
  "Fork받은 팀의 정책을 skill이 강제 ❌"); autonomy is a stated project value, and the asymmetry
  between ax-template's own HEAD (fully gated) and a fork's (opt-in) is design, not oversight.

---

## TD-2026-07-30-(P1-privileged-startup) — a startup file runs before line 1, so the fix cannot live in the script

- Date: 2026-07-30
- Status: accepted
- Trigger: cross-family reviewer, ROUND 7, P1-1 and P1-3.

### P1-1 — the pure-keyword preflight is defeated by a SELF-ERASING `$BASH_ENV`

Round 6 answered "what executes first INSIDE the file". The reviewer's round-7 finding is that
this is the wrong question: bash sources `$BASH_ENV` **before the script's first line**, so any
in-script sensor is structurally too late. RED, measured (bash 3.2.57(1)-release / Apple):

```
$BASH_ENV file:   builtin unset BASH_ENV ENV
                  function set(){ builtin exit 0; }
BASH_ENV=<payload> bash .githooks/pre-push …     → exit 0      (gate never ran)
```

The payload leaves **nothing to detect**: the function is not exported, so no `BASH_FUNC_*` entry
exists for the preflight's `/usr/bin/env` read; and `BASH_ENV` is unset from the export list, so
the preflight's `case "${BASH_ENV:-}${ENV:-}"` sees an empty string. `shopt -s expand_aliases`
plus an alias is the same class.

Round 6 also made a claim that was FALSE, and it is corrected in the code rather than argued away:
"the pure-keyword preflight is the literal first executable text of all 8 entries". It was not.
`.githooks/pre-push-lib.sh:27` and `practices/scripts/lib/release_anchor.sh:251` executed
`export GIT_NO_REPLACE_OBJECTS=1` first, and `export` is an ordinary command lookup — an exported
`export()` plus `alias exit=:` produced `SOURCE_RC=0` for both. Both exports now sit BELOW the
preflight, and the banner in all eight files states what is actually true.

**FIX — bash PRIVILEGED MODE at the invocation boundary.** In privileged mode bash does not
process `$BASH_ENV`/`$ENV` and does not import functions from the environment. As its first
executable text, each of the **SEVEN** EXECUTED entries detects privileged mode and, if absent,
`exec`s the SAME interpreter with `-p`, re-running itself with the original arguments.
(CORRECTION, reviewer ROUND 8 / P1-B: this said "six". The count on disk is seven —
`.githooks/pre-push`, `practices/scripts/verify-completion.sh`,
`practices/evals/run-all-guards.sh`, `practices/evals/completion_checklist_recency_guard.sh`,
`practices/evals/evidence_quote_spotcheck_guard.sh`,
`practices/evals/manifest_snapshot_integrity_guard.sh`,
`practices/evals/pre_push_decision_guard.sh` — verifiable as
`grep -rl 'AX_PRIV_REEXEC=1 "$BASH" -p'`. The two SOURCED libs assert instead, for the reason
below, which is where "six + two = eight files" came from.) **And read it with the ROUND-8
AMENDMENT in TD-2026-07-30-(ratchet-threat-model): this is defense in depth against accidental /
inherited contamination, NOT a seal against a hostile startup file — `$-` attests the current
mode, not startup history.**

```
case $- in
    *p*) ;;
    *) case "${AX_PRIV_REEXEC-}" in
           1) <abort: HERMETIC_PRIVILEGED_UNREACHABLE> ;;
           *) case "${BASH:-}" in
                  /*) exec /usr/bin/env AX_PRIV_REEXEC=1 "$BASH" -p "$0" "$@" ;;
                  *)  <abort> ;;
              esac ;;
       esac ;;
esac
case $- in *p*) ;; *) <abort: the re-exec returned ⇒ exec is shadowed> ;; esac
unset AX_PRIV_REEXEC
```

Every construct before the `exec` is unhijackable: `case` is a KEYWORD, `$-`/`${x-}`/`${x:?}` are
PARAMETER EXPANSIONS, and `/usr/bin/env` is invoked by ABSOLUTE PATH. `exec` itself IS shadowable,
which is why the SECOND `case` re-asserts privileged mode after it — a neutered `exec` falls
through to a non-zero abort instead of continuing unprivileged.

**THE LOOP MARKER IS DERIVED FROM PRIVILEGED MODE, NOT TRUSTED.** `AX_PRIV_REEXEC` means only "a
re-exec was already attempted". An attacker who PRESETS it does not skip the re-exec — that branch
ABORTS (measured, exit 1). It is `unset` the instant privileged mode holds, so it never reaches a
child entry and cannot become a one-shot disable for the ~100 guards an entry launches.

**THE TWO SOURCED LIBS ASSERT INSTEAD OF RE-EXECUTING.** A sourced file cannot `exec` without
replacing its caller's process, and it does not need to: privileged mode is a property of the
PROCESS, so a sourced file is covered by the entry that sourced it. Both libs therefore assert
`$-` contains `p` as their first executable text and refuse otherwise. Verified: the three
in-repo call sites that source them from a bare `bash -c` (two in
`ax-prove-hermetic-runtime.sh`) are now `bash -p -c`; `verify-completion.sh`,
`evidence_quote_spotcheck_guard.sh` and `manifest_snapshot_integrity_guard.sh` source them from
their own privileged process.

MEASURED, post-fix (all in throwaway sandboxes, `ax-prove-hermetic-runtime.sh` cases M/M'/M''/N/N'):

```
(M)   BASH_ENV self-erase, live gate            → exit 1 (honest verdict)  [pre-fix: 0]
(M')  same payload, privileged re-exec removed  → exit 0 (reproduces ⇒ attributable)
(M'') AX_PRIV_REEXEC=1 preset by the attacker   → exit 1 HERMETIC_PRIVILEGED_UNREACHABLE
(N)   exported export() + non-privileged caller sourcing release_anchor.sh → exit 127, no SOURCED_OK
(N')  same attack, round-6 shape restored       → exit 0 (reproduces)
```

`bash -p` was verified not to break the PyYAML sites: privileged mode changes startup-file and
function-import behaviour only, and the python guards run under `-I -S` with a scrubbed
`PYTHON*` environment either way — the full guard sweep is green with the re-exec in place. Git
invokes the pre-push hook by `execve` on the hook path, so the re-exec keeps stdin (the push spec),
the arguments (remote, url) and the cwd; the hook's own scenarios in
`push_evidence_tree_binding_guard.sh` / `midrun_tree_mutation_guard.sh` exercise exactly that path.

**HONEST RESIDUAL, and it is a boundary rather than a bug:** a `$BASH_ENV` whose first line is
`exit 0` ends the shell before this line exists (measured, exit 0, nothing printed). No in-script
construct can survive that. `#!/usr/bin/env -S bash -p` WOULD close it for `execve`-invoked
entries (verified working on this machine), and was DECLINED: `env -S` is not portable to
pre-2018 coreutils, a fork-receiver on an older system would get a hook that fails for an
unrelated reason, and — decisively — it changes nothing about the boundary, because the same
adversary can simply not install the hooks. Recorded in
TD-2026-07-30-(ratchet-threat-model).

### P1-3 — `filter.<driver>.process` bypasses GIT_FILTERS_PRESENT

All three filter declarations matched only `clean|smudge`
(`tree_fingerprint.py:102`, `release_anchor.sh:625`, the recency guard's check 12b0). Git performs
the SAME conversion through the long-running filter protocol, declared as `filter.<n>.process`.
RED, reproduced end to end: commit a file plus a `.gitattributes` entry, configure ONLY
`.process`, replace the file on disk with different bytes of the SAME LENGTH →
`git status --porcelain` is EMPTY → the path never enters `modified` → the fingerprint's raw
`open()` loop never reads it → the tree fingerprints as the CLEAN-TREE CONSTANT
`0a815065ebf5…`, so R25 tests the bytes on disk while the push ships the committed blob.

(The equal length matters and is now documented in the harness: git's `ie_modified()` returns
"modified" from the stat comparison alone when the recorded size differs and is non-zero, WITHOUT
consulting the filter. An attacker pads; a reproduction that did not would pass for the wrong
reason.)

**FIX (a) — refuse ANY filter driver declaration**, `^filter\.`, at all three sites. Enumerating
protocol keywords is a losing game: `.process` was the key round 6 did not think of, and the next
one is not something this catalog can be asked to predict.

**FIX (b) — stop deriving "unchanged" from a filtered `git status`.** For every tracked path git
reports as CLEAN, the bytes on disk are hashed AS A GIT BLOB in python — no git process in the
loop — and compared to the object id in the index (`GIT_RAW_INDEX_DIVERGENCE`, exit 6 from
`tree_fingerprint.py`, and check 12c in the recency guard, which is the PUSH-blocking surface).

**COVERAGE, stated exactly rather than implied.** WHOLE TREE: all of `git ls-files -s`, minus
(i) gitlinks (submodule commits: no bytes of ours on disk), (ii) paths git already reports dirty
(their raw bytes are hashed into the fingerprint anyway), and (iii) paths absent from disk
(sparse checkout / skip-worktree; absence carries no bytes). Whole-tree coverage was measured
before it was chosen: **5,745 tracked paths / 5.4 MB / ~0.2 s** on this catalog, so nothing is
scoped out for cost and there is no remainder to register. A mismatch also fires for eol
conversion (`core.autocrlf`, `core.eol`, a `text` attribute), which is not an attack but is
equally fatal to a byte claim; the message names both causes so an honest operator is not left
guessing.

MEASURED (`ax-prove-hermetic-runtime.sh` cases O/O', plus a standalone reproduction):

```
(O)  .process mask on an ordinary tracked file, live gate → exit 1, GIT_FILTERS_PRESENT
(O') same tamper, keyword subset restored + raw sweep removed → exit 0 (reproduces)
standalone: declaration check neutered, raw comparison alive  → exit 6, GIT_RAW_INDEX_DIVERGENCE
standalone: both neutered (= round-6 behaviour)               → exit 0, clean-tree constant
negative control: live tree, no filter, 5,745 paths           → exit 0, digest in 0.25 s
```

The target of (O) is an ORDINARY tracked file, not a toolchain path: round 6 already ran
`check-attr filter` over the nine ratchet-critical paths, so attacking one of those would have
been caught by a check that predates this round and the reproduction would have proved nothing.
Everything ELSE in the tree — which is what R25 actually tests — had no cover at all.

### Harness reconciliation forced by this round, recorded because it is a real weakening

`ax-prove-hermetic-runtime.sh` case (G') reproduces the round-6 preflight's attack by stripping
the preflight. With round 7 in place that reproduction became structurally impossible — privileged
mode alone blocks the exported-`set` import — so (G') silently became a tautology, measured as
`exit=2` where `0` was required. Fixed by making the "pre-round-6" neuter also strip the round-7
block: *pre-round-6 also means pre-round-7*, and a pre-fix world must be the world as it actually
was. Without that, a non-vacuity proof would have quietly turned into a no-op — the exact failure
mode this catalog exists to catch.

- P1-2 is NOT fixed here. See TD-2026-07-30-(ratchet-threat-model) for the boundary and
  `docs/BACKLOG.md` P2-68 for the external-trust-root work item.
- Commits: (this commit — PRD-final-4 wave, P1-seal round-7)

---

## TD-2026-07-31-(P1-worktree-representation) — a representation the sweep did not expect is not a `continue`

- Date: 2026-07-31
- Status: accepted
- Trigger: cross-family reviewer, ROUND 8, P1-A. Round 7 closed "a claim about BYTES must be made
  on RAW BYTES" by hashing every tracked path git calls clean directly, in python, with no git
  process in the loop. But the loop only handled the paths whose ON-DISK SHAPE matched the index,
  and spelled everything else `continue` — in **both** implementations
  (`practices/scripts/lib/tree_fingerprint.py:197,209` and the recency guard's 12c sweep at
  `:1113,1125`). Two of those skipped states hide bytes that a push ships, and reaching them
  needs **no environment control at all** — one `git update-index` does it, squarely inside the
  declared threat model.

### The two reproductions (measured here, in throwaway repos)

```
(1) SYMLINK SWAP
    HEAD carries a malicious practices/verification-checklist.yaml
    git update-index --assume-unchanged practices/verification-checklist.yaml
    rm practices/verification-checklist.yaml
    ln -s <benign file OUTSIDE the repo> practices/verification-checklist.yaml
    → git status --porcelain : EMPTY
    → git ls-files -s        : 100644 …  (the index still names the malicious blob)
    → what R25 reads         : "steps: [benign]"
    → what the push ships    : "steps: [MALICIOUS]"

(2) SKIP-WORKTREE ABSENCE
    git update-index --skip-worktree <malicious tracked source>; rm it
    → git status --porcelain : EMPTY, the build simply omits the file

PRE-FIX, three trees, ONE digest — both_bypasses = true:
    clean tree              → 0a815065ebf5…  exit 0
    (1) symlink swap        → 0a815065ebf5…  exit 0
    (2) skip-worktree gone  → 0a815065ebf5…  exit 0
```

### Fix, applied SYMMETRICALLY to both implementations

For any live/push-eligible run an unexpected worktree representation now BLOCKS. New codes:

| code | fires when | fingerprint exit |
|---|---|---|
| `GIT_INDEX_FLAGS_SET` | `git ls-files -v` reports a lowercase tag (assume-unchanged) or `S` (skip-worktree) | 9 |
| `GIT_WORKTREE_TYPE_MISMATCH` | index mode 100644/100755 but the worktree is a SYMLINK — **and the mirror**, index mode 120000 but the worktree is a regular file/directory; also a gitlink that is a symlink | 7 |
| `GIT_TRACKED_PATH_ABSENT` | a tracked path git calls clean is missing or unreadable (`OSError`); the message names assume-unchanged / skip-worktree / sparse checkout as the legitimate-looking causes | 8 |
| `GIT_GITLINK_DIVERGENCE` | an INITIALIZED submodule whose HEAD is not the commit the superproject records | 10 |

The BITS are read directly rather than inferred one symptom at a time: both reproductions begin
with `git update-index`, and `ls-files -v` reports the bits. Measured on this catalog: **5,745
tracked paths, ZERO non-`H` tags**, so the refusal costs an honest tree nothing. Verified on a
deliberately dirty tree (one modified, one deleted, one staged, one untracked path) that every
tag stays `H` — this is the two bits, not a proxy for dirtiness.

**Gitlinks are BOUND, not exempt.** Round 7 exempted them with "no bytes of ours on disk", which
is true of the superproject's blobs and false of what a run tests: an initialized submodule
sitting at a different commit means R25 tested one submodule while the push ships another, and
`submodule.<n>.ignore=all` keeps `git status` silent about it. An **UNINITIALIZED** gitlink is
deliberately NOT blocked — all three in this catalog are empty post-clone fixture directories and
nothing was tested from them; blocking would refuse every fresh clone to close nothing. The
remainder (dirt inside an initialized submodule's own work tree) is `docs/BACKLOG.md` **P3-119**.

```
POST-FIX
  clean control (scratch)            exit 0  0a815065ebf5…      (constant preserved)
  (1) symlink swap                   exit 9  GIT_INDEX_FLAGS_SET
  (2) skip-worktree absence          exit 9  GIT_INDEX_FLAGS_SET
  dirty control (M/D/staged/??)      exit 0  3204bed0bd39…      (no false block)
LAYER INDEPENDENCE (bits refusal neutered — a sparse checkout SETS the bit, so without this
the representation backstop would be dead code behind it):
  (1) symlink swap                   exit 7  GIT_WORKTREE_TYPE_MISMATCH
  (1b) mirror: index symlink → file  exit 7  GIT_WORKTREE_TYPE_MISMATCH
  (2) skip-worktree absence          exit 8  GIT_TRACKED_PATH_ABSENT
  clean control                      exit 0  0a815065ebf5…
NEGATIVE CONTROL on the LIVE tree (5,745 paths):
  pre-fix (HEAD copy)  b622ac117ad6…  0.249s total
  post-fix             b622ac117ad6…  0.222s total   ← identical digest, same cost
GITLINK
  uninitialized                      exit 0  (not blocked, by design)
  initialized at a different commit  exit 10 GIT_GITLINK_DIVERGENCE
```

### Why both copies had to change, and why the guard's own sweep is load-bearing

The recency guard recomputes the fingerprint with the **PREVIOUS RELEASE'S** copy of the helper.
Until this fix reaches `origin/main`, that copy is the pre-round-8 one and answers the clean-tree
constant for both attacks — so the recompute cannot be the layer that catches them. The guard's
own 12c sweep is. That is the general shape: the writer and the verifier must not disagree about
what a tree IS, so the change lands in both files in the same commit.

### Falsification

`practices/scripts/ax-prove-hermetic-runtime.sh` gains cases **(P)** symlink swap, **(Q)**
skip-worktree deletion, **(P′)/(Q′)** the same attacks in a COMMITTED pre-round-8 sandbox (both
must land, exit 0, or the harness is stale), **(R1)/(R2)** the bits-only neuter proving the
representation backstop refuses on its own, and **(S)** an over-correction control asserting an
uninitialized gitlink still PASSES. Each attack asserts `git status --porcelain` is EMPTY before
the gate runs — if a future git reports the swap, the scenario says so instead of passing quietly.

### Not fixture-covered, and why (stated rather than glossed)

There is no committed `fail_*` fixture for these codes. The recency guard's fixture roots are
**by construction not git work trees** — they declare `.ax-verify/expected_head.txt`, and the
whole 12b0/12b/12c block is gated on `live_git_root and not expected_head_file.is_file()`. The
attack IS a disagreement between the git **INDEX** and the worktree, so a directory with no index
has nothing to disagree with: a fixture able to express it would have to BE a git repository,
which the fixture contract excludes. The evidence is therefore the live falsification cases above
(sandboxed, with pre-fix twins), the same posture already used for the other live-root-bound codes
(`MONOTONIC_FLOOR_REGRESSION`, `ANCHOR_*`).

- P1-B is NOT a code change. See the ROUND-8 AMENDMENT in
  TD-2026-07-30-(ratchet-threat-model): bash startup-file control is environment control and
  therefore out of scope; the round-7 privileged-mode work is defense in depth against
  accidental/inherited contamination, not a seal; `$-` attests current mode, not startup history.
- Registered from this round: `docs/BACKLOG.md` **P2-69** (toolpaths.json interpolation +
  symlink-following output) and **P3-119** (submodule-internal residue).
- Commits: (this commit — PRD-final-4 wave, P1-seal round-8)

---

## TD-2026-07-30-(P1-representation-parity) — separating the shapes is not comparing them: the bits a digest never carried

- Date: 2026-07-31
- Status: accepted
- Trigger: cross-family reviewer, ROUND 9, P1-1 and P1-2, plus five corrections (a)-(e). Round 8
  stopped `continue`ing past worktree representations it did not expect — it separated
  regular / symlink / gitlink / absent and refused the ones it could not account for. Then it
  compared **only blob BYTES**. A tracked path's representation carries more than its bytes, and
  every fact it carries that no digest here holds is a fact the index and the working tree can
  disagree about while `git status --porcelain` stays EMPTY. The reviewer found two, and both are
  reachable with ordinary git commands — no environment control, squarely inside the declared
  threat model (TD-2026-07-30-(ratchet-threat-model)).

### P1-1 — the EXECUTABLE BIT was accepted in both directions

Both sweeps read the index entry (`git ls-files -s`), split regular from symlink, and then compared
the blob id. git's regular modes are `100644` and `100755`; **nothing compared that bit to the
filesystem.** `core.fileMode=false` tells git to stop *reporting* a mode difference — it does not
change the *record* — so the divergence is invisible to `git status` and to a digest built from it.

```
REPRODUCTION (measured here, throwaway repo; direction A)
    git config core.fileMode false
    chmod -x gradlew; git update-index --chmod=-x gradlew; git commit      # index := 100644
    chmod +x gradlew                                                        # disk  := executable
    → git status --porcelain : EMPTY
    → git ls-files -s        : 100644 …
    → stat                   : -rwxr-xr-x
    PRE-FIX  tree_fingerprint → 0a815065ebf5…  exit 0   (THE CLEAN-TREE CONSTANT)
    POST-FIX tree_fingerprint → GIT_EXEC_BIT_DIVERGENCE exit 11

direction B (the mirror), same repo
    git update-index --chmod=+x gradlew; git commit; chmod -x gradlew
    → git status --porcelain : EMPTY
    PRE-FIX  → 0a815065ebf5…  exit 0
    POST-FIX → GIT_EXEC_BIT_DIVERGENCE exit 11
```

Why it matters concretely: R25 invokes `./gradlew` **directly, 118 times**. Under direction A every
one of them runs the locally-executable file, R25 goes green, and the push records `100644` — a
fresh checkout of the certified commit cannot execute it at all. Direction B is the mirror and is
the *likelier accident*: a script fails locally, an operator fixes it with a `chmod` the index never
learns about, and the mode nobody tested is what ships.

**Fix, symmetric in both implementations.** The index mode is read from `git ls-files -s` — the
RECORD, not git's filtered view — and compared against `os.lstat()` on the regular path, as a
CLASS (any-x ⇔ `100755`, no-x ⇔ `100644`; `0o700` vs `0o755` is not a divergence). Both directions
block with `GIT_EXEC_BIT_DIVERGENCE` (fingerprint exit 11). The check runs BEFORE the
`path in dirty` skip, because a path that is dirty for content can still be lying about its mode
and the digest carries the content, never the bit. Measured on this catalog: 5,745 tracked paths,
**zero** divergences — the refusal costs an honest tree nothing.

### P1-2 — a NONEMPTY "uninitialized" gitlink was accepted

Round 8 bound INITIALIZED submodules (recorded sha must equal the submodule's HEAD) and
deliberately exempted uninitialized ones, on the reasoning "nothing on disk, nothing was tested".
The implementation tested the wrong proposition: it returned success on the **absence of
`<gitlink>/.git`** and never required the directory to be EMPTY.

```
REPRODUCTION (measured here)
    git update-index --add --cacheinfo 160000,<sha>,vendor/sub ; git commit
    mkdir -p vendor/sub ; printf 'echo owned\n' > vendor/sub/check.sh     # NO .git anywhere
    → git status --porcelain : EMPTY   (git does not descend into a gitlink)
    PRE-FIX  tree_fingerprint → 0a815065ebf5…  exit 0
    POST-FIX tree_fingerprint → GIT_GITLINK_UNINITIALIZED_POPULATED exit 12
```

A committed mandatory step that runs `bash vendor/sub/check.sh` therefore executes a file that the
push does not ship: a gitlink commits a **sha**, and a fresh clone of that commit gets an EMPTY
directory. The round-8 reasoning is preserved exactly where it was right — the fresh-clone shape.
The corrected rule: an uninitialized gitlink is acceptable **only** when its worktree path is ABSENT
or an ACTUALLY-EMPTY directory. All three gitlinks in this catalog are empty post-clone fixture
directories and still pass (prover control (S), re-verified). Distinct from `docs/BACKLOG.md`
**P3-119**, which is dirt inside an INITIALIZED submodule's own work tree.

### New blocking codes

| code | fires when | fingerprint exit |
|---|---|---|
| `GIT_EXEC_BIT_DIVERGENCE` | index `100644`/`100755` disagrees with the filesystem x-bit CLASS, either direction | 11 |
| `GIT_GITLINK_UNINITIALIZED_POPULATED` | a gitlink with no gitdir whose worktree path is a nonempty directory (or not a directory at all) | 12 |
| `GIT_CASEFOLD_ALIAS` | two index entries differing only in case that lstat to the same `(st_dev, st_ino)` | 13 |

Both implementations carry all three: `practices/scripts/lib/tree_fingerprint.py` and the recency
guard's 12c sweep. The clean-tree constant `0a815065…` is preserved by construction (nothing new is
appended to the hash), which matters because the recency guard recomputes with the PREVIOUS
RELEASE'S copy of the helper.

### The five corrections the reviewer also required

**(a) OVERCLAIMING PROSE.** `verify-completion.sh:514` still read "the AUTHENTICATED interpreter"
and its live failure output said "the authenticated interpreter $AX_PY_BIN" — while the same file's
banner correctly says *transparency, not authentication*. Both are corrected to "resolved", with a
comment that names what actually happened (absolute path + regular + executable + a fixed PUBLIC
smoke test that a wrapper forwards) and points at TD-2026-07-30-(ratchet-threat-model). A
class-wide re-sweep (every tracked `.sh`/`.py`/`.md`/`.yaml` under `practices/`, `docs/`,
`.githooks/`; any line matching authenticat|verified|identity within ±1 line of a
tool/interpreter word, excluding negative statements) found **two** further instances and **one**
was ours: the prover's neuter key `identity`, renamed to `smoketest`. The other survivors are
either explicit negations ("does NOT prove the interpreter is authenticated"), byte-equality
against a git-recorded blob (which IS an authentication of *content*, not of a PATH executable), or
upstream snapshot text.

**(b) THE `env -i` RATIONALE WAS OVERSTATED, and the reviewer is right on every point.** The
round-8 entry declined the allowlisted `env -i` re-exec for two reasons, and MEASUREMENT shows
both were wrong as written:
- *"It would silently drop `AX_RELEASE_ANCHOR_*`"* — true of a BARE `env -i`, false of an
  **allowlist**, which is what was proposed. Verified: every consumer reads them as
  `[ -n "${AX_RELEASE_ANCHOR_SHA:-}" ]`, so `VAR="${VAR:-}"` passes the value when set and an
  empty string when unset, and empty is indistinguishable from unset at every consuming site.
- *"Guards also take env-borne configuration (`STRICT=` / `LIVE_ROOT=` / `GIT_ANCHOR=`)"* —
  **factually wrong.** Measured: those three are set INSIDE the guards
  (`evidence_quote_spotcheck_guard.sh:585,614,740`, `manifest_snapshot_integrity_guard.sh:487,619`)
  as a prefix on an internal `python3` call, LONG AFTER the re-exec. Nothing inherits them.
- *"It would convert a LOUD refusal into SILENT tolerance"* — also wrong as written, because
  `BASH_ENV`/`ENV` can be passed through VISIBLY (`BASH_ENV="${BASH_ENV:-}"`), and the preflight
  tests `case "${BASH_ENV:-}${ENV:-}" in ?*)`, so a set value still fires
  `HERMETIC_PREFLIGHT_HOSTILE` while an unset one still passes.

So the stated rationale is WITHDRAWN. What replaces it is a measurement and a scoped decision, not
a shrug:
- Measured: the recency guard runs to its honest verdict under
  `/usr/bin/env -i PATH=… /bin/bash -p <guard>` (exit 0, `recency_pass`, same fingerprint), and the
  **entire** guard suite does too. Measured BACK TO BACK at this commit, on a clean tree, with
  nothing else editing the repository (an earlier pair of runs was taken while this lane was still
  editing the files under test, and is discarded as confounded):

  ```
  bash practices/evals/run-all-guards.sh                             → Total: 192 passed, 0 failed
  /usr/bin/env -i PATH=… HOME=… TMPDIR=… /bin/bash -p  (same script) → Total: 192 passed, 0 failed
  identical FAIL set (empty)
  ```

  That includes `vacuity_class_proof_guard.sh`, which shells out to `./gradlew pitest` (both
  mutants KILLED under the stripped environment).
  The allowlist is therefore **small and tractable for the guard surface**: `PATH`, `HOME`,
  `TMPDIR`, `JAVA_HOME`, `BASH_ENV`, `ENV`, and the `AX_*` family. The only non-`AX_` variables
  read anywhere on that surface are `PATH`, `TMPDIR` (always with a `:-/tmp` default) and
  `JAVA_HOME` (read at exactly one site, `verify-completion.sh:1166`).
- NOT IMPLEMENTED IN THIS ROUND, with the reason: `verify-completion.sh` is the one entry that
  execs FOREIGN TOOLCHAINS — gradle **and** npm — whose environment surface is not enumerable from
  this repository (`GRADLE_*`, `JDK_*`, `NODE_*`, `npm_config_*`, and whatever a fork-receiver's
  wrapper adds), and the frontend step cannot be exercised from this lane (the main loop owns R25,
  and npm is out of bounds here). Shipping the construct at five of six entries and not the sixth
  would be an asymmetry that reads as an oversight; shipping it at all six without ever running the
  npm step would be an untested change to the gate that decides whether anything may ship.
  Registered as `docs/BACKLOG.md` **P2-70** with the allowlist and the measurement above, so the
  next person implements a measured design rather than re-deriving it.
  **The claim now matches what is done: the environment is refused LOUDLY when contaminated
  (`HERMETIC_PREFLIGHT_HOSTILE`, `HERMETIC_ENV_HOSTILE`), and it is NOT stripped.**

**(c) THE TWO MISSING POSITIVE REGRESSIONS.** Round 8 implemented two branches and never drove
them, so both could have rotted into dead code behind a green harness. Added to
`ax-prove-hermetic-runtime.sh` as explicit sandbox cases: **(X)** the MIRROR of (P) — an
index-SYMLINK path that is a REGULAR FILE on disk, run with ONLY the index-bit refusal neutered so
the code under test is the representation backstop → `GIT_WORKTREE_TYPE_MISMATCH`; and **(Y)** an
INITIALIZED submodule moved off the recorded commit → `GIT_GITLINK_DIVERGENCE`.

**(d) THE HARNESS SILENTLY SKIPPED SETUP FAILURES.** `r8_apply … || return 0` turned a case whose
attack could not even be applied into a SILENT PASS — the scenario never ran, nothing was measured,
and the harness still printed its green summary. Setup failure is now LOUD: `exit 2` (harness
error) for an inapplicable attack, `return 1` only for the premise-broken path that already called
`violation()`. Every other `ax-prove-*.sh` was checked for the same shape;
`ax-prove-gate-blocks-agent.sh:38` and `ax-prove-evidence-gate-blocks-agent.sh:43` are `grep -c`
arithmetic, already commented, under `set -euo pipefail` — NOT this class. **Correction
(2026-07-30, independent verification lane): those two are not the ONLY other `|| true` sites.**
Four more exist — `ax-prove-evidence-gate-blocks-agent.sh:86,116` and
`ax-prove-gate-blocks-agent.sh:82,117` — trailing `|| true` on ledger-log calls that run AFTER
their assertions, so they also are not the silent-skip class. The claim that was wrong is the
word "only"; the classification of each site stands. Recorded rather than quietly edited: a
census sentence that overstates its own completeness is the same defect this catalog keeps
finding in its guards.

**(e) — CORRECTED IN ROUND 10, AND THE CORRECTION IS THAT THIS CLAIM WAS OVERCLAIMED.** What (e)
shipped is a **LEAF-ONLY** check: both implementations grouped **complete** folded paths, so two
entries whose LEAF names differ never landed in the same group even when a shared DIRECTORY
component was a casefold alias on APFS. The paragraph below says "two index entries differing only
in case", which reads as the whole class and is not — the reviewer's round-10 topology needs no
leaf collision at all:

```
    Index: A/check.sh   (contains: cat A/helper)
    Index: a/helper     (contains: PASS)
    Disk on APFS: A and a are the SAME directory inode
```

`A/check.sh` and `a/helper` have different full folded keys, so neither implementation detected the
shared directory inode; `cat A/helper` succeeds locally so R25 goes green, and the PUSHED tree
contains only `a/helper`, so on a case-sensitive receiver `A/helper` is absent. Two further
statements below are corrected by measurement in round 10: the harness case (W) did **not** carry
divergent blobs at gate time (`write_audit`'s `git add -A` healed them, unasserted), and the
"the ordinary form is already refused by the clean-tree precondition" backstop that made (e)
"defense in depth" **does not exist for the directory form** — that one was silently open. See
TD-2026-07-31-(P1-casefold-prefix).

**(e) APFS CASEFOLD-ALIAS — IMPLEMENTED, with an honest scope.** Two index entries differing only
in case are ONE file on APFS/NTFS: the filesystem can hold one, the push ships two blobs, and every
read answers about the single file for both entries. `GIT_CASEFOLD_ALIAS` fires when a casefold
group's members lstat to the same `(st_dev, st_ino)` — a MEASUREMENT, so a case-sensitive
fork-receiver is unaffected. Measured on this catalog: zero casefold collisions, zero inode aliases.
**What it is NOT:** with divergent blobs the alias ALSO shows up as a modification
(measured: `git status` printed ` M alias.txt`), so the gate's clean-tree precondition already
refuses the ordinary form, and with identical blobs there is no lie to catch. This refusal is
therefore **defense in depth that names the fault instead of the symptom** — one file, two blobs,
one of them never on disk to be verified — and it fires in check 12c before the clean-tree check at
12a. It is not the closure of a demonstrated silent hole, and it is not sold as one.

### Registered rather than done

- `docs/BACKLOG.md` **P2-70** — the allowlisted `env -i` re-exec, with the measured allowlist.
- `docs/BACKLOG.md` **P3-120** — UNICODE NORMALIZATION aliasing. APFS also folds NFC/NFD, so `é`
  composed and decomposed are one file; the casefold key is `bytes.lower()`, which does not group
  them, so that alias is still accepted. Same class as (e), different fold.
- `docs/BACKLOG.md` **P3-121** — `run-all-guards.sh:1458` emits four `command substitution: syntax
  error` lines on stderr because the ledger prose contains backticks inside a double-quoted string.
  Cosmetic, present identically before this round (verified in both suite runs), noticed here.

- Commits: (this commit — PRD-final-4 wave, P1-seal round-9)

---

## TD-2026-07-31-(P1-casefold-prefix) — an alias is a property of a PATH, not of its last component

- Date: 2026-07-31
- Status: accepted
- Trigger: cross-family reviewer, ROUND 10, one P1 plus register-only P2/P3 items. Round 10
  accepted every round-9 disposition except (e), and confirmed by running the individual neuters
  itself that the exec-bit and populated-gitlink seals are live.

### P1 — the casefold check was LEAF-ONLY, and round 9's (e) said otherwise

Both implementations grouped **complete** folded paths — `tree_fingerprint.py:443` and
`completion_checklist_recency_guard.sh:1291` at d567c37 — so two entries whose LEAF names differ
never landed in the same group, however their DIRECTORY components were spelled.

```
REPRODUCTION (the reviewer's, replayed here against the d567c37 implementations, APFS)
    index: A/check.sh   (committed content: `cat A/helper`)
    index: a/helper     (committed content: PASS)
    disk : A and a are ONE directory — stat: dev 16777229, ino 34423509 for both spellings
    → git status --porcelain : EMPTY
    → cat A/helper           : PASS        (so every gate that reads or runs it goes green)
    → git ls-tree -r HEAD    : A/check.sh, a/helper      ← what the push actually ships
    PRE-FIX  (git show d567c37:…/tree_fingerprint.py) → 0a815065ebf5…  exit 0  (CLEAN-TREE CONSTANT)
    PRE-FIX  recency 12c                              → violation set EMPTY (prover twin (Z2):
                                                         recency_pass, tree "clean", exit 0)
    POST-FIX tree_fingerprint                         → GIT_CASEFOLD_DIR_ALIAS exit 14
    POST-FIX recency 12c                              → GIT_CASEFOLD_DIR_ALIAS, exit 1
```

On a case-sensitive receiver `A/helper` does not exist, so the committed check is broken on
arrival — and unlike the leaf form, **nothing else refused it**: the tree is genuinely clean, so
the clean-tree precondition that made (e) "defense in depth" has no purchase here.

**Fix, symmetric in both implementations, and it is the reviewer's prescription.** Every tracked
entry contributes **every prefix of its path** (each directory component, folded, plus the full
path) to a map `folded prefix → {(st_dev, st_ino): {spellings}}`; a group in which one inode is
reached by two DISTINCT spellings is refused. `160000` entries are included — round 9 registered
the map after the gitlink `continue`, which the reviewer registered separately and which closes
here because it is the same walk. Leaf groups (every spelling is a full tracked path) keep
`GIT_CASEFOLD_ALIAS`; groups involving a directory component are the new
`GIT_CASEFOLD_DIR_ALIAS` (fingerprint exit **14**) — a different remedy (`git mv` a directory) and
a distinguishable RED demo.

**It stays a MEASUREMENT, so case-sensitive forks are unaffected.** Verified on a real
case-sensitive APFS image (`hdiutil create -fs "Case-sensitive APFS"`), not by simulation: a tree
holding `A/`+`a/` as distinct directories (inodes 52 and 53) and `Alias.txt`+`alias.txt` as
distinct files (57 and 58) fingerprints to the clean-tree constant at exit 0, and the live push
gate passes it (prover (Z5), real arm via `AX_PROVE_CS_DIR`).

| control | measurement |
|---|---|
| live tree, 5,745 entries (2 symlinks, 3 gitlinks) | exit 0, digest unchanged at `0a815065…` |
| distinct `A/`+`a/` on a case-sensitive volume | exit 0 — no false positive |
| leaf alias (round 9's `Alias.txt` ≡ `alias.txt`) | still blocks, `GIT_CASEFOLD_ALIAS` — no regression |
| individual neuter of the new refusal | attack lands again (exit 0) — non-vacuous |

**Cost.** One `lstat` per DISTINCT prefix, cached (`practices/scripts/lib/tree_fingerprint.py`
`_lstat_cached`), and the full path is itself the last prefix, so the exec-bit check reads the same
cached stat instead of taking a second one: +~1.1k directory stats, −5,745 duplicate stats. Measured
on the live tree, five runs each: **0.23–0.26 s before, 0.25–0.28 s after** — +~0.02 s, no material
regression.

### P2 items actioned in the harness

- **Per-implementation attribution.** The shipped round-9 neuters disabled the 12c sweep and the
  fingerprint helper TOGETHER, so a twin that landed again proved the PAIR load-bearing and neither
  member. `round9_neuter` now takes `guard` / `fp` (and `r10guard` / `r10fp`) and every alias and
  representation class runs both: sweep-only → the helper still refuses, surfacing as
  `AUDIT_FINGERPRINT_UNVERIFIABLE` because the recompute runs the PRIOR RELEASE'S copy ((Z3) `release
  39e7fb10f801`, (W3) `release ab8df1640cad`, plus (T3)/(V3)); helper-only → the sweep still refuses
  on its own code ((Z4)/(W4)/(T4)/(V4)).
- **The (W2) twin's premise.** It had no post-neuter assertion, and TWO `git add -A` calls could
  heal the alias (the neuter's, and — the one that actually did it — `write_audit`'s, which re-reads
  every path from disk so the two aliased entries collapse onto the one file's bytes). Both are now
  narrowed to the paths they mean to stage, and `r9_premise` asserts the premise at gate time.
  **What that exposed, and it is a correction to round 9:** with the healing removed and divergent
  blobs restored, the leaf case never reaches the casefold code at all — ` M alias.txt` makes the
  tree dirty and the gate refuses with `AUDIT_TREE_DIRTY_NOW`. The shape that ISOLATES the leaf
  refusal is the equal-blob one, which is what round 9's case had in fact been running unknowingly.
  (W) now asserts exactly that: two entries, one inode, one blob, status empty.
- **Dirty-representation handling** narrows the fingerprint's usefulness for a dirty resume state
  — registered with the tradeoff stated (`docs/BACKLOG.md` **P2-71**), not silently changed.

### Registered rather than done

- `docs/BACKLOG.md` **P2-71** — dirty-representation handling vs. dirty-resume usefulness.
- `docs/BACKLOG.md` **P3-122** — `stage > 0` duplicate-path casefold misdiagnosis. **Closed as it
  fell out:** the spellings are now a SET, so the same path listed once per merge stage collapses
  and cannot produce a `path ≡ path` self-report. Kept as a row for the record with its evidence.
- `docs/BACKLOG.md` **P3-123** — `.DS_Store`-only gitlink directory usability refusal (a Finder
  visit to an empty gitlink now blocks R25 with `GIT_GITLINK_UNINITIALIZED_POPULATED`).
- `docs/BACKLOG.md` **P3-120** (round 9) still stands: Unicode NFC/NFD folding is not covered; the
  prefix walk folds case only.
- The prover's PASS banner claimed "every round-5/6/7/8 addition has a neutered twin" while round-9
  twins were already shipping. Corrected to 5/6/7/8/9/10 and to say what the new twins prove.

- Commits: (this commit — PRD-final-4 wave, P1-seal round-10)

---

## TD-2026-08-01-(P1-unicode-prefix-fold) — the prefix was right; the FOLD was ASCII

- Date: 2026-08-01
- Status: accepted
- Trigger: cross-family reviewer, ROUND 11, one P1 plus register-only P3 items. Round 11 confirmed
  the round-10 seal dead against its own neuter matrix in BOTH directions and found no other
  P0/P1 — the prefix map, symlinked directory components, gitlink prefixes, hardlinked directories
  (APFS does not support them) and the walk ordering all held. The one P1 is the row already
  registered as `docs/BACKLOG.md` **P3-120**, ESCALATED.

### P1 — `bytes.lower()` is neither Unicode-aware nor normalization-aware

Rounds 9 and 10 keyed the alias map with `bytes.lower()` — `tree_fingerprint.py:421` and
`completion_checklist_recency_guard.sh:1285` at beee364. That function lowercases `A-Z` and nothing
else, and it has no notion of canonical equivalence. So two spellings that the filesystem serves
from ONE inode were placed in DIFFERENT groups and their shared inode was never compared.

```
REPRODUCTION (the reviewer's, replayed here against the beee364 implementations, APFS)
    index: é/check.sh    (NFC   c3a9,   committed content: cat "é/helper")
    index: e◌́/helper      (NFD  65cc81, committed content: PASS)
    disk:  é and e◌́ are the SAME directory inode (16777229, 34664959)

    git status --porcelain -uall   → EMPTY
    bash é/check.sh                → PASS          (so every gate goes green)
    tree_fingerprint.py .          → exit 0, 0a815065…  (the clean-tree constant)
    the 12c sweep                  → all nine violation buckets EMPTY
```

The pushed tree records only `e◌́/helper`. A normalization-SENSITIVE receiver has no `é/helper`, so
the committed check is broken on arrival and nothing measured it. **`É/` (c389) vs `é/` (c3a9) is
the same defect through the same hole** — non-ASCII case, which `bytes.lower()` also does not touch.

**No environment control is involved.** Everything above is committed content. Apple Git 2.50.1
precomposes an NFD path at every text-level entry point measured (`git add`, `update-index
--cacheinfo`, `update-index --index-info`, `mktree`) *even with `core.precomposeunicode=false`*, so
the state cannot be typed into existence on macOS — but it is what a repository created on a
normalization-sensitive filesystem hands a macOS clone. **Verified:** `git clone` of such a repo
with default config yields an index holding `65cc81`, `git status` EMPTY, and `bash é/check.sh` →
PASS. That is why the prover plants the tree OBJECT rather than using porcelain: it reproduces an
arriving state, not an unreachable one.

**THE FIX — canonical caseless keys in both implementations, `NFC(casefold(NFD(s)))`.**
`_fold_path_key` (helper) and `_ax_fold_path_key` (12c sweep) are the same function.

- **inner NFD** — UAX #21 §1.3 defines default caseless matching over NFD forms. Load-bearing, not
  ceremony: measured, `casefold()` with no normalization separates the NORMALIZATION pair.
  **CORRECTED 2026-08-01 (round 12)**: this bullet said "separates **every pair** above", which is
  FALSE — unnormalized `casefold()` already equates `É`/`é` and `ſ`/`s`; only the normalization
  pair needs the NFD. The round-11-follow-up correction fixed the identical sentence in
  `tree_fingerprint.py` and left this copy standing, which is the same partial-correction defect
  it was itself correcting.
- **outer normalization** — **NOT load-bearing. CORRECTED 2026-08-01 (round 12).** This bullet
  claimed it was, on the grounds that `U+1E9B U+0323` vs `U+1E69` "fold EQUAL with an outer
  normalization and UNEQUAL without one". Measured, that is false: the comparison was against
  `casefold()` with NO normalization at all, and given the inner NFD, `casefold(NFD(s))` alone
  already equates that pair (and every other pair cited here). The prior round corrected only the
  `.py` docstring and left this copy — the exact defect named in the bullet above. The outer step
  is retained as redundant-but-harmless key canonicalization (this value is only ever compared for
  EQUALITY, and a canonical spelling is cheaper to reason about), **not** as a correctness lever.
  The round-9 row's proposed `normalize('NFC', …).lower()` is still **not** adopted, but for one
  reason and not two: measured, it DOES equate `É`≡`é` (Python's `str.lower()` is Unicode-aware —
  the round-9/10 bug was `bytes.lower()`), and it fails on `ſ`≡`s` and on `U+1E9B U+0323`≡`U+1E69`
  because `lower()` is a round-trip operation and caseless MATCHING needs full case folding.
- **outer form = NFC, not the standard's NFD** — the value is only ever compared for EQUALITY, and
  two strings are canonically equivalent iff their NFC forms are equal iff their NFD forms are
  equal. NFC is the shorter key and the conventional canonical target here (git spells it
  `core.precomposeunicode`).
- **`casefold()` not `lower()`** — full case folding is what caseless MATCHING means; `lower()` is a
  round-trip operation (`U+017F ſ` lowercases to itself but folds to `s`).
- **ASCII fast path is not an approximation** — for pure-ASCII input NFD/NFC are the identity and
  full casefold coincides exactly with `lower()`, so it returns the same bytes the slow path would.
  It also creates no seam: a non-ASCII prefix folding INTO ASCII (`ſ` → `s`) still meets the ASCII
  spelling. It exists so the live catalog's 8,276 distinct prefixes — all ASCII — cost what they did.
- **non-UTF-8 paths: NOT a crash and NOT a block.** Paths are bytes and a latin-1 filename is not an
  attack. The decode is `surrogateescape`; undecodable bytes survive as lone surrogates (unassigned,
  combining class 0), pass normalization and casefold untouched, and re-encode to the ORIGINAL
  bytes — `A/\xff/b` still folds its ASCII half to `a/\xff/b`. Verified over 3,000 random byte
  strings: no exception, byte-exact round trip.

**UNCHANGED ON PURPOSE:** the `(st_dev, st_ino)` discriminator (so this stays a MEASUREMENT and
case-/normalization-sensitive forks are unaffected), the GIT_CASEFOLD_ALIAS (13) vs
GIT_CASEFOLD_DIR_ALIAS (14) leaf/directory split, and byte-level path handling everywhere else.
**No new code was added** — the remedy for all three alias shapes is the same (settle on one
spelling), so a fourth code would name a difference that does not exist.

### Evidence

| Arm | Result |
|---|---|
| (AA) NFC/NFD directory alias | pre-fix exit 0 + clean-tree constant → post-fix `GIT_CASEFOLD_DIR_ALIAS`, BOTH implementations |
| (AB) non-ASCII case (`É/` ≡ `é/`) | same |
| (AC) round-10 ASCII (`A/` ≡ `a/`) | still refused — no regression |
| (AA2)/(AB2) | fold reverted to `bytes.lower()` in BOTH → the attacks land again (exit 0) |
| (AA3)/(AB3) sweep-only revert | the fingerprint helper still refuses (`AUDIT_FINGERPRINT_UNVERIFIABLE`) |
| (AA4)/(AB4) helper-only revert | the 12c sweep still refuses on its own code |
| (Z5) real case-sensitive APFS volume | distinct-inode `A/`+`a/` NOT refused |
| (AA5) real case-sensitive APFS volume | distinct-inode `É/`+`é/` NOT refused |
| (AD) fold parity | 8,298 inputs in-harness (8,276 live-tree prefixes + 22 adversarial) — 0 disagreements; a wider standalone run of 11,299 (the same corpus + 3,000 random byte strings) also disagreed 0 times and raised no exception |
| (AE) normalization false-positive | **SIMULATED** — distinct synthetic inodes → 0 reports; one inode → reported |
| live tree | digest IDENTICAL pre and post, exit 0. **Corrected TWICE, and the second correction was the same defect as the first (round 12).** The row first printed `751098…`, not re-derivable; the round-11 follow-up replaced it with `4983b60c…` from "a reconstructed dirty tree" and gave no reconstruction — swapping one unauditable number for another. Both are removed. What survives is the claim plus a recipe anyone can run: at a clean checkout of ANY of these commits `python3 practices/scripts/lib/tree_fingerprint.py .` returns the clean-tree constant `0a815065…` **by construction** (the digest covers status + `diff HEAD` + untracked bytes, all empty on a clean tree), so the clean comparison is real but weak; the round-12 entry below therefore states a dirty-tree control with the exact recipe that re-derives its number |
| performance | 0.265 → 0.266 s/run over 5 runs (+0.4%, within noise; 0 non-ASCII tracked paths) |

**Why (AE) is simulated, stated rather than hidden:** a distinct-inode `é/`+`e◌́/` pair needs a
normalization-SENSITIVE filesystem and this platform has none. Measured with `hdiutil` on this
machine: case-insensitive APFS, **case-sensitive APFS**, case-sensitive HFS+, ExFAT and FAT32 are
**all** normalization-INSENSITIVE (the FAT variants additionally rewrite the spelling to NFD on
write). The reviewer's prescription assumed the case-sensitive APFS volume would also be
normalization-sensitive; it is not, and that is reported rather than papered over.

### Correction carried in this round

`tree_fingerprint.py:252` still claimed the distinct-inode control was "verified with a simulated
distinct-inode pair, because this machine has no case-sensitive volume to build the twin on". That
was stale as of round 10 — (Z5) builds it on a real `hdiutil create -fs "Case-sensitive APFS"`
volume. Corrected, and the paragraph now distinguishes what is real (both CASE controls) from what
is simulated and why (the NORMALIZATION control).

### Registered rather than done

- `docs/BACKLOG.md` **P3-126** — pathological-prefix cost: O(P) memory in distinct prefixes and
  O(depth²) bytes to build one entry's keys. **Graded honestly as a resource concern, not a
  bypass** — exhaustion makes the sweep fail and the gate BLOCK (unknown never passes). Measured at
  this catalog's scale: no observable cost (5,745 paths → 8,276 prefixes, +0.4% wall clock).
- `docs/BACKLOG.md` **P3-127** — Windows/NTFS trailing-dot/space filename equivalence as an
  **unverified** sibling. **Explicitly NOT APFS and not reproduced here**: no Windows host was
  available to measure either (1) that NTFS serves both spellings from one inode or (2) that git for
  Windows can create such index entries. Adding a trailing-dot/space strip to the fold without that
  measurement would risk false positives on POSIX trees that use those characters legitimately, so
  the row is registered with **no code change**.
- `docs/BACKLOG.md` **P3-120** — **CLOSED by this entry**; it is the row this P1 escalated from. Its
  original grading ("the clean-tree precondition already refuses the ordinary form") was true of the
  LEAF form only and does not hold for the directory form round 10 opened.

- Commits: (this commit — PRD-final-4 wave, P1-seal round-11)

---

## TD-2026-08-01-(P1-ignorable-fold) — the fold was caseless and canonical; the filesystem also skips characters

**Round 12, cross-family reviewer P1.** Rounds 10 and 11 taught the prefix map to see two of the
three ways a filesystem can serve two spellings from one inode: CASE and CANONICAL NORMALIZATION.
There is a THIRD, it is independent of both, and it is written down in the format specification.
Case-insensitive HFS+ folds designated formatting controls to **zero** and skips them entirely —
Apple TN1150, `FastUnicodeCompare`: *"All ignorable characters are folded to the value zero."*
Round 11's key `NFC(casefold(NFD(s)))` **preserves** those code points, so `SAFE/` and
`SAFE<U+200C>/` produced different keys and their shared inode was never compared.

```
REPRODUCTION — the reviewer's topology, on a REAL case-insensitive HFS+ volume built here
    hdiutil create -size 48m -fs HFS+ -volname AXHFS -ov axhfs.dmg && hdiutil attach …

    index: SAFE/check.sh          (committed content: cat "SAFE/helper")
    index: SAFE<U+200C>/helper    (committed content: PASS)
    disk:  SAFE and SAFE<U+200C> are the SAME directory inode (16777237, 404)

    git status --porcelain -uall   → EMPTY
    bash SAFE/check.sh             → PASS                    (so every gate goes green)
    e5fbd0a tree_fingerprint.py .  → exit 0, 0a815065…       (the CLEAN-TREE CONSTANT)
    HEAD    tree_fingerprint.py .  → exit 14, GIT_CASEFOLD_DIR_ALIAS — SAFE ≡ SAFE‌
```

The pushed tree records only `SAFE<U+200C>/helper`. A receiver that treats U+200C as significant —
every filesystem measured here except case-insensitive HFS+ — has no `SAFE/helper`, so the
committed check is broken on arrival and nothing measured it. **U+202E RIGHT-TO-LEFT OVERRIDE is
the same defect through the same hole.** Committed path names only; no environment control.

### THE FIX — strip general category Cf, FIRST, in both implementations

`NFC(casefold(NFD(strip_Cf(s))))`. One line added to `_fold_path_key` and to its byte-identical
twin `_ax_fold_path_key`. **The `(st_dev, st_ino)` discriminator is UNCHANGED for the third round
running**, so this remains a MEASUREMENT of an observed alias and not an assumption about the
filesystem; the leaf/directory code split is unchanged too, so **no new code was added** — the
remedy for all four alias shapes is the same (settle on one spelling), and the message prints both
spellings, so a fourth code would name a difference that does not change what the operator does.

**Why category Cf and not the two alternatives — decided by measurement, and the measurement is
now asserted in the harness (`(AI)`) so it fails here rather than in a fork-receiver's evidence.**
The requirement is asymmetric: the strip set must be a **superset** of what a target filesystem
ignores, because a MISSING character is a silent false-green, while an EXTRA one cannot produce a
refusal on its own — a verdict still requires an OBSERVED shared inode.

| candidate | size | covers the reproduction? | verdict |
|---|---|---|---|
| TN1150's 16 (`U+200C-200F`, `U+202A-202E`, `U+206A-206F`, `U+FEFF`) | 16 | yes | **rejected** — correct today, but it is a literal hand-list, i.e. the "absence assertion rot" shape this catalog has already been bitten by. Cf is the same set plus a rule. |
| **general category Cf** | **170** | **yes** | **adopted.** Measured: all 16 of TN1150's are Cf; `Cf ∩ ASCII = ∅`; the extra 154 are inert (the same live volume gives all 154 DISTINCT inodes, so no measured filesystem folds them). |
| `Default_Ignorable_Code_Point` | 4,174 | yes | **rejected** — Python exposes no such property, so it ships as a UCD-pinned literal table of which **3,769 are UNASSIGNED**; and it is **not a superset of Cf** — it EXCLUDES 32 Cf characters (`U+0600-0605`, `U+06DD`, `U+070F`, `U+0890-0891`, `U+08E2`, `U+FFF9-FFFB`, `U+110BD`, `U+110CD`, `U+13430-1343F`). It would ADD variation selectors and Hangul fillers, which the live volume measurably does **not** ignore (`SAFE` vs `SAFE<U+FE0F>` → distinct). |

- **The published table and the live volume agree exactly.** The 16 were DERIVED by evaluating the
  HFS+ case-fold table (TN1150 publishes the algorithm but omits the table data; the table itself
  was read from the live implementation of the same table) and then CONFIRMED against the volume:
  **16/16 fold to one inode, 0/154 of the remaining Cf do.**
  **WIDENED 2026-08-01 by the independent verification lane, and the widening matters because the
  original evidence was narrower than this prose implied.** The probe above only ever asked the
  170 Cf characters, so "Cf is a superset of what HFS+ folds" rested on the TN1150 table being
  complete — an absence assertion about characters nobody had measured. The verifier swept the
  ENTIRE BMP on the same volume — **63,486 characters, 7.9 s — and exactly 16 fold, all of them
  Cf**. The superset property is therefore BMP-complete by measurement, not by trusting a
  published table. What remains unswept by anyone: the astral planes (U+10000 and above).
- **U+202E was CHECKED, not assumed.** It is a bidi control, and the reviewer warned that some
  derivations omit it. Measured: it IS category Cf, it IS `Default_Ignorable_Code_Point` in
  `DerivedCoreProperties-17.0.0`, and the live volume DOES fold it. Covered under every candidate.
- **U+0000 is deliberately NOT ignorable** in TN1150 — the algorithm maps NUL to a non-zero
  sentinel precisely so zero can mean end-of-string — and NUL cannot occur in a path anyway.
- **The strip runs FIRST**, because removing a combining-class-0 character can unblock canonical
  reordering of the marks around it, so stripping before NFD is strictly more canonical than after.
  **One pass is provably enough**: over all 1,114,112 scalars, neither `casefold(NFD(·))` nor
  `NFC(·)` ever INTRODUCES a Cf character (0 of them), so nothing downstream restores what the
  strip removed.
- **The ASCII fast path stays a TRUE equivalence** on all three axes: measured, no ASCII scalar is
  Cf, so an ASCII-only prefix can contain nothing the strip would remove.
- **Case-sensitive HFSX does NOT ignore these characters** (TN1150 says so explicitly). That is not
  a problem — it is the false-positive side, and it is exactly what the inode discriminator handles.

### Evidence

| Arm | Result |
|---|---|
| (AF) ZWNJ directory alias, REAL HFS+ volume | pre-fix exit 0 + clean-tree constant → post-fix `GIT_CASEFOLD_DIR_ALIAS — SAFE ≡ SAFE‌`, BOTH implementations |
| (AG) RLO directory alias, REAL HFS+ volume | same → `GIT_CASEFOLD_DIR_ALIAS — SAFE ≡ SAFE‮` |
| (AF2)/(AG2) strip reverted in BOTH | the attacks land again — `recency_pass`, `tree_fingerprint 0a815065…` |
| (AF3)/(AG3) sweep-only revert | the fingerprint helper still refuses (`AUDIT_FINGERPRINT_UNVERIFIABLE`, recompute exit 14) |
| (AF4)/(AG4) helper-only revert | the 12c sweep still refuses on its own code (`GIT_CASEFOLD_DIR_ALIAS`) |
| (AC)/(AA)/(AB)/(Z)/(W) | round-10 ASCII, round-11 NFC/NFD and non-ASCII case ALL still refused — no regression |
| (AH) false-positive control | **LIVE, not simulated** — distinct-inode `SAFE/` + `SAFE<U+200C>/` NOT refused |
| false-positive, REAL case-sensitive APFS | distinct inodes → post-fix helper exit 0, not refused |
| (AD) fold parity | 8,307 inputs (8,276 live-tree prefixes + 31 adversarial incl. the ignorable corpus) — 0 disagreements |
| (AI) strip-set claims | all 4 hold; `Cf` = 170 code points at Unicode 16.0.0; TN1150's 16 ⊆ Cf |
| live tree, CLEAN | digest IDENTICAL pre and post: `0a815065…`, exit 0 — **weak by construction** (a clean tree hashes empty inputs) |
| live tree, DIRTY — **with the recipe** | `8a91e493…` pre AND post. Recipe: at a clean checkout of the commit, `printf 'X\n' > .ax-fp-probe.txt`, then `python3 practices/scripts/lib/tree_fingerprint.py .`. Run at e5fbd0a and at this commit, each with that commit's own helper |
| performance | 0.264 → 0.257 s/run mean over 5 runs (no measurable cost; 0 non-ASCII tracked paths, 8,276 prefixes all on the ASCII fast path) |

**Why this round's ATTACK and FALSE-POSITIVE arms are real, unlike round 11's.** (The heading of
this paragraph used to read "why this round has no SIMULATED arm" — **corrected 2026-08-01**: that
shorthand is false as written, because `(AI)` is an unconditional simulated grouping arm and its
own log line says `SIMULATED`. The paragraph below always said so; the heading overstated it.
A summary that contradicts the body it summarizes is the same defect this round corrected in the
prover's banner, reappearing one level up.) Round 11 could not build a
normalization-SENSITIVE volume and said so. Round 12 needs the opposite pair and both exist: a
volume that FOLDS the character (case-insensitive HFS+, attached by the harness itself with
`hdiutil create -fs HFS+`) and a volume that does NOT (both APFS variants, i.e. the ordinary
sandbox). The harness still carries a simulated grouping check in `(AI)` so the RED direction is
measured even on a host where no folding volume can be attached — but on this machine it ran REAL,
and the banner reports which arm actually ran rather than asserting the stronger one.

### Corrections carried in this round — and the PATTERN they share

All three are the same failure mode: **a correction that fixes one instance of a disproved claim
and leaves the same claim standing somewhere else.** Round 11's follow-up made exactly this mistake
three times, and it is worth naming because it is not carelessness — it is what happens when the
correction is aimed at the *line the reviewer quoted* instead of at the *claim*.

1. `tree_fingerprint.py:457` still said unnormalized `casefold()` "separates **every pair** above".
   Measured false: unnormalized `casefold()` already equates `É`/`é` AND `ſ`/`s`; only the
   NORMALIZATION pair needs the inner NFD. **The identical sentence was also live at
   `DECISIONS.md:2215`** — the reviewer flagged one, and fixing only that one would have repeated
   the very defect. Both corrected.
2. `DECISIONS.md:2216` still called the outer normalization load-bearing and repeated the disproved
   `U+1E9B U+0323` ≡ `U+1E69` claim, months after the `.py` docstring had retracted it. Corrected,
   with the measurement: given the inner NFD, `casefold(NFD(s))` alone already equates that pair.
   The same bullet's parenthetical was ALSO wrong in a way nobody had flagged — it said the round-9
   proposal `normalize('NFC', …).lower()` "misses `É`≡`é`", and measured, it does not (Python's
   `str.lower()` is Unicode-aware; the round-9/10 bug was `bytes.lower()`). It is rejected for one
   reason, not two: `lower()` is a round-trip operation and fails `ſ`≡`s`.
3. `DECISIONS.md:2257` replaced one unauditable dirty digest (`751098…`) with **another**
   (`4983b60c…`) and no reconstruction recipe. Both removed. What replaces them is a number anyone
   can re-derive — see the `live tree, DIRTY` row above, which states the recipe next to the value.

### Registered rather than done

- `docs/BACKLOG.md` **P3-128** — NFKC/compatibility equivalence (fullwidth `Ａ`, roman `Ⅳ`).
  **Graded out of scope BY MEASUREMENT, not by assertion**: all three volumes built here
  (case-insensitive APFS, case-SENSITIVE APFS, case-insensitive HFS+) give these DISTINCT inodes, so
  no ordinary target filesystem is established as collapsing them, and widening the fold to an
  unestablished equivalence is the same unanchored move that got `Default_Ignorable` rejected.
  Noted there: part of NFKC is already covered incidentally — `ﬁ` (U+FB01) folds to `fi` under full
  casefold, and case-insensitive APFS does serve that pair from one inode. **The row also carries
  the honest residual that Cf is proven a superset only for HFS+** — ZFS `normalization=` and
  server-side-folding SMB/NFS mounts are unmeasured.
- `docs/BACKLOG.md` **P3-129** — NTFS 8.3 short-name aliases. **Not live-reproduced**: generation is
  a per-volume policy (`fsutil 8dot3name`), disabled by default off the system volume on modern
  Windows, so it is not a fixed property of the filesystem; and a short name is a lookup alias
  rather than a spelling git records, so it is unverified whether this class's premise (two index
  entries, one file) can even be built. No Windows host was available. **No code change** — same
  discipline as P3-127.
- `docs/BACKLOG.md` **P3-130** — Turkish `I`/`ı`. **This is a REFUTED candidate, not an unverified
  gap, and is graded below the other two.** Measured: all three volumes give `I`/`ı` and `i`/`İ`
  DISTINCT inodes. exFAT compares through an up-case table **stored on the volume**, not the process
  locale, and its recommended table does not fold U+0131 to U+0049 — so the premise that a locale
  changes filename comparison does not hold for the target filesystem. Our side is locale-independent
  too: `str.casefold()` never consults the locale, so running R25 under `tr_TR` does not move the
  key. No code change.
