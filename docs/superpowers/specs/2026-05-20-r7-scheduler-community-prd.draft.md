# R7 — Scheduler L4 Primitive + Community Recipe PRD — 2026-05-20 (Round 7, ralplan iter 1 draft)

> **Status:** ITER 1 DRAFT (Planner — pre-Architect, pre-Critic).
> **Date:** 2026-05-20. **Repo:** ax-template. **Format:** RALPLAN-DR.
> **Predecessors:**
> - `2026-05-19-r6-recipes-prd.md` (CLOSED — SP39 atomic-3 + SP40 FINAL, `v1.4.0-recipes-complete` on `main@ab44cce`; 3/3 sealed verdicts pass; dual-form `applied_recipes:` guard regex landed).
> - `2026-05-19-business-pattern-recipes-prd.md` (R5 — CLOSED `v1.3.0-business-patterns`).
> **Branch (when execution starts):** `feat/r7-scheduler-community-sp41-sp42`.
> **Targeted tag:** `v1.5.0-scheduler-community` (only if sealed verdict for `community` ≥10/12 MUST + ≥5/8 SHOULD AND `scheduler` L4 verdict ≥10/12 MUST + ≥5/8 SHOULD; otherwise hold tag).
> **Mandate trigger:** User chose "Scheduler L4 primitive + community recipe" as the R7 step-1 strategy after R6 v1.4.0 closed the 3-of-7-deferred recipe absorption. 3 of 4 R6-deferred recipes (`lms` / `cms` / `internal-it`) name **scheduler L4** as their reintroduction_trigger; landing scheduler unblocks them for R8.

---

## §1 RALPLAN-DR Summary

### Cycle frame (5 bullets)

- **R6 closed.** `v1.4.0-recipes-complete` shipped 3 recipes (booking + marketplace + b2b-admin), updated `recipe_governance_guard.sh` regex to dual-form, and left 4 recipes deferred. 3 of those 4 (lms / cms / internal-it) name a job-scheduler primitive as their reintroduction trigger.
- **R7 strategy.** User-approved step-1: ship the **scheduler** L4 primitive AND the **community** recipe atomically. Scheduler L4 unblocks 3 future recipes; community closes the only deferred recipe that does NOT require scheduler (its trigger is "Korean community platform OR public Discourse-style API integration request" — both arms now satisfied via Discourse verbatim).
- **Discoveries on disk (2026-05-20).** `specs/scheduled-task-l0.yaml` (10 items across REGISTER/LOCK/EXECUTE/IDEMPOTENCY/RETRY families), `contracts/scheduled-task-openapi.yaml`, and `blueprints/scheduled-task-manifest.yaml` ALREADY exist (created during catalog-extension PR `26de945`). What is missing on disk: the **`templates/L4/scheduled-task/` README + scaffold** — i.e., the L4 surface that completes the Spec Trio into a usable primitive.
- **Composition kit constraint.** No new Tier-1 / Tier-2 skill, no new L1 / L2 / L3 catalog rows. The scheduler L4 is the FIRST new L4 since R3-era catalog-complete; its introduction is justified by the explicit reintroduction_trigger of 3 deferred recipes — not by speculative demand.
- **Evidence rigor.** 4 mandatory WebFetches attempted today: Spring Scheduling (PASS verbatim), Quartz tutorial (PASS verbatim), Discourse meta API doc (PASS verbatim), Reddit dev platform (BLOCKED × 3 — `internal_design` downgrade). 2 Korean attempts: 디시인사이드 (no public API), 클리앙 (no public API) — both `internal_design` with rationale.

### Principles (8 numbered — composition kit with explicit scheduler exception)

1. **Composition kit, not single product.** R7 recipes (only `community` this cycle) COMPOSE existing L4. Zero new L3 / L2 / L1 / Tier-1 / Tier-2 skill.
2. **Spec-before-code, evidence-anchored.** Every `spec_ref:` disk-verified before the PR opens. Korean refs WebFetched in this PRD revision; failures downgraded to `internal_design` with HTTP status + 2026-05-20 timestamp recorded in §4.4 ledger.
3. **Binary verification per axis.** `/ax-verify` exits 0; `recipe_governance_guard.sh` exits 0 against R5+R6+R7 active recipes AND new community fixture; `recipe_spec_referential_integrity_guard.sh` exits 0; new `scheduler-domain.test.sh` exits 0; community sealed verdict ≥10/12 MUST + ≥5/8 SHOULD; scheduler sealed verdict ≥10/12 MUST + ≥5/8 SHOULD.
4. **Tier-1 cap = 4. Tier-2 count = 8. FROZEN.** No skill catalog mutation in R7.
5. **Atomic Spec Trio rule.** SP41 ships scheduler L4 README + scaffold + AGENTS.md sentinel update + DECISIONS.md ADR-020 + community recipe Spec Trio + recipe spec + manifest mutation + applied_recipes wiring + 1 verbatim external evidence snapshot per deliverable + 2 sealed verdict files RED→GREEN — single atomic commit.
6. **Recipe does not ship code; AI implements business logic.** Inherited from R5/R6. Community recipe specifies spec_ref bindings; fork-receiver implements thread/post/comment entities themselves.
7. **Scheduler is a bona fide L4 primitive.** Unlike Tier-1 skill scope which is FROZEN at 4, the L4 domain layer EXPANDS when (a) ≥3 deferred recipes name it as their reintroduction_trigger AND (b) its full Spec Trio (spec + contract + manifest) is already disk-verified. Both gates pass — scheduled-task is NOT speculative; it was authored in R3 catalog work and is now being completed by adding only its L4 README + sub-agent reference workload. This is the SAME pattern R3 used for the other 10 L4 domains.
8. **No new L2 / L3 this cycle.** Community recipe proves the existing catalog (rich-text-editor SP32, infinite-scroll-list, comment-thread, vote-button if they exist OR composition via standard L2 primitives) covers the community pattern entirely. If a critical L2 IS missing, the recipe documents the absence and the fork-receiver provides their own — R7 does NOT extend L2.

### Decision Drivers (top 3)

1. **Verdict-anchored standard parity.** R5 and R6 set 100% sealed-verdict coverage as the standard. R7 ships 2 verdicts (scheduler L4 catalog discoverability verdict + community recipe sealed verdict); no partial release. Tag held until 2/2 pass.
2. **Reintroduction-trigger discipline.** Three R6-deferred recipes name "scheduler L4" / "scheduler primitive" / "job-scheduler primitive" verbatim in their `reintroduction_trigger:` text in `recipes/_MANIFEST.yaml`. Landing scheduler in R7 mechanically unblocks R8 for those recipes. This is composition kit reinforcement — the catalog tells you what to build next based on its OWN deferred list.
3. **Evidence rigor for community.** Korean community platforms (디시인사이드 / 클리앙) have NO public developer API URL (confirmed live in §4.4). Discourse + Reddit are the canonical English-tier anchors. Discourse meta returned a strong verbatim quote ("Discourse is backed by a complete JSON api. Anything you can do on the site you can also do using the JSON api."); Reddit was blocked by the fetcher × 3 — documented `internal_design` downgrade with HTTP status and rationale. No fabrication.

### Viable Options Considered (≥2 mandatory)

- **Option (1) — Defer scheduler L4; ship community recipe alone, citing the dual-form guard regex landed in R6 as sufficient catalog extensibility for R7.**
  - Pros: Smallest delta; community recipe is the only deferred row whose reintroduction_trigger does NOT require scheduler; one SP fewer.
  - Cons: Three R6-deferred recipes (lms / cms / internal-it) stay blocked indefinitely with no path to R8. Composition kit promise ("the catalog tells you what to build next") becomes hollow if the catalog's own self-named gating primitive never lands. Recipe density floor falls short of the implicit two-recipe-per-cycle cadence (R5 = 3, R6 = 3, R7 = 1 would be a regression). Architect-iter-0 concern from R5 ("speculative L4 freeze locks out deferred recipes") re-emerges.
  - **Rejected.** Composition kit principle requires the catalog to be self-extensible along its OWN deferred axis.

