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

- **P1-2 — an absolute path authenticates neither the binary nor python's startup.** The round-5
  validator accepted anything lexically absolute that passed symlink-FOLLOWING `-f`/`-x`.
  MEASURED: a symlink named `python3` → `/usr/bin/true` passed every test and turned the recency
  guard's entire python body into **exit 0** (honest baseline 1); a `sitecustomize.py` calling
  `os._exit(0)`, reached through an inherited `PYTHONPATH`, did the same. FIX — (a) the tool's
  DIRECTORY is canonicalised with `builtin pwd -P` and the program is then made to IDENTIFY ITSELF
  by being RUN (`git --version` must produce a git banner; python3 must print a self-report under
  `-I -S`), new code **HERMETIC_TOOL_UNAUTHENTIC**; (b) the whole `PYTHON*` family is scrubbed in
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
  (**HERMETIC_PY_YAML_UNAVAILABLE**) and never degrades to a skip. The residual is a
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
