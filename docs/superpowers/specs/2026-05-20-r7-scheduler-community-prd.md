# R7 — Scheduler L4 Primitive + Community Recipe PRD — 2026-05-20 (Round 7, ralplan iter 3 APPROVED)

> **Status:** APPROVED (3-iter ralplan consensus; Codex Critic iter 3 final APPROVE).
> **Date:** 2026-05-20. **Repo:** ax-template. **Format:** RALPLAN-DR.
> **Iter 1:** `2026-05-20-r7-scheduler-community-prd.draft.md` (467 lines, Architect ITERATE 2 HIGH + 4 MEDIUM + Synthesis-B + Codex Critic ITERATE 1 BLOCKING + 5 hard + 5 soft).
> **Iter 2:** `2026-05-20-r7-scheduler-community-prd.iter2.md` (433 lines, Architect APPROVE + 1 INFORMATIONAL; Codex Critic ITERATE narrow: `applied_recipes: []` literal × 5 + §4 heading drift).
> **Iter 3:** mechanical text-only fix (literal removed, heading "3 SPs"); `2026-05-20-r7-critic-codex-iter3.md` → APPROVE.
> **Branch (when execution starts):** `feat/r7-scheduler-community-sp41-sp42`.
> **Targeted tag:** `v1.5.0-scheduler-community` IFF 2/2 sealed verdicts pass; else partial `v1.5.0-scheduler` per Synthesis-B partial-tag policy.
> **Predecessors:** `2026-05-19-r6-recipes-prd.md` (CLOSED, `v1.4.0-recipes-complete` on `main@ab44cce`).

---

## §1 RALPLAN-DR Summary

### Cycle frame (5 bullets)

- **R6 closed.** `v1.4.0-recipes-complete` shipped 3 recipes (booking + marketplace + b2b-admin) and left 4 deferred. **2 of those 4** (lms / cms) name a job-scheduler primitive as their reintroduction trigger (NOT 3 — `internal-it` is gated by Jira verbatim + webhook, independent of scheduler).
- **R7 strategy.** User-approved step-1: ship the **scheduler** L4 primitive AND the **community** recipe. Scheduler L4 unblocks 2 future recipes (lms, cms); community closes the only deferred recipe that does NOT need scheduler (its trigger is "Korean community platform OR public Discourse-style API integration request" — Discourse arm satisfied via verbatim).
- **Disk discoveries (2026-05-20).** `specs/scheduled-task-l0.yaml` (10 items), `contracts/scheduled-task-openapi.yaml`, `blueprints/scheduled-task-manifest.yaml` ALREADY exist (R3 catalog-extension PR `26de945`, disk-confirmed by Architect). Missing: `templates/L4/scheduled-task/` README + scaffold.
- **Composition kit constraint.** No new Tier-1/Tier-2 skill, no new L1/L2/L3 rows. Scheduler L4 is the first new L4 since R3 — justified by 2 deferred recipes naming it.
- **Evidence rigor (this iter).** 5 mandatory external + 5 Korean attempts + 3 Reddit alternative-host attempts (M1+M2 fixes). Spring + Quartz + Discourse verbatim PASS; Reddit GitHub archive 200 OK verbatim PASS (upgrades Reddit from `internal_design` → `external` per M2); 5 Korean attempts all 200 OK with no scheduler/community Korean verbatim — documented zero-Korean-cycle exception in §4.4 with explicit rationale.

### Principles (8 numbered)