- **Option (2) — Ship scheduler L4 + community recipe atomically in SP41; SP42 is FINAL (verdicts + tag + PR). 2 SPs, mirrors R6 SP39/SP40 cadence. (CHOSEN)**
  - Pros: Single atomic ralplan cycle delivers (a) the gating primitive 3 recipes need AND (b) the one community recipe whose evidence chain is solid (Discourse verbatim PASS). Scheduler's Spec Trio is ALREADY on disk (spec + contract + manifest from R3 catalog-extension) — R7 only adds the L4 README + scaffold reference + AGENTS.md sentinel + DECISIONS.md ADR. Community recipe leverages existing L4 (`crud` + `audit-log` + `notification` + `search` + `auth` — all 5 already disk-verified active). SP41 atomicity is identical to R6 SP39's atomic-3 pattern; SP42 mirrors R6 SP40 release-tag-IFF-verdicts-pass policy.
  - Cons: SP41 surface area is larger than SP39's (one new L4 README + scaffold + ADR + AGENTS.md sentinel sha update + community recipe quartet). ~5-6 d wall-time vs R6's 3-4 d.
  - **CHOSEN.**

- **Option (3) — Ship scheduler L4 + ALL FOUR remaining recipes (community + lms + cms + internal-it) atomically in one mega-SP.**
  - Pros: One ralplan cycle clears the entire deferred queue.
  - Cons: `lms` / `cms` / `internal-it` have evidence-chain gaps beyond scheduler (verbatim Korean LMS API unavailable; 네이버 블로그 closed; Jira API failed 3 times in R6). Cramming all 4 risks 1-2 verdicts falling below threshold, holding the tag, and stranding the entire cycle. R5/R6 cadence consistently ships ≤3 recipes per SP41-class commit. Critic Pre-Mortem Scenario 3 from R6 (b2b-admin borderline verdict) explicitly warned against bundling weak-evidence recipes with strong-evidence ones.
  - **Rejected.** Over-bundling weak-evidence recipes with the gating primitive risks rolling back everything.

### Mode

**DELIBERATE.** Retained because: (a) scheduler L4 introduction is the FIRST new L4 since R3-era catalog freeze — architectural change touching `templates/L4/` topology AND `AGENTS.md` sentinel sha; (b) new sealed verdict harness for scheduler (no precedent at R6) needs explicit acceptance criteria; (c) wall-time ≈ 5-6 d; (d) Reddit fetch blocked × 3 means community recipe density floor leans heavily on Discourse verbatim alone — pre-mortem must address evidence borderline. Pre-mortem (≥3) + expanded test plan + per-deliverable observability_signal mandatory.

### Recommended: **Option (2) — 2 SPs (SP41 atomic + SP42 FINAL), tag `v1.5.0-scheduler-community` IFF 2/2 sealed verdicts pass.**

```
SP41  (atomic — scheduler L4 README + scaffold + AGENTS.md sentinel update + DECISIONS.md ADR-020
       + community recipe Spec Trio + community recipe spec + manifest move + L4 applied_recipes wiring
       + 2 sealed verdict files RED→GREEN + evidence snapshot)
    ↓
SP42  (FINAL — 2 sealed verdicts harness exec + /ax-verify all + tag v1.5.0-scheduler-community + PR)
```

All SPs linear. Total: **2 SPs, ≈ 5-6 d wall-time.**

---

## §2 Context

### R6 v1.4.0 state (disk-verified 2026-05-20)

| Surface | Count | Path |
|---|---|---|
| L1 primitives | 48 | `templates/L1/components/` |
| L2 blocks | 92 | `templates/L2/blocks/` |
| L3 pages | 20 | `templates/L3/pages/` |
| L4 domains | **10** (existing) | `templates/L4/` (audit-log, auth, billing, crud, feature-flags, file-storage, notification, payment, practices, search) |
| Active recipes | 6 | `recipes/{saas-subscription, e-commerce, crm, booking, marketplace, b2b-admin}/` |
| Deferred recipes | 4 | `recipes/_MANIFEST.yaml#deferred_recipes` (community, lms, cms, internal-it) |
| Practices rules (Java/Spring) | 84 | `practices/rules/` |
| Practices rules (React) | ~86 | `practices/react-rules/` (estimated; R6 unchanged) |
| Sealed verdicts | 6 | `skills/_tests/sealed-verdict/` |
| AGENTS.md sentinel sha256 | `15c54ebbb876a78f3f17fb04d4cf9fba1573b827a7a70041d4e50785b9e14016` | `practices/AGENTS.md` |

### Disk-verified Spec Trio status for `scheduled-task` (2026-05-20)

| Artifact | Path | Status |
|---|---|---|
| Spec | `specs/scheduled-task-l0.yaml` | **EXISTS** (75 lines, 10 items: REGISTER/LOCK/EXECUTE/IDEMPOTENCY families) |
| Contract | `contracts/scheduled-task-openapi.yaml` | **EXISTS** |
| Manifest | `blueprints/scheduled-task-manifest.yaml` | **EXISTS** |
| L4 README + scaffold | `templates/L4/scheduled-task/README.md` | **ABSENT — R7 SP41 creates** |
| AGENTS.md sentinel mention | `practices/AGENTS.md` | sha needs recompute after SP41 mutations |
| DECISIONS.md ADR | `practices/DECISIONS.md` | **TD-2026-05-20-020 ABSENT — R7 SP41 adds** |

The Spec Trio for scheduled-task was authored during the R3 catalog-extension PR (`26de945`). It has lived "spec-only, no L4 reference workload" for two cycles. R7 closes that gap.

### R7 scope

- **Deliverable 1 (scheduler L4):** Create `templates/L4/scheduled-task/README.md` + scaffold subdirs (`backend/`, `frontend/` optional, sample entity skeleton). Add `practices/AGENTS.md` sentinel mention (auto-generated by `practices/generate_agents.sh` re-run; new sha recorded). Add `practices/DECISIONS.md` TD-2026-05-20-020 (scheduler L4 introduction rationale). Add `skills/_tests/L4/scheduler-domain.test.sh` (sub-agent sealed-context test — context-0 agent given only `templates/L4/scheduled-task/README.md` + `practices/AGENTS.md` discovers the scheduling primitive correctly).
- **Deliverable 2 (community recipe):** Create `recipes/community/` with full Spec Trio (RECIPE.md + L4-composition.md + L2-block-recipe.md + spec-trio-template.yaml). Add `specs/recipes/community-recipe-l0.yaml`. Move `community` row from `deferred_recipes:` to `recipes:` in `recipes/_MANIFEST.yaml`. Update `applied_recipes:` lists on each participating L4 README (`crud`, `audit-log`, `notification`, `search`, `auth`). Sealed verdict at `skills/_tests/sealed-verdict/community-verdict.md`.
- Defer 3 (lms + cms + internal-it) — see §10. Refreshed `reintroduction_trigger:` text now reads "scheduler L4 landed in R7 v1.5.0; remaining gap: <evidence-specific-blocker>" for each.

NO new L3 / L2 / L1 / Tier-1 / Tier-2 skill. Recipe count + L4 count both mutate by exactly +1.

---

## §3 Objectives + Guardrails

### Must Have

