# Architect Review — Functional Capability Extension PRD (draft)

> Reviewer: oh-my-claudecode:architect
> Target: `docs/superpowers/specs/2026-05-18-functional-extension-prd.draft.md` (851 lines)
> Date: 2026-05-18
> Mode: ralplan consensus loop, Step 5 (Architect round 1). DELIBERATE mode auto-enabled.
> Posture: read-only, evidence-anchored. Every claim cites a PRD line, prior PRD, or repo path.
> Predecessors reviewed for pattern: `2026-05-17-frontend-templatization-architect-review-iter2.md`, `2026-05-18-catalog-extension-prd.md`.

---

## TL;DR verdict

**Likely-ITERATE.** The PRD is structurally well-formed and inherits the §6 autonomous-execution scaffolding from Catalog Extension cleanly. Six of seven soundness dimensions land **PASS** or **PASS-WEAK borderline**. But three structural issues need a binary fix before APPROVE:

1. **(strongest) Tier-1 cap exception is ceremonial, not architectural.** 4→7 Tier-1 doubles the user-facing surface in one cycle on top of a cap already exceeded in Catalog Extension. The fallback path the PRD itself documents (`/ax-verify --policy-check`, `--evidence-fetch`, `--explain`) is the correct architecture; the proposed Tier-1 split is over-ceremony.
2. **Parallelizability claim for SP24‖SP25‖SP26 is partially false.** SP24's `ExportJobService.java` is declared a "forward reference" to SP25's `JobDispatcher` (PRD `:355`); SP25's `BaseEntityWithSoftDelete.java` collides with the SP13 `BaseEntity.deleted` field that **every existing domain entity already extends** (`templates/backend/notification/Notification.java:54`, `templates/backend/email-outbox/EmailOutbox.java:55`); SP26's search domain needs the SP25 JPA-audit + paging-cap scaffolding because Search results need pagination policy. The graph in §6 claims parallel; the dependency text in §6 SP24/SP25/SP26 contradicts the claim.
3. **SP27 SSE on serverless is acknowledged as a feature trap but the mitigation ships the trap.** Pre-mortem Scenario 1 (line 628) correctly identifies "Vercel + Next.js is the default React deployment path for Korean enterprise SaaS. Most fork receivers will hit this within their first deployment." The mitigation adds a `SERVERLESS_WARNING_MISSING` README gate but still ships SSE as the primary code path. For a composition kit whose stated audience is exactly this deployment shape (CLAUDE.md vision), this is the wrong default.

The other dimensions (b/c/d/e/f/g) PASS with one PASS-WEAK on (a) F11 form orchestration overlap with SP15 shells.

**Recommendation to Critic: likely-ITERATE.** All three issues are addressable inside this draft (no structural rewrite); see §4 Synthesis.

---

## §1 Strongest steelman antithesis

**Headline:** *"The Tier-1 cap exception is over-engineered ceremony. `/ax-policy-check`, `/ax-evidence-fetch`, `/ax-explain` are correctly modeled as `/ax-verify` subcommands. The PRD even names the fallback path itself (`:550`); it should be the default, not the rejected option."*

**Full argument:**

The PRD-1 §3.2 (line 149 of `2026-05-17-frontend-templatization-prd.md`) set the Tier-1 cap at **exactly 3**: `/ax-transform`, `/ax-verify`, `/ax-scaffold`. The Catalog Extension already burned the exception once for `/ax-fork-receiver` (Catalog Extension §11, line 787: "Adds 1 Tier-1 → total 4 … the cap of 3 was a heuristic"). That ADR established a precedent: **the cap is a heuristic and exceptions ship via ADR.**

This PRD now proposes 4→7 Tier-1 in a single cycle. Three observations:

1. **None of F13/F14/F15 are first-class verbs from a user-mental-model standpoint.** They are all sub-modes of verification:
   - `/ax-policy-check` = "verify before mutation" — temporal sibling of `/ax-verify` (after mutation).
   - `/ax-evidence-fetch` = "verify snapshot freshness + refresh" — direct extension of `time_decay_guard.sh` which already runs inside `/ax-verify`.
   - `/ax-explain` = "verify failure interpretation" — reads the same rule index `/ax-verify` already loads.
   The Critic mandate per §1 Decision Driver 3 (line 24) names "Skill-orchestrated pre-execution gating" as the gap. That's a real gap. But "subcommand of `/ax-verify`" is the surface where pre-execution gating belongs — it's the same skill family.

2. **The 17-skill defense in PRD-1 §4.14 (`2026-05-17-frontend-templatization-prd.iter2.md:425-448`) was built on the pathPattern-partition invariant: "Tier-3 guards are NOT pathPattern-triggered; they are invoked by Tier-2 only."** Adding 3 more Tier-1 entries does not break that invariant per se, but it does break the **mental-model invariant** that Tier-1 = the verb the user types directly. `/ax-policy-check --file <path> --intent <text>` (per `:528`) requires the user to know which Tier-1 to invoke when. Subcommands let the user always type `/ax-verify <mode>` and dispatch internally — same surface area for the user, less surface for the catalog to maintain (single SKILL.md, single registry entry, single self-test runner).

