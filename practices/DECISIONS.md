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

- Commits: (this commit — PRD-final-4 wave, P1-seal round-12)

---

## TD-2026-08-01-(P1-symlink-target-alias) — four rounds of alias census, and none of them looked at what a symlink POINTS AT

**Round 13, cross-family reviewer P1.** Rounds 9-12 widened the alias KEY three times — ASCII case,
canonical normalization, non-ASCII case, ignorable Cf — and never once widened the SUBJECT. Every
one of them registered **INDEX PATHS** (`tree_fingerprint.py` `_register_prefixes`;
`completion_checklist_recency_guard.sh` the prefix walk in 12c). A symlink's **TARGET is not an
index path.** It is **blob content**: read as bytes, hashed as a blob, and **never resolved** — so
the entire four-round census did not apply to it on any of the four axes. The gap was not a
weakness of the key; it was a hole in the domain the key was ever applied to.

```
REPRODUCTION — the reviewer's topology, COMMIT ONLY, no environment control
    git mv backend/gradlew backend/gradlew-real
    ln -s GRADLEW-REAL backend/gradlew          # the CASE is the whole attack
    git add backend/gradlew backend/gradlew-real && git commit

    git status --porcelain -uall   → EMPTY
    ./backend/gradlew              → runs      (so R25 EXECUTES the wrapper and goes green)
    9c8f339 tree_fingerprint.py .  → exit 0, 0a815065…              (the CLEAN-TREE CONSTANT)
    9c8f339 recency guard          → exit 0, completion_checklist.recency_pass
    HEAD    tree_fingerprint.py .  → exit 15, GIT_SYMLINK_TARGET_ALIAS
    HEAD    recency guard          → exit 1,  GIT_SYMLINK_TARGET_ALIAS
```

On a case-SENSITIVE checkout the committed symlink is **DANGLING**: the receiver gets a broken
`backend/gradlew` while our evidence says the tree is clean and the build passed. Confirmed live by
the reviewer against the shipped detector: `backend/gradlew` and `backend/GRADLEW` resolve to the
same APFS inode, `backend/GRADLEW` is not an indexed spelling, the detector produced
`helper_verdict ([], [])`, and the target folds equal to `backend/gradlew-real` while
`target_is_registered_prefix` is FALSE.

### THE RULE THAT SHIPS — and why precision matters more here than in any previous round

Symlinks legitimately point at untracked paths, outside the repository, and at absolute paths. A
bare *"the target must equal the recorded spelling"* rule would refuse most of the honest ones. The
seven steps, in both implementations:

1. take the target's **bytes** (already read for the blob comparison);
2. resolve them **lexically, relative to the link's own directory** — which is the *index's*
   recorded spelling, so the base is authentic;
3. **absolute** / escapes the root through `..` / resolves to the root itself → not this class;
4. `lstat` the candidate; **does not exist** → not this class;
5. look its `(st_dev, st_ino)` up in the **registered prefix set** — the same
   tracked-paths-and-their-directory-components map the prefix walk already builds. Not found
   (**untracked target**) → not this class;
6. candidate spelling **is** a registered spelling → **PASS**;
7. otherwise, **fold-equal** (`_fold_path_key` / `_ax_fold_path_key`, the SHARED key) to a
   registered spelling → **BLOCK**; not fold-equal → not this class.

**Step 7 is where the precision lives.** Gating on FOLD-EQUALITY rather than on bare inequality is
what makes `..` traversal, chains through an intermediate symlinked directory, and absolute targets
fall out automatically instead of needing exceptions — and it means the class inherits every axis
the fold already has, and every axis it gains later, for free.

| edge case | treatment | why |
|---|---|---|
| `..` traversal staying inside the repo | **resolved, then compared** | both live tracked symlinks in this catalog are exactly this shape and land on the EXACT record; they pass |
| lexical `..` that POSIX would resolve elsewhere (a symlinked component) | silent | the candidate lstats to a different inode or none — **under**-inclusive, never over |
| **absolute** target | not blocked | it names a location on the receiver's root filesystem; the index cannot record it, so there is no recorded spelling to be an alias OF. `docs/BACKLOG.md` **P3-131** |
| target escaping the repo through `..` | not blocked | same reason |
| **chained** symlink (target is another tracked symlink) | **compared** | `lstat` does not follow the FINAL component, so the candidate's inode is the second link's own, which the prefix walk registered. Exact spelling passes; an aliased spelling of the second link BLOCKS — the same defect one level up |
| target reached **through** an intermediate symlinked DIRECTORY | not blocked | `lstat` follows the intermediate components so the inode is the real file's, but the candidate does not FOLD-EQUAL the record. Correct: that intermediate link is itself committed and resolves at the receiver too |
| target resolving to a **DIRECTORY** that is a tracked prefix | **compared** | the registry is the PREFIX map, not the leaf set. `link -> BACKEND` over a recorded `backend` BLOCKS |
| **untracked** target | not blocked | no recorded spelling to alias; a link to a build output is ordinary |
| **dangling** target | **not blocked, deliberately** | it is a real defect but a DIFFERENT class: identically broken here and at the receiver, so R25's evidence does not *lie* about it — whatever would have read it failed here first. Blocking it would refuse a link to a not-yet-built ignored artifact. `docs/BACKLOG.md` **P3-132** |
| **dirty** symlinks | not examined | the on-disk target is not the committed one, and 12a refuses a dirty tree outright |

### New blocking code

| code | fires when | fingerprint exit |
|---|---|---|
| `GIT_SYMLINK_TARGET_ALIAS` | a tracked mode-120000 entry whose target resolves inside the repo to a REGISTERED prefix's `(st_dev, st_ino)` under a FOLD-EQUAL but textually different spelling | **15** |

**Why a new code rather than widening 13/14** (rounds 11 and 12 both widened rather than added):
the SUBJECT is different — blob content, not an index path — and so is the REMEDY. 13/14 say
`git mv` a path; this one says `ln -sf <recorded-spelling>`. A shared code would print an
instruction the operator cannot follow.

The clean-tree constant `0a815065…` is preserved by construction: nothing new is appended to the
hash, only a new refusal. **Measured with the SAME auditable recipe round 12 used**, and it is
stated that way on purpose — round 12's own corrections list "replacing an unauditable digest with
ANOTHER unauditable digest" as one of the three defects it fixed, and quoting a working-state
digest here would repeat it one round later. Recipe: at a CLEAN checkout of the commit,
`printf 'X\n' > .ax-fp-probe.txt`, then `python3 practices/scripts/lib/tree_fingerprint.py .` —
run with the 9c8f339 copy of the helper and with HEAD's, in the same tree state.

| tree state | 9c8f339 helper | this commit's helper |
|---|---|---|
| CLEAN | `0a815065…` | `0a815065…` |
| DIRTY (the probe recipe) | `8a91e493…` | `8a91e493…` |

The dirty value is byte-identical to the one round 12 recorded for the same recipe, so the digest
has not moved across two rounds of added refusals.

### Evidence

| Arm | Result |
|---|---|
| (AJ) CASE — the reviewer's gradlew topology, APFS | pre-fix exit 0 + clean-tree constant + `recency_pass` → post-fix helper exit 15 / guard exit 1, `GIT_SYMLINK_TARGET_ALIAS` |
| (AK) NORMALIZATION — index `symdir/é-real` NFC, link blob NFD | same shape → `GIT_SYMLINK_TARGET_ALIAS`. **This is the arm that proves the fix reuses the SHARED fold**: a case-only check is silent here. git does NOT precompose blob content, so ordinary plumbing expresses it |
| (AL) IGNORABLE Cf — target `safe-real<U+200C>`, REAL case-insensitive HFS+ volume | pre-fix exit 0 + clean-tree constant → post-fix `GIT_SYMLINK_TARGET_ALIAS` |
| (AJ2)/(AK2)/(AL2) census removed in BOTH | the attacks land again — exit 0 |
| (AJ3)/(AK3) sweep-only neuter | the fingerprint helper still refuses (`AUDIT_FINGERPRINT_UNVERIFIABLE`, recompute exit 15) |
| (AJ4)/(AK4) helper-only neuter | the 12c sweep still refuses on its own code (`GIT_SYMLINK_TARGET_ALIAS`) |
| (AM) FALSE-POSITIVE control | **nine** legitimate tracked symlinks in ONE tree — exact · `..` onto the exact record · absolute · escaping · untracked (gitignored) · dangling · chained · tracked directory · through a symlinked directory — **all exit 0** |
| (AM) non-vacuity | adding ONE directory-aliased target (`ln -s B/real.txt` over a recorded `a/b`) to that same tree blocks, naming only that link |
| live tree, 2 tracked symlinks | both are `..`-traversal links onto the exact record; exit 0, **zero** refusals |
| performance | 0.300 → **0.296** s/run mean over 5 runs each, same tree, 9c8f339's helper vs this commit's — **no measurable cost**, and the direction is noise, not a speedup. The inode map is one pass over the ~6.8k prefixes already in `statcache` (no new `lstat`), and the target resolution costs exactly 2 extra `lstat` calls, one per tracked symlink |

### What this round did NOT close, stated

- **`docs/BACKLOG.md` P3-131** — an absolute target that happens to point back inside this checkout
  is unportable, but not by *aliasing*; refusing it is a different (portability) check.
- **`docs/BACKLOG.md` P3-132** — a committed DANGLING symlink. Reasoned above; registered rather
  than silently accepted.
- **Lexical vs POSIX `..`.** The resolution is textual, so a `..` that crosses a symlinked component
  resolves somewhere the kernel would not. That direction is **under**-inclusive (the lookup misses
  and the check is silent), never over-inclusive, and it is what a receiver types when it opens the
  committed path component by component.

### Carried in this round, on the reviewer's instruction

- `docs/BACKLOG.md` **P3-128** amended rather than duplicated — the row already named ZFS
  `normalization=` as its own unmeasured residue, so a new row would have been a duplicate
  registration. OpenZFS `normalization=formKC`/`formKD` compares filenames in a **compatibility**
  normalization form, which collapses exactly the equivalence class (`Ａ`/`A`, `Ⅳ`/`IV`) that the
  round-12 key deliberately excludes. **Graded honestly and NOT promoted**: it is a documented
  **non-default** dataset property, settable only at creation, so it is an administrator's option
  rather than a property of the filesystem; and **no live ZFS was available here**, so there is no
  ordinary-use reproduction. Same grade as P3-129. No code change.
- **The round-12 astral-plane gap is now CLOSED BY ARGUMENT, not left hanging.** Round 12's
  superset claim for category Cf was measured BMP-complete (63,486 characters, exactly 16 fold, all
  Cf) and the entry above honestly recorded "what remains unswept by anyone: the astral planes".
  The reviewer closed it structurally: HFS+'s comparison (TN1150 `FastUnicodeCompare`) operates on
  **UTF-16 code units** and indexes its fold table by unit; the **surrogate units (U+D800-DFFF) are
  identity entries, not ignorables**, so neither half of a surrogate pair folds to zero and an
  astral character cannot vanish. There is therefore no structural path by which an astral scalar
  enters HFS+'s ignorable set, and "Cf is a superset" holds astral-inclusive. **The basis is the
  algorithm's structure, not a sweep** — promoting it to a measurement would require an astral
  enumeration, which is not claimed here. Recorded in P3-128 alongside the OpenZFS note.

- Commits: (this commit — PRD-final-4 wave, P1-seal round-13)

---

## TD-2026-08-01-(P1-posix-resolution-and-runtime-paths) — a lexical `..` is not a POSIX `..`, and R25 executes its own inputs verbatim

**Round 14, cross-family reviewer, TWO P1s that need two different treatments.** One is a precise
fix; the other is a bounded fix plus a statement of what is not decidable. They are recorded
together because they are the same doctrine at two levels: *the thing R25 verified is not the
thing the push ships*.

### P1-A — the resolver answered a different question than the receiver's kernel asks

Round 13 resolved a tracked symlink's target **lexically**: it popped `..` textually, before
following anything. The kernel pops `..` **after** following an intermediate symlink. Whenever a
component before the `..` is itself a link, the two answers are different paths — and round 13's
own docstring called that divergence "usually … under-inclusive", which is a description of a
hole, not a bound on one.

**The reviewer's topology, committed content only, no environment control:**

```
backend/real/gradlew-real     (tracked regular file)
backend/real/sub/.keep        (tracked)
backend/jump    -> real/sub                 (tracked symlink)
backend/gradlew -> jump/../GRADLEW-REAL     (tracked symlink)
```

POSIX follows `jump` first, so `jump/..` is `backend/real` and the target is
`backend/real/GRADLEW-REAL` — which case-insensitive APFS serves as the **tracked**
`backend/real/gradlew-real`. So R25 **executes** the wrapper and goes green, `git status
--porcelain -uall` is EMPTY, and the fingerprint is the clean-tree constant. A case-**sensitive**
receiver gets a **dangling** `backend/gradlew`. The lexical resolver produced
`backend/GRADLEW-REAL`, which does not exist, so **both** implementations took their dangling exit
and reported nothing. Measured at `b259b25`:

| implementation | pre-fix (round 13) | post-fix (round 14) |
|---|---|---|
| `tree_fingerprint.py` | **exit 0**, digest `0a815065…` (the clean-tree constant) | **exit 15** `GIT_SYMLINK_TARGET_ALIAS` |
| recency guard 12c sweep | silent (same sweep, same walk) | `GIT_SYMLINK_TARGET_ALIAS` |

The reviewer also demonstrated the kernel behaviour with **no filesystem writes at all**, using
macOS's stock `/var` link: `/var/../TFTPBOOT` and `/private/tftpboot` are one inode, the lexical
`/TFTPBOOT` is absent, and both detectors returned `[]`.

**The fix.** `_resolve_link_target` now walks **component by component**, exactly as the kernel
does: a component is followed iff something remains after it (`lstat` never follows the final one,
which is what keeps the chained-symlink disposition correct, and what a trailing slash legitimately
overrides); following replaces the component, so a later `..` pops the **resolved** stack; an
intermediate link with an absolute target leaves for the receiver's root filesystem
(`absolute`, not this class); a missing or unreadable intermediate is left alone so the final
`lstat` still decides "dangling". Every round-13 not-this-class exit is preserved verbatim
(absolute / escapes the root / the root itself / dangling / untracked inode / exact spelling).

**Two budgets, and they BLOCK rather than going silent.** A resolver that follows can cycle, so
the walk is bounded at 40 link follows and 4096 components. Exhaustion raises the new
`SymlinkResolutionUnbounded` (**fingerprint exit 16**, guard code
`GIT_SYMLINK_RESOLUTION_UNBOUNDED`). Two reasons it blocks: an unfinished walk has **not answered**
the alias question, and converting "unanswered" into "nothing found" is the exact defect every
round since 8 has been closing; and a committed chain that exhausts the budget is one **no**
kernel resolves — the receiver gets `ELOOP`. The budget is the **larger** of the two kernel limits
(Linux `MAXSYMLINKS` 40, macOS `SYMLOOP_MAX` 32) precisely so that a chain some receiver would
resolve is never refused here. Measured: `loopa -> loopb/x` + `loopb -> loopa`, kernel `ELOOP`,
pre-fix **exit 0** → post-fix **exit 16**.

**P3-133 is superseded, and its own remedy is rejected.** That row proposed "cross-check the
lexical candidate against realpath and go **silent** on divergence". Silence on divergence is
precisely what the topology above exploits, so adopting it **would have preserved this P1**. The
row is closed as REJECTED-AND-SUPERSEDED with the reviewer's reasoning quoted, and its
over-inclusive counterexample was **re-measured** under the new resolver rather than assumed away:

| `ln -s outdirlink/../SECRET.txt`, `outdirlink -> ../outside`, tracked `secret.txt` | verdict |
|---|---|
| round 13 (lexical) | **exit 15** — a FALSE refusal (the row's complaint) |
| round 14 (component-wise) | **exit 0**, digest `0a815065…` — "escapes" on the first `..`, because the follow has already returned the stack to the root |

The over-inclusion is **gone**, not tolerated. Both directions of divergence are now closed by
construction instead of by argument.

**Controls, all re-run rather than inherited.**
- The **nine legitimate shapes** were rebuilt in one tree (exact spelling · `..` onto the exact
  record — the shape both live tracked symlinks in this catalog have · absolute · escapes the root
  · untracked/gitignored target · dangling · chain through another tracked symlink · tracked
  directory target · target through an intermediate symlinked directory): **exit 0 under HEAD and
  under round 14**. Non-vacuity: adding one FINAL-component alias (`-> sub/REAL.TXT`) to the same
  tree is named and refused, **exit 15**.
- The round-13 direct shapes still block: (AJ) case `-> GRADLEW-REAL` **exit 15**, (AK)
  normalization (index NFC `é-real`, target blob NFD) **exit 15**, under both HEAD and round 14.
- Live tree: 2 tracked symlinks pass, digest **unchanged** on the auditable recipe (clean
  `0a815065…` / dirty `8a91e493…`, byte-identical between the HEAD copy and this one), and 5-run
  means 0.2738 → 0.2758 s/run (+0.7%, measurement noise).
- Prover: (AN) the jump topology, (AN2) the pre-round-14 twin whose neuter **restores the lexical
  walk** in both implementations (refusing to follow any intermediate component IS lexical
  semantics — one condition-constant edit, leaving the round-13 report, the fold and the
  discriminator live, so what lands again is attributable to the resolution algorithm alone),
  (AN3)/(AN4) the per-implementation splits, (AO) the cycle budget. Each carries a premise
  assertion; (AN)'s premise additionally requires that the POSIX resolution **reach the tracked
  inode** and that the **lexical candidate be absent**, so the case cannot silently degenerate
  into round 13's.

### P1-A2 (round 14b) — following an intermediate made its spelling unjudged, and the residual above was mis-graded

*This is an **amendment to this entry**, not a new one: same subject, same resolver, same code.
The round-14 text above shipped the residual below as P3-134 "register-only"; that grade was
wrong and the correction belongs where the claim was made.*

Round 14 followed intermediates correctly and then asked the alias question **once**, about the
**final** candidate. Following an intermediate therefore **discarded its spelling**. An
independent verification lane accepted the attribution-hygiene argument for deferring it as
procedurally defensible, and then produced the reproduction the row did not record:

| tree: tracked `mid/real/real.txt`, tracked symlink `mid/dirlink -> real` | verdict |
|---|---|
| `ln -s DIRLINK  mid/x` | **exit 15** `GIT_SYMLINK_TARGET_ALIAS` — final component, the round-13/14 class |
| `ln -s DIRLINK/ mid/x` | **exit 0** — the SAME alias, one keystroke, refused by NEITHER resolver |
| `ln -s DIRLINK/real.txt mid/x` | **exit 0** — the shape actually registered as P3-134 |

The trailing slash is honoured **legitimately** — the kernel follows the final component when
something follows it, which round 14's own docstring says — and that legitimacy is exactly what
moved the alias out of reach: the follow lands on the correctly-spelled `mid/real`, which passes
at step 5, while `mid/DIRLINK`, the spelling that dangles at a case-sensitive receiver, was never
asked about. So this is a **one-character bypass of a shipped refusal**, reachable with committed
content only, capable of false-green push evidence.

**THE MIS-GRADING IS THE LESSON, and it is the part worth carrying forward.** P3-134 was graded
from the shape that had been *measured* (`-> DIRLINK/real.txt`, filed as a surface expansion)
rather than from the shape's **reachability** — one keystroke away from an attack the same file
already refuses. A defect's grade follows what an adversary can reach, not the tidiness of the
argument for deferring it; "attribution hygiene" is a reason to ship a fix in its own commit, never
a reason to grade it lower. The register-only decision was procedurally defensible and materially
wrong at the same time, which is the combination that gets things shipped.

**The fix.** `_resolve_link_target` returns every **non-final** component it resolved (recorded
before the follow pops it off the stack — no extra syscall, the `lstat` is the one the walk
already needed) and `_symlink_target_verdicts` applies **steps 3-6 unchanged** to each. Same code
(`GIT_SYMLINK_TARGET_ALIAS`): the subject (a committed spelling that dangles at the receiver) and
the remedy (respell the target) are identical, so a second code would only fragment one class —
only the sentence changes, to name the component. It runs for **every** walk outcome, including
`escapes` / `root` / `unbounded`, because an intermediate the receiver cannot resolve is broken
there regardless of where this walk ended up. Both implementations, symmetrically. Every
not-this-class exit is preserved verbatim (absolute · escapes the root · dangling · unregistered
inode · exact spelling) and both budgets (40 / 4096) are untouched.

**Precision was the risk, so the false-positive control grew rather than the detector.** The
legitimate-shape tree goes from **nine to thirteen** shapes (15 tracked symlinks — `d1` and
`chainmid/d2` are the intermediates the `deep` shape traverses), the four new ones all in the intermediate
position: a trailing slash on a correctly-spelled tracked **directory** (`-> sub/` — the
legitimate twin of the attack), the same through a correctly-spelled tracked **symlink**
(`-> dirlink/`), **several** correctly-spelled intermediates in one target (`-> d1/d2/real.txt`
over `d1 -> chainmid`, `chainmid/d2 -> ../sub`), and an **untracked** intermediate
(`-> build/blink/real.txt` through a gitignored directory holding an untracked symlink). All
thirteen **exit 0**.

**UNTRACKED INTERMEDIATE — treatment, stated rather than left implicit: NOT refused.** An
untracked intermediate has no recorded spelling for it to be an alias *of*, so there is no
comparison to make; this is exactly the exit step 4 has always taken for an untracked final
candidate. What the receiver gets through such a component is governed by whether the path is
shipped at all, which is the dangling class (P3-132), not this census. Refusing it would refuse a
link through any gitignored build directory — the first thing a fork-receiver has.

**Controls and measurements, all re-run.**
- The verifier's exact pair, **both implementations**: `-> DIRLINK` **exit 15** before and after;
  `-> DIRLINK/` **exit 0 → exit 15**. The no-slash intermediate `-> DIRLINK/real.txt`:
  **exit 0 → exit 15**. Normalization (record `mid/é-link` NFC, target NFD) and ignorable Cf
  (`safe-link<U+200C>/real.txt`) do the same, which is what shows the intermediate verdict rides
  the **shared** fold rather than a case-only check one component to the left.
- **The round-14 C2 non-block still holds**: `ln -s outdirlink/../SECRET.txt` with
  `outdirlink -> ../outside` and a tracked `secret.txt` is still **exit 0** — `outdirlink` is the
  **recorded** spelling, so the new intermediate verdict passes it at step 5 and the walk still
  takes "escapes" on the first `..`. The over-inclusion round 14 removed was **not** reintroduced.
- Live tree, measured on a CLEAN clone of HEAD so the recipe is reproducible: 2 tracked
  symlinks pass, digest **unchanged** — clean `0a815065eb…`, dirty `8a91e4934b…` after
  `printf 'X\n' > .ax-fp-probe.txt`, byte-identical between the HEAD copy and this one.
  5-run means, two rounds each: 0.2817 / 0.2795 s/run (HEAD) vs 0.2800 / 0.2798 s/run
  (14b) — the difference is smaller than the run-to-run spread, which is what the added
  work predicts: no new syscall, only a dict lookup per resolved component.
- Prover: (AP)/(AP2) the final-component control, still blocking **through** the 14b neuter —
  which is what makes the twins attributable to the new *subject* rather than to any change in the
  old one; (AQ)-(AQ4) the trailing slash with its twin and per-implementation splits;
  (AR)/(AR2) the no-slash shape; (AS)/(AS2) normalization; (AT)/(AT2) ignorable Cf on the folding
  volume; (AM) the thirteen legitimate shapes. The 14b neuter is the round-9/10/13 shape (kill the
  `if`), leaving the resolution algorithm, the round-13 report, the fold, the discriminator and
  both budgets live.

**Cosmetic, same lane:** the 12c comment named the budget code `R25_SYMLINK_RESOLUTION_UNBOUNDED`
while the emitted code is `GIT_SYMLINK_RESOLUTION_UNBOUNDED`; corrected. And guard [104]'s
descriptive echo in `run-all-guards.sh` quoted its own reproduction verbatim, so
`--advisory-scripts` reported one hit on a pristine tree. **Decision: rephrased**, not left — an
advisory that always fires is one nobody reads, which is the same habituation failure this family
keeps closing; the narrative spells the alias out instead of quoting it and the scan now reports
**0** on a pristine tree while still reporting a planted one.

### P1-B — R25 executes its own inputs verbatim, and nobody checked their spelling

`verify-completion.sh` takes `command` and `working_directory` out of
`practices/verification-checklist.yaml` verbatim (`:1086`) and executes them through
`cd "$exec_wd"` + `bash -c` (`:1561`). **Reproduction, committed content only:** rewrite one
command's `practices/evals/spec_policy_ref_guard.sh` as `PRACTICES/evals/…`. On case-insensitive
APFS the command **exits 0**; `git cat-file -e HEAD:PRACTICES/evals/…` **exits 128** (the recorded
tree has no such path); `git status --porcelain -uall` is EMPTY with the tamper committed;
`tree_fingerprint.py` returns the clean-tree constant `0a815065…`; guard [58]
(`verification_checklist_task_coverage`) exits 0; and all ten R25 violation buckets stay empty. R25
publishes **green evidence for a command that fails immediately on a case-sensitive receiver**.

**(1) The tractable, mandatory part — the gate's OWN inputs.** New guard [104]
`checklist_command_path_spelling_guard.sh`. Every path-like token of every `command`, plus every
effective `working_directory`, is resolved repo-relative against its working directory and must
**be a recorded spelling** — a tracked path or a directory component of one. A token that is not
recorded but **folds equal** to one blocks as `CHECKLIST_PATH_ALIAS`. The fold is **imported** from
`practices/scripts/lib/tree_fingerprint.py` (`_fold_path_key`), not re-implemented: a third copy of
a rule that already exists twice is a third chance to drift, and importing means this guard gains
every axis the fold gains (case, normalization, non-ASCII case, ignorable Cf — and whatever comes
next).

**No `(st_dev, st_ino)` discriminator here, deliberately.** The tree sweep compares two spellings
that the **verifying** filesystem serves from one file, so it must measure that identity. This
guard's subject is a **string inside a committed file**: there is no local resolution to measure,
and the filesystem that decides is the **receiver's**. A token that is not a recorded spelling but
folds onto one is broken on any faithful filesystem and silently green on an aliasing one — both
verdicts say BLOCK. The consequence is that this guard, and its fixtures, are **filesystem-
independent**.

**Why a new guard and not a fold-in** (the choice is load-bearing): the recency guard owns the fold
and the tree sweep, but it runs at **pre-push**, only after a full audit-log chain, and its fixture
roots are non-git trees where check 12 never runs — folding there would make the reproduction
detectable only at push time and not fixture-provable. Guard [58] parses the same file but answers
a different question and has its own fixtures. `run-all-guards.sh` **is an R25 step**, so a guard
registered there fails the very R25 run that contains the aliased command.

**Candidate rules, enumerated** (an extractor that is not enumerated is a false-positive
generator). A token is a candidate iff: it survives `shlex.split(..., posix=True)`; it is not a
shell operator; it does not start with `-`; it contains no `=`; it is not absolute and contains no
`://`; it is not `.`/`..`; and it contains no unexpanded shell metacharacter (`$ \` * ? [ ] ~`).
`working_directory` is a candidate unconditionally (minus `.`), because it is a repo path by
schema. **Zero false positives, measured** on the live checklist: **367 tokens → 240 recorded /
125 unrelated / 2 skipped flags / 0 aliases**, printable with `--show`. Fixtures:
`pass_recorded_spellings`, `fail_command_case_alias` (the reproduction), `fail_working_directory_alias`.

**(2) The honest limit — the general class stays OPEN as `docs/BACKLOG.md` P2-72.** Arbitrary committed content names paths in strings: shell `source`, gradle `apply from:`,
npm/node specifiers, python imports, JSON/YAML file references, Dockerfile `COPY`, CI workflow
paths. Deciding which substrings of an arbitrary file are paths is **undecidable by inspection**
(concatenation, variable expansion, runtime composition). A heuristic scanner that claimed to close
the class would be exactly the green-but-hollow shape this catalog keeps catching, so none is
shipped. **The only complete remedy is to verify on a filesystem that does not alias** — a
case-sensitive and normalization-sensitive checkout, which this project has already proven
constructible (`hdiutil create -fs "Case-sensitive APFS"`, used by the round-10..13 prover).
Graded honestly: **in scope** (no environment control, false-green capable), **not closed**, and
the remedy is **infrastructural** — an R25 opt-in mode or a periodic job that checks out onto such
a volume and runs the suite there. Cost and adoption conditions are stated in the row. A cheap
mitigation ships and is **labelled advisory**: `--advisory-scripts` applies the same verdict to
path-like literals in the shell scripts the checklist **itself names** (one level), printing
`ADVISORY` lines that never touch the exit code (live: 3 scripts, 0 aliases; non-vacuity: a planted
alias is reported). **The row stays open.**

- Commits: (this commit — PRD-final-4 wave, P1-seal round-14)

## TD-2026-08-01-(backlog-decision-closures) — eleven decision-class rows adjudicated with REFUSE-TO-CLOSE as the default

- Date: 2026-08-01
- Status: accepted
- Trigger: final-4 wave, Lane E. Eleven open BACKLOG rows whose honest resolution might be a
  recorded decision rather than code. "Close it as a decision" is exactly how a catalog launders
  work it did not do, so each row was held against four closure grounds — (A) premise REFUTED by
  measurement, (B) OUT OF DECLARED SCOPE per TD-2026-07-30-(ratchet-threat-model), (C) DESIGN
  BOUNDARY with recorded reversal condition, (D) UNVERIFIABLE ON ANY AVAILABLE PLATFORM, bounded
  and converted to a platform-gated done-when — and any row naming real work this project can do
  stays OPEN. Result: **6 closed / 5 stay open**, with two of the open verdicts resting on NEW
  refuting measurements taken for this adjudication.

### New measurements (2026-08-01, this machine, read-only scratch repos; helper = `practices/scripts/lib/tree_fingerprint.py` at 6242e65)

1. **P3-132's premise is FALSE for a reachable shape.** Scratch repo: `.gitignore` = `build/`,
   untracked-but-EXISTING `build/out.txt`, committed symlink `link -> build/out.txt`. `git status
   --porcelain -uall` EMPTY; `cat link` serves `payload-bytes` HERE; fingerprint = clean-tree
   constant `0a815065…`, **exit 0**. A fresh clone has no `build/out.txt` → the receiver gets a
   DANGLING link. So "identically broken here and at the receiver, the evidence does not lie"
   holds ONLY for the dangling-on-both-sides subclass; the resolves-here-via-ignored-path shape is
   an evidence-divergence channel (verified tree reads bytes the push does not carry).
2. **Same shape via an ABSOLUTE target** (P3-131): committed `abslink -> <abs path of this
   checkout>/real.txt` resolves here (serves bytes), fingerprint clean-tree constant, exit 0;
   dangling at any receiver whose checkout path differs (i.e. all of them).
3. **Live-tree boundedness for both**: the catalog's only two tracked symlinks
   (`…/fail_protected_ledger_symlink/…` and `…/fail_receipts_symlink/…`) both resolve to TRACKED
   targets (measured via index + normpath) — the gap is a class gap, not a live defect.
4. **Turkish I/ı refutation confirmed on the live volume** (P3-130): on this case-insensitive
   APFS data volume, `I`(U+0049)·`ı`(U+0131)·`İ`(U+0130) coexist as THREE distinct inodes
   (35920221/2/3) while the control `touch i` after `I` COLLIDES (1 entry) — the volume folds
   ordinary case and does NOT fold the Turkish pairs. `str.casefold()` is locale-independent
   (`casefold('I')=='i'` regardless of `tr_TR`), so there is no environment-control path on our
   side either.
5. **NFKC distinctness re-confirmed on the live volume** (P3-128): `A`≠`Ａ`(U+FF21) and
   `IV`≠`Ⅳ`(U+2163), 4 entries / 4 distinct inodes — consistent with R12's three-volume sweep.
6. **P3-118 window boundedness verified in source**: the hard-guards step is ONE command
   (`run-all-guards.sh --include-fixtures`) under `timeout_seconds: 2700`;
   `verify-completion.sh` `run_with_timeout` watchdog SIGTERMs at the cap, SIGKILLs after a 30s
   grace, and propagates the exit code → a stalled guard ends in a BLOCK, never a pass. The
   window is bounded and stated; what is missing is per-guard attribution (grep: no per-guard
   timing instrumentation exists in `run-all-guards.sh`).

### Verdicts

- **P2-68 — CLOSE (B).** TD-2026-07-30-(ratchet-threat-model) declares the surface out of scope
  verbatim: "It does NOT defend against an adversary who already controls the execution
  environment — `PATH` wrappers, shell startup files, the user account, the kernel. … A catalog
  gate is a correctness ratchet, not a sandbox," and registers this very row as "gated on
  ax-template ever running in an untrusted-environment context (hosted CI for third-party forks,
  an agent sandbox with an attacker-writable PATH). Until then it would be cost without a threat."
  The row duplicates a shipped decision as open inventory. Reclassified as a trigger-bound
  deferral (P4 discipline); the reopen trigger is the quoted condition, unchanged.
- **P2-71 — CLOSE (C), done-when branch (b).** Blocking every representation mismatch is the
  DEFINITION of push evidence (that tree is not the tree that ships); the cost surfaces as a loud
  refusal with a documented recovery (unset the bits, restore the files, run the release gate).
  The alternative — a labelled, non-refusing "resume" digest — is declined on this catalog's own
  measured grounds: an always-weaker artifact distinguished only by a label is the habituation
  failure recorded at P3-134/[104] ("an advisory that always fires is read by no one") and the
  loud-refusal-over-silent-tolerance principle in the threat-model AMENDMENT. REVERSAL CONDITION:
  a demonstrated fork-receiver workflow where restoring the bits is infeasible — then build (a)
  as a STRUCTURALLY distinct artifact (different filename, different schema, never prints a bare
  digest), not a labelled variant.
- **P2-72 — MUST STAY OPEN.** "Undecidable by inspection" justifies not shipping a heuristic
  scanner; it does not close the row, because the complete remedy is not inspection but
  EXECUTION on a non-aliasing filesystem, and that remedy is CONSTRUCTIBLE today — this project
  already proved the volume (`hdiutil create -fs "Case-sensitive APFS"`, used by the round-10..13
  provers), and a plain Linux ext4 runner (no casefold feature) is byte-preserving, i.e. both
  case- and normalization-sensitive, for free. Closing a row whose remedy is available
  infrastructure would launder an unbuilt job as a logical impossibility. Sharpened done-when:
  (1) one-time baseline NOW — clone HEAD onto a case-sensitive volume (or a Linux host) and run
  R25 full, recording the outcome (this alone converts "undecidable" into a measured baseline);
  (2) standing — an R25 `--case-sensitive-volume` opt-in mode or a release-cadence BLOCKING job.
- **P3-118 — MUST STAY OPEN.** The premise is honest but the window is bounded AND fail-closed
  (measurement 6): delay-then-BLOCK, not a bypass — availability cost, same family as P3-126.
  It stays open because the done-when names real, cheap work: the 103-guard sweep shares ONE
  2700s budget with zero per-guard attribution. Sharpened done-when: instrument per-guard wall
  time in `run-all-guards.sh` → derive observed p99 → wrap each guard in an individual timeout
  `max(k·p99, floor)`, keeping 2700s as the outer aggregate cap.
- **P3-126 — MUST STAY OPEN (should simply be implemented).** Done-when (c) (close by showing
  real trees cannot reach it) is not honestly satisfiable — pathological depth is COMMITTED
  content, so a fork can reach it by construction; only the availability grade (fail-closed,
  never a bypass) is confirmed. Done-when (a) is a trivial fix: build each prefix key as
  parent-key + `/` + component (O(d) instead of O(d²)), symmetric in both implementations.
- **P3-127 — CLOSE (D).** No Windows host; both preconditions ((1) NTFS serves the trailing-dot/
  space spelling as one file, (2) git for Windows can index such entries) unverifiable here; the
  row already refuses unverified fold changes — the same no-anchorless-expansion principle R12
  recorded. Platform-gated done-when preserved verbatim, plus one sharpening for the platform
  holder: measure whether `core.protectNTFS` (default-on for Windows git) already refuses the
  checkout, which would bound the receiver-side risk before any fold work.
- **P3-128 — CLOSE (D).** Refuted on every locally constructible target filesystem (R12's three
  volumes) and re-confirmed on the live volume today (measurement 5). The only residue — OpenZFS
  `normalization=formKC/formKD` — is a non-default creation-time option with no live ZFS
  available here; R13 already promoted it into the concrete reopen condition, and the astral gap
  is closed by argument (R13, TN1150 surrogate-identity). Reopen trigger: measured single-inode
  `Ａ`/`A` on a formKC dataset, or a server-side-folding SMB/NFS mount. Until then a fold
  expansion is exactly the anchorless expansion R12 refused.
- **P3-129 — CLOSE (D).** 8.3 generation is per-volume policy (`fsutil 8dot3name`), the premise
  of the alias family (two index entries / one file) is itself unconfirmed for short names (they
  are a lookup namespace, not a spelling git records), and no Windows host exists here. Platform-
  gated done-when preserved: measure (1) whether the two-entries/one-file shape is constructible
  at all, (2) whether `core.protectNTFS` already refuses `GIT~1`-class names, before touching any
  fold.
- **P3-130 — CLOSE (A).** The row scheduled its own closure ("close at the next grooming with the
  measurement record") and the grooming measurement is now taken on the LIVE volume with a
  positive control (measurement 4): the volume demonstrably folds ordinary case yet serves the
  Turkish pairs as distinct inodes; exFAT's stored up-case table does not fold U+0131 (R12); our
  key is locale-independent. Reopen condition unchanged: a real filesystem whose VOLUME TABLE
  (not process locale) serves the pair from one inode.
- **P3-131 — MUST STAY OPEN.** The alias-census exclusion itself is a correct design boundary
  (an absolute target has no recorded spelling to compare) — but the row's "portability is a
  separate defect" underclaims: measurement 2 shows the absolute-target class CONTAINS a
  false-green shape (resolves here, clean-tree constant, exit 0, dangling at every receiver),
  which is this gate family's own theme. It cannot be closed as fork-receiver autonomy while that
  shape is unhandled; fold it into the P3-132 parity check below.
- **P3-132 — MUST STAY OPEN.** The stated rationale is REFUTED for a reachable shape
  (measurement 1); P3-134 explicitly routed untracked-intermediate outcomes to this row, so the
  row's class includes {dangling-both-sides, resolves-here-via-untracked/ignored,
  absolute-into-checkout}, and the latter two are evidence-divergence, not hygiene. Live tree is
  currently clean (measurement 3). Sharpened done-when: a per-tracked-symlink RECEIVER-RESOLUTION
  PARITY check classifying the resolved target as tracked-content (pass) /
  untracked-or-ignored-content (evidence divergence — decide block vs. record ON THE TRUE
  PREMISE) / absolute-into-checkout (same) / dangling-both-sides (hygiene advisory only, where
  R13's "evidence does not lie" argument actually holds), with a false-positive control for the
  legitimate build-artifact-link shape R13 named. If the non-blocking choice is kept for the
  divergence shapes, it must be re-recorded as an ACCEPTED FALSE-GREEN CHANNEL, not as "the
  evidence does not lie."

Convergence accounting: 6 rows leave the open inventory (1 refuted, 1 out-of-scope duplicate of
a shipped decision, 1 design boundary with reversal condition, 3 platform-unverifiable converted
to platform-gated done-whens); 5 rows stay open because each names work this project can do —
two of them (P3-131/132) now carry refuting measurements that RAISE their honest urgency.

- Commits: (this commit — final-4 wave, Lane E backlog adjudication)

## TD-2026-08-01-(template-lock-boundary-and-manifest-content) — a manifest that is only checked to EXIST, and a lock whose transaction outlives the job

- Date: 2026-08-01
- Status: accepted
- Trigger: final-4 wave, Lane C (BACKLOG P2-59 / P2-60 / P2-62 / P2-61 / P3-106).

### P2-59 — the guard read the filename, not the file

`trio_integrity_guard.sh` asserted `blueprints/<domain>-ui-manifest.yaml` EXISTED and stopped
there. Everything the manifest CLAIMS — which backend operation each surface binds, which spec
item each policy block backlinks, which page/view file the render boundary governs — could drift
to nonsense while the guard printed PASS. Five checks now resolve those claims
(`MANIFEST_UNPARSEABLE`, `MANIFEST_UNRESOLVED_OPERATION_ID` / `MANIFEST_OPERATION_ID_IN_FRONTEND_ONLY`,
`MANIFEST_DANGLING_SPEC_ITEM`, `MANIFEST_MISSING_ROUTE_SOURCE`, `MANIFEST_ROUTE_NOT_IN_CONTRACT`).

They are STRUCTURE-CONDITIONAL by necessity — the older manifests (auth, crud, payment,
notification, …) use a different shape and declare none of these keys — so the failure mode is
vacuity, not false positives. That is answered where it can be: the live catalog sweep asserts
shrink-only census floors (5 operation ids / 30 spec_item backlinks / 6 route sources against an
observed 5 / 36 / 6). Deleting the manifest content the checks read is now a FAILING run, not a
green one. Fixture roots are exempt from the floors; they are minimal by construction.

Route SOURCES resolve LITERALLY, not by glob: Next.js segments (`[id]`, `(admin)`) are glob
metacharacters, and a glob-based existence check would silently match the wrong file.

### P2-60 — DECISION: align with the production REQUIRES_NEW pattern, but move BOTH boundaries

`templates/backend/scheduled-task` acquired its pessimistic row lock inside the caller's
transaction, so the lock was held for the whole job and for every remaining task in the poll
loop; a competing node BLOCKED on the row instead of returning `false`, inverting
`LockingPolicy`'s own "a lost race returns false, never waits" contract.

Making the acquire `REQUIRES_NEW` **alone** would NOT have been sound, and this is the part worth
recording. In this template the lock state lives ON the domain row
(`scheduled_tasks.lock_holder` / `locked_at`), not in a separate table as the production SPI's
`task_locks` does. An outer transaction would therefore still hold a managed copy of the same row
whose `@Version` (BaseEntity) the inner commit had bumped, and the end-of-job `lastRunAt` write
would fail the version check. So: acquire and release are `REQUIRES_NEW`, the two scheduling
entry points are `NOT_SUPPORTED`, and `recordLastRun` re-reads the row in its own transaction.

RESIDUAL, recorded rather than inherited: because the lock key IS the domain row, a fork calling
the execute path from inside its own transaction re-opens both hazards and can additionally
self-deadlock (a `REQUIRES_NEW` acquire waiting on a row lock the suspended outer transaction
holds). The production SPI is immune because of its key shape; a fork needing transactional
callers should adopt that shape first. This is stated in the template's own meta block.

### P2-62 — DECISION: harness, not a recorded limit

The row offered "build a template verification harness" or "state the structural limit". The
harness was chosen because it turned out to be reachable with NO new build infrastructure: a
JUnit class in the existing `backend/src/test` tree, `@Tag("SCHEDULED_TASK")` so the existing
`testScheduledTask` task runs it, carrying an executable COPY of the template's
`tryAcquire`/`release`/`isLockHeld` against hand-rolled collaborators. A copy alone would rot
within one edit, so the same test re-reads BOTH files from disk and asserts the copy is
character-identical after whitespace normalisation — comments included, because a comment that
outlives the code it describes is the same rot in a slower form.

Boundary, stated: this proves the LOGIC, not the Spring wiring. No container runs, so the
`REQUIRES_NEW` propagation and the JPA `@Lock(PESSIMISTIC_WRITE)` are NOT exercised — an
in-memory map has no row locks. What WOULD verify those is a fork-side integration test against a
real datasource, which the catalog cannot run for a skeleton whose package is `com.example.app`.

### P2-61 / P3-106 — corrections, not decisions

`specs/storage-reconciliation-l0.yaml#RECON-IDEMPOTENT-001` still described the scheduled-task
lock as `FOR UPDATE SKIP LOCKED` after (P2-48) had corrected that claim itself; it now states the
two-branch semantics (row-present = pessimistic `SELECT ... FOR UPDATE`; row-absent = PK
arbitration) with the reason SKIP LOCKED is wrong here. `applyOptimisticDelete` decremented
`totalElements` even when the id was not on the cached page — `Math.max(0, …)` hid the arithmetic
but not the lie — and now decrements only what the filter actually removed.

- Commits: (this commit — final-4 wave, Lane C)

---

## P2-58 / P2-57 / P3-103 / P2-63 — 2026-08-01 evidence-freshness wave (final-4 Lane A)

- Status: ACCEPT
- Date: 2026-08-01
- Maintainer: final-4 Lane A (final4-closeA)
- Evidence: `practices/upstream/_FETCH-RECEIPTS.yaml` rows r056–r158 (fetch + assembly)
- Re-evaluation trigger: any of the refreshed hosts changing shape, or the next time_decay
  cohort (2026-10-12 / 2026-10-13) coming due

### Mechanism, so a later reader can tell a refresh from a timestamp edit

96 manifest ids were re-fetched through the committed pipeline
`practices/scripts/snapshot-extract.sh` (curl → deterministic HTML→text extractor, no model in
the loop). No `fetched_at` was bulk-touched: EVERY attempted URL — including every failure — has
a `kind: fetch` row, and every id whose body changed has a `kind: assembly` row binding the
body's sha256 to the fetch rows it was built from. Bodies were refreshed APPEND-ONLY: the prior
body is preserved byte-for-byte and the unmodified extractor output is appended below it, so the
185 existing rule quotes and 74 template anchors stayed verbatim (0 findings) while the manifest
digests became true.

### Two deliberate deletions, and one that was refused

`kisa-identity-verification-2026-05` was DELETED from `practices/upstream/_MANIFEST.yaml`. It was
a body-less plan registration carrying a rolling-nibble placeholder sha
(`b4c5d6e7f8a9b0c1…`) — a fabricated digest for a file that never existed — and `www.kisa.or.kr`
WAF-blocks a plain client on every path including `/robots.txt` (HTTP 400, receipt r067). Nothing
in `practices/rules`, `practices-react/rules` or `templates/**` cited it; the only references are
prose in three superseded PRD drafts. Keeping a fabricated record solely to avoid a time_decay RED
would be gaming our own gate.

`pipa-article-24-2026-05` was NOT deleted, and the reason is recorded because it is the more
interesting half. It is cited: `templates/DECISIONS.md[TD-2026-05-18-031]` carries
`upstream_id: pipa-article-24-2026-05` inside an ADR evidence block, and `evidence_guard`'s
`entry_kind()` returns `upstream_id` whenever that key is present — `source_type: external`
alongside it does not downgrade the resolution. Measured, not assumed: dropping the entry produced
`VIOLATION templates/DECISIONS.md[TD-2026-05-18-031]: evidence[0] upstream_id=… is not registered
in any upstream/_MANIFEST.yaml`. Its source is also unreachable to the committed extractor —
law.go.kr serves an iframe/overload shell to plain curl, and the article text is in none of
`/법령/…`, `LSW/lsInfoP.do`, `lsSc.do`, `DRF/lawService.do`, elaw.klri.re.kr or Wayback (receipt
r068). So the entry stays stale by choice, and `practices` goes time_decay RED on 2026-08-17
because of it. That is a stated cost, not an oversight.

### Residual policy for the allowlist

`practices/evals/manifest_snapshot_integrity_allowlist.yaml` went 71 → 63 → 49 → 1. The single
survivor, `practices::iso-4217`, keeps its entry because iso.org answers a plain client with an
HTTP 403 Cloudflare interstitial (58 bytes, receipt r117) — a bot wall, not a document. Its
`reason:` now records url, status, bytes, date and receipt id, so the residual is evidence of an
unreachable host rather than an assertion inherited from the wave that created the list.

### What the refresh revealed about the old records

At the pre-wave anchor, 21 manifest records carried rolling-nibble placeholder shas, and five of
those shas were shared by two or three different entries. A sha256 is a function of the bytes, so
most of them were never computed from any file. Twenty are now true whole-file digests; the
twenty-first is the deleted stub.

### Boundary, stated rather than hidden

Append-only refresh keeps the guards green by construction, and that is the mechanism working as
designed — but "the quote is in our snapshot" is a weaker property than "the quote is on the live
page". Testing the stronger one, 26 citations anchored to refreshed ids do NOT appear on the
2026-08-01 page in three classes: authored digest prose that was never page text, a
wrong-granularity source (an index page that does not contain the cited sentence), and page text
that genuinely moved. That is registered as its own backlog row rather than silently re-anchored
here.

- Commits: e25f598, ce85dc5, and this commit (final-4 wave, Lane A)

## TD-2026-08-01-Lane-G — P2-73 B-class anchors (additive ratchet) + P2-72 standing job

- Status: ACCEPT
- Date: 2026-08-01
- Maintainer: Lane G (backlog-closure wave, branch final4)
- Evidence: template/rule frontmatter `evidence:` blocks; the live and throwaway-clone
  measurements recorded below (the anchor-ratchet exit codes are live-root-bound and NOT
  fixture-coverable, so this record is their evidence)
- Rationale: Lane A's B-class prescription was tested rather than executed, and it failed
  both halves of the test: the defect it describes is not present in the body that actually
  resolves, and the edit it prescribes is refused by the ratchet as a substitution.
- Re-evaluation trigger: any change to `resolve_snapshot_any_catalog`'s catalog order, or a
  second upstream_id registered against different pages in the two catalogs.

### Which snapshot a templates/** citation actually resolves against — MEASURED

`evidence_quote_spotcheck_guard.resolve_snapshot_any_catalog` tries `practices/` and only
then `practices-react/`. That is a reading of the loop; the measurement is stronger. In a
throwaway clone at 4b161fd the practices-side copy was renamed away and the protected sweep
went from `64 file(s), 68 anchor(s), 0 finding(s)` / exit 0 to **exit 1 with 4 findings** —
`TEMPLATE_QUOTE_NOT_IN_SNAPSHOT` + `TEMPLATE_SECTION_NOT_IN_SNAPSHOT` for skeleton.tsx and
sonner.tsx against the practices-react copy. So `practices/upstream/` is what resolves.

That matters because ONE upstream_id names TWO DIFFERENT PAGES:

| catalog | `wcag-22-techniques-2026-05` source | carries the SC 4.1.3 sentence? |
|---|---|---|
| `practices/upstream/` | `…/WCAG22/Understanding/status-messages.html` (receipt r158, HTTP 200) | YES, verbatim, under a `## Success Criterion (SC)` heading |
| `practices-react/upstream/` | `…/WCAG22/Techniques/` — the INDEX (receipt r102, HTTP 200) | NO |

Lane A's B-class finding ("the cited page is an index that does not contain the sentence")
was measured against the react-side body. The body that RESOLVES is the Understanding page,
and it does contain it. The live protected sweep is green because of that, not because the
gate is asleep.

### Why the prescribed respell was refused — MEASURED, three ways

At a CONSTANT count of 64 (the case the subset check exists for):

| edit | result |
|---|---|
| ledger rows + `# require:` respelled, guard frozenset untouched | exit 2 `PROTECTED_LEDGER_REQUIRED_IDENTITY_MISSING` (both identities named) |
| both in-tree surfaces respelled, COMMITTED in a throwaway clone | exit 5 `PROTECTED_IDENTITY_REMOVED` vs origin/main — "the pin set may only GROW" |
| guard file edited but uncommitted | exit 8 `RATCHET_TOOLCHAIN_MODIFIED` — the ratchet refuses to certify from an implementation that exists only in someone's tree |

So the honest move is ADDITIVE: each file gains a SECOND anchor stating the same normative
sentence against `wcag-2-2` (the WCAG 2.2 Recommendation, practices-react, tier 1), verified
as a substring of a NON-HEADING prose block. Nothing leaves the fatal set; the claim stops
depending on which catalog wins the resolution race. Five surfaces moved together, 64 → 66.
The additive half-edits were measured too: ledger-only → exit 3 `PROTECTED_LEDGER_CENSUS_UNEQUAL`
(66/66/66/64/64 printed), guard-only → exit 2 `PROTECTED_LEDGER_FLOOR`. Final live state:
all five surfaces == 66, `66 file(s), 70 anchor(s), 0 finding(s)`, exit 0; all 33 registered
`evidence_quote_spotcheck` invocations at their expected exits.

### The third file, disposed of rather than carried

`practices/rules/background-poll-must-show-refresh-state.md` was flagged as the same B-class
defect on the practices side, where `wcag-2-2` does not exist. It is NOT defective: a rule
resolves within its own catalog, and the practices-side `wcag-22-techniques-2026-05` IS the
Understanding page, which contains both its quote ("status messages can be programmatically
determined through role or properties") and its section ("SC 4.1.3 Status Messages (Level
AA)") verbatim. No WCAG 2.2 body needs fetching into `practices/upstream/`, and no re-anchor
is warranted. What remains is the NAMING hazard — an id called "techniques" whose
practices-side body is the Understanding page, and whose react-side body is the index — which
is registered as a residual rather than renamed here (renaming an id touches every citation
that uses it and is its own wave).

### P2-72 — the standing job, and what it does not close

`practices/scripts/ax-case-sensitive-sweep.sh` makes Lane F's one-time baseline repeatable:
case-sensitive APFS volume → probes → clone of a committed revision → `run-all-guards.sh
--include-fixtures` → detach with a VERIFIED leak check. First standing run (rev cd9210e):
**357 passed / 0 failed, exit 0, 932 s** — the same 357/357 Lane F measured by hand.

Its own leak check was the interesting part. The first implementation asked
`hdiutil info | grep -F "$MNT"`, and MEASURED DURING THAT RUN that returns 0 for a volume
that IS attached: hdiutil prints the resolved `/private/var/...` spelling while `mktemp -d`
produced `/var/folders/...` with a doubled slash. A "detached cleanly" claim from that check
would have been unearned. It now scans BOTH `hdiutil info` and `mount` for BOTH spellings and
is ARMED on every run by a positive control — if the predicate cannot see a volume that is
demonstrably attached, the run exits 5 instead of reporting a clean detach.

Honest remainder: **nothing schedules it.** It is invocable, registered in CLAUDE.md's
enforcement-surface table as periodic/manual, and proven to run — but no hook, no CI and no
R25 step calls it, and its coverage statement excludes the gradle and npm steps (no JDK or
node_modules is provisioned on the volume) and unicode normalization (the volume folds
NFC/NFD — measured, printed, not papered over).

## TD-2026-08-01-Lane-H — P3-137 cross-catalog id collision (guard shipped, rename refused) + P2-72 boundary argument tested and REJECTED

- Status: ACCEPT (P3-137 closed) / P2-72 stays OPEN
- Date: 2026-08-01
- Maintainer: Lane H (backlog-closure wave, branch ax-template-final)
- Evidence: the throwaway-clone measurements and guard exits recorded below; the new guard
  [107] and its four fixtures; `.github/workflows/` as committed
- Re-evaluation trigger: any change to `resolve_snapshot_any_catalog`'s catalog order; any
  sanctioned retirement path for an id whose receipt would otherwise orphan; a Linux CI job
  that actually runs the guard suite

### P2-72 — the boundary argument was tested and it does not hold

The judgement offered for closing P2-72 as a design boundary was: ax-template ships probes,
not schedules; scheduling is fork-receiver autonomy; therefore "nothing schedules it" is the
correct end state. What the repo actually says is narrower. CLAUDE.md: *"Fork받은 팀의 정책을
skill이 강제 ❌ … Git branch / PR / merge 정책 … catalog 품질을 넘는 CI gate"*, and
*"sentinel CI는 catalog quality probe로만 제공. merge gate 여부는 fork받는 팀이 결정"*. That is
a rule about **not imposing gates on fork-receivers**. It says nothing about ax-template
scheduling its own probes — and the repo demonstrably does, four times:

| workflow | trigger | posture |
|---|---|---|
| `practices-drift.yml` | `cron: "0 6 * * 1"` weekly | opens a PR when a snapshot is > 30 days old |
| `practices-portability.yml` | `cron: "0 7 * * 1"` weekly | `continue-on-error: true` — advisory, never blocks |
| `practices-chub-feedback.yml` | `cron: "0 7 1 * *"` monthly | ecosystem probe |
| `practices-sentinel.yml` | push/PR | *"Any guard failing → CI red, blocking merge"* |

`practices-portability.yml` is precisely the shape P2-72 needs — a weekly advisory cron on
`ubuntu-latest` — so the objection "we do not schedule things" is not available. **P2-72 stays
OPEN**, and the honest closure is cheaper than the row assumed: a Linux runner's filesystem is
case-sensitive AND byte-preserving, i.e. it covers BOTH the case half and the normalization
half that `hdiutil`'s volume cannot (that volume folds NFC/NFD — measured by Lane G, printed by
the script on every run). What is **not** measured: whether the 107-guard suite is clean on
Linux. Every guard in this tree has only ever executed on macOS, and at least four scripts
carry `stat -f` (BSD spelling). An attempt to measure it here failed for environmental reasons
— Docker on this machine did not return output from a trivial `ubuntu:24.04` probe within
repeated 2-minute windows — so shipping a workflow on the strength of an unmeasured premise was
refused rather than guessed at. The invocation point is now documented where a maintainer will
find it: `practices/MAINTAINER.md` §5d ("Periodic jobs — nobody schedules these, you do"),
which lists both unscheduled jobs, their cost, what the sweep does NOT cover, and the fact
above that this repo does schedule its own probes.

### P3-137 — the rename is refused on BOTH sides, measured first-hand

Prescription (1) was to rename the misnamed id. It cannot be done, and the refusal is a
property of the provenance system rather than an accident. In throwaway clones of cf42258, with
nothing edited but the snapshot filename and the manifest `id`:

| edit | result |
|---|---|
| react side: `git mv …/wcag-22-techniques-2026-05.snapshot.md → …-index-…` + manifest id | `manifest_snapshot_integrity_guard` **exit 2 `RECEIPT_ORPHANED`** — "has an assembly receipt but no `wcag-22-techniques-2026-05.snapshot.md` on disk" |
| practices side: same edit to `wcag-22-understanding-status-messages-2026-05` | **exit 2 `RECEIPT_ORPHANED`** *and* `evidence_quote_spotcheck --templates-only-protected` **exit 1**, `66 file(s), 70 anchor(s), 4 finding(s)` — the two pinned template anchors stop resolving |
| (Lane G, prior) count-preserving RESPELL of a pinned identity | exit 5 `PROTECTED_IDENTITY_REMOVED` — "the pin set may only GROW" |

`_FETCH-RECEIPTS.yaml` is append-only and binds an id to the bytes a fetch produced, so an id is
**part of a provenance record, not a label**. Re-spelling it is the "manufacture a provenance
claim" move the receipts chain exists to prevent; deleting the react-side registration orphans
its receipt for the same reason; and the guard offers no sanctioned retirement path (searched:
no tombstone/supersede branch exists). The practices-side id is additionally pinned by the
protected-anchor ledger, so it is refused twice over. **Nothing was renamed. The naming
mismatch — an id called "techniques" whose practices-side body is the SC 4.1.3 Understanding
page — is permanent under current mechanics, and saying so is more useful than a rename that
would have to fabricate a fetch.**

### P3-137 — what did ship: guard [107], and why "same source" rather than "no sharing"

`cross_catalog_upstream_id_collision_guard.sh` forbids one `upstream_id` registered in both
catalogs against two different `source` URLs. Live census, measured at introduction: 61
practices ids × 42 practices-react ids share exactly **4** — `next-themes-2026-05`,
`stripe-billing-2026-05`, `toss-billing-2026-05` carry an **identical** source on both sides
(one upstream fact legitimately cited by both catalogs), and only `wcag-22-techniques-2026-05`
names two pages. Forbidding shared ids outright would therefore block three honest ids to catch
one dishonest one; the defect is not name reuse but a name **meaning two things**.

The one live collision is GRANDFATHERED in a frozen frozenset literal in the guard — because it
provably cannot be dissolved (above), and a guard that ships red is a guard that gets ignored.
Two properties stop the exception becoming padding: NON-REDUNDANCY fails a grandfathered id
that is registered on both sides and now AGREES (any root), and additionally one that is not
shared at all any more (live root only — a fixture root has no reason to carry this repo's ids,
and requiring it would make the exception's own test untestable; this branch split was forced by
the fixtures, which caught the first formulation firing on every unrelated root). The guard also
blocks `SHARED_SNAPSHOT_UNREGISTERED` — a body present in both `upstream/` dirs while one
manifest never registered it — because resolution is by FILE and "the sources cannot be compared
at all" is worse than "they disagree". Non-vacuity: `LIVE_MIN_SHARED = 4` on a live root (exit 2),
so a collapsed census cannot report a green "no collisions" about an empty set.

Fixtures: `pass_shared_ids_agree` 0 · `fail_same_id_two_sources` 1 · `fail_stale_grandfather` 1 ·
`fail_shared_body_unregistered` 1. The collision fixture is registered with [87]
`fixture_kill_proof` (`anchor: 'a != b'` → `neuter: 'False'`), floors ratcheted **71 → 72** in
both halves (`fixture_kill_manifest.yaml: min_items` and the guard-pinned `LIVE_MIN_ITEMS`).
Headline reconciliation: guard files 106 → 107 on disk, bumped in README ×3, CLAUDE.md ×2,
`skills/ax-transform/SKILL.md` ×2 (`doc_headline_count_guard` PASS: 233 Java · 102 React · 15
ESLint · 25 L4 · 107 guards). Two unguarded CLAUDE.md prose counts that had already rotted were
corrected in the same pass: `*_guard.sh` 105 → 107 (practices/evals 103 → 105) and the
enforcement table's "95 live guards" → 102 (`run_guard "*/live"` census).

### Lane H addendum — an existing meta-guard caught the new guard, and the verification numbers

The first version of [107] carried a hand-rolled line reader as a PyYAML fallback. [95]
`pyyaml_preflight_coverage_guard` caught it on the first full sweep: under simulated PyYAML
absence the fallback read **46** practices-react ids where PyYAML reads **42**, and the guard
still printed PASS — a green verdict about a census that was not the tree's. That is the same
unsound-static-parse class this catalog has repeatedly burned itself on, arriving in a guard
written to prevent a truth defect. The fallback was deleted rather than fixed: [107] now fails
closed on PyYAML absence like the other 28 dependent scripts (`exit 2`, no verdict without a
real parser). Recorded because the catch is the interesting part — the meta-guard was doing
exactly its job on a same-wave addition.

Verification at `c19965b` (post-fix), every number as printed:

| check | result |
|---|---|
| `run-all-guards.sh --include-fixtures` | **362 passed / 0 failed, exit 0** (357 → 362: the five new [107] invocations) |
| [107] live | exit 0 — 4 shared ids across 61 practices + 42 practices-react, 3 agree, 1 grandfathered, 0 new |
| [107] fixtures | pass 0 · same_id_two_sources 1 · stale_grandfather 1 · shared_body_unregistered 1 |
| [95] `pyyaml_preflight_coverage` | exit 0 |
| [87] `fixture_kill_proof` | exit 0 — **72 items all non-vacuous**, floor 72 (was 71) |
| `evidence_quote_spotcheck` — all **33** registered invocations | every one matched its expected exit; protected census **66 file(s) / 70 anchor(s) / 0 finding(s)**, five surfaces == 66, ratchet floor 66 → 66, pin set 66 → 66 (superset) — **not shrunk** |
| `evidence_guard` practices / practices-react | exit 0 / exit 0 |
| `time_decay_guard` practices / practices-react | exit 0 / exit 0 |
| `manifest_snapshot_integrity` live | exit 0 — 92 ids checksummed, 90 through the full chain, 0 changed vs origin/main |
| `manifest_snapshot_integrity` fixtures (9) | 1/2/2/2/2/2/1/0/0 — all as registered |
| `doc_headline_count_guard` | exit 0 — 233 Java · 102 React · 15 ESLint · 25 L4 · **107 guards** |

Residual, stated: [107] compares the manifests' `source` field. Two catalogs could register one
id against the SAME url and still hold different BODIES (a re-fetch of a page that changed).
That is not this guard's subject — it is what `manifest_snapshot_integrity`'s file←body←receipt
chain already covers per catalog — but the two guards only meet at the id, so a cross-catalog
BODY divergence under one url is unowned by either. No instance exists today (the three
agreeing ids are also byte-consistent in the census printed by `--show`); it is named here so
the boundary is not mistaken for coverage.

---

## TD-2026-08-01-Lane-I — the Linux advisory cron: the portability objection re-measured, and an unmeasured premise given an instrument instead of a verdict

- Status: ACCEPT — `.github/workflows/practices-case-normalization.yml` shipped (advisory).
  P2-72 and P3-138 both CLOSE **as mechanism**; the Linux *result* stays explicitly unmeasured.
- Date: 2026-08-01
- Maintainer: Lane I (branch `lane-i-linux-advisory`)
- Evidence: the greps and the container rehearsal recorded below; the workflow as committed;
  `.github/workflows/practices-portability.yml` as the shape precedent
- Re-evaluation trigger: the first scheduled or dispatched run of the new workflow — whatever
  it reports is the first real measurement and must be written back here; also any proposal to
  promote the job from advisory to blocking, which requires that baseline first

### 1. The stated reason for not shipping was half wrong, and the half that was wrong is checkable

Lane H declined to ship this workflow and recorded two reasons: (a) the guard suite has only
ever run on macOS, so Linux cleanliness is unmeasured; (b) *"가드 4개+가 BSD `stat -f` 사용"* —
i.e. the suite is presumed to contain BSD-only code that would fail on GNU coreutils.

Reason (b) does not survive a grep. Every executable site that spells `stat -f` in this tree
already carries a GNU `stat -c` fallback immediately after it, in the shape this very wave
introduced (P2-67, *BSD then GNU, BLOCK if neither answers*). Measured at `54463a9`, per file,
as `(count of "stat -f", count of "stat -c")`:

| file | `stat -f` | `stat -c` |
|---|---|---|
| `.githooks/pre-push` | 2 | 2 |
| `.githooks/pre-push-lib.sh` | 1 | 1 |
| `practices/evals/completion_checklist_recency_guard.sh` | 1 | 1 |
| `practices/evals/evidence_quote_spotcheck_guard.sh` | 1 | 1 |
| `practices/evals/manifest_snapshot_integrity_guard.sh` | 1 | 1 |
| `practices/evals/run-all-guards.sh` | 1 | 1 |
| `practices/scripts/ax-prove-evidence-gate-blocks-agent.sh` | 1 | 1 |
| `practices/scripts/ax-prove-gate-blocks-agent.sh` | 1 | 1 |
| `practices/scripts/ax-prove-helper-injection-blocked.sh` | 1 | 1 |
| `practices/scripts/ax-prove-hermetic-runtime.sh` | 11 | 11 |
| `practices/scripts/lib/release_anchor.sh` | 1 | 1 |
| `practices/scripts/verify-completion.sh` | 1 | 1 |

**Files using one spelling without the other: zero.** The only unpaired mentions in the tree are
prose — `docs/BACKLOG.md` (the P2-72 and P3-138 rows, which state the objection) and one
sentence in this file. Both fallback halves are also *fail-closed*, not best-effort: where the
value is used, an unanswered `stat` yields the `HERMETIC_TEMPDIR_UNVERIFIABLE` refusal rather
than a permissive default, so a platform where **neither** spelling answers blocks instead of
proceeding. No gaps were found and therefore none were fixed.

The lesson is narrow and worth stating plainly: **a portability objection is a claim about the
tree, and claims about the tree are grep-checkable.** Lane H's own wave had already closed the
gap it went on to cite as a blocker. Carrying a fixed problem forward as a live reason is the
same failure mode as a stale absence-assertion — the belief outlived the code.

### 2. Reason (a) is genuine, and an advisory cron is the honest instrument for it

What remains true is that **nobody has ever run this suite on Linux.** That is a real unknown,
and the catalog's own discipline forbids two responses to it: do not assert cleanliness
(unmeasured), and do not ship a *blocking* gate on the assumption (an unmeasured premise
promoted to a merge gate is exactly the false-green shape this repo keeps punishing).

The third response is the correct one and the repo already owns the pattern:
`practices-portability.yml` is a weekly `ubuntu-latest` cron with `continue-on-error: true`. An
advisory job **measures without gating**. It cannot produce a false green, because it produces
no verdict at all — only a report a human reads. It also imposes nothing on any fork-receiver,
so the autonomy boundary (which Lane H had already tested and rejected as a closure argument)
is untouched by construction.

Linux rather than a macOS cron, for a reason that is not cost: `hdiutil`'s
`Case-sensitive APFS` volume **folds NFC/NFD — measured** — so the macOS script can never sweep
the normalization half no matter how often it runs. ext4/overlayfs is case-sensitive *and*
byte-preserving, so one Linux job covers both halves. That is why P2-72 and P3-138, filed
separately because their remainders were unrelated (operational vs. capability), are answered by
the same artifact.

### 3. Premise first, and loudly — the design that keeps this from becoming its own false green

A sweep whose whole point is "run where the filesystem does not alias" is worthless if the
runner's filesystem quietly aliases. GitHub's `ubuntu-latest` is ext4 today; that is an
assumption, and this catalog does not let assumptions stand in for measurements. So the **first**
step of the job is a capability probe, run **inside the checkout** (the filesystem the guards
will actually touch, not `$RUNNER_TEMP`):

- creates `A` and `a` and requires **two distinct inodes**, printing `%d %i` for each;
  (dirents are counted with `shopt -s nullglob` + a glob, never `ls | grep` — shellcheck SC2010,
  caught by `actionlint` and fixed before commit; `actionlint` over all five workflows then
  exits **0**)
- creates NFC `café` (`63 61 66 c3a9`) and NFD `cafe`+U+0301 (`63 61 66 65 cc81`) and requires
  **two distinct inodes**, printing the same;
- then deletes its own directory and requires `git status --porcelain` to be empty — the suite
  itself asserts that, so a probe that dirtied the tree would fabricate a failure.

If any assertion fails the job **does not run the sweep** and writes
*PREMISE NOT ESTABLISHED — no measurement was taken* into the job summary, plus a
`::error::` annotation. It does not skip quietly and it does not report a pass. A pass measured
on a folding filesystem is worse than no run, because someone would cite it.

The guard log is written to `$RUNNER_TEMP`, never next to the checkout, for the same reason:
`run-all-guards.sh` and `midrun_tree_mutation_guard.sh` both assert a clean porcelain, so a log
file in the workspace would be a self-inflicted failure. Each `run:` block turns `-e` **off**
deliberately and does its own exit accounting, so a failing probe renders as a failure rather
than as a step that aborted before it could write its outputs.

The summary publishes the `Total: N passed, M failed` line and every `FAIL [` line, and prints
its own exclusions every run: **gradle steps, the npm step, and therefore R25 as a whole.** No
JDK and no `node_modules` are provisioned, on purpose — the first run should answer one question
cleanly rather than three questions muddily.

### 4. What was measured here, and what was not

A local rehearsal WAS obtained, which is more than Lane H got (its four docker attempts returned
no output). `docker run ubuntu:24.04` with `git python3 python3-yaml curl jq unzip`, cloning
`54463a9` into the container's own overlayfs and running
`bash practices/evals/run-all-guards.sh --include-fixtures`:

| what | measured |
|---|---|
| filesystem probe (case) | `entries=2`, distinct inodes — **case-SENSITIVE** |
| filesystem probe (normalization) | `entries=2`, distinct inodes — **byte-preserving** |
| `run-all-guards.sh --include-fixtures` | **358 passed / 4 failed, exit 1**, n=362 invocations, 7m24s wall |

The same probe block, run on this Mac's ordinary APFS as a NEGATIVE control, exits **1** with
`case: entries=1` / `norm: entries=1` and writes `case_sensitive=no` /
`normalization_sensitive=no` to `$GITHUB_OUTPUT` — i.e. the probe is not vacuous, it discriminates,
and (because `-e` is off) a failed probe still renders as a measured *no* rather than as `unknown`.

The four failures are worth naming, because their character is the whole argument for shipping
this advisory rather than a blocking job:

- `vacuity_class_proof/live` — runs a scoped `./gradlew pitest`; **no JDK in the container**
  (`which java` → nothing). Toolchain absence, not portability.
- `fixture_kill_proof/live` and `pyyaml_preflight_coverage/live` — both exercise surfaces that
  reach the R25 toolchain preflight; the container had no JDK, no node and no `yq`. Plausibly the
  same cause, **not confirmed**.
- `hermetic_runtime/inherited_runtime_blocked` — **this one is not a toolchain artifact and it is
  the interesting result.** Several of its cases refuse to run with
  *"VIOLATION: premise broken (symmidslash / symmidsub / symmidnfd): this class needs a committed
  link whose target reaches a tracked path THROUGH an aliased spelling of a recorded one. Without
  the alias … the case measures the round-13/14 refusal instead."* The prover's own attack cases
  were authored on a **folding** filesystem and require folding to construct their premise; on
  ext4 the alias simply does not resolve, so the prover honestly reports that it could not set up
  the false-green it exists to demonstrate. Its `(AD)` fold-parity census still ran clean (8517
  inputs, 0 disagreements) and its simulated `(AE)`/`(AI)` controls still discriminate.

So the first thing a non-aliasing filesystem finds is **a proof harness that is premised on
aliasing** — not a defect in the guarded code. That is a genuine portability finding and it is
also exactly the kind of finding that would have been a catastrophe as a merge gate: a blocking
job would have gone red on day one over a premise mismatch, taught everyone to ignore it, and
proven nothing. Advisory is not timidity here; it is the correct instrument for a first
measurement. Fixing (or explicitly exempting on non-aliasing filesystems) the three
`symmid*` cases is follow-up work this lane does not do and does not claim.


**This is a rehearsal, not the runner.** A container on a developer's Mac differs from
`ubuntu:24.04` on GitHub in filesystem driver, tool versions, git version, locale and network
posture. It raises confidence; it is not the measurement. The measurement is the first
scheduled or dispatched run, and until that exists **nobody may write that the guard suite
passes on Linux.**

### 5. Closure judgement, stated precisely

Both rows asked for a mechanism, and each row's stated remainder is now answered:

- **P2-72** — remainder was *"아무도 스케줄하지 않는다"* (operational). Something now does:
  a weekly cron plus `workflow_dispatch`. **CLOSED.**
- **P3-138** — remainder was *"이 기계에는 능력 자체가 없다"* (capability: no
  normalization-sensitive volume is constructible on macOS). The runner has the capability, and
  the row's own `done-when` demanded a normalization probe that **fails loudly rather than
  skipping** — which is precisely the first step of the job, evaluated per run rather than
  assumed once. **CLOSED.**

Neither closure asserts a green suite on Linux. Both rows were about *the absence of an
instrument*; the instrument exists. If a future reader wants the rows to have required a green
**result**, the precise condition to reopen is: *the first scheduled or dispatched run of
`practices-case-normalization` reports `Total: N passed, 0 failed` with both probe properties
measured `yes`.* Anything less than that — including this session's container rehearsal — is
evidence, not closure of a result-shaped row. The rows as written were not result-shaped.

**Residual, stated:** (i) the *runner* result is unmeasured — the container rehearsal above is
evidence, not the measurement, and what it does show is **4 failures**, so nobody may write that
this suite is clean on Linux; the first scheduled run is what settles it, and the four names above
are the prediction it will test; (ii) the job is advisory, so a red run blocks nothing and depends
on a human reading the summary — promoting it to blocking is a separate decision that must wait
for a baseline, and on this evidence it must ALSO wait for the `symmid*` premise problem to be
resolved, or the first blocking run would be red for a reason that is not a defect; (iii) `stat` is the only BSD-ism this lane
audited — other GNU/BSD divergences (`sed -i`, `date -r`, `readlink -f`, `base64 -d/-D`,
`shasum` vs `sha256sum`) were **not** swept by grep, deliberately, because the workflow is the
instrument that finds them empirically and a hand-audit would only anticipate a subset;
(iv) the probe measures case and NFC/NFD only — other aliasing families (e.g. filesystems that
strip trailing dots or fold width) are not probed and would not be detected.

## R109 — Claude Code plugin 소비 채널 (D-track): 스킬은 안내, 강제는 설치
- Status: ACCEPT
- Date: 2026-08-01
- Drivers: ax-template은 fork-as-base 모델이라 신규 프로젝트가 참조 워크로드와 `com.ax.template`
  패키지를 통째로 상속해야 하고, 다른 레이아웃의 기존 프로젝트에는 적용 경로가 0이다. Java 룰의
  다수가 `verification.type: review`라 "지식"으로 이식 가능함이 확인되어, 카탈로그를 임의
  프로젝트에 적용하는 소비 채널이 성립한다 (vercel:react-best-practices 형태).
- Alternatives considered:
  - 스킬이 강제까지 나른다 — rejected. `practices/evals/*_guard.sh`는 REPO_ROOT를 스크립트
    자기 위치(`SCRIPT_DIR/../..`) 기준으로 해석하므로 스킬 위치에서 실행하면 ax-template 자신을
    검사하거나 vacuous pass한다. git hook·gradle test·CI는 대상 프로젝트에 설치돼야만 작동한다.
    따라서 아키텍처를 "스킬=안내·라우팅 / 강제=온디맨드 설치 가이드"로 분리한다.
  - 별도 dist 레포 신설 — rejected (maintainer 결정). 본 레포에 `.claude-plugin/marketplace.json`을
    병설해 레포 자체가 marketplace가 된다. 카탈로그와 배포물의 드리프트 표면을 만들지 않는다.
  - ESLint 플러그인 npm publish — 보류 (maintainer 결정). `file:` 설치가 공식 경로다. 다만
    패키지 온전성을 위해 `package.json`의 `files`에 `lib/`·`schemas/`를 추가했다 —
    `lib/feature-layout.js`는 5개 룰이 import하는데 tarball에서 누락돼 있었다.
  - 21st.dev 파생 블록을 배포 패키지에서 제외 — rejected (maintainer 결정). 내부 사용 전제로
    포함하되 `templates/DERIVED-SOURCES.yaml` provenance 대장 + `derived_block_license_guard`로
    등재 누락을 기계 차단한다.
  - 레이어 개수/이름 자체를 커스터마이즈 — rejected (비범위). `LAYER_RANK`는 3계층 단방향
    순서 자체가 불변식이다. 변수화하는 것은 각 레이어의 **디렉터리명**뿐이다.
- Why chosen: R53의 "Lift to a published npm package — rejected, fork-receivers fork source,
  not consume packages"는 *fork가 직접 편집하는 L0 소스 헬퍼*의 스코프 결정이다. 스킬과
  lint-plugin은 *as-is로 소비되는 도구*라 범주가 다르며, R53과 충돌하지 않는다. D-track은
  composition-kit 정체성을 바꾸지 않는 **소비 경로 추가**다 — fork 경로는 그대로 1급이고,
  D-track은 fork하지 않는 프로젝트를 위한 두 번째 문이다.
- Consequences: `feature-layout.js`가 `DEFAULT_LAYOUT` + `layoutFrom(settings)`로 레이아웃을
  변수화하고, 5개 룰이 flat-config 공유 `settings.ax` 채널로 이를 받는다(룰 `schema` 불변).
  카탈로그 루트에 결정론적 `INDEX.md`가 생성된다(rules/ 안이 아니라 — 4개 hard gate가
  `{catalog}/rules/*.md`를 스캔하므로). 스킬 5종이 추가된다(ax-practices / ax-init-config /
  ax-install-{react,java}-enforcement / ax-install-hooks). hard guard가 107→108이 되어
  `[0-9]+ hard guards` 문자열 7곳이 동기화된다(그중 4곳만 doc_headline_count_guard가 BLOCK하고
  나머지 3곳은 같은 사실의 P3 doc-drift). D-track 백로그는 `## D` 섹션에 두어 P0–P3 수렴
  분모(북극성 2)를 오염시키지 않는다.
  **INDEX.md 재생성은 자동 배선하지 않고 문서화된 수동 스텝으로 남긴다** —
  `agents_md_toc_disk_truth_guard.sh`와 `practices_react_sentinel_disk_truth_guard.sh`가
  guard 실행 중 `generate_agents.sh`를 재실행하므로, 거기에 인덱스 생성을 배선하면 (a) 모든
  R25/guard-suite 실행이 워킹 트리를 변형하고(두 guard는 AGENTS.md만 복원한다) (b) 인덱스
  생성기의 parse-fail이 무관한 guard 2개를 경유해 R25 전체를 깨는 전이 결합이 생긴다.
  **대가는 INDEX staleness이며 이를 P3 doc-drift 계열로 명시적으로 수용한다** — 게이트가 자기
  실행으로 트리를 오염시키는 것보다 엄격히 싸다. staleness 감시용 새 guard는 만들지 않는다
  (그 guard 역시 재생성·diff가 필요해 같은 커플링을 다른 이름으로 재도입한다).
  ESLint 플러그인 `package.json`의 `files`에 `lib/`·`schemas/`가 추가된다 — `file:` 설치는
  심링크라 이 결함을 감지하지 못하므로 `npm pack` 파일 목록 단언이 검증 절차에 포함된다.
- Follow-ups: npm publish는 보류 상태로 남는다 — 재검토 트리거는 `file:` 설치가 실제
  fork-receiver에게 마찰을 일으켰다는 관측이다. 레이어 개수/이름 커스터마이즈는 비범위로
  남는다. 설치 스킬의 java 측은 review-type 룰을 기계 게이트로 옮기지 않는다(구조적 한계).
  **V1(외부 비공허성 증명) 재검증 트리거**: ESLint **메이저 버전 범프**(플랫 config·`context.settings`
  전달 의미가 바뀔 수 있음) 또는 **Claude Code plugin/marketplace 스키마 변경**(`${CLAUDE_PLUGIN_ROOT}`
  해석·스킬 탐지 방식이 바뀔 수 있음) 시 §6 V1 절차를 **대조군 포함 전체** 재실행한다 — 두 경우
  모두 설치 경로가 조용히 0위반으로 퇴화할 수 있는 표면이고, 그 퇴화는 정의상 침묵한다.
- Kill-proof 등재 결과 (D-4 실행 기록): `derived_block_license/fail_unregistered` 를
  `fixture_kill_manifest.yaml` 에 정상 등재 (anchor `path not in registered_paths` 소스 내
  유일성 검증, 1-item 미니 manifest로 non-vacuity 증명 original=exit1→neutered=exit0,
  `min_items`/`LIVE_MIN_ITEMS` 72→73 lockstep). 등재 불가 사유 없음. 추가 실측: `claude
  plugin validate <repo>` 는 marketplace.json 존재 시 비-strict·strict 모두 rc=0 —
  F14-a 잔여 경고는 `plugin.json` 을 직접 지정해 검증할 때만 발화한다 (PRD 예상보다 강한 결과).
- Commits: 5412fb2 (D-0 거버넌스) · f4c4b59 (D-1 ESLint 레이아웃 변수화) · 5d85876
  (D-2 INDEX 생성기 + 진입 스킬) · 86186d4 (D-3 설치 가이드 3종) · f8be8fd (D-4
  marketplace + provenance guard [108]) · D-5 finalization은 본 커밋.

## R110 — `layoutFrom()`의 crash-free 계약을 `srcDir` 한 형태에 대해 번복 (fail-open → fail-closed)

**날짜**: 2026-08-09 · **번복 대상**: D-1 P3 "crash-free defaults" 계약
(`practices-react/eslint-plugin-ax/tests/feature-layout.test.js` 헤더에 명시돼 있던
"모든 malformed shape는 throw하지 않는다")

**무엇을 바꿨나.** `layoutFrom()`은 `ax.srcDir`가 `/`를 포함하면 이제 즉시 `Error`를
throw한다. 그 외 malformed shape(alias 엔트리, layers 배열)는 **종전대로 무예외 폴백**을
유지한다 — 계약을 통째로 폐기한 것이 아니라 정확히 한 필드-형태만 예외로 도려냈다.

**왜 번복이 정당한가.** crash-free 계약의 목적은 "config 오타 하나가 lint 전체를 죽이지
않게" 하는 것이다. 그 목적은 blast radius가 좁을 때만 성립한다:

- 잘못된 `alias` 엔트리 → 그 alias를 쓰는 import만 out-of-scope로 떨어진다 (좁음)
- 잘못된 `layers` 항목 → 그 디렉터리명만 매칭 실패 (좁음)
- 잘못된 `srcDir` → `classifySrcPath`의 **첫 비교**(`parts[0] !== layout.srcDir`)가
  전 파일에서 실패 → 모든 파일이 `layer:null` → **4개 레이어 경계 룰이 프로젝트 전역에서
  한 건도 발화하지 않는다.** 그런데 lint는 0위반 green을 낸다.

즉 이 필드에서 fail-open의 결과는 "관대함"이 아니라 **강제의 전역 침묵 + 거짓 green**이다.
강제가 존재 이유인 산출물에서 거짓 green은 crash보다 엄격히 나쁘다 — crash는 보이고,
침묵은 안 보인다. 그래서 이 한 자리에서는 fail-closed가 이긴다.

**왜 스키마로 충분하지 않았나.** `schemas/ax.config.schema.json`은 `pattern: ^[^/]+$`로
이를 제약하고 있었고 문서는 "스키마 차원에서 막는다"고 주장했다. 그러나 **lint 시점에
그 스키마를 실행하는 코드가 없다**(레포 전체에 ajv 부재; 스키마를 참조하는 것은 SKILL.md
산문 3곳뿐). 즉 강제 경로가 `/ax-init-config`라는 **에이전트 매개**에만 존재했고,
손편집·오생성 config는 아무 저항 없이 통과했다. 선언된 스키마에 실행 소비자가 없는
계열 문제로 P2-76에 등재.

**번복 조건 (이 결정을 되돌려야 하는 신호).** ESLint가 `create()` throw를 삼키도록
동작을 바꾸면(현재는 exit 2로 전파됨을 CLI 실측) 이 처방은 무력해지므로, 그때는
throw가 아니라 도달 가능한 다른 fail-closed 기제로 재설계한다.

**증거.** 재현: `layoutFrom({ax:{srcDir:'packages/web/src'}})` → 수정 전 수용 +
`classifySrcPath` `{"layer":null}` / 수정 후 throw. 회귀 테스트 4건(stash bisection으로
3건이 수정 없이는 FAIL함을 확인 = 비공허), 플러그인 스위트 56/56. 배선 실재성은 별도
양방향 대조군으로 확인: 커스텀 레이아웃 소비자 프로젝트에서 `settings.ax` 주입 시
eslint exit 1 + `ax/no-upward-layer-import` 1건, 주입 제거 시 동일 파일에서 exit 0 + 0건.

**동반 문서 정정**: `ax.config.schema.json` 설명문 · `docs/PLUGIN-CHANNEL.md` 경로B 2단계 ·
`docs/USAGE-GUIDE.md` §3 필드 레퍼런스 및 §7 T-2. 백로그: P1-74(closed) · P2-76(open).

## R111 — plugin.json 버전 릴리스 규율 도입 + `doc_headline_count_guard`가 3-필드 일치를 강제 (D-7 종결)

**날짜**: 2026-08-10 · **닫는 백로그 항목**: BACKLOG D-7

**측정한 것.** `claude plugin update <name>@<marketplace>`는 CLI 2.1.220에서
**`.claude-plugin/plugin.json`의 top-level `version` 필드만**을 비교 기준으로 삼는다.
`marketplace.json`의 plugin-entry `version`이나 top-level `metadata.version`은 이 비교에
전혀 관여하지 않는다 — plugin.json이 0.1.0으로 고정된 채 두 값만 올려도 updater는
`✔ ax-transform is already at the latest version (0.1.0).`을 출력하는 진짜 no-op이었고,
스냅숏 디렉터리와 gitCommitSha는 이동하지 않았다. 반대로 plugin.json을 0.1.0 → 0.1.1로
올리자 updater는 `✔ Plugin "ax-transform" updated from 0.1.0 to 0.1.1 for scope user.
Restart to apply changes.`를 출력했고, `cache/ax-transform/ax-transform/0.1.1/` 스냅숏이
새로 생성되며 기록된 gitCommitSha가 새 HEAD로 이동했다.

**왜 entry version이 비교 기준이 아닌가.** 확인된 사실일 뿐이고 이유는 CLI 내부 구현이다 —
marketplace 엔트리는 marketplace의 자기서술(카탈로그가 스스로를 광고하는 값)이고, 실제
설치본과 비교하는 진실은 설치되는 패키지 자신의 manifest(`plugin.json`)다. fork-base
repo가 곧 배포 채널이라 별도 릴리스 아티팩트가 없는 이 프로젝트에서는 그 두 값이 서로
다른 파일에 두 번 적혀 있다는 사실 자체가 드리프트 표면이다.

**무엇을 도입했나.**
1. **릴리스 규율**: `skills/`·`practices/`·`templates/` 등 소비자에게 보이는 내용이 바뀌는
   릴리스는 `.claude-plugin/plugin.json`의 `version`을 올린다. 이번 릴리스에서 0.1.0 → 0.1.1로
   실제로 올렸다(D-7 종결 커밋 자체가 첫 적용 사례).
2. `.claude-plugin/marketplace.json`의 plugin-entry `version`과 top-level `metadata.version`도
   함께 0.1.1로 올렸다 — **updater 비교에는 관여하지 않지만**, 세 필드가 서로 다른 이야기를
   하면 안 된다는 원칙(레포가 자기 자신에 대해 하나의 진실만 말해야 한다)에 따른 것이다.
3. **기계 강제**: `doc_headline_count_guard.sh`(guard [60], 새 guard 파일을 추가하지 않고
   기존 guard를 확장 — 아래 "왜 새 guard가 아닌가" 참조)가 `check_plugin_marketplace_version_sync()`
   함수로 plugin.json의 `version`과 marketplace.json의 entry `version` + `metadata.version`이
   전부 일치하는지 매 실행마다 검증한다. 불일치 시 `ENTRY_VERSION_MISMATCH` /
   `METADATA_VERSION_MISMATCH`로 exit 1 — 한쪽만 올리고 잊는 릴리스를 커밋 시점에 차단한다.
   격리 fixture 검증용으로 `--version-fixture-root DIR` 플래그를 신설(README/CLAUDE.md/
   SKILL.md 헤드라인 체크는 전체 문서 replica가 필요해 최소 두-JSON fixture로는 통과시킬 수
   없으므로, 이 하나의 불변식만 독립적으로 시험하는 별도 진입점). `pass_version_sync` /
   `fail_version_mismatch` fixture 쌍을 `practices/evals/fixtures/doc-headline-count/`에
   신설, `run-all-guards.sh` [60] 섹션에 `--include-fixtures` 조건부로 등록.
   `fixture_kill_manifest.yaml`에 `doc_headline_count/fail_version_mismatch` 항목을 등재하고
   (anchor `entry_version != plugin_version`, neuter `False`) `LIVE_MIN_ITEMS`를 73 → 74로
   래칫(`fixture_kill_proof_guard.sh`) — 원본 guard가 fixture에서 exit 1, anchor를 neuter로
   치환한 임시 guard가 exit 0로 뒤집힘을 직접 확인(오케스트레이터 자체의 아래 기지 결함으로
   `--manifest`를 통한 자동 실행은 막혔으나, 동일한 두 단계를 손으로 재현해 non-vacuity를
   확인했다 — 결과는 동일: exit 1 → exit 0 flip).

**왜 새 guard 파일이 아니라 기존 guard 확장인가.** `doc_headline_count_guard.sh`는
`practices/evals/*_guard.sh` 개수를 세어 README.md·CLAUDE.md·SKILL.md의 "<N> hard guards"
헤드라인 주장과 대조한다. 새 guard **파일**을 추가하면 그 개수가 108 → 109로 올라가고,
README.md와 CLAUDE.md의 헤드라인 숫자도 함께 갱신해야만 guard가 계속 exit 0을 낸다 — 그런데
이 릴리스의 작업 지시는 README.md와 CLAUDE.md를 건드리지 말라는 명시적 제약을 안고 있었다
(다른 레인이 소유). 기존 guard를 확장하면 guard 파일 개수가 그대로이므로 이 충돌이 원천
발생하지 않는다 — "확장이 자연스러운 자리인가"를 먼저 판단하라는 지시에 대한 답이기도
하다: 이 guard는 이미 plugin.json의 다른 헤드라인 주장("<N> rules")을 disk-truth와 대조하는
자리이므로, plugin.json의 또 다른 필드(version)를 다른 파일(marketplace.json)과 대조하는
로직을 같은 guard에 얹는 것은 파일 개수를 건드리지 않는 자연스러운 확장이었다.

**기지 결함 (수정 범위 밖, 기록만 함).** `fixture_kill_proof_guard.sh`를 `--manifest`로
직접 실행하면(신규 항목 유무와 무관하게 기존 등재 항목 단독으로도 재현됨) `line 386:
parse_rc: unbound variable`로 죽는다 — 이 릴리스의 diff가 만든 결함이 아니라 HEAD에 이미
있던 결함이며(`git diff`로 확인: 이번 변경은 주석 한 단락과 `LIVE_MIN_ITEMS` 상수 한 줄뿐),
수정은 이 릴리스의 범위 밖으로 남긴다. 그래서 신규 kill-proof 항목의 non-vacuity는
오케스트레이터를 우회해 ALGORITHM의 2·3·4단계(원본 guard exit 1 → anchor를 neuter로
치환 → 임시 guard exit 0)를 수동으로 재현해 확인했다.

**증거.** `.claude-plugin/plugin.json` version 0.1.0 → 0.1.1. `.claude-plugin/marketplace.json`
entry version + `metadata.version` 0.1.0 → 0.1.1. live 실행:
`doc_headline_count_guard.sh` exit 0("headline counts match disk ... and plugin/marketplace
versions agree"). `--version-fixture-root` pass/fail fixture 각각 exit 0/exit 1(정확히
`ENTRY_VERSION_MISMATCH` 1건). `backlog_convergence_integrity_guard.sh`는 D-7 체크박스
전환 전후 동일하게 exit 0(`## D` 섹션은 수렴 분모에서 명시 제외 — 가드 소스가 `## P0-3`만
순회함을 직접 확인).

**동반 문서 정정**: `docs/USAGE-GUIDE.md` §6(정상 경로 = marketplace update + plugin update
한 쌍, no-op 재발 조건과 재설치 우회 경로는 유지·amend). 백로그: D-7(closed). `docs/
START-PROMPTS.md`의 "plugin.json 버전이 0.1.0에 고정돼 있어 updater가 no-op한다"는 서술은
이제 정확하지 않으나 해당 파일은 이 릴리스의 소유 범위 밖이라 갱신하지 않았다 — 소유
레인에 인계.


## R112 — 설치 스킬을 단일 진실원으로 둔 downstream-fixture 검증(마커 추출 + 조건 평가), 3층 배치 (GH #92 [META])

**날짜**: 2026-08-14 · **닫는 백로그 항목**: BACKLOG P2-101(구조적 봉합) · 동반 closed
P2-93~P2-100 (GH #89/#90/#91 + F-030~F-034)

**결정.** `skills/ax-install-{hooks,java-enforcement,react-enforcement}/SKILL.md`를 설치 산출물의
**단일 진실원**으로 확정하고, 그 fence들에 비가시 마커(`<!-- ax:artifact ... -->`)를 달아
`practices/scripts/lib/ax_markers.py`가 **기계 추출**한다. 조건 분기(`ax:if`)와 값 치환(`ax:subst`)도
같은 파서가 평가한다. `practices/scripts/verify-downstream.sh`가 이 산출물을 stock-shaped 2-스택
픽스처(`practices/evals/fixtures/consumer-e2e/project/` — Spring Boot 4.1.0 / Gradle 9.5.1 /
Spring Initializr가 실제로 생성하는 eager `tasks.withType<Test>` 블록 보존 / Next.js app-router /
**두 root 모두 `.` 아님**)에 **verbatim 설치**한 뒤, 설치된 게이트에게 **진짜 위반을 차단하라고 요구**해
11개 단언(A-pc · A0~A8 · A7b)을 검증한다. 모든 단언은 **exit code와 게이트 자신의 신호 문자열을
쌍으로** 확인한다 — `git commit`은 게이트와 무관한 여러 이유로 실패하므로 "커밋이 실패했다"와
"우리가 심은 이유로 ax 게이트가 거부했다"는 다른 주장이고 값이 있는 것은 후자뿐이다([85]의 규칙).
배치는 **3층**이다:

1. **오프라인·결정론 (R25 안)** — guard **[112]** `install_artifact_extractability_guard.sh`가 추출
   **가능성 자체**를 게이트하고(마커가 깨지면 하네스는 조용히 아무것도 설치하지 않으며, 그 로그는
   "설치할 게 없었다"와 구분되지 않는다), guard **[113]** `cross_artifact_contract_guard.sh`가 Class C
   교차 산출물 합치를 **양쪽에서 독립 도출해 diff**한다(훅의 `-P` 플래그 이름 ↔ java 스니펫의
   `gradleProperty(...)` 이름; eslint 룰 id ↔ INDEX 전수). 한쪽 리터럴만 바꿔도 도출 집합이 갈라지므로
   **비-tautological**이다 — 고친 쪽의 문자열을 다른 쪽에서 grep하는 형태(자기 재확인)를 의도적으로
   피했다.
2. **릴리스 게이트 (pre-push)** — guard **[114]** `downstream_release_recency_guard.sh`가
   `.claude-plugin/plugin.json`의 `version`이 푸시 범위에서 **값으로** 바뀔 때만 발화하고,
   `.ax-downstream/runs.jsonl` 최신 행의 `head_sha` · `tree_clean` · 단언별 boolean **전건 true** ·
   **산출물 digest 재계산 일치**를 요구한다.
3. **E2E 실행 (주기/수동 seam)** — `verify-downstream.sh`는 사람이 부르는 seam이고,
   `.github/workflows/consumer-e2e.yml`가 주간 advisory로 같은 것을 돌린다(premise probe를 **먼저**
   돌려 실패하면 스윕을 아예 돌리지 않고 "PREMISE NOT ESTABLISHED"로 무측정을 명시 — 전제가 깨진
   위에서 낸 pass는 무측정보다 나쁘다는 기존 관례, `practices-case-normalization.yml`와 동형).

**동인.** (a) **살아있는 결함 검출** — 114개 guard와 R25는 전부 ax-template 자기 트리만 측정했고,
설치 스킬이 소비 프로젝트에 만드는 산출물이 거기서 도는지는 어떤 게이트도 묻지 않았다. 1개 downstream
bench 2일이 13건(F-017~F-029 = GH #78~#84 · #86~#91)을 냈는데 **전부 사람이** 찾았다(guard 발견 0건).
(b) **stock scaffold 형상이 결함의 조건이다** — GH #78은 Gradle 8.14.5에서는 동작하고 9.5.1에서만
inert였고, GH #79/F-030은 `react.root != "."`에서만, GH #90은 Initializr의 eager `withType<Test>`가
있을 때만 재현된다. 즉 "우리 트리에서 green"은 이 결함들에 대해 **아무 말도 하지 않는다**.
(c) **오프라인·결정론 게이트 posture 보존** — 이 카탈로그의 R25는 네트워크 없이 재현 가능해야 한다는
기존 결정을 이번 도입으로 깨지 않는다(아래 대안 C 참조).

**검토한 대안.**
- **(A) 산출물 사본을 repo에 두고 parity guard로 SKILL.md와 대조** — rejected. 사본은 **조건 분기의
  스냅숏을 고정**한다: `ax:if`/`ax:subst`가 있는 산출물은 렌더 결과가 config마다 다른데, 사본은 그중
  한 렌더만 박제하므로 나머지 분기의 드리프트는 **영구 미관측**이 된다. 사본이 있으면 "무엇이 진실인가"가
  두 곳에 적히고, 이 저장소가 반복해서 닫아온 결함(두 진리원이 조용히 갈라짐 — R111의 3-필드 version
  동기화, [113]이 codify한 Class C)을 새로 만드는 셈이다. 마커 추출은 **사본 0**을 유지한다.
- **(C) E2E 자체를 R25에 편입** — rejected. 하네스는 npm registry와 `services.gradle.org`에 접근하고
  수십 분이 걸린다. R25는 오프라인·결정론이어야 하며, **직전 라운드에 Gradle 9 fixture가 정확히 같은
  이유로 기각된 선례**가 있다. 대신 [112]/[113]이라는 **오프라인 대리 게이트**를 R25에 두어, E2E가
  돌지 않는 날에도 "추출 가능성"과 "교차 계약 합치"는 매 커밋 강제된다.
- **(D) 릴리스 게이트를 R25 안에** — rejected, **반증됨**: **부트스트랩 데드락**이다. 버전을 올리는
  그 커밋은 자기 자신에 대한 audit 로그를 가질 수 없다(R25는 커밋 이전에 돈다). 그래서 [114]를
  pre-push로 옮겼다 — 49th guard(`completion_checklist_recency_guard.sh`)가 같은 논리로 pre-push에
  있는 것과 동일한 배치이며, 실행 순서는 `commit → verify-downstream.sh(로그 기록) → R25 → push([114])`다.
- **(E) 컴파일되는 소스에서 문서 fence를 생성** — **연기·등재**(BACKLOG P2-102). Class B(문서 fence
  안의 코드가 한 번도 컴파일된 적 없음, = GH #89)의 **더 강한 치료**다: 생성 방향이면 #89를 오프라인
  javac로 **R25 안에서** 잡는다. 연기 사유는 커버리지와 비용 — 4개 산출물 중 java ArchUnit 1개만
  대상이고(hook body는 셸, eslint config는 mjs, package.json 조각은 JSON) 별도 소스셋 신설이 필요하다.
  **B와 배타적이지 않다**: 마커 추출 경로는 그대로 두고 fence의 **생성원**만 바뀐다.

**결과(귀결).**
- SKILL.md에 **비가시 마커 + 조건 문법**이 들어간다. 인간 가독 설명은 지시자 줄의 괄호 안에 보존해,
  마커를 모르는 독자가 읽어도 문서가 그대로 읽히도록 유지한다.
- **마커된 산출물은 기계 계약 아래에 놓인다** — fence를 지우거나 id를 충돌시키거나 `ax:subst` 선언을
  본문과 어긋나게 두면 [112]가 커밋 시점에 BLOCK한다. 자유롭게 편집하던 문서 블록이 아니게 된다.
- **E2E는 자동 강제되지 않지만 릴리스는 강제된다** — 평상시 커밋은 [112]/[113]만 통과하면 되고,
  `plugin.json` version을 올리는 push는 [114]가 실제 하네스 GREEN 로그를 요구한다. 즉 "소비자에게
  나가는 순간"에만 E2E 증거가 필수다.
- fork-receiver는 `AX_SKIP_DOWNSTREAM_RELEASE_GATE=1`로 **명시 opt-out**할 수 있다 — 훅 자체가 clone마다
  opt-in(`install-hooks.sh`)인 기존 posture와 같은 자율성 경계다. 우리 트리에서는 켜져 있다.
- `ax.config.schema.json`에 **optional** 필드 2개가 생긴다: `java.testTask`(F-032 — 훅이 하드코딩하던
  gradle 태스크명을 `ax:subst`로 치환) · `react.typescript`(F-033 — 파서 배선을 `ax:if`로 분기). 둘 다
  **optional**로 둔 이유는 기존 fork의 `ax.config.json`을 소급 파괴하지 않기 위해서다(미지정 시 각각
  `testPractices` / espree 기본 파서). **`java.testTask`의 오기가 #86류 조용한 결함이 아닌 근거**:
  존재하지 않는 태스크명은 Gradle이 `Task 'x' not found`로 시끄럽게 실패하므로 오배선이 관측 가능하다 —
  침묵하는 잘못된 기본값(#86의 `?: "com.example.app"`이 존재하지 않는 패키지를 스캔해 0 클래스를
  PASS로 확정하던 형태)과 범주가 다르다. 따라서 이 필드에 fail-closed 강제를 걸지 않는다.

**증거.** 하네스 11/11 PASS. **회귀 차등 3건** — `--artifact-override`로 #78/#79/#86의 pre-fix 형상을
재주입하면 각각 A4/A2/A3이 RED(단언이 실제로 그 결함을 잡는다는 증명이지, "green이 났다"가 아니다).
세 guard 전부 fixture 쌍 + **mutation differential**(탐지 로직을 무력화하면 fail fixture의 exit이 1→0으로
flip, 이후 원상복구 diff 확인). M1/M2 — 8.14.5 wrapper jar가 Gradle 9.5.1을 기동하고, Spring Boot 4.1.0
플러그인·Kotlin DSL 접근자·`includeTags`가 9.5.1에서 성립. guard 111 → **114**, plugin 0.1.6 → **0.1.7**.
`backlog_convergence_integrity_guard.sh` exit 0(합계 347/354).

**정직한 한계.** 이 하네스는 **재현 도구이지 발견 도구가 아니다** — 13건 전부 clean-room 인간 설치가
찾았고 그것이 여전히 1차 발견 메커니즘이다. SKILL.md의 **산문 단계**(탐지 휴리스틱, husky/lefthook
분기, worktree preflight, 수동 probe→detect→delete)는 실행되지 않아 미검증이고, **단일 형상**만 돈다.
그래서 이번 웨이브의 수정 중 **하네스가 반증할 수 없는 것**들이 있다: `java.testTask`의 다른 이름 분기 ·
`react.typescript=false` 분기 · F-030 수정의 `root:"."` 분기 · worktree preflight · husky/lefthook 분기.
이 목록을 문서에만 적지 않고 백로그 행으로 등재했다(아래 후속).

**후속 (BACKLOG 등재분).** P2-102(Option E — 컴파일되는 소스에서 fence 생성) · P2-103(husky/lefthook +
worktree 배선 분기 미검증) · P2-104(config 조건 분기 미검증: `root:"."` / `typescript=false` /
`testTask` 대체 이름) · P2-105(Next 특화 룰이 downstream에서 미발화 — P2-81/P2-87이 닫은 것은 문서
도달성이지 발화가 아니다) · P2-106([114]의 위조저항이 49th guard 선례보다 약함 — digest 재계산 1종으로
대체, 강도 차이는 헤더에 명시) · P3-143(버전 매트릭스 단일 점). 수렴률은 이 정직 등재로 100% → **98%**
(347/354)로 내려가며, 북극성(2)의 IDW18+ 동결 해제선 70%는 계속 상회한다.

**후속 2 (2026-08-15 외부 adversarial critic — codex, read-only).** shipped `ed5ca2a7`(위 R112 자신)에
대해 `VERDICT: ITERATE`와 함께 9건이 나왔다. **CRITICAL 2건은 즉시 봉합**: P2-107(픽스처
`frontend/package-lock.json`이 이 머신의 로컬 global gitignore에 흡수되어 미커밋 상태였다 — 위
"증거"란의 11/11 PASS는 그 미커밋 파일 덕분이었고, 이는 이 R112 자신이 막으려던 "환경 누수
false-green" 클래스가 R112 안에서 재발한 사례다; 신규 guard [115] `fixture_tracked_completeness_guard.
sh`로 봉합) · P2-108(guard [114]가 단언 명부를 하드코딩 검사해 `{"forged-single": true}` 같은 위조
로그를 통과시켰다 — [114]가 `verify-downstream.sh`의 11개 단언 이름을 파싱 도출해 정확 일치를 요구하는
형태로 강화). **잔여 7건은 BACKLOG P2-109~P2-115로 정직 등재**(마커 필수속성 미검사 · [113] 비독립
도출 · [114] 잔여 우회 3종+로드경로 불일치 · "사본 0"이 본문에만 참 · react-ts-eslint-dep 마커 공허 ·
F-032/F-033 RED-차등 부재 · 커버리지 산문 과대주장). 수렴률 98%(347/354) → **96%**(349/363).

**후속 3 (2026-08-15 잔여 7건 종결).** 위 후속 2가 정직 등재한 P2-109~P2-115를 전건 봉합, 각 항목
RED→GREEN 차등 실측 동반(상세는 BACKLOG 해당 행). 두 가지가 R112의 배치 자체에 직결된다: **(1)**
P2-112 — "산출물 사본 0"이 본문에만 참이고 placement(JSON deep-merge / Gradle dependency 삽입 / append)는
`verify-downstream.sh`에 하드코딩돼 있던 것을, 마커 계약(`ax_markers.py`)에 `merge=` attribute를
신설해(closed vocabulary: `json-deep`/`gradle-dependencies`/`append`/`replace`; `kind=file-fragment`는
필수, `kind=command`는 금지) 흡수 — 위 "결과(귀결)" 절의 "마커된 산출물은 기계 계약 아래에 놓인다"는
주장이 이제 placement까지 포함해 참이 된다. **(2)** P2-111(d) — guard [114]와 `ax_markers.py`가 로컬
체크아웃에서 로드되던 것을, git에서 blob-hash 검증 후 추출해 실행하는 **3-rung**(STRONG=직전 릴리스
사본 · WEAK=pushed sha 사본 · LEGACY=체크아웃)으로 강화. **정직하게 기록해야 할 잔여 한계**: 훅
자신과 `pre-push-lib.sh`는 여전히 체크아웃에서 실행되므로 self-attest할 수 없고, `python3`는 여전히
ambient dependency다 — 이 두 가지는 이번 라운드로도 닫히지 않았다. `verify-downstream.sh` 단언
11 → **13**(A9-eval · A10-tsdep 신설). 수렴률 96%(349/363) → **98%**(356/363).

**후속 4 (2026-08-17 P2-117 종결 — 토픽은 R112 자신이 아니라 P3-144/P2-116 계열이지만, 이 결정
로그가 세션 연속성의 running append 지점이라 여기 이어 적는다).** P2-116이 `ApprovalWorkflowTestSupport`
한 곳만 봉합하고 남긴 확산 범위 — status 단언 없이 응답에서 값을 꺼내는 `*TestSupport.java` **86개**를
공용 헬퍼 `backend/src/test/java/com/ax/template/authblueprint/common/HttpExtract.java`
(`path`/`pathAt`, 검사 순서 status → Content-Type → JSON 계열 → 추출 → null 거부, 실패 시
context·status·content-type·헤더 전량·본문 발췌(400자 상한)·`RestAssured.port`를 담은
`AssertionError`) 경유로 전건 이전하고, 신규 guard `test_support_response_validation_guard.sh`
**[116]**로 재발을 구조적으로 봉인했다(두 탐지자 — A: `.extract()` 뒤 체인이 `.response()`/terminal이
아니면 위반, B: `.path(`/`.pathAt(`/`.jsonPath(` qualifier가 `HttpExtract`가 아니면 위반 — 이 서로를
백스톱해 2-문장 우회를 닫는다). 부수 발견(`ApiNoEntityLeakTest`/`ErrorNoStacktraceLeakTest`의
all-negative 단언이 응답을 검증 없이 스캔하던 것)도 같은 라운드에 봉합했다(BACKLOG P2-118).

**이 라운드가 남긴 방법론적 교훈은 두 가지다.** (1) **파일 단위 grep으로 blindness를 판정하면
놓친다.** 최초 census는 `HandoffTestSupport`·`CommerceOrderTestSupport` 2개 파일을 "이미 status
단언이 있으니 blind 아님"으로 분류했지만, 그 단언은 blind하게 추출하는 메서드와는 **다른 메서드**에
있었다 — 파일이 어딘가에서 status를 확인한다는 사실은 그 파일의 모든 추출 지점이 안전하다는 것을
함의하지 않는다. 판정은 메서드 단위여야 하고, 그것이 정확히 guard [116]의 탐지자가 하는 일이다(파일
전체가 아니라 `.extract()` 호출부 각각의 다음 체인을 본다). (2) **fixture 자신도 vacuous할 수
있다.** guard [116]의 최초 `fail_blind_extract` fixture는 탐지자 A·B 둘 다에 동시에 걸리는 코드만
담고 있어, A를 mutation으로 무력화해도 B가 같은 줄을 잡아 exit 1이 유지됐다 — A가 실제로 무언가를
검증하고 있다는 증명이 성립하지 않았다(differential kill-proof 불성립). A·B 각각을 독립적으로 죽이는
mutation(MUT-1/MUT-2)이 여전히 exit 1을 내는 것을 보고서야, 두 탐지자 모두가 커버하는 shape만으로
구성된 fixture가 얼마나 쉽게 "통과하는 fail fixture"라는 자기모순을 숨기는지 드러났다 — 셋째
mutation(MUT-1+MUT-2, 둘 다 무력화)만이 1→0 flip을 낸다는 것을 확인한 뒤에야 fixture가 두 탐지자
모두에 대해 load-bearing임을 주장할 수 있었다. 이 저장소가 이미 R112 후속 2(P2-108, [114]의 위조
로그 통과)와 2026-06-29 STO pilot(fixture non-vacuity를 old-guard differential로 증명)에서 되풀이
확인한 패턴 — **green-but-hollow는 guard 자신에게도, guard의 fixture에게도 적용된다** — 이 세 번째
독립 사례로 재확인됐다. 상세 실측(4-shape 진단성 차등, 회귀 카운트)은 BACKLOG P2-117 행.

**후속 5 (2026-08-17 P2-120 종결 — 토픽은 R112 자신이 아니라 P3-144/P2-116/P2-117 계열의 연속이지만,
이 결정 로그가 세션 연속성의 running append 지점이라 여기 이어 적는다).** P2-117이 blindness 축을
닫은 뒤 남긴 잔여(BACKLOG P2-120) — 프로세스 전역 mutable static `io.restassured.RestAssured.port`를
**파일 139개**(그중 86개는 `useRandomPort` 정의 보유 — 정의 본문 자체가 대입문 1개를 담는다 —
나머지 53개는 정의 없이 직접 대입문만 보유; 대입문 총 141건)가 각자 대입하던 것 — 을 신규 JUnit5
확장 `AxPort`(`backend/src/test/java/com/ax/template/authblueprint/common/AxPort.java`) 단일
writer로 흡수하고, 신규 guard `restassured_port_single_writer_guard.sh` **[117]**로 재발을
구조적으로 봉인했다(확장은 테스트 인스턴스의 `@LocalServerPort` 필드를 리플렉션으로 찾아 세팅·기록하고,
필드가 없는 MOCK 클래스는 건드리지 않고 기록만 한다).
**정정(2026-08-17, fable5 적대적 검토 결함3 봉합)**: 이 항목의 원 서술("138개(실측 134개)")은
같은 라운드에 작성된 BACKLOG P2-120 행("파일 134개 + 정의 84건")·AxPort.java 헤더("140 = 86+54")와
서로 모순됐다 — 세 문서가 각기 다른 수를 주장하면서도 무엇을 세는지 명시하지 않았다. 기준 커밋
`f4457530`(P2-120 직전) 대비 `git diff`로 재측정해 위 수치(정의 86 / 대입문 141 / 대입-보유 파일
139)로 세 문서를 통일했다.

**이 라운드가 남긴 처방 원칙은 하나다: 전역 mutable 상태가 서드파티 라이브러리 소유라 제거할 수 없을
때, 처방은 "없앤다"가 아니라 "쓰기 지점을 하나로 만드는 것"이다.** `RestAssured.port`는 RestAssured
API 자신이 노출하는 static field라 이 저장소가 그 존재 자체를 없앨 수 없다 — 할 수 있는 것은 기존
141개 대입 지점(139개 파일)을 **읽기로 격하**하고, 신규 도입한 단일 writer(`AxPort` 확장)만 대입을
유지하는 것뿐이었다. 이것은 이
카탈로그가 이미 알던 immutability 원칙(코딩 스타일 규칙의 "새 객체를 만들되 기존 객체를 변형하지
않는다")과 정확히 같은 형태를 서드파티 API가 준 제약 아래에서도 재현한 사례다 — 원본을 소유하지
못하면 원본을 없애는 대신 **원본에 닿는 경로를 하나로 좁힌다.**

**그리고 그 처방의 형태(단일 writer 확장 vs 요청별 명시 포트)는 추측이 아니라 측정이 먼저 결정했다.**
`given()` 호출부가 저장소 전체에 몇 개인지 세기 전까지는 "매 요청에 포트를 명시적으로 넘겨라"는 더
국소적인(그리고 프레임워크 개입이 적은) 대안도 후보였다. 실제로 센 결과는 **1561개**였고, 이 disk-truth
하나가 그 대안을 실행 불가능으로 배제하고 단일 writer 확장을 유일하게 남은 경로로 만들었다. 순서가
중요하다 — 처방을 먼저 고르고 규모를 나중에 확인했다면 1561개 호출부 각각을 수정하려다 도중에
포기하거나, 부분 적용으로 새로운 불일치(일부는 명시 포트, 일부는 stale 전역)를 만들었을 것이다.
차등 실측(확장 ON: 67/67+17/17 BUILD SUCCESSFUL / 확장 OFF: 34/67 failed,
`RestAssured.port = -1 ... signup status=404 content-type= headers=[] body=404 Not Found`)이 이
전역이 실제로 P3-144급 실패를 만들 수 있었음과, 단일 writer가 그것을 막는다는 것 둘 다를 증명했다.
상세 실측(guard [117] 비공허성, 회귀 카운트, `testPayment` 환경 artifact)은 BACKLOG P2-120 행.

**후속 6 (2026-08-18 하네스 커버리지 확장 라운드 — R112 자신이 "하네스가 반증할 수 없다"고 정직
등재했던 잔여를 닫는다).** R112 본문의 "정직한 한계" 절이 명시했던 5개 미검증 분기 —
`java.testTask`의 다른 이름 · `react.typescript=false` · `root:"."` · worktree preflight ·
husky/lefthook — 를 봉합한 BACKLOG P2-103~106과, 별개 계열(P3-144/P2-116/P2-117의 blind-read
축)의 잔여를 봉합한 P2-119, Gradle 9 승격의 전제조건이던 P2-88을 이 라운드가 전량 닫았다(상세
실측은 BACKLOG 해당 행). `verify-downstream.sh`의 `ax:assertions` 매니페스트가 **13개
(A-pc·A0~A8·A7b·A9-eval·A10-tsdep) → 23개**로 확장됐다(`A11-route`~`A20-rootdot-skip` 10건
신설) — guard [114]는 이 명부를 하드코딩하지 않고 스크립트에서 파싱 도출하는 설계(R112 후속 2가
도입한 형태)라 확장을 별도 수정 없이 자동 흡수한다.

**이 라운드가 남긴 방법론적 교훈은 세 가지다.**

**(a) 조건 분기는 저술 수준 검증만으로 충분하지 않다 — 실행 수준에서 돌려야 한다.** P2-104(b)가
정확히 이 형태로 출하됐었다: `react.typescript=false` 분기는 마커 트리를 렌더하는 `A9-eval`을
통과했다 — 즉 "조건이 조건답게 갈라지는가"는 검증됐다. 그러나 렌더된 산출물을 실제 ESLint 9에
**돌려본 적**은 없었고, 그 산출물은 `languageOptions`를 통째로 비워 모든 `.jsx`를 파싱 단계에서
죽였다 — React 파일을 한 줄도 못 읽는 게이트가 조용히 출하됐다. 더 나쁜 것은, 그것을 잡았어야
할 스킬 §5의 필수 self-verification probe가 **JSX 없는 파일**이었다는 점이다 — 파싱이 되는지
자체를 검증 대상에서 빼놓았으므로 probe는 통과를 보고했다. 저술(렌더)이 옳다는 것과 실행(파싱·
런타임)이 옳다는 것은 서로 다른 주장이고, 전자의 증거로 후자를 대신할 수 없다.

**(b) census는 정의가 결과를 지배한다 — 정의를 먼저 확정하고 무엇을 셌는지 말하라.** P2-119의
"호출자 측 blind read" 규모는 정의를 바꿀 때마다 자릿수가 바뀌었다: 좁은 리터럴
`.extract().path(` 체인 **209**사이트 → guard [116] 탐지자 A/B의 넓은 shape 정의 **1710**사이트
→ 문장 단위 재계수 **1413** → 응답객체(단일 handoff) 단위 **561** → origin-resolved 후 dedup
**278**. 이 중 어느 것도 "틀린" 숫자가 아니다 — 각각 다른 질문("리터럴 체인이 몇 개인가"
vs "탐지자가 이론상 볼 수 있는 shape가 몇 개인가" vs "실제로 고쳐야 하는 독립 사이트가 몇 개인가")에
대한 정답이다. 정의를 명시하지 않고 숫자만 보고하면 같은 결함에 5개의 서로 다른 "심각도"가
붙는다 — 209만 보면 소규모, 1710만 보면 광범위, 278(실제 fix 대상)이어야 작업량이 맞는다. 숫자를
먼저 정하고 정의를 나중에 끼워 맞추는 순서는 이 함정을 감춘다; 정의를 먼저 고정하고 그 정의
아래에서 잰 숫자만 발표해야 한다.

**(c) guard의 유일한 이빨이 공허해질 수 있는지 물어라.** guard [114]의 위조저항 감사(P2-106)가
찾은 G2가 이 교훈의 가장 날카로운 사례다: [114]의 유일한 실질 판별자는 "SKILL.md 마커 본문에서
재계산한 산출물 digest가 로그의 digest와 일치하는가"였는데, 마커가 **0개** 추출되는 실행 경로에서는
재계산 결과가 빈 dict `{}`가 되고, 로그의 digest 역시 (같은 이유로) 빈 dict일 수 있어 `{} == {}`가
참으로 평가된다 — 즉 "아무것도 검증하지 않았다"가 "digest가 일치한다"와 구분되지 않았다. 이것은
P2-108(같은 [114], 빈 단언 dict가 위조 로그를 통과시키던 결함)과 **같은 형태를 다른 층에서**
반복한 사례다 — 값이 일치하는지를 확인하기 전에 "그 값이 애초에 뭔가를 측정한 결과인가"를 먼저
물어야 한다는 것이 G1-G5 전체를 관통하는 단일 원칙이다. `if not recomputed: BLOCK`(재계산
결과가 비어 있으면 그 자체로 실패)이 처방이었다 — 게이트를 강화할 때마다 "이 판별자가 텅 빈
입력에서도 참을 낼 수 있는가"를 별도로 물어야 한다.

상세 실측(pre-fix→post-fix RED/GREEN 차등, mutation kill-proof, 신규 등재 4건의 tier 근거)은
BACKLOG P2-88·P2-103~106·P2-119·P2-121~123·P3-146 행. 수렴률 97%(361/371) → **98%**(368/375).