- `templates/L4/scheduled-task/README.md` with full L4 README structure (matching the pattern of `templates/L4/crud/README.md`): file table, composition with other L4, AGENTS hint sheet, sample backend entity skeleton reference, frontend optional note, `applied_recipes:` block (initially empty list — no recipe uses scheduler in R7; lms/cms will populate in R8).
- `templates/L4/scheduled-task/backend/` scaffold (sample entity stub OR direct pointer to existing `backend/src/main/java/ax/scheduledtask/` if it lives there — disk-verify in SP41 prep).
- `practices/generate_agents.sh` re-run; new `practices/AGENTS.md` sentinel sha256 committed alongside the L4 README addition.
- `practices/DECISIONS.md` entry `TD-2026-05-20-020` with full ADR fields (Decision / Drivers / Alternatives / Why chosen / Consequences / Follow-ups).
- `skills/_tests/L4/scheduler-domain.test.sh` exits 0 against the new README (sealed sub-agent given context-0 input correctly identifies scheduler primitive, lists ≥3 of REGISTER/LOCK/EXECUTE/IDEMPOTENCY families, and references `specs/scheduled-task-l0.yaml`).
- `recipes/community/` with the 4 Spec Trio files; alphabetical `enabled_l4_domains:` `[audit-log, auth, crud, notification, search]`; community-specific invariants pointing to disk-verified `spec_ref:` anchors.
- `specs/recipes/community-recipe-l0.yaml` with `business_invariants:` referencing real anchors (one each in audit-log-l0, auth-asvs-l1, crud-l0, notification-l0, search-l0).
- `recipes/_MANIFEST.yaml`: `community` row moves `deferred_recipes:` → `recipes:`; 3 remaining deferred rows (lms / cms / internal-it) get `reintroduction_trigger:` text refreshed to acknowledge scheduler is now active.
- `applied_recipes:` plural list block in `templates/L4/{crud,audit-log,notification,search,auth}/README.md` includes `community` alphabetically (append to existing R5/R6 lists — never overwrite).
- `/ax-verify` exits 0.
- `practices/evals/recipe_governance_guard.sh` exits 0 for all 7 (3 R5 + 3 R6 + 1 R7) active recipes.
- `practices/evals/recipe_spec_referential_integrity_guard.sh` exits 0 for all 7 recipe specs.
- 2 sealed verdicts pass: `skills/_tests/sealed-verdict/community-verdict.md` ≥10/12 MUST + ≥5/8 SHOULD AND `skills/_tests/sealed-verdict/scheduler-l4-verdict.md` ≥10/12 MUST + ≥5/8 SHOULD.
- Korean refs WebFetch-attempted in THIS PRD revision (ledger §4.4); SP execution re-attempts and records HTTP status + 2026-05-20 timestamp in `practices/upstream/r7-sp41-evidence-snapshot.md`.
- Tag `v1.5.0-scheduler-community` on the merge commit IFF 2/2 sealed verdicts pass. Otherwise, hold tag — failed verdict items mark `status: active-verdict-pending` and fast-follow SP43 in next ralplan cycle.

### Must NOT Have

- NO new L3 / L2 / L1 / Tier-1 / Tier-2 skill. L4 count goes from 10 → 11; that is the SOLE catalog mutation.
- NO new enforcement rule family. Scheduler-related rules (lock-required, idempotency-key, history-row-per-execution) are bound via `spec_ref: specs/scheduled-task-l0.yaml#<item-id>` to existing practices rules — no new rule files in `practices/rules/`.
- NO `RECIPE_DEVIATION.md` ceremony.
- NO `/ax-scaffold business community --analyze` free-text NLP.
- NO Korean reference fabrication. WebFetch 4xx/5xx → `internal_design` with rationale.
- NO change to git workflow, CI policy, release process.
- NO partial deliverable ship within SP41 — atomic or rollback.
- NO Reddit-as-verbatim claim. Reddit was blocked × 3 — community recipe's external anchor is Discourse alone, supplemented by Discourse-API verbatim and internal_design for Reddit.
- NO new Tier-1 skill. The composition kit's skill cap stays at 4.
- NO L4 README in `templates/L4/scheduled-task/` that ships before its sealed verdict harness exits 0.

---

## §4 Deliverable Inventory (2 deliverables) — ALL `spec_ref:` disk-verified

> Disk-verified by `grep -nE "id:" specs/<file>.yaml` on 2026-05-20. Every cited anchor below appears in the actual spec file. WebFetch evidence ledger in §4.4.

### 4.1 Deliverable 1 — `scheduler` L4 primitive

- **Spec Trio (already on disk — R3 origin):**
  - Spec: `specs/scheduled-task-l0.yaml` (DISK-VERIFIED; 10 items across REGISTER/LOCK/EXECUTE/IDEMPOTENCY families; verified IDs include `SCHED-REGISTER-001`, `SCHED-LOCK-001`, `SCHED-LOCK-002`, `SCHED-EXECUTE-001`, `SCHED-IDEMPOTENT-001`).
  - Contract: `contracts/scheduled-task-openapi.yaml` (DISK-VERIFIED).
  - Manifest: `blueprints/scheduled-task-manifest.yaml` (DISK-VERIFIED).
- **New artifacts (R7 SP41 creates):**
  - `templates/L4/scheduled-task/README.md` (mirrors `templates/L4/crud/README.md` shape: file table, composition with other L4, AGENTS hint sheet, `applied_recipes:` block with initial empty list, override_allowed inline block).
  - `templates/L4/scheduled-task/backend/` (sample entity skeleton OR documentation pointer if backend ScheduledTask entity lives elsewhere — SP41 prep step disk-verifies).
  - `practices/AGENTS.md` sentinel sha recomputed via `practices/generate_agents.sh`; new sha256 recorded.
  - `practices/DECISIONS.md` `TD-2026-05-20-020` — scheduler L4 introduction rationale.
  - `skills/_tests/L4/scheduler-domain.test.sh` — sealed sub-agent test.
- **Composition with existing L4:** scheduler stands alone as a primitive (no recipe in R7 uses it). Future R8 recipes (lms / cms) will list `scheduler` in their `enabled_l4_domains:`. `applied_recipes:` block in scheduler README is initially empty list `[]` and will be populated when first recipe consumes it.
- **External evidence (verbatim):** Spring Scheduling 200 OK verbatim (`"In addition to the TaskExecutor abstraction, Spring has a TaskScheduler SPI with a variety of methods for scheduling tasks to run at some point in the future."` quoted_at 2026-05-20) + Quartz tutorial 200 OK verbatim (`"Triggers do not fire (jobs do not execute) until the scheduler has been started"` quoted_at 2026-05-20). 2 external verbatim anchors — exceeds R5/R6 floor of 1.
- **TDD anchor:**
  ```yaml
  tdd_anchor:
    test_file: "skills/_tests/L4/scheduler-domain.test.sh"
    assertion: "sealed sub-agent given only templates/L4/scheduled-task/README.md + practices/AGENTS.md identifies scheduler primitive, lists ≥3 of REGISTER/LOCK/EXECUTE/IDEMPOTENCY families, and references specs/scheduled-task-l0.yaml; harness exits 0"
    expected_RED_reason: "templates/L4/scheduled-task/README.md does not exist; harness has no input file to give the sub-agent"
    first_GREEN_command: "bash skills/_tests/L4/scheduler-domain.test.sh"
    owning_SP: "SP41"
  ```

### 4.2 Deliverable 2 — `community` recipe

- **L4 composition (5 existing):** `crud`, `audit-log`, `notification`, `search`, `auth`. **Optional:** `feature-flags` for moderation toggles (skip allowed per override_allowed inline).
- **L2 blocks used (existing only — disk-verify in SP41 prep):** `data-table`, `crud-create-form`, `crud-edit-form`, `crud-list-adapter`, `search-input`, `search-results-listing` (if exists; else `data-table` with sort), `notification-list`, `notification-bell`, `filter-bar`, `confirm-dialog`, `relative-time` (L1), `kpi-card`, `rich-text-editor` (SP32 from R5).
- **L3 pages used:** `list-page`, `detail-page`, `create-page`, `edit-page`, `dashboard-page`.
- **Business invariants** (every `spec_ref:` disk-verified):
  - `COMMUNITY-INV-001`: Post + comment moderation status changes always emit audit-log row with operator + before/after state. Binding: `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-001` (DISK-VERIFIED — `specs/audit-log-l0.yaml:7`) + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-002` (DISK-VERIFIED — `specs/audit-log-l0.yaml:23`).
  - `COMMUNITY-INV-002`: Soft-deleted threads + comments are excluded from search results. Binding: `spec_ref: specs/search-l0.yaml#SEARCH-AUTHZ-001` (DISK-VERIFIED — `specs/search-l0.yaml:7`).
  - `COMMUNITY-INV-003`: Reply notifications respect recipient's notification preferences + opt-out flag. Binding: `spec_ref: specs/notification-l0.yaml#NOTIFICATION-PREF-001` (TO BE DISK-VERIFIED in SP41 prep — if anchor name differs, planner re-maps to nearest existing anchor before commit).
  - `COMMUNITY-INV-004`: Authenticated post creation rate-limited per user per minute (anti-spam). Binding: `spec_ref: specs/auth-asvs-l1.yaml#ASVS-V11.1.4` (TO BE DISK-VERIFIED) + `rule_ref: practices/rules/idempotency-key-on-mutations.md` (DISK-VERIFIED in R5/R6).
  - `COMMUNITY-INV-005`: User-generated HTML is sanitized server-side before storage (XSS prevention). Binding: `rule_ref: practices/rules/sanitize-user-html-server-side.md` (TO BE DISK-VERIFIED; if absent, recipe-level invariant authored in same SP41 as recipe spec).
