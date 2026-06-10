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