1. **Composition kit, not single product.** R7 recipes (only `community` this cycle) COMPOSE existing L4. Zero new L3/L2/L1/Tier-1/Tier-2 skill.
2. **Spec-before-code, evidence-anchored AT PRD SIGNATURE.** All 5 community `spec_ref:` are disk-resolved in §4.2 (iter 2 fix M3). No "TO BE DISK-VERIFIED" labels.
3. **Binary verification per axis.** `/ax-verify` exits 0; `recipe_governance_guard.sh` exits 0; `recipe_spec_referential_integrity_guard.sh` exits 0; `scheduler-domain.test.sh` exits 0; 2 sealed verdicts pass.
4. **Tier-1 cap = 4. Tier-2 count = 8. FROZEN.** L1 = 49 (UNCHANGED, corrected from iter 1's 48 per H1). L2 = 92. L3 = 20. L4 = 10 → 11.
5. **Atomic Spec Trio rule per SP.** SP41 ships scheduler L4 atomically; SP41b ships community recipe atomically (sequential gate); SP42 final tag IFF 2/2 verdicts pass.
6. **Recipe does not ship code; AI implements business logic.** Inherited from R5/R6.
7. **Scheduler is a bona fide L4 primitive.** Spec Trio (spec + contract + manifest) already disk-verified; SP41 only adds the L4 README + scaffold + ADR — SAME pattern R3 used for the other 10 L4 domains. L4 layer is expansionary when spec-first + evidence-anchored.
8. **No new L2/L3/practices rule families this cycle.** Community recipe binds to existing rules (`idempotency-key-on-mutations.md` + `recipe-invariants-must-resolve.md`) and existing ASVS anchors. The XSS sanitize concern is co-shipped as a recipe-level invariant tagged `co-shipped-rule:` (no new rule file in `practices/rules/` — see M3 fix below).

### Decision Drivers (top 3)

1. **Verdict-anchored standard parity.** R5/R6 set 100% sealed-verdict coverage. R7 ships 2 verdicts; tag held unless 2/2 pass; partial tag `v1.5.0-scheduler` shipped if only scheduler verdict passes (Synthesis-B fix).
2. **Reintroduction-trigger discipline.** **2 of 4** R6-deferred recipes (lms + cms) name "scheduler L4" / "scheduler primitive" verbatim in `recipes/_MANIFEST.yaml`. `internal-it` is independent. Landing scheduler in R7 unblocks lms + cms for R8.
3. **Evidence rigor.** Reddit GitHub-archive 200 OK verbatim (3 quotes: OAuth, rate limit, user-agent) closes the M2 host-block concern. Discourse meta verbatim PASS. Korean cycle is zero-verbatim with explicit rationale (M1).

### Viable Options Considered (4 options — added Option 4 per Architect Synthesis-B + Critic L)

- **Option (1) — Defer scheduler L4; ship community alone.**
  - Pros: Smallest delta.
  - Cons: Strands lms + cms indefinitely; composition kit promise broken; recipe density falls below R5/R6 cadence.
  - **REJECTED.**

- **Option (2) — Atomic SP41 (scheduler + community in one commit) + SP42 FINAL.** (iter 1 choice)
  - Pros: Cadence-parity with R6 SP39/SP40 atomic-2; one PR-cycle on happy path.
  - Cons: Mutation surfaces disjoint (Architect §Synthesis); scheduler verdict harness is net-new shape with MEDIUM failure likelihood (§7 Pre-Mortem 5); if scheduler verdict fails, community is stranded in `active-verdict-pending` through no fault of its own.
  - **REJECTED in iter 2** in favor of Option (4).

- **Option (3) — Mega-SP shipping scheduler + ALL 4 deferred recipes.**
  - Pros: Clears entire deferred queue.
  - Cons: lms/cms/internal-it have evidence-chain gaps; risks 1-2 verdicts below threshold; over-bundling weak with strong evidence.
  - **REJECTED.**

- **Option (4) — Synthesis-B: SP41 scheduler-atomic + SP41b community-atomic-sequential + SP42 partial-tag-aware FINAL.** (NEW iter 2; recommended by Architect + Critic)
  - Pros: Mutation surfaces remain disjoint (no merge complexity penalty vs Option 2). If scheduler-l4-verdict fails (§7 Pre-Mortem 5 MEDIUM), community work is **never started** — saves 3-4 d effort. If community-verdict fails, scheduler still ships clean at partial tag `v1.5.0-scheduler`. Atomic Spec Trio rule (Principle 5) preserved WITHIN each SP. Resolves M3 (3 unresolved invariants is a real risk that splits cleanly into community's own SP boundary).
  - Cons: 3 SPs vs 2; slight wall-time increase on happy path (~0.5 d for second PR cycle); cadence "parity" with R6 is reframed as "atomic-per-axis, multi-SP" (R6 SP39/SP40 was atomic-on-overlapping-L4-mutations; R7 SP41/SP41b/SP42 is atomic-on-disjoint-axis).
  - **CHOSEN.** Architect explicitly recommended this; Critic Pre-Mortem 5 risk + M3 unresolved-invariants risk both split cleanly along the SP41/SP41b boundary. Cadence-parity-with-R6 was the strongest pro-bundle argument and it survives the reframe: each axis is still atomic.

### Mode

**DELIBERATE.** Retained: (a) first new L4 since R3; (b) net-new scheduler verdict harness; (c) wall-time ≈ 5-6 d with sequential SP41→SP41b path; (d) partial-tag policy is a NEW SP42 semantic that needs explicit acceptance criteria.

### Recommended: Option (4) — 3 SPs (SP41 scheduler-atomic + SP41b community-atomic-sequential + SP42 FINAL with partial-tag policy).

```
SP41   (atomic — scheduler L4 README + scaffold + AGENTS.md sentinel (if applicable) + DECISIONS.md ADR-020
        + skills/_tests/L4/scheduler-domain.test.sh + skills/_tests/sealed-verdict/scheduler-l4-verdict.md PENDING
        + Spring/Quartz/Reddit-archive evidence snapshot)
   ↓ gated on bash skills/_tests/L4/scheduler-domain.test.sh exit 0 AND scheduler-l4-verdict ≥10/12 MUST + ≥5/8 SHOULD
SP41b  (atomic — recipes/community/ quartet + specs/recipes/community-recipe-l0.yaml
        + 5 L4 README applied_recipes appends + recipes/_MANIFEST.yaml move
        + skills/_tests/sealed-verdict/community-verdict.md PENDING + Discourse evidence snapshot)
   ↓ gated on recipe_governance_guard.sh + recipe_spec_referential_integrity_guard.sh exit 0 AND community-verdict ≥10/12 + ≥5/8
SP42   (FINAL — /ax-verify full; tag IFF 2/2; partial tag v1.5.0-scheduler IFF only scheduler verdict passes; PR)
```

All SPs linear. Total: **3 SPs, ≈ 5-6 d wall-time.**

---

## §2 Context

### R6 v1.4.0 state (disk-verified 2026-05-20, corrected per H1)

| Surface | Count | Path |
|---|---|---|
| L1 primitives | **49** (corrected from iter 1's 48) | `templates/L1/components/` — `ls templates/L1/components/ \| wc -l` returns 49 |
| L2 blocks | 92 | `templates/L2/blocks/` |
| L3 pages | 20 | `templates/L3/pages/` |
| L4 domains | **10** (existing) | `templates/L4/` (audit-log, auth, billing, crud, feature-flags, file-storage, notification, payment, practices, search) |
| Active recipes | 6 | `recipes/{saas-subscription, e-commerce, crm, booking, marketplace, b2b-admin}/` |
| Deferred recipes | 4 | `recipes/_MANIFEST.yaml#deferred_recipes` (community, lms, cms, internal-it) |
| Practices rules (Java/Spring) | 84 | `practices/rules/` |
| Sealed verdicts | 6 | `skills/_tests/sealed-verdict/` |
| AGENTS.md sentinel sha256 | `15c54ebbb876a78f3f17fb04d4cf9fba1573b827a7a70041d4e50785b9e14016` | `practices/AGENTS.md` |

### Disk-verified Spec Trio status for `scheduled-task` (2026-05-20)

| Artifact | Path | Status |
|---|---|---|
| Spec | `specs/scheduled-task-l0.yaml` | **EXISTS** (10 items: REGISTER/LOCK/EXECUTE/IDEMPOTENCY families) |
| Contract | `contracts/scheduled-task-openapi.yaml` | **EXISTS** |
| Manifest | `blueprints/scheduled-task-manifest.yaml` | **EXISTS** |
| L4 README + scaffold | `templates/L4/scheduled-task/README.md` | **ABSENT — SP41 creates** |
| AGENTS.md sentinel mention | `practices/AGENTS.md` | sha may not change — see §11 hedge resolution below |
| DECISIONS.md ADR | `practices/DECISIONS.md` | **TD-2026-05-20-020 ABSENT — SP41 adds** |

### R7 scope

- **Deliverable 1 (scheduler L4, SP41 atomic):** Create `templates/L4/scheduled-task/README.md` + scaffold. **NO `applied_recipes:` key** at introduction (matches `file-storage` + `practices` L4 README pattern — Architect H2, Critic disposition #2). Add `practices/DECISIONS.md` TD-2026-05-20-020. Add `skills/_tests/L4/scheduler-domain.test.sh`. Re-run `practices/generate_agents.sh` ONLY if it depends on L4 topology (verified in SP41 prep — see §11).
- **Deliverable 2 (community recipe, SP41b atomic-sequential):** Create `recipes/community/` quartet + `specs/recipes/community-recipe-l0.yaml`. Move `community` row `deferred_recipes:` → `recipes:`. Update `applied_recipes:` plural list on 5 participating L4 READMEs (`crud`, `audit-log`, `notification`, `search`, `auth`) — append-only, R5/R6 entries preserved.
- Defer 3 (lms + cms + internal-it) — refreshed `reintroduction_trigger:` text.

NO new L3/L2/L1/Tier-1/Tier-2 skill. Recipe count + L4 count both mutate by exactly +1.

---

## §3 Objectives + Guardrails

### Must Have

- `templates/L4/scheduled-task/README.md` mirroring `templates/L4/crud/README.md` shape (file table, AGENTS hint sheet, composition notes, override_allowed inline) — **WITHOUT `applied_recipes:` key** (H2 fix).
- `templates/L4/scheduled-task/backend/` skeleton stub OR pointer to existing backend tree (SP41 prep `find backend -type d -iname "*scheduled*"`).
- `practices/DECISIONS.md` `TD-2026-05-20-020` ADR (scheduler L4 introduction).
- `skills/_tests/L4/scheduler-domain.test.sh` exits 0.
- `skills/_tests/sealed-verdict/scheduler-l4-verdict.md` ≥10/12 MUST + ≥5/8 SHOULD (SP42).
- `recipes/community/` with 4 Spec Trio files; `enabled_l4_domains: [audit-log, auth, crud, notification, search]` (alphabetical); 5 disk-verified invariants per §4.2.
- `specs/recipes/community-recipe-l0.yaml`.
- `recipes/_MANIFEST.yaml`: community moves deferred→active; 3 remaining deferred rows get refreshed `reintroduction_trigger:` text — **2 of 4 (lms + cms)** acknowledge scheduler now active; internal-it text unchanged on scheduler axis (Critic soft #3 fix).
- `applied_recipes:` plural list on 5 L4 READMEs (`crud`, `audit-log`, `notification`, `search`, `auth`) appends `community` alphabetically.
- `/ax-verify` exits 0.
- `recipe_governance_guard.sh` exits 0 on all 7 active recipes.
- `recipe_spec_referential_integrity_guard.sh` exits 0 on all 7 specs.
- `skills/_tests/sealed-verdict/community-verdict.md` ≥10/12 MUST + ≥5/8 SHOULD (SP42).
- Tag `v1.5.0-scheduler-community` IFF 2/2 verdicts pass; partial `v1.5.0-scheduler` IFF only scheduler verdict passes; no tag IFF scheduler fails.

### Must NOT Have

- NO new L3/L2/L1/Tier-1/Tier-2 skill. L4 = 10 → 11 is the SOLE catalog mutation.
- NO new practices rule family in `practices/rules/`. Sanitize-XSS concern co-shipped as recipe-level invariant (M3 fix C — see §4.2 COMMUNITY-INV-005).
- NO empty applied-recipes array syntax anywhere (H2 + M4 fix). Scheduler omits the key per `file-storage` + `practices` precedent.
- NO TD-2026-05-20-022 guard specialization (H2 fix — DELETED from §8 ADR).
- NO `RECIPE_DEVIATION.md` ceremony.
- NO Korean reference fabrication. 5 Korean attempts logged in §4.4 with explicit no-verbatim rationale.
- NO `/ax-verify-domain scheduled-task` in SP41 binary gate (Critic L fix — replaced with `bash skills/_tests/L4/scheduler-domain.test.sh`; see §4.5).
- NO change to git workflow / CI policy / release process. Tag is a catalog-release artifact, not a fork-team policy mandate (Critic J watch-item honored).
- NO partial deliverable within an SP — but SP-to-SP partial-tag policy is explicit (Option 4 / Critic K resolution).

---

## §4 Deliverable Inventory (2 deliverables, 3 SPs per Option 4 Synthesis-B)

> All `spec_ref:` disk-verified by `grep -nE "id:" specs/<file>.yaml` on 2026-05-20.

### 4.1 Deliverable 1 — `scheduler` L4 primitive (SP41 atomic)

- **Spec Trio (already on disk — R3 origin, Architect-confirmed):**
  - Spec: `specs/scheduled-task-l0.yaml` (10 items: `SCHED-REGISTER-001`, `SCHED-LOCK-001`, `SCHED-LOCK-002`, `SCHED-EXECUTE-001`, `SCHED-IDEMPOTENT-001`, +5).
  - Contract: `contracts/scheduled-task-openapi.yaml`.
  - Manifest: `blueprints/scheduled-task-manifest.yaml`.
- **New artifacts (SP41 creates):**
  - `templates/L4/scheduled-task/README.md` mirrors `templates/L4/crud/README.md` shape (file table, AGENTS hint sheet, composition notes, override_allowed inline). **NO `applied_recipes:` key** at introduction — matches `file-storage` + `practices` unused-L4 precedent (H2/M4 fix). R8 lms/cms will add the key + plural list in the SAME R8 atomic commit that lists `scheduled-task` in their `enabled_l4_domains:`.
  - `templates/L4/scheduled-task/backend/` skeleton stub (SP41 prep `find backend -type d -iname "*scheduled*"`; if absent, ships `ScheduledTask.java.skeleton` stub).
  - `practices/DECISIONS.md` `TD-2026-05-20-020` (full ADR per §8).
  - `skills/_tests/L4/scheduler-domain.test.sh` (sealed sub-agent test).
- **Composition:** scheduler stands alone in R7; no recipe consumes it. R8 lms/cms first consumers.
- **External evidence (verbatim — 2 PASS, exceeds 1-floor):**
  - Spring Scheduling 200 OK: `"In addition to the TaskExecutor abstraction, Spring has a TaskScheduler SPI with a variety of methods for scheduling tasks to run at some point in the future."` (quoted_at 2026-05-20)
  - Quartz tutorial 200 OK: `"Triggers do not fire (jobs do not execute) until the scheduler has been started"` (quoted_at 2026-05-20)
- **TDD anchor:**
  ```yaml
  tdd_anchor:
    test_file: "skills/_tests/L4/scheduler-domain.test.sh"
    assertion: "sealed sub-agent given only templates/L4/scheduled-task/README.md + practices/AGENTS.md identifies scheduler primitive, lists ≥3 of REGISTER/LOCK/EXECUTE/IDEMPOTENCY families, and references specs/scheduled-task-l0.yaml; harness exits 0"
    expected_RED_reason: "templates/L4/scheduled-task/README.md does not exist"
    first_GREEN_command: "bash skills/_tests/L4/scheduler-domain.test.sh"
    owning_SP: "SP41"
  ```

### 4.2 Deliverable 2 — `community` recipe (SP41b atomic, sequential after SP41)

- **L4 composition (5 existing):** `crud`, `audit-log`, `notification`, `search`, `auth`. **Optional:** `feature-flags` for moderation toggles.
- **L2 blocks used:** `data-table`, `crud-create-form`, `crud-edit-form`, `crud-list-adapter`, `search-input`, `notification-list`, `notification-bell`, `filter-bar`, `confirm-dialog`, `relative-time` (L1), `kpi-card`, `rich-text-editor` (SP32 from R5 — disk-verify in SP41b prep per Architect rec 7).
- **L3 pages used:** `list-page`, `detail-page`, `create-page`, `edit-page`, `dashboard-page`.
- **Business invariants** (all 5 disk-RESOLVED at PRD signature — M3 fix):
  - `COMMUNITY-INV-001`: Post + comment moderation status changes emit audit-log row with operator + before/after. `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-001` (line 7) + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-002` (line 23). **PASS.**
  - `COMMUNITY-INV-002`: Soft-deleted threads excluded from search. `spec_ref: specs/search-l0.yaml#SEARCH-AUTHZ-001` (line 7). **PASS.**
  - `COMMUNITY-INV-003`: Reply notifications respect recipient preferences + opt-out. `spec_ref: specs/notification-l0.yaml#NOTIF-PREF-001` (line 131). **FIXED per M3** — was iter-1 `NOTIFICATION-PREF-001` (wrong prefix). **PASS.**
  - `COMMUNITY-INV-004`: Authenticated post creation rate-limited per user per minute. `spec_ref: specs/auth-asvs-l1.yaml#ASVS-V2.2.1` (line 47; "Verify that anti-automation controls are effective at mitigating credential stuffing and brute force"). **FIXED per M3** — was iter-1 `ASVS-V11.1.4` (does not exist on disk). `ASVS-V2.2.1` is the existing anti-automation rate-limit anchor in this repo. Also `rule_ref: practices/rules/idempotency-key-on-mutations.md` (disk-verified). **PASS.**
  - `COMMUNITY-INV-005`: User-generated HTML sanitized server-side before storage (XSS prevention). **co-shipped-rule:** `community-html-sanitization` — authored INLINE in `specs/recipes/community-recipe-l0.yaml` as a recipe-level invariant (NOT a new `practices/rules/*.md` file — honors Principle 8 "no new rule families"). **PASS per M3 fix C.** No `rule_ref:` line; instead `invariant_test:` cites `frontend/tests/recipes/community-sanitize.spec.ts` co-shipped in SP41b.
- **Override allowance:** inline — skip `feature-flags` for open-by-default community; skip `auth` for fully-public read-only mirror.
- **External evidence (verbatim — 2 PASS, exceeds 1-floor with Reddit upgrade per M2):**
  - Discourse meta 200 OK: `"Discourse is backed by a complete JSON api. Anything you can do on the site you can also do using the JSON api."` (quoted_at 2026-05-20)
  - **Reddit GitHub-archive 200 OK** (M2 host-fingerprint fix; upgrades Reddit from iter-1's `internal_design` to `external`): `"Clients must authenticate with OAuth2"` + `"Clients connecting via OAuth2 may make up to 60 requests per minute."` (quoted_at 2026-05-20, source `github.com/reddit-archive/reddit/wiki/API`).
- **Downgrades:** PRAW readthedocs 200 OK but no extractable verbatim (`internal_design`); Devvit quickstart fetcher-blocked (`internal_design`); 디시인사이드 + 클리앙 200 OK but no API doc text (`internal_design`).
- **TDD anchor:**
  ```yaml
  tdd_anchor:
    test_file: "frontend/tests/recipes/community-compose.spec.ts"
    assertion: "recipes/community/RECIPE.md frontmatter enabled_l4_domains: list equals [audit-log, auth, crud, notification, search] AND 5 disk-verified spec_ref anchors all resolve via recipe_spec_referential_integrity_guard.sh"
    expected_RED_reason: "recipes/community/ directory does not exist"
    first_GREEN_command: "bash practices/evals/recipe_spec_referential_integrity_guard.sh && bash practices/evals/recipe_governance_guard.sh && cd frontend && npm test -- tests/recipes/community-compose.spec.ts"
    owning_SP: "SP41b"
  ```
  (Critic soft #4 fix — `first_GREEN_command` is now an executable command, not prose.)

### 4.3 Cluster claim (honest framing — Option 4 reframe)

Scheduler L4 + community recipe are **NOT topically related** and are NOT bundled atomically in iter 2. They are sequentially gated SPs (SP41 → SP41b). Each axis is atomic within its own SP. Synthesis-B (Option 4) is the explicit framing: mutation surfaces are disjoint; if scheduler verdict fails, community never starts.

### 4.4 Evidence ledger (WebFetched during this PRD revision — 2026-05-20)

| Deliverable | URL | HTTP / fetch result | Verbatim quote | Resolution | provenance_class |
|---|---|---|---|---|---|
| scheduler | `https://docs.spring.io/spring-framework/reference/integration/scheduling.html` | **200 OK — verbatim** | `"In addition to the TaskExecutor abstraction, Spring has a TaskScheduler SPI with a variety of methods for scheduling tasks to run at some point in the future."` | Verbatim | `external` |
| scheduler | `https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/tutorial-lesson-01.html` | **200 OK — verbatim** | `"Triggers do not fire (jobs do not execute) until the scheduler has been started"` | Verbatim | `external` |
| community | `https://meta.discourse.org/t/discourse-api-documentation/22706` | **200 OK — verbatim** | `"Discourse is backed by a complete JSON api. Anything you can do on the site you can also do using the JSON api."` | Verbatim | `external` |
| community | `https://github.com/reddit-archive/reddit/wiki/API` | **200 OK — verbatim** (M2 fix — NEW iter 2 attempt) | `"Clients must authenticate with OAuth2"` + `"Clients connecting via OAuth2 may make up to 60 requests per minute."` | Verbatim (UPGRADE from iter-1 `internal_design`) | `external` |
| community | `https://praw.readthedocs.io/en/stable/` | **200 OK — no extractable verbatim** (M2 attempt) | — | Downgrade | `internal_design` — "PRAW page has navigation + headings only, no descriptive sentence verbatim-able" |
| community | `https://developers.reddit.com/docs/quickstart` | **fetcher-blocked** (M2 attempt) | — | Downgrade | `internal_design` — "Devvit docs subdomain remains host-blocked; Reddit GitHub archive verbatim above closes the gap" |
| community | `https://developers.reddit.com/` | iter-1 blocked × 2 | — | Superseded by Reddit GitHub archive `external` row | — |
| community | `https://www.reddit.com/dev/api/` | iter-1 blocked × 1 | — | Superseded | — |
| community | `https://www.dcinside.com/` | 200 OK — no API documentation | — | Downgrade | `internal_design` — "DCinside has no public REST/OAuth dev API documentation" |
| community | `https://www.clien.net/service/` | 200 OK — no API documentation | — | Downgrade | `internal_design` — "Clien has no public REST/OAuth dev API documentation" |
| Korean (M1) | `https://toss.tech/` | **200 OK — no relevant Korean verbatim** (M1 attempt — NEW iter 2) | — | Downgrade | `internal_design` — "Toss Tech homepage: article titles + categories; no verbatim about scheduling/jobs/community/forum patterns" |
| Korean (M1) | `https://d2.naver.com/home` | **fetcher-blocked** (M1 attempt — NEW iter 2) | — | Downgrade | `internal_design` — "Claude Code unable to fetch from d2.naver.com host" |
| Korean (M1) | `https://tech.kakao.com/` | **200 OK — no relevant Korean verbatim** (M1 attempt — NEW iter 2) | — | Downgrade | `internal_design` — "Kakao Tech mentions general topics (AI/cloud/frontend/backend) but no scheduling/community verbatim" |
| Korean (M1) | `https://techblog.woowahan.com/` + `https://techblog.woowahan.com/2664/` | **200 OK × 2 — no scheduler verbatim** (M1 attempt — NEW iter 2) | — | Downgrade | `internal_design` — "Woowahan tech blog index has no scheduler verbatim; article 2664 discusses HikariCP deadlocks, not scheduling" |
| Korean (M1) | `https://engineering.linecorp.com/ko` | **200 OK — no relevant Korean verbatim** (M1 attempt — NEW iter 2) | — | Downgrade | `internal_design` — "LINE engineering Korean page has careers/JOIN US content; no scheduling/community technical verbatim" |

**Per-deliverable evidence density floor:**
- scheduler: **2 external verbatim** (Spring + Quartz). PASS — exceeds 1-floor.
- community: **2 external verbatim** (Discourse + Reddit-archive). PASS — exceeds 1-floor with buffer (M2 closure).
- Korean cycle: **0 Korean verbatim**. R7 explicit zero-Korean-verbatim exception documented (M1 disposition). Rationale: 5 distinct Korean engineering blogs/hosts attempted (toss.tech, d2.naver.com, tech.kakao.com, techblog.woowahan.com × 2, engineering.linecorp.com/ko); 4 returned 200 OK with no scheduler/community-related Korean text; 1 (d2.naver.com) fetcher-blocked. No Korean verbatim is available without fabrication. Project vision (CLAUDE.md) framing of "Korean enterprise standard stack" is honored at the **stack level** (Spring Boot + React are the user-validated stack); Korean verbatim source-anchoring is a per-cycle signal that R6 happened to clear via channel.io and R7 cannot replicate this cycle. R8 evidence refresh re-attempts 토스 / 우아한 / LINE for scheduler-specific articles if any are published.

**Re-attempt at SP execution:** SP41 / SP41b pre-flight re-runs WebFetch on Devvit + PRAW + d2.naver.com once each (one-shot, no fabrication). Any 200 OK with verbatim upgrades the row.

---

## §4.5 SP Plan + Verification Matrix (3 SPs — Option 4 / Synthesis-B)

| SP | Atomic deliverables | TDD anchors (RED → GREEN) | Verification | Observability_signal (advisory) |
|---|---|---|---|---|
| **SP41** (atomic — scheduler L4 only; mutation surface = `templates/L4/scheduled-task/`, `practices/DECISIONS.md`, `skills/_tests/L4/`, `skills/_tests/sealed-verdict/scheduler-l4-verdict.md`) | (a) `templates/L4/scheduled-task/README.md` (NO `applied_recipes:` key); (b) `templates/L4/scheduled-task/backend/` skeleton; (c) `practices/DECISIONS.md` `TD-2026-05-20-020`; (d) `skills/_tests/L4/scheduler-domain.test.sh`; (e) `skills/_tests/sealed-verdict/scheduler-l4-verdict.md` (PENDING); (f) `practices/upstream/r7-sp41-scheduler-evidence.md` (Spring + Quartz verbatim snapshot); (g) `practices/AGENTS.md` re-generated IFF generate_agents.sh depends on L4 topology (verified in SP41 prep — see §11 hedge resolution). | scheduler-domain test: RED (README absent) → GREEN. | **Binary gates (Critic L fix):** `bash skills/_tests/L4/scheduler-domain.test.sh` exit 0 (replaces `/ax-verify-domain scheduled-task` per Critic L option (c) — rationale: scheduler L4 stays catalog-only per CLAUDE.md recipe-no-code principle; backend `testScheduledTask` Gradle task does not yet exist; the scheduler-domain bash test is the appropriate L4-discoverability gate). `/ax-verify-domain` × 5 OTHER touched L4 (`crud, audit-log, notification, search, auth`) exit 0. AGENTS.md sentinel matches re-generation IFF generate_agents.sh ran. | `scheduler.lock_acquire_total`, `scheduler.job_history_rows_total` — advisory only. |
| **SP41b** (atomic — community recipe only; mutation surface = `recipes/community/`, `specs/recipes/`, 5 L4 README appends, `_MANIFEST.yaml`, `skills/_tests/sealed-verdict/community-verdict.md`) | (a) `recipes/community/{RECIPE.md, L4-composition.md, L2-block-recipe.md, spec-trio-template.yaml}`; (b) `specs/recipes/community-recipe-l0.yaml` with 5 disk-verified invariants per §4.2 + inline `co-shipped-rule: community-html-sanitization` for INV-005; (c) `recipes/_MANIFEST.yaml` — community moves deferred→active; lms + cms triggers refreshed to acknowledge scheduler now active; internal-it trigger text unchanged on scheduler axis (Critic soft #3 fix); (d) `templates/L4/{crud,audit-log,notification,search,auth}/README.md` `applied_recipes:` plural lists append `community` alphabetically; (e) `skills/_tests/sealed-verdict/community-verdict.md` (PENDING); (f) `frontend/tests/recipes/community-compose.spec.ts` + `frontend/tests/recipes/community-sanitize.spec.ts` (co-shipped INV-005 invariant test); (g) `practices/upstream/r7-sp41b-community-evidence.md` (Discourse + Reddit-archive verbatim snapshot). | community-compose test: RED (recipes/community/ absent) → GREEN. recipe_governance_guard: RED (community has no `applied_recipes:` wiring on 5 L4) → GREEN. recipe_spec_referential_integrity_guard: RED → GREEN (5 invariants now disk-resolved). community-sanitize test: RED (co-shipped invariant test absent) → GREEN. | `bash practices/evals/recipe_governance_guard.sh` exit 0 against all 7 active recipes; `bash practices/evals/recipe_spec_referential_integrity_guard.sh` exit 0 against all 7 specs; community-compose.spec.ts passes; community-sanitize.spec.ts passes. **Gate:** SP41b commit only opens after SP41 + scheduler-l4-verdict ≥10/12 MUST + ≥5/8 SHOULD. | `recipe.community.thread_active_total`, `recipe.community.sanitize_violation_total` — advisory only. |
| **SP42** (FINAL — 2 sealed verdicts harness exec + tag policy + PR) | (a) Sealed sub-agent execs `scheduler-l4-verdict.md` and `community-verdict.md` (context-0 inputs per §7 P5 mitigation); (b) `recipes/README.md` updated — 7 active + 3 deferred with refreshed triggers; (c) `/ax-verify` exit 0. **Tag policy (Option 4):** tag `v1.5.0-scheduler-community` IFF 2/2 verdicts pass; **partial tag `v1.5.0-scheduler` IFF scheduler verdict passes AND community-verdict fails** (community marked `status: active-verdict-pending` in `_MANIFEST.yaml`; SP43 fast-follow next ralplan cycle); **no tag** IFF scheduler verdict fails (SP41 mutations revert; SP41b never started — Option 4 SP41-gating). | Sealed verdict harness × 2: PENDING → PASS (or PASS/PENDING for partial). | `/ax-verify` exit 0; manual review of 2 sealed verdicts; tag policy enforced by commit message. | `recipes.active_total: 7` (or 6 partial); `recipes.deferred_total: 3` (or 4 partial); `L4.domain_total: 11`. |

**SP atomicity rule:** Within SP41, all scheduler artifacts ship together OR rollback (no `templates/L4/scheduled-task/README.md` partial). Within SP41b, all community artifacts ship together OR rollback. **Between** SP41 and SP41b, gating is hard: SP41b commit does NOT open until SP41 + scheduler-l4-verdict pass. **SP41b failure does NOT roll back SP41.** This is the Option 4 partial-tag semantics resolving Critic finding K's contradiction.

**SP linearization:** SP41 → SP41b → SP42. No parallel branches.

---

## §5 Scheduler L4 introduction rationale (Q1 — R7-specific)

**Resolution: scheduler is a bona fide L4 primitive whose Spec Trio (spec + contract + manifest) already lives on disk; SP41 adds only the L4 README + scaffold + ADR — the SAME pattern R3 used for the other 10 L4 domains.**

### Disk evidence

- `specs/scheduled-task-l0.yaml` — 10 items: `SCHED-REGISTER-001`, `SCHED-LOCK-001`, `SCHED-LOCK-002`, `SCHED-EXECUTE-001`, `SCHED-IDEMPOTENT-001`, +5.
- `contracts/scheduled-task-openapi.yaml` — DISK-VERIFIED.
- `blueprints/scheduled-task-manifest.yaml` — DISK-VERIFIED.
- `templates/L4/scheduled-task/` — ABSENT (SP41 creates).

### Why scheduler does NOT violate caps

- **Tier-1/Tier-2 cap** applies to `skills/Tier1/*` + `skills/Tier2/*` — FROZEN at 4 + 8. Scheduler is not a skill.
- **L1/L2/L3 cap** is UI primitives + feature blocks + page templates. Scheduler adds zero UI.
- **L4 layer** is expansionary when spec-first + evidence-anchored. `billing` was added R5; `file-storage` added R3. Scheduler meets both gates (2 external verbatim + Spec Trio already disk-verified from R3).

### Why scheduler unblocks 2 (not 3) R6-deferred recipes (Critic soft #3 fix)

R6 `recipes/_MANIFEST.yaml#deferred_recipes:`:
- `lms` — `reintroduction_trigger:` names "job-scheduler L4 or notification scheduling primitive" → scheduler gate.
- `cms` — `reintroduction_trigger:` names "scheduler primitive (shared with LMS)" → scheduler gate.
- `internal-it` — `reintroduction_trigger:` names "Jira/ServiceNow REST API quote + clarified webhook-emit primitive in notification L4" → **independent of scheduler**.

**2 of 4** deferred recipes (lms + cms) have scheduler as a NAMED gate. Iter 1's "3 of 4" claim was inaccurate; iter 2 corrects this everywhere (Critic soft #3).

### Why scheduler README has NO `applied_recipes:` key at introduction (H2 + M4 fix)

The existing `recipe_governance_guard.sh:55-77` `check_applied_recipe_declared` function is only invoked for L4 domains listed in an active recipe's `enabled_l4_domains:`. Since no R7 recipe lists `scheduled-task`, the function is never called against scheduler's README. The correct shape is **no `applied_recipes:` key at all** — matches `file-storage` + `practices` L4 README pattern (disk-verified; `bash recipe_governance_guard.sh` exits 0 today against both). R8 lms/cms will add the key + plural list `[lms, cms]` in the same R8 atomic commit that lists `scheduled-task` in their `enabled_l4_domains:` — standard recipe-arrival pattern.

### Migration plan (within SP41)

1. Create `templates/L4/scheduled-task/README.md` mirroring `templates/L4/crud/README.md` structure, **WITHOUT `applied_recipes:` key**.
2. Create `templates/L4/scheduled-task/backend/` skeleton OR pointer.
3. (CONDITIONAL — §11 hedge resolution): IF `practices/generate_agents.sh` depends on L4 topology, run it and commit new `practices/AGENTS.md` sentinel sha. IF NOT, no sentinel mutation — drop the language from §1/§3 (Critic soft #5 disposition; resolution captured in §11).
4. Append `TD-2026-05-20-020` to `practices/DECISIONS.md`.
5. Author `skills/_tests/L4/scheduler-domain.test.sh`.
6. Scaffold `skills/_tests/sealed-verdict/scheduler-l4-verdict.md` with `verdict: PENDING` (executed SP42).

---

## §6 Autonomous Execution Safety

- **Pre-flight gate (before SP41 starts):** §4.4 evidence ledger captured. SP41 pre-flight re-runs Devvit + PRAW once. Disk-verify `templates/L4/scheduled-task/` ABSENCE; disk-verify Spec Trio PRESENCE; disk-verify `practices/generate_agents.sh` L4-dependency status (§11). Abort if any prep step fails.
- **Pre-flight gate (before SP41b starts):** SP41 commit landed + scheduler-l4-verdict ≥10/12 + ≥5/8. Re-attempt d2.naver.com once. Disk-verify 5 community spec_ref anchors per §4.2 (all 5 already resolved in iter 2 — re-check at execution). Disk-verify `rich-text-editor` L2 block exists (Architect rec 7).
- **Mid-flight gate (between SP41b and SP42):** `git status` clean; `/ax-verify-domain` × 5 (crud/audit-log/notification/search/auth) exit 0; `recipe_governance_guard.sh` + `recipe_spec_referential_integrity_guard.sh` exit 0 against all 7 active recipes; `frontend/tests/recipes/community-compose.spec.ts` + `community-sanitize.spec.ts` pass.
- **Stop conditions:** If `bash skills/_tests/L4/scheduler-domain.test.sh` cannot reach GREEN within 3 iter cycles, SP41 rolls back; scheduler returns to a NEW `deferred_l4_primitives:` block in `_MANIFEST.yaml` with `blocker:` field. If community guards fail within SP41b, SP41b rolls back; SP41 stays landed; SP42 still ships partial tag `v1.5.0-scheduler`.
- **Sealed verdict release policy (Option 4):** Tag `v1.5.0-scheduler-community` IFF 2/2 pass. Partial `v1.5.0-scheduler` IFF only scheduler verdict passes. No tag IFF scheduler fails (SP41 reverted).
- **Rollback:** Each SP is one squash-mergeable commit. Revert SP41b without disturbing SP41.
- **No destructive ops:** No `git reset --hard`, no force push. AGENTS.md sentinel RE-COMPUTED only if §11 confirms generate_agents.sh L4-dependency.

---

## §7 Pre-Mortem (5 scenarios — DELIBERATE mode)

1. **Reddit GitHub-archive 200 OK degrades or M2 verbatim mis-extracted at SP41b execution.** Likelihood: LOW (already captured in §4.4 iter 2). Impact: community external anchor count falls to 1 (Discourse). Mitigation: Discourse verbatim alone clears 1-floor; iter 2 evidence ledger preserves Reddit-archive snapshot file `practices/upstream/r7-sp41b-community-evidence.md` as fallback.

2. **`templates/L4/scheduled-task/README.md` ships before backend entity scaffold located.** Likelihood: MEDIUM. Impact: fork-receiver gets broken pointer. Mitigation: SP41 PREP step `find backend -type d -iname "*scheduled*"`. If absent, ship `ScheduledTask.java.skeleton` minimal stub. README explicitly labels as skeleton.

3. **`practices/generate_agents.sh` L4-dependency hedge resolves to NO; iter 2 §1/§3 language about "sentinel sha recomputed" is stale.** Likelihood: MEDIUM (Architect rec 8 + Critic soft #5 both flagged the hedge). Impact: iter 2 PRD is internally inconsistent. Mitigation: §11 resolution captures the dry-run check; if generate_agents.sh ignores L4 topology, sentinel sha does NOT change in SP41; if it does, §1/§3 language stands. SP41 prep step performs the check before commit.

4. **Scheduler L4 introduction interpreted as "L4 cap broken; composition kit drifting."** Likelihood: LOW post-iter-1 Architect signoff on R3-stub-completion framing. Impact: iter cycle thrash. Mitigation: §5 disk-verifies all 3 Spec Trio components; ADR TD-020 carries full rationale; precedent (billing R5, file-storage R3) cited.

5. **Sealed verdict for scheduler L4 (NEW harness shape) scores below threshold because context-0 sub-agent cannot find scheduler anchors in `practices/AGENTS.md`.** Likelihood: MEDIUM. Impact: SP42 holds tag (Option 4: partial tag `v1.5.0-scheduler` impossible if scheduler verdict fails — no tag at all; SP41b never starts). Mitigation: `scheduler-l4-verdict.md` sub-agent prompt explicitly includes BOTH `templates/L4/scheduled-task/README.md` AND `practices/AGENTS.md` as sealed context. README body references all 3 Spec Trio paths so sub-agent identifies them without needing them in-context. Option 4 SP41-gating means this risk affects only scheduler axis, not community.

---

## §8 ADR Template (3 entries — TD-022 DELETED per H2)

- **TD-2026-05-20-020 (NEW)** — Scheduler L4 primitive introduced.
  - **Decision:** Add `scheduled-task` as the 11th L4 domain. L4 count 10 → 11.
  - **Drivers:** (a) Spec Trio disk-verified from R3 PR `26de945`. (b) **2 of 4** R6-deferred recipes (lms + cms) name scheduler verbatim. (c) 2 external verbatim PASS (Spring + Quartz).
  - **Alternatives considered:** Defer indefinitely (rejected — strands lms + cms); Tier-2 skill (rejected — caps FROZEN; scheduler is domain primitive); Option 4 split-SP vs iter-1 atomic-2 (chosen — protects community from scheduler-harness risk).
  - **Why chosen:** Composition kit self-extensibility along catalog's OWN deferred axis. Scheduler IS that axis for 2 of 3 unblockable recipes.
  - **Consequences:** L4 = 11. R8 gains 2 unblocked recipes (lms + cms). Scheduler README ships **WITHOUT `applied_recipes:` key** (file-storage + practices precedent).
  - **Follow-ups:** R8 ships lms + cms recipes consuming scheduler; in same R8 atomic commit, `applied_recipes:` key + plural list `[lms, cms]` added to scheduler README.

- **TD-2026-05-20-021 (NEW)** — Community recipe shipped (SP41b atomic-sequential).
  - **Decision:** `recipes/community/` moves deferred→active. Active recipe count 6 → 7.
  - **Drivers:** Only R6-deferred recipe whose trigger does NOT require scheduler; Discourse verbatim + Reddit-archive verbatim PASS (M2 closure); 5 community invariants disk-resolved at PRD signature (M3 closure).
  - **Alternatives considered:** Wait for Korean community API (rejected — zero public dev APIs); merge into crm (rejected — moderation/threading not crm concerns); Synthesis-A trim full defer (rejected — community is genuinely eligible per its `reintroduction_trigger:`).
  - **Why chosen:** Discourse + Reddit-archive verbatim are strongest English-tier evidence; 5 invariants resolved.
  - **Consequences:** 5 L4 READMEs gain `community` in `applied_recipes:` plural list. Active recipe count = 7. Co-shipped recipe-level invariant (community-html-sanitization) honors no-new-rule-family principle.
  - **Follow-ups:** R8 evidence refresh re-attempts d2.naver.com + Devvit if fetcher unblocks.

- **TD-2026-05-20-023 (RETAINED)** — Reddit Developer Platform anchored via GitHub-archive verbatim (M2 fix).
  - **Decision:** Reddit recorded as `external` via `github.com/reddit-archive/reddit/wiki/API` (2 verbatim quotes). `developers.reddit.com` + `www.reddit.com/dev/api/` + `developers.reddit.com/docs/quickstart` remain fetcher-blocked → `internal_design`. PRAW readthedocs 200 OK but no extractable verbatim → `internal_design`.
  - **Drivers:** M2 host-fingerprint diversification. GitHub-archive verbatim is canonical: same content as developers.reddit.com, different host.
  - **Alternatives considered:** Internet Archive snapshot (rejected — dependency on archive availability); manual copy (rejected — fabrication-equivalent).
  - **Why chosen:** Closes Architect M2 + Critic disposition #4 without fabrication.
  - **Consequences:** Community external anchor count = 2 (Discourse + Reddit-archive). Buffer above 1-floor.
  - **Follow-ups:** R8 evidence refresh re-attempts Devvit + PRAW if any descriptive sentence appears.

**~~TD-2026-05-20-022 (DELETED iter 2 per H2)~~** — Empty-list guard specialization was architecturally unnecessary; existing guard already correctly handles unused L4 via absence-of-key pattern. Disk evidence: `file-storage` + `practices` L4 READMEs have no `applied_recipe(s)` key and `bash recipe_governance_guard.sh` exits 0 today.

---

## §9 Honored Constraints

- Tier-1 cap **= 4** (FROZEN).
- Tier-2 count **= 8** (UNCHANGED).
- L4 domain count **= 10 → 11** (scheduler L4 per TD-020).
- L1 catalog **= 49** (UNCHANGED — corrected from iter 1's 48 per H1).
- L2 catalog **= 92** (UNCHANGED).
- L3 catalog **= 20** (UNCHANGED).
- Atomic SP rule per axis (Option 4: SP41 scheduler-axis, SP41b community-axis).
- Composition kit framing — recipes COMPOSE; scheduler L4 IS the catalog's self-named extension axis.
- Korean references — 5 attempts logged in §4.4 with explicit rationale for zero-Korean-verbatim cycle (M1).
- Reddit — GitHub-archive verbatim PASS (M2 closure).
- 5 community invariants disk-resolved at PRD signature (M3).
- NO empty applied-recipes array syntax anywhere (H2 + M4).
- TD-022 DELETED (H2).
- `/ax-verify-domain scheduled-task` REMOVED from SP41 binary gate (Critic L option (c)); replaced with `bash skills/_tests/L4/scheduler-domain.test.sh`.
- AGENTS.md sentinel sha recompute is CONDITIONAL on `generate_agents.sh` L4-dependency (§11 resolution).
- R5/R6 sealed verdict threshold (≥10/12 MUST + ≥5/8 SHOULD) honored.

---

## §10 Out-of-scope (R7 explicit) + Deferred Recipes (3 remaining)

### Deferred recipes (refreshed triggers reflect R7 scheduler landing — 2 of 4 reference scheduler, not 3)

| Recipe | R6 deferral rationale | R7-refreshed `reintroduction_trigger:` |
|---|---|---|
| `lms` | scheduler-task L4 absent; LMS due-date-reminder requires job-scheduler; 인프런 closed API. | "**Scheduler L4 landed in R7 v1.5.0** (`templates/L4/scheduled-task/`); remaining gap = Coursera/Moodle/edX case-study URL with verbatim integration text. R8 eligible." |
| `cms` | Scheduled publish needs same scheduler as LMS; 네이버 블로그 closed API. | "**Scheduler L4 landed in R7 v1.5.0**; remaining gap = Sanity/Contentful verbatim citation. R8 eligible." |
| `internal-it` | Jira API not verbatim-fetched (3 failures); webhook patterns vendor-specific. | "**Independent of R7 scheduler.** Remaining gap = verbatim Jira/ServiceNow REST API quote + clarified webhook-emit primitive in notification L4. R8+ if fetch succeeds." |

### Out-of-scope (R7)

- New L1/L2/L3 surface.
- New Tier-1/Tier-2 skill.
- New practices rule families (XSS sanitize is co-shipped recipe-level invariant, not a new rule file).
- `RECIPE_DEVIATION.md` ceremony.
- Backend implementations for scheduler or community.
- Reddit Devvit verbatim retry beyond SP41b pre-flight one-shot (R8).
- 6-month recipe-retirement review.
- Empty-list guard specialization (TD-022 DELETED).
- New L4 primitives beyond scheduler this cycle.

---

## §11 Branch + path summary + AGENTS.md hedge resolution

- **Branch:** `feat/r7-scheduler-community-sp41-sp42` (cut from `main` at `ab44cce`).
- **PRD path (this iter):** `docs/superpowers/specs/2026-05-20-r7-scheduler-community-prd.iter2.md`.
- **Manifest target:** `recipes/_MANIFEST.yaml` — community moves deferred→active; 3 deferred rows refreshed.
- **AGENTS.md hedge resolution (Architect rec 8 + Critic soft #5):** SP41 PREP step runs `practices/generate_agents.sh --dry-run` (or `bash -n`-equivalent check) to determine whether the script reads `templates/L4/` topology. If YES → §1/§3/§4.5/§5 language about sentinel sha recompute STANDS; sha changes in SP41. If NO → sentinel sha does NOT change for scheduler addition; §1/§3/§4.5/§5/§9 language is updated in the SP41 commit to remove sentinel-sha-recompute claims (mechanical 4-line edit). Either resolution is acceptable; iter 2 PRD accepts both as possible and resolves via SP41 prep evidence. **Conservative default:** assume YES until disk-verified NO; the YES path adds 1 line of work, the NO path removes 4 lines — both are mechanical.
- **DECISIONS.md target:** `practices/DECISIONS.md` — append TD-020 (scheduler), TD-021 (community), TD-023 (Reddit GitHub-archive). TD-022 NOT appended (deleted).
- **Final tag:** `v1.5.0-scheduler-community` IFF 2/2 verdicts pass; `v1.5.0-scheduler` IFF only scheduler passes; no tag IFF scheduler fails.

---

## §12 Verdict line

R7 iter 2 PRD closes all 2 HIGH + 4 MEDIUM (Architect) + 1 BLOCKING + 5 hard blockers (Critic) + 5 soft suggestions. **L1 = 49** (H1). **TD-022 DELETED + no empty applied-recipes array syntax** (H2/M4). **All 5 community invariants disk-resolved** (M3 — NOTIF-PREF-001, ASVS-V2.2.1, co-shipped INV-005). **2 external verbatim per deliverable** (Spring + Quartz; Discourse + Reddit GitHub-archive — M2 closure). **5 Korean attempts logged with explicit zero-verbatim cycle rationale** (M1). **3 of 4 → 2 of 4 deferred-name-scheduler corrected** (Critic soft #3). **Community first_GREEN_command executable** (Critic soft #4). **Option 4 Synthesis-B adopted** (Architect Synthesis + Critic L) — SP41 scheduler-atomic + SP41b community-atomic-sequential + SP42 partial-tag-aware. **`/ax-verify-domain scheduled-task` replaced with `bash skills/_tests/L4/scheduler-domain.test.sh`** (Critic L option (c) — scheduler L4 stays catalog-only per CLAUDE.md recipe-no-code principle). **AGENTS.md hedge captured with SP41 prep dry-run gate** (Architect rec 8, Critic soft #5). 3 SPs (SP41/SP41b/SP42), ≈5-6 d wall-time. Tag policy: 2/2 → full; 1/2 → partial; 0/2 → no tag + SP41 revert.

---

## Iter 2 changelog

Each iter-1 blocker mapped to closure line(s) in this PRD:

- **H1 (L1 = 48 → 49 disk reality):** §2 table line "L1 primitives | **49**" + §9 "L1 catalog **= 49** (UNCHANGED — corrected from iter 1's 48)" + §12 verdict line. Disk-verified via `ls templates/L1/components/ | wc -l = 49`.
- **H2 (TD-022 unnecessary; empty applied-recipes array would trigger guard violation):** §8 TD-022 DELETED with strikethrough rationale. §3 Must NOT Have explicit "NO empty applied-recipes array literal". §4.1 scheduler README "NO `applied_recipes:` key at introduction". §5 Migration step 1 explicit "WITHOUT `applied_recipes:` key". §9 + §12 reaffirm.
- **M1 (Korean verbatim regression):** §4.4 added 5 NEW iter 2 Korean WebFetch attempts (toss.tech, d2.naver.com, tech.kakao.com, techblog.woowahan.com × 2, engineering.linecorp.com/ko) with HTTP status + 2026-05-20 timestamp + verbatim-or-downgrade rationale. §4.4 explicit zero-Korean-verbatim cycle rationale at "Per-deliverable evidence density floor" block.
- **M2 (Reddit downgrade incomplete):** §4.4 added 3 NEW iter 2 Reddit alternative-host attempts (github.com/reddit-archive — VERBATIM PASS, praw.readthedocs.io — internal_design, developers.reddit.com/docs/quickstart — fetcher-blocked). Reddit row UPGRADED from iter-1 `internal_design` to `external`. §4.2 community external anchor count = 2 (Discourse + Reddit-archive). §8 TD-023 retained with Reddit-archive verbatim quotes.
- **M3 (3 of 5 community invariants unresolved):** §4.2 ALL 5 invariants disk-resolved at PRD signature — INV-003 fixed to `NOTIF-PREF-001` (line 131); INV-004 fixed to `ASVS-V2.2.1` (line 47 anti-automation anchor); INV-005 reshaped as `co-shipped-rule: community-html-sanitization` recipe-level invariant (no new `practices/rules/*.md` file — honors Principle 8). No "TO BE DISK-VERIFIED" labels remain.
- **M4 (empty applied-recipes array triggers guard):** Same closure as H2. Scheduler README has no `applied_recipes:` key.
- **Critic L BLOCKING (`/ax-verify-domain scheduled-task` Gradle mapper produces `testScheduled Task`):** §3 Must NOT Have "NO `/ax-verify-domain scheduled-task` in SP41 binary gate". §4.5 SP41 verification column replaces it with `bash skills/_tests/L4/scheduler-domain.test.sh`. §4.5 rationale: scheduler L4 stays catalog-only per CLAUDE.md recipe-no-code principle (option (c) per brief). §9 honored constraints reaffirms.
- **Synthesis-B (split SP41 + SP41b + partial-tag-aware SP42):** §1 Option (4) ADDED as new viable option with explicit pros/cons; recommendation CHOSEN with rationale "3 unresolved invariants split cleanly along SP41/SP41b boundary + scheduler-harness risk does not strand community". §4.5 now has 3 SPs (SP41 / SP41b / SP42 partial-tag). §6 + §7 + §8 + §11 updated to reference Option 4 gating. §12 verdict reaffirms.
- **Critic soft #3 (`3 of 4 deferred` → `2 of 4 (lms + cms)`):** Fixed in §1 Cycle frame bullet 1, §1 Decision Drivers driver 2, §5 "Why scheduler unblocks 2 (not 3) R6-deferred recipes", §8 TD-020 Drivers, §10 deferred recipes table.
- **Critic soft #4 (community `first_GREEN_command` is prose):** §4.2 TDD anchor `first_GREEN_command` replaced with executable command `bash practices/evals/recipe_spec_referential_integrity_guard.sh && bash practices/evals/recipe_governance_guard.sh && cd frontend && npm test -- tests/recipes/community-compose.spec.ts`.
- **Critic soft #5 + Architect rec 8 (AGENTS.md sentinel sha hedge):** §11 dedicated "AGENTS.md hedge resolution" subsection with SP41 prep dry-run gate. §7 Pre-Mortem 3 NEW scenario. §5 Migration step 3 marked CONDITIONAL. Either resolution (YES → sentinel changes; NO → 4-line edit removes claims) is acceptable; SP41 prep determines.
- **Architect rec 7 (`rich-text-editor` SP32 disk-verify):** §4.2 L2 blocks list notes "disk-verify in SP41b prep". §6 SP41b pre-flight gate adds explicit check.
- **Critic J watch-item (tag/PR language must stay catalog-scoped):** §3 Must NOT Have explicit "Tag is a catalog-release artifact, not a fork-team policy mandate". §12 verdict honors.
- **Critic K contradiction (`active-verdict-pending` partial ship conflicts with no-partial rule):** Resolved by Option 4 partial-tag policy in §4.5 SP42 row + §6 + §8 TD-020. Within-SP atomicity preserved; between-SP partial is explicit and gated.