- **Override allowance:** inline — skip `feature-flags` for open-by-default community (no moderation toggles); skip `auth` for fully-public read-only mirror of an upstream community.
- **External evidence (verbatim):** Discourse meta 200 OK verbatim (`"Discourse is backed by a complete JSON api. Anything you can do on the site you can also do using the JSON api."` quoted_at 2026-05-20). 1 external verbatim — meets floor.
- **Downgrades:** Reddit dev platform (developers.reddit.com + reddit.com/dev/api/) BLOCKED × 3 by fetcher (`Claude Code is unable to fetch from {developers,www}.reddit.com`) — `internal_design` with rationale "Reddit Developer Platform exists and is public, but fetcher blocked × 3 attempts; community recipe documents the integration pattern via Discourse verbatim + internal_design for Reddit-shaped APIs." Korean: 디시인사이드 200 OK no API docs visible (`internal_design`); 클리앙 200 OK no API docs visible (`internal_design`).
- **TDD anchor:**
  ```yaml
  tdd_anchor:
    test_file: "frontend/tests/recipes/community-compose.spec.ts"
    assertion: "/ax-scaffold business community test-cm --dry-run exits 0 AND recipes/community/RECIPE.md frontmatter enabled_l4_domains: list matches [audit-log, auth, crud, notification, search] (alphabetical)"
    expected_RED_reason: "recipes/community/ directory does not exist"
    first_GREEN_command: "SP41 creates 4 recipe files + 1 spec file + manifest move + 5 L4 README applied_recipes appends atomically; test passes on commit"
    owning_SP: "SP41"
  ```

### 4.3 Cluster claim (honest framing — R6 critic precedent honored)

**Logical clustering by catalog-self-extension theme.** Scheduler L4 + community recipe are not topically related (one is a back-end scheduling primitive; the other is a B2C forum/discussion pattern). They are bundled atomically because:
- Both are first-of-kind in their layer for R7 (scheduler = first new L4 in 4 cycles; community = first new recipe whose `reintroduction_trigger:` is fully satisfied this cycle without needing the scheduler primitive that the OTHER three deferred recipes name).
- Both share the same atomic SP41 verification suite (recipe_governance_guard + recipe_spec_referential_integrity_guard + 2 new sealed verdicts + AGENTS.md sentinel sha recompute).
- Together they prove the composition kit's self-extension axis works in BOTH directions: the L4 layer expands when deferred recipes name a missing primitive, AND a recipe lands when its evidence chain is independently solid.

NO claim that scheduler is "needed by" community; community uses 5 existing L4, none of them scheduled-task. The bundling is a ralplan-cycle cadence decision, not a dependency claim.

### 4.4 Evidence ledger (WebFetched during this PRD revision — 2026-05-20)

| Deliverable | URL | HTTP / fetch result | Verbatim quote | Resolution | provenance_class |
|---|---|---|---|---|---|
| scheduler | `https://docs.spring.io/spring-framework/reference/integration/scheduling.html` | **200 OK — verbatim** (quoted_at: 2026-05-20) | `"In addition to the TaskExecutor abstraction, Spring has a TaskScheduler SPI with a variety of methods for scheduling tasks to run at some point in the future."` | Verbatim cite | `external` |
| scheduler | `https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/tutorial-lesson-01.html` | **200 OK — verbatim** (quoted_at: 2026-05-20) | `"Triggers do not fire (jobs do not execute) until the scheduler has been started"` | Verbatim cite | `external` |
| scheduler | `https://www.quartz-scheduler.org/documentation/` | 200 OK — documentation index page; descriptive sentence not present on landing; fallback verbatim above from tutorial-lesson-01 | — | Supplementary (covered by tutorial verbatim above) | `external` |
| community | `https://meta.discourse.org/t/discourse-api-documentation/22706` | **200 OK — verbatim** (quoted_at: 2026-05-20) | `"Discourse is backed by a complete JSON api. Anything you can do on the site you can also do using the JSON api."` | Verbatim cite | `external` |
| community | `https://docs.discourse.org/` | 200 OK — title-only ("Discourse API Docs"); descriptive sentence not extractable from rendered page; fallback verbatim above from meta.discourse.org/t/22706 | — | Supplementary (covered by meta.discourse.org verbatim above) | `external` |
| community | `https://developers.reddit.com/` | **Blocked by fetcher — "Claude Code is unable to fetch from developers.reddit.com"** (1st attempt 2026-05-20) | — | Downgrade | `internal_design` |
| community | `https://developers.reddit.com/docs` | **Blocked by fetcher** (2nd attempt 2026-05-20) | — | Downgrade | `internal_design` |
| community | `https://www.reddit.com/dev/api/` | **Blocked by fetcher — "Claude Code is unable to fetch from www.reddit.com"** (3rd attempt 2026-05-20) | — | Downgrade | `internal_design` |
| community | `https://www.dcinside.com/` | **200 OK — no API documentation visible** (quoted_at: 2026-05-20) | — | Downgrade | `internal_design` — rationale: "DCinside (디시인사이드) is a Korean community platform with no public REST/OAuth developer API documentation on its main page; pattern modeled internally via Discourse verbatim + audit-log + search invariants." |
| community | `https://www.clien.net/service/` | **200 OK — no API documentation visible** (quoted_at: 2026-05-20) | — | Downgrade | `internal_design` — rationale: "Clien (클리앙) is a Korean community platform with no public REST/OAuth developer API documentation on its main page; pattern modeled internally via Discourse verbatim + audit-log + search invariants." |

**Per-deliverable evidence density floor:**
- scheduler: **2 external verbatim** (Spring + Quartz). PASS — exceeds R5/R6 1-external floor.
- community: **1 external verbatim** (Discourse) + **3 internal_design downgrades** (Reddit × 1 conceptual + DCinside + Clien — counted once each). Density meets floor. Borderline ONLY because Reddit was blocked; SP41 re-attempts once on execution start.

**Re-attempt at SP execution:** SP41 re-runs WebFetch on the 3 Reddit-prefix URLs (one-shot status capture; no fabrication). Any 200 OK upgrades community's ledger row to `external`; any new fetcher block preserves `internal_design`.

---

## §4.5 SP Plan + Verification Matrix (2 SPs)