3. **The PRD's own fallback (`:550`) is "ship F13/F14/F15 as sub-commands of `/ax-verify`."** This is named as the fallback if Architect rejects the Tier-1 path. **Architect's job here is to flag that the fallback is the correct architecture.** The "default = Tier-1" choice does not flow from any architectural property of the catalog; it flows from a presentation preference (each capability gets a top-level name). For a composition kit whose adopters need a clean mental model, fewer Tier-1 names = better.

**Specific cost of the Tier-1 path being taken:**

- 3 new SKILL.md files with frontmatter that the `skill.topology.tier_count` probe (the binary check from PRD-1 SP4b/SP12, `2026-05-17-frontend-templatization-prd.iter2.md:1003`) must be amended to allow.
- 3 new self-test scripts. With subcommands, one self-test runner harness with 3 cases.
- The "applies before mutation" property (F13's unique value-add over `/ax-verify`) is achievable with `/ax-verify --policy-check --pre-mutation --file X --intent Y`. The pre-mutation semantics live in the **flag**, not the **skill name**.
- The "false positive rate <5%" eval set (line 706) is independent of whether the skill is Tier-1 or a subcommand — it lives in `skills/ax-verify/_eval/` either way.

**Architect verdict on this antithesis: the steelman holds.** This is the load-bearing change to recommend.

---

### §1.1 Other steelman cuts (named, dispositioned)

**Search atomic with PostgreSQL FTS default + mecab-ko opt-in (per SP26, line 444–448):** The PRD mitigation is correct (`PostgresFtsAdapter` default = zero infra; Korean tokenizer opt-in via blueprint). The pre-mortem Scenario 3 (line 672) explicitly names the 70% baseline and Korean enterprise upgrade path. **This is a real residual risk but not blocking** — the catalog is honest about the trade. Composition-kit framing says fork receivers can swap the adapter; the default is intentionally the minimum-infra path. **Steelman survives but does not block.**

**SSE on serverless (Scenario 1, line 628):** This **is** blocking-class — see §3(b) below. The mitigation ships a warning gate but defaults the L4 to SSE. For Vercel-default Korean SaaS forks, this is shipping a known trap.

**F11 form orchestration overlap with SP15 shells (line 144):** SP15 shipped `field-array.tsx`, `form-section.tsx`, `conditional-field.tsx`, `form-error-summary.tsx` (Catalog Extension P0 items #19, #18, #20, #21 — `2026-05-18-catalog-extension-prd.md:97-100`). Critic flagged in SP15 they were "shells with TODO" (per PRD draft line 144). SP27 ships `-extended` siblings. **The `-extended` naming is a smell** — it duplicates the L2 block by adding a suffix instead of upgrading in place. Composition-kit framing prefers in-place upgrade with the file path unchanged. **Steelman partially survives**; needs naming-policy ADR in SP27 (see §4 Synthesis #3).

**SP23/24/25/26 secret serialization on shared infra:** Partially true — see §3(g) verdict. SP24's `ExportJobService.java` is documented as a forward reference to SP25's `JobDispatcher` (line 355). SP25's `BaseEntityWithSoftDelete` collides with SP13's `BaseEntity` already extended by every existing domain entity (verified: `templates/backend/notification/Notification.java:54`, `templates/backend/email-outbox/EmailOutbox.java:55`, `templates/backend/file-storage/StoredFile.java:26`). **This is a blocking-class issue** — see §3(g).

---

## §2 REAL tradeoff tension

**Side A (chosen):** SP26 ships search as an atomic full_trio domain with PostgreSQL FTS as the default adapter and Meilisearch as opt-in. Korean tokenization defaults to `to_tsvector('simple', ...)` (line 448) which the PRD itself estimates at ~70% recall for Korean queries.

**Side B (alternative):** SP26 could ship Meilisearch as the default adapter (it has built-in CJK tokenization via its segmentation library and handles Korean text without an extra dependency), or could defer the L4 page until a fork-receiver picks an adapter (ship only Spec Trio + interface + the PostgresFtsAdapter).

**What Side A wins:** Zero-infra default. Fork receiver runs `/ax-fork-receiver` install and the catalog is runnable without provisioning anything new. Matches the composition-kit "everything runs out of the box" property.

**What Side A loses:** 30% Korean recall hole on day 1 for the exact audience this catalog targets (Korean enterprise SaaS). The §3 pre-mortem (Scenario 3) acknowledges this as "Korean enterprise forks adopt `mecab-ko` for the full-fidelity path. Default behavior keeps the catalog runnable on day 1." That last clause is the **real cost**: the catalog is "runnable" but the **default user experience for the catalog's stated audience is broken**. A Korean enterprise fork-receiver hitting search day-1 and getting `<70%` results on `"강남 결제"` will not interpret that as "I should opt into mecab-ko"; they will interpret it as "the catalog is broken."

**What Side B wins:** Default UX matches the stated audience. Meilisearch CJK handles Korean without a Korean-analyzer install step. Or, deferring the L4 page means the L4 ships when the adapter pick is informed by deployment context.

**What Side B loses:** Meilisearch binary distro is heavy. Deferring loses the atomic full_trio SP that the Critic mandate (Catalog Extension §1) was built to honor.

**The genuine tension:** *Side A maximizes "runs out of the box for everyone"; Side B maximizes "runs correctly for the stated audience."* This catalog's CLAUDE.md vision (lines 7–43) emphasizes Korean enterprise as the *primary* audience, not the universal audience. The "everyone" framing dilutes the targeting.

**Architect resolution recommendation:** Side A is defensible **only if** the L4 page ships with an explicit "first-run health check" that runs the Korean tokenization smoke (`practices/evals/fixtures/search_korean_tokenization/`, line 684), and if the install-time output to fork receivers is **loud and Korean-localized** ("한글 검색 정확도 70%; 90%+ 정확도를 위해 mecab-ko 설치를 권장합니다"). The pre-mortem Scenario 3 mitigation gestures at this but leaves it as `install.sh prints WARNING` — for the stated audience, the warning needs to be an interactive prompt on `/ax-fork-receiver` install, not a passive line of output. **See §4 Synthesis #4.**

---

## §3 Architectural soundness verdict (per 7 dimensions)

### (a) Boundary integrity — F5 charts at L2; F6 search at L4; F11 forms at L2 — each defensible per PRD-1 §4.11 Layer Membership Decision Table?

**Verdict: PASS-WEAK.**

- **F5 charts at L2:** Defensible. `time-series-chart.tsx`, `bar-chart.tsx` etc. (line 188) compose recharts primitives + project tokens; they're not L4 because no L4 domain owns them; they're not L1 because they're not purely visual primitives (data binding). L2 is correct per the decision table extension policy.
- **F6 search at L4:** Defensible. Search is a vertical-slice domain (Spec Trio + backend + L4 page) per Critic atomic-ordering rule. L4 boundary at `templates/L4/search/` matches the precedent set by `templates/L4/payment/`, `templates/L4/notification/` etc.
- **F11 form orchestration at L2:** Defensible by membership but **PASS-WEAK on naming**. The `-extended` suffix convention (line 188, 191) is novel — no prior SP used this. PRD-1 §4.11 doesn't address suffix conventions for upgraded blocks. The decision table extension policy ("if an SP author proposes a component not listed here, they must extend this table") is silent on `-extended` siblings of existing blocks. **Recommendation:** SP27 must either (i) upgrade `field-array.tsx` in place + atomic deprecation of the SP15 shell, or (ii) extend §4.11 with an explicit `-extended` policy as part of SP27 deliverables. Currently the PRD says SP27 ships `-extended` files and "SP15 shells retained for back-compat" (line 196) — this leaves both files alive with overlapping function. For a composition kit, this is duplication.

### (b) Verification closure — every new template covered by named verify skill? Tier-3 guard walks new path automatically?

**Verdict: PASS.**

- §5.1–§5.7 enumerate every new template. Cross-checked with the §6.5 Verification Matrix: every SP names its `verify_skill` column. New `templates/backend/observability/`, `templates/backend/cache/`, etc. are walked by `/ax-verify-java`. New L2 blocks walked by `/ax-verify-L2`. Search domain walked by `/ax-verify-domain search` after the registry entry lands in SP26.
- Tier-3 guards (`evidence_guard.sh`, `substance_guard.sh`, `spec_ref_guard.sh`, `time_decay_guard.sh`, `trio_integrity_guard.sh`): the PRD §3 Guardrails (line 109) require `evidence:` frontmatter on every new template; §7.3 (line 600) calls out `ax-verify-domain` re-runs but does not explicitly enumerate that the 4 hard gates walk the new template tree. **Mild gap:** SP23–SP28 should each have an SP-internal acceptance line "`ax-guard-evidence` walks new template paths" (SP23 has this at line 327; SP24–SP28 do not). **Not blocking; SP-internal clarification.**

### (c) Evidence chain density — every new template has external (recharts/Meilisearch/OpenTelemetry docs) or internal_design rationale? snapshot count adequate?

**Verdict: PASS.**

- 12 new upstream snapshots listed (§5.8, line 240). Each new template family maps to a snapshot: observability → opentelemetry-java + micrometer-prometheus; cache → (extends spring-boot-actuator); integration → resilience4j; data → spring-flyway + spring-data-jpa-auditing; charts → recharts; search → postgres-fts + meilisearch; realtime → spring-mvc-sse; forms → react-hook-form; i18n → next-intl.
- All 7 ADRs (§5.9) carry `provenance_class` per PRD-1 §4.12. TD-2026-05-18-026 correctly classified as `external_canonical` (recharts + PostgreSQL FTS are external canon). TD-2026-05-18-028 correctly classified as `internal_design + locked_constraint` (KRW formatting per ISO 4217 + 개인정보보호법).
- **Minor:** `meilisearch` snapshot is listed but the PRD does not name a section/quote anchor. `time_decay_guard` runs on snapshot age, but the rule referencing the snapshot needs a `quote_match_check`-passing substring. SP26 deliverables should explicitly include the section + quote in the new rule's `evidence:` block. **SP-internal clarification.**

### (d) Anti-pattern resistance — no MockMvc-only tests for search/realtime? No new governance loops via /ax-policy-check?

**Verdict: PASS.**

- §3.2 Guardrail Must-NOT line 125: "No new MockMvc tests; RestAssured only." Explicit.
- SP26 acceptance line 439: "POST /api/v1/search with Korean query "강남 결제" → results render in palette" — RestAssured. Line 366 (SP24 webhook) also RestAssured.
- SP27 SSE acceptance line 473 also RestAssured (`SseSubscribeIT.java`).
- §11 line 820 explicitly addresses the governance-loop concern for the new skills: "/ax-policy-check is pre-mutation advice for AI agents, not a merge-gate. /ax-evidence-fetch doesn't enforce refresh; advises. /ax-explain is explanatory. None enforce team policy." This is a direct response to CLAUDE.md "거버넌스 무한루프 금지." Solid.

### (e) TDD anchoring — each SP names concrete test_file + assertion + RED reason + first_green_command? Search/realtime particularly hard to TDD — check those rows.

**Verdict: PASS.**

- §6.5 Verification Matrix (line 560) has all 7 rows filled with 8 columns each (verify_skill, script_path, test_file, assertion, expected_RED_reason, first_green_command, observability_signal). Matches the format the iter2 Architect review marked as PASS for PRD-1.
- **Search row (SP26):** TDD anchor "practices/evals/fixtures/trio_integrity/fail_search_missing_frontend_spec/" with named RED reason `MISSING_BACKEND_SPEC: search`. Concrete.
- **Realtime row (SP27):** TDD anchor `templates/L2/_fixtures/dirty-guard.spec.ts` Playwright — concrete. **But:** SSE TDD is weak — the row asserts "SSE delivers 3 events in 5s" but no fixture path is named for the RED state (pre-SP27 RED reason is `ENOENT` on `SseEmitterConfig.java`, which is structurally fine but not as concrete as the form fixture). **PASS but the SSE half could be tighter.**
- **SP29 row:** named `skills/ax-policy-check/_tests/policy-check-cold.spec.sh` + 50-fixture eval set with FP rate <5% gate. This is **above** standard rigor.

### (f) Spec Trio atomic rule — SP26 search ships Spec Trio + backend + L2 + L4 in ONE SP per Critic SP integrity rule? Check F9 feature-flags too.

**Verdict: PASS.**

- SP26 §6 line 421: "Spec Trio + backend + L2 + L4 in ONE SP" — explicit. Atomic rollback boundary (line 453: "Atomic revert (search domain is atomic per Critic mandate)").
- SP28 F9 feature-flags: line 494–498 lists Spec Trio + 6 backend files + L1 `feature-gate.tsx` + L2 `feature-flag-toggle.tsx` + L4 admin pages. **Per-cluster revert at line 518** ("i18n + feature-flags can revert independently"). The PRD says "if feature-flags atomic deliverables fail, atomic revert of that cluster" which honors the atomic rule within-cluster. **PASS** but the §7.1 line 584 rollback table phrasing is slightly looser than SP26's: SP26 says "ALL search domain deliverables"; SP28 says "atomic revert of that cluster." A reader could read SP28 as allowing partial revert within the feature-flags cluster, which would violate atomic ordering. **SP28 rollback row should be tightened to match SP26 wording.** Not blocking.

### (g) Parallelizability claim — §5 dependency graph claims SP24/25/26 parallel after SP23 — really?

**Verdict: WEAK.**

The §6 dependency graph (line 277) shows SP24, SP25, SP26 fanning out from SP23 in parallel. The text at line 297 says "SP24/SP25/SP26 can run in parallel after SP23 (disjoint surfaces)." Three concrete issues against this claim:

1. **Forward reference SP24→SP25.** PRD line 355: "ExportJobService.java — Long-running via `JobDispatcher` from SP25 (forward reference; SP24 ships interface, SP25 supplies impl)." A forward reference across parallel SPs is a real dependency. If SP24 is running in parallel with SP25 and SP24 ships `ExportJobService.java`, the interface contract belongs to SP24 but the impl belongs to SP25 — if the interface shape changes after merge, SP25 has to rebase. This is the exact "secret serialization" the Architect should flag.

2. **`BaseEntityWithSoftDelete` collision (SP25 vs every existing domain).** SP25 ships `templates/backend/data/BaseEntityWithSoftDelete.java` (line 165, §5.1). But the SP13 `BaseEntity` already provides a `deleted` field that **every existing entity uses** — verified at:
   - `templates/backend/notification/Notification.java:54` — `extends BaseEntity`
   - `templates/backend/email-outbox/EmailOutbox.java:55` — `extends BaseEntity`
   - `templates/backend/email-outbox/EmailTemplate.java:52` — `extends BaseEntity`
   - `templates/backend/notification/NotificationPreferences.java` — extends BaseEntity
   - `templates/backend/file-storage/StoredFile.java:26` — `@Entity`
   - Repository queries reference `e.deleted = false` throughout `EmailOutboxRepository.java`, `NotificationRepository.java`.
   
   The new `BaseEntityWithSoftDelete` is either (a) a parallel hierarchy (which violates DRY and confuses fork receivers — "which base entity do I extend?") or (b) a rename of `BaseEntity` (which is a breaking change for every existing domain entity). The PRD does not specify which. **This is the load-bearing technical defect.** SP25 should be either:
   - Extend SP13's `BaseEntity` in place to add `@SQLDelete` + `@Where` (touching existing files — but a surgical change, not a rename).
   - Ship `BaseEntityWithSoftDelete` and explicitly migrate every existing entity in the same SP (large blast radius, conflicts with the atomic-per-SP rule).
   - Document explicitly that `BaseEntityWithSoftDelete` is an opt-in additional base for **future** entities only, and the `soft-delete-only-on-base-entity` rule (line 223) applies only to new entities. The PRD currently does not name this trade.

3. **SP26 search depends on SP25 paging/normalizer in practice.** Search results are paginated. `PageRequestNormalizer.java` (line 165) is SP25's deliverable. If SP26 ships search results paging without the SP25 normalizer, the search API won't enforce the max-limit cap. The PRD's SP26 acceptance (line 435) doesn't reference paging — but `SearchController.java` (line 167) must return paginated results. This is a soft dependency the parallel-graph claim hides.

**WEAK because:** the §6.5 Verification Matrix and §6 SP definitions are individually fine; the bug is in the §6 graph + §7.2 shared-artifact ownership table not naming these three cross-SP touch points. The §7.2 table (line 590) names `domain-registry.yaml`, `trio_integrity_allowlist.yaml`, `_MANIFEST.yaml`, sentinels — but does not name `BaseEntity.java` or the `JobDispatcher` interface contract. Adding these to §7.2 + reflecting the serialization in §6 closes the gap. **Recommended fix in §4 Synthesis #1 and #2.**

---

## §4 Synthesis (concrete fixes that improve the plan without rejecting it)

### Synthesis #1 — Re-serialize SP24/25/26 OR partition shared infra explicitly

**Choose ONE:**

- **(Preferred)** Re-order: `SP23 → SP25 (data + jobs, foundational infra) → SP24 ‖ SP26 (parallel; both depend on SP25's JobDispatcher impl + PageRequestNormalizer)`. This makes SP25 the foundation tier (mirrors PRD-1 §5.0 Phase 3 pattern: shared-foundation lands first, parallel forks land second). Total wall-time is unchanged or shorter because SP24 + SP26 can then truly run parallel.
- **(Alternative)** Keep parallel, but partition SP25's deliverables: ship `JobDispatcher` interface + `PageRequestNormalizer` in SP23 (or a new tiny SP23.5) so SP24's `ExportJobService.java` and SP26's `SearchController.java` have stable contracts to consume. SP25 then ships only impls.

In both cases, §7.2 shared-artifact ownership table must add rows:
- `templates/backend/jobs/JobDispatcher.java` (interface) — Sole writer SP25 (or SP23.5 in alt). Readers: SP24 (ExportJobService).
- `templates/backend/data/PageRequestNormalizer.java` — Sole writer SP25. Readers: SP26 (SearchController), all post-SP25 paged endpoints.

### Synthesis #2 — Resolve `BaseEntity` vs `BaseEntityWithSoftDelete` explicitly

SP25 must pick one of three resolutions explicitly in its ADR (TD-2026-05-18-025):

- **Option α — Extend SP13 `BaseEntity` in place.** Add `@SQLDelete` + `@Where` to existing `BaseEntity`. Every existing entity (5+ classes verified above) inherits soft-delete behavior without code change. SP25's `soft-delete-only-on-base-entity.md` rule then applies to all entities. **Pros:** no parallel hierarchy. **Cons:** modifies SP13 surface; rollback boundary for SP25 now includes restoring SP13's `BaseEntity`.
- **Option β — Ship `BaseEntityWithSoftDelete` as a NEW opt-in base for future entities.** Existing entities remain on plain `BaseEntity`. Rule `soft-delete-only-on-base-entity.md` has `applies_to: templates/backend/<new-domain>/**` only. **Pros:** zero existing-entity churn. **Cons:** parallel hierarchy = fork-receiver confusion.
- **Option γ — Rename `BaseEntity` → `BaseEntityWithSoftDelete` everywhere, migrate all existing entities atomically in SP25.** **Pros:** clean. **Cons:** SP25 blast radius blows up; touches every domain.

**Recommendation:** Option α. It matches the existing-entity inheritance pattern, keeps SP25 atomic, and the `@SQLDelete + @Where` change to SP13's BaseEntity is surgical (one file, three annotations).

### Synthesis #3 — Tier-1 cap: ship F13/F14/F15 as `/ax-verify` subcommands (the PRD's own fallback)

Per §1 Steelman: the fallback path the PRD names at `:550` is the correct architecture. Concrete change:

- `/ax-policy-check` → `/ax-verify policy-check --file <path> --intent <text>`
- `/ax-evidence-fetch` → `/ax-verify evidence-fetch --snapshot <id> | --all`
- `/ax-explain` → `/ax-verify explain --rule-id <id> | --violation-msg <msg>`

SP29 becomes "Extend `/ax-verify` with 3 new subcommands + the 50-fixture eval set + skill self-tests for each subcommand." Tier-1 count stays at 4. The 50-fixture FP-rate <5% gate stays. The pre-mutation semantics (the F13 unique value-add) live in the `--pre-mutation` flag.

**Cost:** small — SKILL.md for `/ax-verify` adds 3 subcommand sections. Tier-1 topology probe (`skills/_tests/tier1-topology.test.sh`) needs no amendment (still passes count=4 because `/ax-fork-receiver` is already the 4th).

**If the Planner / Critic insist on 3 new Tier-1s:** the ADR (TD-2026-05-18-029) must explicitly justify *why subcommands are insufficient* beyond presentation preference. The current draft does not.

### Synthesis #4 — SP26 first-run health check made interactive

Pre-mortem Scenario 3 (line 672) correctly identifies the Korean tokenization 70% recall issue but mitigates with passive `install.sh prints WARNING`. For the stated audience (Korean enterprise SaaS), this should be an **interactive prompt** at `/ax-fork-receiver` install time:

```
한글 검색 정확도: 기본 PostgreSQL FTS 어댑터는 한글 쿼리에서 ~70% 정확도를 보입니다.
프로덕션 권장: mecab-ko 분석기 설치 후 ax.search.tokenizer: mecab-ko 설정.
지금 mecab-ko 설치 가이드를 표시할까요? [Y/n]
```

This converts the warning from "passive log line" → "active decision the fork receiver makes during install." Aligns with composition-kit principle: the catalog surfaces real trades to the human, not buries them in stdout.

### Synthesis #5 — SP27 serverless: ship polling as the L4 default; SSE as opt-in

Per §3(b) above and §1.1 SSE steelman: the L4 `templates/L4/notification/` `(notification)/page.tsx` should default to TanStack Query polling (already shipped per SP16 deliberate trade per PRD draft line 143) and ship the SSE bridge as opt-in via blueprint manifest (`ax.realtime.transport: sse` or `polling`). The `SseEmitterConfig.java`, `RealtimeEventBus.java`, `RealtimeOutboxRelay.java` backend templates still ship in SP27 — they're available for fork receivers who provision a non-serverless backend (Spring Boot self-hosted). But the **default L4 wiring** stays on polling.

The current PRD draft (Scenario 1 mitigation, line 641: "WebSocketConfig.java is the alternate path. SP27 ships both paths atomically") names both paths but ships SSE as the L4 default with a README warning. The synthesis is: **flip the default**. L4 polling is the default; SSE/WebSocket are opt-in transports declared in the blueprint manifest.

### Synthesis #6 — SP27 form orchestration: pick "in-place upgrade" OR "extended" suffix, document the policy

Per §3(a) PASS-WEAK and §1.1 F11 steelman: SP27 must add an ADR clause to TD-2026-05-18-027 stating the chosen naming policy:

- (Preferred) In-place upgrade: `field-array.tsx` SP15 shell is replaced atomically by SP27's full implementation. The SP15 file path stays. SP27 acceptance includes "git diff shows additions to existing file, no new sibling."
- (Acceptable) Sibling pattern: `field-array-extended.tsx` ships alongside SP15 shell with the SP15 shell explicitly marked `@deprecated since SP27; use field-array-extended.tsx`. SP27 acceptance + PRD-1 §4.11 must be extended to allow `-extended` siblings as a named convention.

The current draft says "SP15 shells retained for back-compat; SP27 ADR documents the supersede contract" (line 196) but the ADR text isn't sketched. Sketch it.

---

## §5 DELIBERATE mode check

### Pre-mortem ≥ 4 scenarios with thresholds — PASS.

§8 ships 5 scenarios (4 required + 1 bonus). Each has: failure description, likelihood, detection mechanism, executable mitigation with named owner + command + threshold, recovery path.

- Scenario 1 (Vercel SSE leak) — threshold: `SERVERLESS_WARNING_MISSING` exit 1.
- Scenario 2 (i18n KRW / IME) — threshold: ANY format mismatch on KRW/JPY/방금 전 → SP28 halts.
- Scenario 3 (search Meilisearch dep) — threshold: Korean tokenization smoke ≥ 70% expected results on `PostgresFtsAdapter` default.
- Scenario 4 (`/ax-policy-check` false-positive cascade) — threshold: FP rate < 5% on 50-fixture eval set.
- Scenario 5 (parallel race on `domain-registry.yaml`) — bonus; inherits Catalog Extension §6.2 mitigation.

Each scenario meets the DELIBERATE-mode quality bar from PRD-1 §7. **PASS.**

### Verification Matrix observability_signal column populated for every SP — PASS.

§6.5 line 560 — all 7 rows have explicit observability signals:

- SP23: `template.evidence.coverage_ratio`, `archunit.violations`, `traceid.rule.protects_count`
- SP24: `webhook.hmac.violations`, `circuit_breaker.open_count`, `import.chunked.rowcount_p99`
- SP25: `jpa.audit.fields.populated_ratio`, `jobs.dlq.row_count`, `paging.max_limit_violations`
- SP26: `search.query.latency_p99_ms`, `ime.composition.corruption_count`, `trio.coverage_ratio.search`
- SP27: `sse.active_connections`, `sse.event_delivery_latency_ms_p99`, `form.dirty_block.fired_count`
- SP28: `i18n.hardcoded_string.violations`, `feature_flag.cache.hit_ratio`, `trio.coverage_ratio.feature-flags`
- SP29: `policy_check.false_positive_rate`, `evidence_fetch.refresh.attempts_total`, `explain.responses.cache_hit_ratio`

This is more concrete than PRD-1's iter2 Verification Matrix (iter2 Architect review flagged emission mechanism as PARTIAL). **The current draft matches PRD-1 iter2 fidelity.** Still inherits the iter2 PARTIAL — emission mechanism (Micrometer counter vs Playwright probe vs log scrape) is not always named. **PASS for matrix completeness; PARTIAL on emission mechanism (inherited from PRD-1).**

---

## §6 Per-dimension verdict summary

| Dim | Topic | Verdict | Justification |
|---|---|---|---|
| (a) | Boundary integrity (F5/F6/F11 placement) | **PASS-WEAK** | F5/F6 clean; F11 `-extended` suffix is undocumented in PRD-1 §4.11. Fix: Synthesis #6. |
| (b) | Verification closure | **PASS** | Every new template walked by a named verify skill. Tier-3 gate walks new paths (SP23 explicit; SP24–SP28 implicit via §3 guardrail). |
| (c) | Evidence chain density | **PASS** | 12 new snapshots; 7 ADRs with `provenance_class`. Meilisearch + recharts quote anchors need to be section-named in SP26 (SP-internal clarification, not blocking). |
| (d) | Anti-pattern resistance | **PASS** | No MockMvc (line 125). No governance loops — §11 line 820 explicit. |
| (e) | TDD anchoring | **PASS** | §6.5 Verification Matrix complete for all 7 SPs. SP29's 50-fixture FP-rate <5% gate is above-baseline rigor. |
| (f) | Spec Trio atomic rule (SP26 search, SP28 feature-flags) | **PASS** | Both ship atomic. §7.1 SP28 rollback wording slightly looser than SP26 — tighten. |
| (g) | Parallelizability claim (SP24‖25‖26) | **WEAK** | Forward reference SP24→SP25; `BaseEntityWithSoftDelete` collision with SP13; SP26 needs SP25 paging. Fix: Synthesis #1 + #2. |
| (h) | **NEW** Tier-1 cap exception (4→7) | **FAIL** | The PRD's own fallback (`:550`) is the correct architecture. Fix: Synthesis #3. |
| (i) | **NEW** SSE on serverless default | **WEAK** | Acknowledged trap shipped as default. Fix: Synthesis #5. |
| (j) | DELIBERATE pre-mortem ≥ 4 scenarios | **PASS** | 5 scenarios with thresholds + owners + commands. |
| (k) | DELIBERATE observability_signal column | **PASS** (PARTIAL inherited) | All 7 rows populated; emission mechanism not always named (inherits PRD-1 iter2 PARTIAL). |

**Tally: 7 PASS, 1 PASS-WEAK, 2 WEAK, 1 FAIL.** The FAIL on (h) and the WEAK on (g) + (i) are addressable in-draft via Syntheses #1, #2, #3, #5. Synthesis #4 and #6 are SP-internal polish.

---

## §7 Recommendation

**Likely-ITERATE.**

The PRD is execution-ready for SP23 in isolation, and the §6.5 Verification Matrix + §7 autonomous safety scaffolding inherits cleanly from PRD-1 + Catalog Extension. The three structural fixes are:

1. **(FAIL)** Synthesis #3 — collapse F13/F14/F15 into `/ax-verify` subcommands. The PRD already documents this as the fallback; flip default and fallback. **Tier-1 stays at 4.**
2. **(WEAK)** Syntheses #1 + #2 — re-serialize SP24/SP25/SP26 or partition `JobDispatcher` interface + `PageRequestNormalizer` into SP23; pick `BaseEntity` resolution (recommend Option α: extend SP13 `BaseEntity` in place with `@SQLDelete` + `@Where`).
3. **(WEAK)** Synthesis #5 — flip SP27 L4 default to polling; ship SSE backend as opt-in via blueprint manifest. Aligns with the existing SP16 deliberate-trade precedent.

Two SP-internal clarifications worth folding in:

4. Synthesis #4 — SP26 install-time interactive prompt for `mecab-ko` (Korean tokenization).
5. Synthesis #6 — SP27 naming-policy ADR (in-place upgrade vs `-extended` siblings).

**If Critic agrees and Planner implements Syntheses #1–#3 in iter2:** the PRD is APPROVE-ready. The other items (#4, #5) can ship as SP-internal clarifications in the SP26/SP27 PRs without blocking the PRD APPROVE.

**Hand-off:** Critic (Codex) round, then Planner iter2 if Critic concurs on the 3 structural fixes.

---

## §8 References

- `docs/superpowers/specs/2026-05-18-functional-extension-prd.draft.md:24` — Decision Driver 3 (skill-orchestrated pre-execution gating).
- `docs/superpowers/specs/2026-05-18-functional-extension-prd.draft.md:149` — F15 row (3rd new Tier-1).
- `docs/superpowers/specs/2026-05-18-functional-extension-prd.draft.md:165` — §5.1 `templates/backend/data/` lists `BaseEntityWithSoftDelete.java`.
- `docs/superpowers/specs/2026-05-18-functional-extension-prd.draft.md:188` — §5.3 L2 blocks `-extended` cluster.
- `docs/superpowers/specs/2026-05-18-functional-extension-prd.draft.md:191` — Form orchestration `-extended` suffix.
- `docs/superpowers/specs/2026-05-18-functional-extension-prd.draft.md:196` — SP15 shells "retained for back-compat".
- `docs/superpowers/specs/2026-05-18-functional-extension-prd.draft.md:277-300` — §6 SP dependency graph + critical ordering rules.
- `docs/superpowers/specs/2026-05-18-functional-extension-prd.draft.md:355` — SP24 `ExportJobService.java` forward reference to SP25's JobDispatcher.
- `docs/superpowers/specs/2026-05-18-functional-extension-prd.draft.md:444-450` — SP26 Korean tokenization 70% baseline risk.
- `docs/superpowers/specs/2026-05-18-functional-extension-prd.draft.md:478-479` — SP27 SSE on Vercel Scenario.
- `docs/superpowers/specs/2026-05-18-functional-extension-prd.draft.md:528-534` — SP29 3 new Tier-1 skills delivery spec.
- `docs/superpowers/specs/2026-05-18-functional-extension-prd.draft.md:549-550` — SP29 fallback path: ship as `/ax-verify` subcommands.
- `docs/superpowers/specs/2026-05-18-functional-extension-prd.draft.md:560-567` — §6.5 Verification Matrix (7 SPs × 8 cols).
- `docs/superpowers/specs/2026-05-18-functional-extension-prd.draft.md:584` — §7.1 SP28 rollback wording (slightly looser than SP26 line 582).
- `docs/superpowers/specs/2026-05-18-functional-extension-prd.draft.md:587-597` — §7.2 shared-artifact ownership table (missing `BaseEntity` + `JobDispatcher`).
- `docs/superpowers/specs/2026-05-18-functional-extension-prd.draft.md:628-645` — Pre-mortem Scenario 1 (SSE serverless trap).
- `docs/superpowers/specs/2026-05-18-functional-extension-prd.draft.md:672-691` — Pre-mortem Scenario 3 (search Meilisearch / Korean tokenization).
- `docs/superpowers/specs/2026-05-18-functional-extension-prd.draft.md:693-714` — Pre-mortem Scenario 4 (`/ax-policy-check` FP cascade).
- `docs/superpowers/specs/2026-05-18-functional-extension-prd.draft.md:806` — Open Question 1 (Tier-1 cap exception).
- `docs/superpowers/specs/2026-05-18-functional-extension-prd.draft.md:822` — §11 explicitly notes PRD-1 §3.2 VIOLATED by SP29 (+3 Tier-1).
- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.md:149` — PRD-1 §3.2: "No new top-level Tier-1 skill beyond {ax-transform, ax-verify, ax-scaffold}."
- `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.md:1003` — PRD-1 SP12 verification line: `skill.topology.tier_count` (assert tier1=3, tier2=8, tier3=6).
- `docs/superpowers/specs/2026-05-18-catalog-extension-prd.md:787` — Catalog Extension acknowledged 1st Tier-1 cap exception (`/ax-fork-receiver`, 3→4) with explicit ADR + open question.
- `docs/superpowers/specs/2026-05-18-catalog-extension-prd.md:97-100` — SP15 P0 items #18–#21 (form-section, field-array, conditional-field, form-error-summary).
- `templates/backend/notification/Notification.java:54` — `extends BaseEntity` (existing soft-delete via `deleted` field).
- `templates/backend/email-outbox/EmailOutbox.java:55` — `extends BaseEntity`.
- `templates/backend/email-outbox/EmailTemplate.java:52` — `extends BaseEntity`.
- `templates/backend/email-outbox/EmailOutboxRepository.java:60` — `WHERE e.deleted = false` (existing query pattern).
- `templates/backend/notification/NotificationRepository.java:53` — `AND n.deleted = false`.
- `templates/backend/file-storage/StoredFile.java:26` — existing `@Entity`.
- `docs/superpowers/specs/2026-05-17-frontend-templatization-architect-review-iter2.md:38-46` — pattern for "every PARTIAL → SP-internal clarification" disposition style.
- `CLAUDE.md:7-43` — Project Vision (composition kit; Korean enterprise primary audience).

---

**End of Architect review. Hand off to Critic / Codex for re-evaluation. Recommendation: likely-ITERATE with 3 structural fixes (Syntheses #1, #2, #3) + 2 SP-internal polish items (#4, #5, #6).**