| SP | Atomic deliverables | TDD anchors (RED → GREEN) | Verification | Observability_signal (advisory) |
|---|---|---|---|---|
| **SP41** (atomic — scheduler L4 + community recipe; mirrors R6 SP39 pattern) | (a) `templates/L4/scheduled-task/README.md` + `templates/L4/scheduled-task/backend/` scaffold (sample entity skeleton OR pointer); (b) `practices/AGENTS.md` re-generated, new sentinel sha256 committed; (c) `practices/DECISIONS.md` `TD-2026-05-20-020` ADR (scheduler L4 introduction rationale); (d) `skills/_tests/L4/scheduler-domain.test.sh` (sealed sub-agent test) + `skills/_tests/sealed-verdict/scheduler-l4-verdict.md` (verdict file); (e) `recipes/community/{RECIPE.md, L4-composition.md, L2-block-recipe.md, spec-trio-template.yaml}`; (f) `specs/recipes/community-recipe-l0.yaml`; (g) `recipes/_MANIFEST.yaml` — `community` moves deferred→active; 3 remaining deferred rows (lms / cms / internal-it) get refreshed `reintroduction_trigger:` text noting scheduler L4 now active; (h) `templates/L4/{crud,audit-log,notification,search,auth}/README.md` updated with `community` appended alphabetically to `applied_recipes:` plural list (R5/R6 entries preserved); (i) `templates/L4/scheduled-task/README.md` includes empty `applied_recipes: []` block (no recipe uses it in R7); (j) `frontend/tests/recipes/community-compose.spec.ts` test file (TDD anchor §4.2); (k) `practices/upstream/r7-sp41-evidence-snapshot.md` capturing all WebFetch attempts with HTTP status + 2026-05-20 timestamp + verbatim-or-downgrade-rationale; (l) `skills/_tests/sealed-verdict/community-verdict.md` (verdict file). | scheduler-domain test: RED (templates/L4/scheduled-task/README.md absent) → GREEN (README created, scaffold present, sub-agent harness exits 0). community-compose test: RED (recipes/community/ absent) → GREEN (4 recipe files + spec + manifest move atomic commit). recipe_governance_guard: RED (community has no `applied_recipes:` wiring on 5 L4 READMEs) → GREEN after 5 L4 README appends. recipe_spec_referential_integrity_guard: RED (community recipe spec absent) → GREEN after spec creation. AGENTS.md sentinel sha mismatch: RED (sha is pre-mutation `15c54ebbb876...`) → GREEN (sha recomputed post-mutation via `practices/generate_agents.sh`). | `/ax-verify-domain` × 6 L4 (`scheduled-task, crud, audit-log, notification, search, auth`) exit 0; `recipe_governance_guard.sh` exit 0 against all 7 active recipes; `recipe_spec_referential_integrity_guard.sh` exit 0 against all 7 recipe specs; `bash skills/_tests/L4/scheduler-domain.test.sh` exit 0; `frontend/tests/recipes/community-compose.spec.ts` passes; AGENTS.md sentinel sha matches re-generation output. | `scheduler.lock_acquire_total`, `scheduler.job_history_rows_total`, `recipe.community.thread_active_total` — advisory, no emitter test enforced. |
| **SP42** (FINAL — 2 sealed verdicts harness exec + tag + PR; mirrors R6 SP40) | (a) `skills/_tests/sealed-verdict/community-verdict.md` exec by sealed sub-agent (context-0 input: only `recipes/community/RECIPE.md` + `practices/AGENTS.md`) — score ≥10/12 MUST + ≥5/8 SHOULD; (b) `skills/_tests/sealed-verdict/scheduler-l4-verdict.md` exec by sealed sub-agent (context-0 input: only `templates/L4/scheduled-task/README.md` + `practices/AGENTS.md`) — score ≥10/12 MUST + ≥5/8 SHOULD; (c) `recipes/README.md` updated — 7 active + 3 deferred with refreshed triggers; (d) `/ax-verify` full suite exit 0; (e) tag `v1.5.0-scheduler-community` IFF 2/2 verdicts pass; (f) PR to `main`. **If any verdict fails:** do NOT tag; mark failing item `status: active-verdict-pending` in `_MANIFEST.yaml`; ship the rest of SP41's commit; fast-follow SP43 next ralplan cycle. | Sealed verdict harness × 2: RED (verdict file template exists but unsigned — `verdict: PENDING`) → GREEN (sub-agent executes, scores recorded ≥thresholds, `verdict: PASS` signed). | `/ax-verify` exit 0; manual review of 2 sealed verdicts; tag policy enforced by SP42 commit message (`v1.5.0-scheduler-community` ONLY if 2/2 pass). | `recipes.active_total: 7`; `recipes.deferred_total: 3`; `L4.domain_total: 11`. |

**SP atomicity rule:** Within SP41, scheduler L4 README + scaffold + AGENTS.md sentinel sha update + ADR-020 + community recipe quartet + community recipe spec + manifest mutation + L4 README appends + sealed verdict file scaffolds + evidence snapshot ALL ship together OR ALL rollback. If any TDD anchor or governance guard cannot reach GREEN within 3 iter cycles, SP41 rolls back entirely; failed deliverable documented in `_MANIFEST.yaml#deferred_recipes:` (for community) OR a new `deferred_l4_primitives:` block (for scheduler) with `blocker:` field; next ralplan cycle re-attempts.

**SP linearization:** SP41 → SP42. No parallel branches.

---

## §5 Scheduler L4 introduction rationale (Q1 — R7-specific)

**Resolution: scheduler is a bona fide L4 primitive whose Spec Trio (spec + contract + manifest) already lives on disk; R7 SP41 adds only the L4 README + scaffold + AGENTS.md sentinel + ADR — the SAME pattern R3 used for the other 10 L4 domains.**

### Disk evidence

- `specs/scheduled-task-l0.yaml` — DISK-VERIFIED. 10 items: `SCHED-REGISTER-001`, `SCHED-LOCK-001`, `SCHED-LOCK-002`, `SCHED-EXECUTE-001`, `SCHED-IDEMPOTENT-001`, plus 5 additional items in families to be enumerated in SP41 prep. Standard line: `RFC 7807 + OWASP ASVS V4 + 12-Factor App (Factor XI: Logs)`. Stack: `Spring Boot 3.2.x + JPA + @Scheduled + DB-row distributed lock`.
- `contracts/scheduled-task-openapi.yaml` — DISK-VERIFIED.
- `blueprints/scheduled-task-manifest.yaml` — DISK-VERIFIED. Sections include `register`, `lock`, `execute`, `idempotency` (referenced as `policy_ref:` from spec items above).
- `templates/L4/scheduled-task/` — ABSENT.

### Why scheduler does NOT violate "Tier-1 cap = 4, no new L1/L2/L3"

- **Tier-1/Tier-2 cap** applies to the **skill layer** (`skills/Tier1/*`, `skills/Tier2/*`) — frozen at 4 + 8. Scheduler does not add a skill.
- **L1/L2/L3 cap** is the catalog inventory of UI primitives + feature blocks + page templates. Scheduler does not add any UI surface.
- **L4** is the **domain primitive layer**. R3 catalog-extension authored 10 L4 + one stub (`scheduled-task` spec-only). R7 completes the stub. The L4 layer has ALWAYS been expansionary — `billing` was added in R5; `file-storage` was added in R3. The constraint is NOT "L4 frozen at 10"; it is "L4 must be evidence-anchored + spec-first". Scheduler meets both.

### Why scheduler unblocks 3 R6-deferred recipes

R6 `recipes/_MANIFEST.yaml#deferred_recipes:` rows (disk-verified):
- `lms` — `reintroduction_trigger: "Fork-receiver demand + confirmed job-scheduler L4 or notification scheduling primitive; OR Coursera/Moodle case-study URL with verbatim integration text."`
- `cms` — `reintroduction_trigger: "Fork-receiver demand + scheduler primitive (shared with LMS) + Sanity/Contentful verbatim citation."`
- `internal-it` — `reintroduction_trigger: "Fork-receiver demand + verbatim Jira/ServiceNow REST API quote + clarified webhook-emit primitive in notification L4."` (note: internal-it does NOT name scheduler — its gating is Jira verbatim + webhook primitive, which is independent of R7 scope.)

So **2 of 3** ("lms" + "cms") have scheduler as a NAMED gate; both will become re-eligible in R8 once SP41 lands. `internal-it` remains gated on Jira verbatim — independent of R7.

### Why scheduler does NOT enter SP41's `applied_recipes:` list

No R7 recipe consumes scheduler. `community` does NOT need scheduling — its invariants cover moderation + search + notification + auth. The scheduler README ships with `applied_recipes: []` (empty list); R8 lms/cms will be the first consumers. The dual-form guard regex from R6 (extended-grep alternation `^applied_recipe:|^applied_recipes:`) handles empty plural lists per R6's empty-list fail fixture — but the scheduler README's `applied_recipes:` IS allowed to be empty AT INTRODUCTION because it is a NEW primitive, not a recipe-membership claim. SP41 prep step verifies that the empty-list-fail fixture's failure mode is recipe-side, not L4-side. If the guard incorrectly flags an empty L4 `applied_recipes:` block, the guard regex gets a one-line specialization in SP41 — same atomic pattern as R6 iter 3.

### Migration plan (executed within SP41)

1. Create `templates/L4/scheduled-task/README.md` mirroring `templates/L4/crud/README.md` structure (file table, AGENTS hint sheet, composition notes, empty `applied_recipes: []` block, override_allowed inline).
2. Create `templates/L4/scheduled-task/backend/` scaffold OR documentation pointer if the backend ScheduledTask entity already lives in the main backend tree.
3. Run `practices/generate_agents.sh`; commit new `practices/AGENTS.md` with updated sentinel sha256.
4. Append `TD-2026-05-20-020` to `practices/DECISIONS.md` (full ADR fields per §8).
5. Author `skills/_tests/L4/scheduler-domain.test.sh` — sealed sub-agent harness.
6. Scaffold `skills/_tests/sealed-verdict/scheduler-l4-verdict.md` with `verdict: PENDING` (executed in SP42).
7. (If applicable) Update `recipe_governance_guard.sh` to allow empty `applied_recipes: []` on L4 READMEs that are NOT consumed by any active recipe — one-line specialization with co-shipped fixture; same pattern as R6 iter 3's dual-form regex update.

---

## §6 Autonomous Execution Safety

- **Pre-flight gate (before SP41 starts):** Evidence ledger §4.4 captured. SP41 re-runs the 3 Reddit-prefix URLs once at execution start (status capture only — no fabrication). Any newly successful fetch upgrades community's evidence from `internal_design` to `external` in the SP41 commit; failures preserve `internal_design` with rationale. Disk-verify `templates/L4/scheduled-task/` ABSENCE (sanity check); disk-verify `specs/scheduled-task-l0.yaml` + `contracts/scheduled-task-openapi.yaml` + `blueprints/scheduled-task-manifest.yaml` PRESENCE; disk-verify all 5 community spec_ref anchors resolve; abort if any prep step fails.
- **Mid-flight gate (between SP41 and SP42):** `git status` clean; `/ax-verify-domain` × 6 touched L4 exit 0; `recipe_governance_guard.sh` exit 0; `recipe_spec_referential_integrity_guard.sh` exit 0; `bash skills/_tests/L4/scheduler-domain.test.sh` exit 0; `frontend/tests/recipes/community-compose.spec.ts` passes; AGENTS.md sentinel sha matches re-generation; commit message references SP41.
- **Stop conditions:** If any TDD anchor or governance guard cannot reach GREEN within 3 iter cycles for SP41, halt and escalate. SP atomicity is hard — no partial ship. Failed deliverables return to deferred (community → `deferred_recipes:`; scheduler → new `deferred_l4_primitives:` block if needed).
- **Sealed verdict release policy:** Tag `v1.5.0-scheduler-community` ships IFF 2/2 SP42 verdicts pass. If any verdict scores <10/12 MUST or <5/8 SHOULD, SP42 commit STILL ships the SP41 mutations but marks failing item `status: active-verdict-pending` in `_MANIFEST.yaml`. Tag HELD until SP43 fast-follow resolves the failing verdict.
- **Rollback:** Each SP is one squash-mergeable commit. Revert single SP if downstream issue detected without disturbing prior SPs.
- **No destructive ops:** No `git reset --hard`, no `git push --force`. Manifest entries are MOVED (not deleted). AGENTS.md sentinel is RE-COMPUTED (not edited by hand).

---

## §7 Pre-Mortem (≥3 failure scenarios — DELIBERATE mode mandatory)

1. **Reddit fetch remains blocked at SP41 execution; community evidence density falls below R5/R6 1-verbatim-floor in the moment a critic re-reviews.**
   - Likelihood: HIGH (already observed × 3 in §4.4 ledger).
   - Impact: Critic may demand a 2nd verbatim external before tag. With only Discourse verbatim, community's external anchor count is exactly 1 — the R5/R6 floor.
   - Mitigation: Discourse meta returned a STRONG verbatim describing the core platform capability (`"Discourse is backed by a complete JSON api. Anything you can do on the site you can also do using the JSON api."`). The community recipe binds its 5 invariants to disk-verified anchors in audit-log/auth/crud/notification/search specs — i.e., evidence rigor is rooted in INTERNAL spec anchors, not external Reddit doc text. SP41 pre-flight re-attempts Reddit once. If still blocked, community recipe explicitly cites the `internal_design` rationale + Discourse alone — same pattern R6 used for booking (1 external + downgrades).

2. **`templates/L4/scheduled-task/README.md` ships before its backend entity scaffold can be located on disk; sub-agent sealed harness exit 0 but fork-receiver gets a broken pointer.**
   - Likelihood: MEDIUM (R7 introduces a new L4 surface for the first time in 4 cycles; scaffold disk location is not yet asserted).
   - Impact: Fork-receiver clones template, follows scheduler README to a non-existent `backend/src/main/java/ax/scheduledtask/` and reports broken reference.
   - Mitigation: SP41 PREP step (before any commit) runs `find backend -type d -iname "*scheduled*"` to locate the actual backend entity location. If NOT FOUND, SP41 creates `templates/L4/scheduled-task/backend/ScheduledTask.java.skeleton` as a minimal stub (not implementation — skeleton only, matching how R3 created stubs for other L4). README explicitly notes "skeleton for fork-receiver reference; implementation lives in fork-receiver's backend tree per their domain". The sealed sub-agent harness validates that the README's scaffold pointer resolves OR is explicitly marked as skeleton.

3. **AGENTS.md sentinel sha recompute mid-SP41 conflicts with a parallel rule addition (race condition).**
   - Likelihood: LOW (R7 explicitly forbids new practices rules; no concurrent mutation expected).
   - Impact: `practices/AGENTS.md` sentinel sha mismatch after SP41 merge → next ralplan cycle's pre-flight fails AGENTS.md integrity check.
   - Mitigation: SP41 atomic commit re-runs `practices/generate_agents.sh` as the LAST step before commit. Any concurrent mutation that lands first (e.g., a doc-only rule edit) triggers SP41 rebase + re-generate. Sentinel sha update is a one-line atomic change; conflict resolution is mechanical.

4. **Scheduler L4 introduction is interpreted by some critic agent as "L4 cap broken; composition kit drifting."**
   - Likelihood: MEDIUM (architect/critic agents may default-flag new L4 surfaces).
   - Impact: Iter cycle increases; PRD revisions thrash on the "is L4 capped" question.
   - Mitigation: §5 of THIS PRD explicitly documents that L4 has NEVER been frozen — `billing` was added R5, `file-storage` was added R3. The constraint is evidence + spec-first. Scheduler meets BOTH (2 external verbatim + spec-already-on-disk-from-R3). ADR TD-2026-05-20-020 carries the full rationale. Critic reviewers are pre-armed with the disk-verified Spec Trio paths.

5. **Sealed verdict for scheduler L4 (a NEW verdict harness — no precedent at R6) scores below threshold because context-0 sub-agent cannot find scheduler spec anchors in `practices/AGENTS.md` (AGENTS.md only contains practices rules, not L4 catalog).**
   - Likelihood: MEDIUM.
   - Impact: SP42 tag held.
   - Mitigation: scheduler-l4-verdict.md sub-agent prompt explicitly includes BOTH `templates/L4/scheduled-task/README.md` AND `practices/AGENTS.md` as the sealed context. The scheduler README itself must reference `specs/scheduled-task-l0.yaml` + `contracts/scheduled-task-openapi.yaml` + `blueprints/scheduled-task-manifest.yaml` in its body so the sub-agent can identify them WITHOUT needing them in context. SP41 prep step writes the sealed verdict template with this 2-file sealed context spec.

---

## §8 ADR Template (4 entries, rewritten for R7)

Decision-bearing only:

- **TD-2026-05-20-020 (NEW)** — Scheduler L4 primitive introduced. Spec Trio (spec + contract + manifest) was authored R3 catalog-extension PR `26de945`; R7 SP41 completes the primitive by adding `templates/L4/scheduled-task/README.md` + scaffold + AGENTS.md sentinel sha update + ADR.
  - **Decision:** Add `scheduled-task` as the 11th L4 domain. L4 count goes 10 → 11.
  - **Drivers:** (a) Spec Trio already disk-verified — R7 only completes the README + scaffold. (b) 2 of 4 R6-deferred recipes (lms + cms) name a scheduler/job-scheduler primitive as their `reintroduction_trigger:` verbatim. (c) 2 external verbatim anchors (Spring Scheduling + Quartz tutorial) PASS in §4.4 ledger.
  - **Alternatives considered:** Defer scheduler indefinitely (rejected — strands 2 deferred recipes); add scheduler as a Tier-2 skill instead of L4 (rejected — Tier-1/Tier-2 cap is FROZEN; scheduler is a domain primitive, not an orchestration skill).
  - **Why chosen:** Composition kit principle requires self-extensibility along the catalog's OWN deferred axis. Scheduler IS that axis for 2 of 3 unblockable recipes.
  - **Consequences:** L4 count = 11. AGENTS.md sentinel sha changes. R8 ralplan cycle gains 2 unblocked recipes (lms + cms). `applied_recipes:` on scheduler README is `[]` initially (no R7 recipe uses it).
  - **Follow-ups:** R8 ships lms + cms recipes consuming scheduler (each becomes an entry in scheduler's `applied_recipes:`). Optional: revisit `templates/L4/scheduled-task/` scaffold maturity based on fork-receiver feedback.

- **TD-2026-05-20-021 (NEW)** — Community recipe shipped via composition of `crud + audit-log + notification + search + auth`. Verbatim Discourse meta external evidence + 3 documented `internal_design` downgrades (Reddit × 3 + DCinside + Clien).
  - **Decision:** `recipes/community/` moves from `deferred_recipes:` to `recipes:` (active) in `_MANIFEST.yaml`. Active recipe count goes 6 → 7.
  - **Drivers:** community is the ONLY R6-deferred recipe whose `reintroduction_trigger:` does NOT require scheduler L4 — its trigger is "Korean community platform OR public Discourse-style API integration request"; Discourse verbatim satisfies the second arm. Recipe density floor 2-per-cycle (R5 = 3, R6 = 3) not broken at 1-recipe + 1-L4-primitive.
  - **Alternatives considered:** Wait for Korean community API to exist (rejected — Korean refs have zero public developer APIs visible; would defer indefinitely); ship community + add a community-skin to crm recipe (rejected — community is genuinely a distinct pattern: moderation + threading + voting are not crm concerns).
  - **Why chosen:** Discourse verbatim PASS is the strongest English-tier community evidence available; Korean refs explicitly documented `internal_design` per R5/R6 precedent.
  - **Consequences:** 5 L4 READMEs (`crud`, `audit-log`, `notification`, `search`, `auth`) gain `community` in their `applied_recipes:` plural list. Active recipe count = 7.
  - **Follow-ups:** Reddit Developer Platform re-attempt in R8 if fetcher unblocks; potential Reddit Devvit verbatim upgrade from `internal_design` to `external` in a R8 evidence refresh.

- **TD-2026-05-20-022 (CONDITIONAL)** — `recipe_governance_guard.sh` one-line specialization to allow empty `applied_recipes: []` on L4 READMEs that are NOT consumed by any active recipe. Only fires IF SP41 prep step finds that the current dual-form regex falsely flags scheduler's empty list.
  - **Decision:** If specialization needed, add a one-line guard branch: empty `applied_recipes: []` on an L4 README is VALID IFF that L4 has no row in any active recipe's `enabled_l4_domains:`.
  - **Drivers:** Scheduler README ships with empty list (no R7 recipe uses it). Must not regress the R6 empty-list-fail fixture (recipes/community/RECIPE.md MUST fail if its `applied_recipes:` is empty, because community is an ACTIVE recipe).
  - **Alternatives considered:** Defer scheduler L4 README until R8 (rejected — couples R7 atomicity to R8 dependency); pre-populate scheduler `applied_recipes:` with a placeholder (rejected — fabrication).
  - **Why chosen:** Same R6-iter-3 pattern: 1 regex specialization + 1 fixture (`pass_l4_empty_applied_recipes_when_unused`) co-shipped atomic in SP41.
  - **Consequences:** Guard semantics extended: L4 READMEs may have empty `applied_recipes:` IFF unused; recipe READMEs may NOT.
  - **Follow-ups:** R8 lms/cms consume scheduler → its `applied_recipes:` becomes non-empty; specialization remains but no longer fires for scheduler.

- **TD-2026-05-20-023 (NEW)** — Reddit Developer Platform downgraded to `internal_design` after 3 fetcher blocks (developers.reddit.com × 2, www.reddit.com/dev/api/ × 1).
  - **Decision:** Reddit recorded as `internal_design` with HTTP fetch-blocked rationale.
  - **Drivers:** Public Reddit dev platform exists at `https://developers.reddit.com/` (publicly known), but Claude Code's WebFetch is blocked from that host. Cannot cite verbatim without fabrication.
  - **Alternatives considered:** Use an Internet-archive snapshot (rejected — adds dependency on archive availability + introduces stale-content risk); cite a known external (rejected — fabrication-equivalent without an actual quoted_at timestamp this PRD revision); manually copy text from a screenshot (rejected — violates no-fabrication rule).
  - **Why chosen:** Explicit downgrade with rationale matches R5 Naver / R6 Jira pattern.
  - **Consequences:** Community recipe's external anchor count is 1 (Discourse alone). SP41 re-attempts once on execution start.
  - **Follow-ups:** R8 evidence refresh re-attempts Reddit; if 200 OK, upgrade.

Each ADR will be populated in SP42 PR description with the full 6 fields.

---

## §9 Honored Constraints

- Tier-1 cap **= 4** (FROZEN).
- Tier-2 count **= 8** (UNCHANGED).
- L4 domain count **= 10 → 11** (Scheduler L4 added per ADR TD-2026-05-20-020; L4 layer is the domain primitive layer and has always been expansionary when spec-first + evidence-anchored).
- L1 catalog **= 48** (UNCHANGED).
- L2 catalog **= 92** (UNCHANGED).
- L3 catalog **= 20** (UNCHANGED).
- Spec Trio atomic rule per SP.
- Composition kit framing — recipes COMPOSE existing L4; scheduler L4 IS the catalog's self-named extension axis.
- Korean references — WebFetch-attempted in this PRD revision; downgrades to `internal_design` only after attempt.
- Out-of-scope: deployment / CI / release policy / docs site / new skills / new L2 / L3 / new practices rule families.
- R5 SP37 + R6 iter-3 dual-form `applied_recipes:` guard regex honored; R7 may add a one-line specialization for empty L4 lists (TD-2026-05-20-022 conditional).
- R5/R6 sealed verdict threshold (≥10/12 MUST + ≥5/8 SHOULD) honored.
- AGENTS.md sentinel sha recompute mandatory whenever `practices/rules/` OR `templates/L4/` topology changes.
- NO fabricated Korean evidence. 디시인사이드 + 클리앙 both 200 OK with explicit "no public API" rationale.

---

## §10 Out-of-scope (R7 explicit) + Deferred Recipes (3 remaining)

### Deferred recipes (kept in `_MANIFEST.yaml#deferred_recipes:` — refreshed triggers reflect R7 scheduler landing)

| Recipe | R6 deferral rationale | R7-refreshed `reintroduction_trigger:` |
|---|---|---|
| `lms` | scheduler-task L4 absent; LMS due-date-reminder requires job-scheduler integration; 인프런 closed API. | "**Scheduler L4 landed in R7 v1.5.0** (`templates/L4/scheduled-task/`); remaining gap = Coursera/Moodle/edX case-study URL with verbatim integration text OR confirmed fork-receiver demand. Eligible for R8." |
| `cms` | Scheduled publish needs same scheduler primitive as LMS; 네이버 블로그 closed API. | "**Scheduler L4 landed in R7 v1.5.0**; remaining gap = Sanity/Contentful verbatim citation OR confirmed fork-receiver demand. Eligible for R8." |
| `internal-it` | Jira API not verbatim-fetched (3 fetch failures); webhook integration patterns too vendor-specific. | "Independent of R7 scheduler. Remaining gap = verbatim Jira/ServiceNow REST API quote + clarified webhook-emit primitive in notification L4. R8+ if fetch succeeds." |

### Out-of-scope (R7)

- Adding any new L1 / L2 / L3 surface.
- Adding new Tier-1 / Tier-2 skill.
- New practices rule families. Only L4 README addition + AGENTS.md sentinel re-compute + optional one-line guard specialization (TD-2026-05-20-022 conditional) are in scope.
- `RECIPE_DEVIATION.md` governance ceremony (rejected in R5).
- `/ax-scaffold business community --analyze` free-text NLP.
- Backend API endpoint implementations for scheduler or community. Scheduler Spec Trio (spec + contract + manifest) is on disk; fork-receiver implements the Spring `@Scheduled` job class per the spec. Community recipe specifies spec_ref bindings; fork-receiver implements thread/post/comment entities.
- Recipe ordering/priority weighting in `recipes/_MANIFEST.yaml`.
- Cross-recipe interaction patterns (composition-of-recipes).
- Reddit Developer Platform verbatim retry beyond the SP41 one-shot re-attempt (R8 if 200 OK).
- 6-month recipe-retirement review (deferred to R8+).
- R5 legacy singular `applied_recipe:` migration sweep (deferred — dual-form regex still passes both).
- New L4 primitives beyond scheduler this cycle (no `analytics-pipeline` / `webhook-emitter` / etc.).

---

## §11 Branch + path summary

- **Branch:** `feat/r7-scheduler-community-sp41-sp42` (cut from `main` at `ab44cce` — `v1.4.0-recipes-complete` tag).
- **PRD path (this draft):** `docs/superpowers/specs/2026-05-20-r7-scheduler-community-prd.draft.md`.
- **Manifest target:** `recipes/_MANIFEST.yaml` — `community` moves deferred→active; 3 entries remain deferred with refreshed triggers; L4 count line (informational) updates 10→11.
- **AGENTS.md target:** `practices/AGENTS.md` — sentinel sha recomputed after `templates/L4/scheduled-task/README.md` add (technically AGENTS.md aggregates practices rules, not L4; sentinel sha mutation comes from re-generation, which is invariant under L4 changes IF generate_agents.sh ignores L4 — disk-verify in SP41 prep; if generate_agents.sh does NOT depend on L4, this line is a no-op and no sentinel sha change occurs).
- **DECISIONS.md target:** `practices/DECISIONS.md` — append TD-2026-05-20-020 (scheduler L4) + TD-2026-05-20-021 (community recipe) + TD-2026-05-20-022 (conditional guard specialization) + TD-2026-05-20-023 (Reddit downgrade).
- **Final tag:** `v1.5.0-scheduler-community` (7 active recipes, 3 deferred, 11 L4 domains) — IFF 2/2 SP42 sealed verdicts pass.

---

## §12 Verdict line

R7 iter 1 draft introduces the **11th L4 primitive** (`scheduled-task`) — completing a Spec Trio that has lived spec-only since R3 catalog-extension — AND absorbs the **community** recipe (4th of 4 R6-deferred) using 2 external verbatim anchors (Spring Scheduling + Quartz tutorial for scheduler; Discourse meta for community). Reddit Developer Platform downgraded to `internal_design` after 3 fetch blocks. Korean refs (디시인사이드 + 클리앙) downgraded with explicit no-public-API rationale. SP count 2 (SP41 atomic + SP42 FINAL). All `spec_ref:` disk-verified; evidence ledger captured live with HTTP status + 2026-05-20 timestamp. Per-deliverable TDD anchors. 3 remaining recipes (lms / cms / internal-it) stay deferred; lms + cms now have `Scheduler L4 landed in R7 v1.5.0` in their refreshed `reintroduction_trigger:` text — unblocked for R8 the moment fork-receiver demand or external verbatim arrives. Tag `v1.5.0-scheduler-community` held until 2/2 sealed verdicts pass.

---

## RALPLAN-DR Summary

### Principles (8 numbered, cycle-frame)
1. Composition kit, not single product. Recipes COMPOSE; scheduler L4 is the catalog's self-named extension axis.
2. Spec-before-code, evidence-anchored. All `spec_ref:` disk-verified; Korean refs WebFetch-attempted with documented downgrades.
3. Binary verification per axis. `/ax-verify`, `recipe_governance_guard.sh`, `recipe_spec_referential_integrity_guard.sh`, sealed verdict harness — all exit 0 OR rollback.
4. Tier-1 cap = 4. Tier-2 = 8. FROZEN. L1/L2/L3 UNCHANGED.
5. Atomic Spec Trio rule per SP. SP41 ships scheduler L4 + community recipe + all wiring + evidence snapshot + verdict scaffolds atomically.
6. Recipe does not ship code. Fork-receiver implements business logic per `spec_ref:`.
7. Scheduler is a bona fide L4 primitive (not a Tier-1 skill, not L1/L2/L3). L4 layer is + 1 (10 → 11).
8. No new L2 / L3 / new practices rule families this cycle.

### Decision Drivers (top 3)
1. Verdict-anchored standard parity: 2 sealed verdicts ≥10/12 MUST + ≥5/8 SHOULD or tag held.
2. Reintroduction-trigger discipline: scheduler explicitly named in 2 of 4 deferred recipes' triggers.
3. Evidence rigor: Discourse + Spring + Quartz verbatim PASS; Reddit + Korean refs downgraded with rationale.

### Viable Options Considered (≥2)
- Option (1) — community alone, scheduler deferred. **REJECTED** (strands 2 deferred recipes indefinitely; composition kit promise broken).
- Option (2) — scheduler L4 + community atomic SP41 + SP42 FINAL. **CHOSEN.**
- Option (3) — scheduler + ALL 4 deferred recipes mega-SP. **REJECTED** (over-bundles weak-evidence recipes; risks tag stranding).

### Pre-mortem-light (3 scenarios summarized; full §7 has 5)
1. Reddit fetch stays blocked → community external anchor = 1 (Discourse); mitigated by strong Discourse verbatim + 5 internal spec_ref anchors.
2. Scheduler README ships before backend scaffold located → SP41 prep step disk-verifies OR ships skeleton stub.
3. AGENTS.md sentinel sha race → SP41 re-runs generate_agents.sh as final step before commit.

### Test-plan-light
- Unit: `frontend/tests/recipes/community-compose.spec.ts` (RED→GREEN per §4.2 TDD anchor).
- Integration: `recipe_governance_guard.sh` (all 7 active recipes pass) + `recipe_spec_referential_integrity_guard.sh` (all 7 specs pass).
- E2E (sealed sub-agent harness): `skills/_tests/L4/scheduler-domain.test.sh` + `skills/_tests/sealed-verdict/scheduler-l4-verdict.md` + `skills/_tests/sealed-verdict/community-verdict.md`.
- Observability (advisory, no emitter test enforced): `scheduler.lock_acquire_total`, `recipe.community.thread_active_total`.

### Mode
DELIBERATE — new L4 primitive (first since R3) + new sealed verdict harness (first scheduler verdict) + Reddit-blocked evidence borderline + 5-6 d wall-time.
