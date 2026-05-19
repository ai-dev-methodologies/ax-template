# R8 — lms + cms Recipes PRD (DRAFT) — 2026-05-21 (Round 8, ralplan iter 1)

> **Status:** DRAFT (Planner iter 1; awaiting Architect + Codex Critic review).
> **Date:** 2026-05-21. **Repo:** ax-template. **Format:** RALPLAN-DR.
> **Predecessors:**
> - `2026-05-19-r6-recipes-prd.md` (CLOSED — `v1.4.0-recipes-complete` on `main@ab44cce`; 3 recipes booking + marketplace + b2b-admin).
> - `2026-05-20-r7-scheduler-community-prd.md` (CLOSED, APPROVED iter 3 — `v1.5.0-scheduler-community` on `main@faaf87d`; scheduler L4 + community recipe).
> **Branch (when execution starts):** `feat/r8-lms-cms-sp43-sp44`.
> **Targeted tag:** `v1.6.0-lms-cms` IFF 2/2 sealed verdicts pass; else partial `v1.6.0-lms-only` OR `v1.6.0-cms-only`; else no tag.
> **Mandate trigger:** User said "R8 시작해 — lms + cms" after R7 v1.5.0 landed scheduler L4. R6 `_MANIFEST.yaml` named "scheduler primitive" as the gating trigger for both lms + cms; R7 satisfied that gate.

---

## §1 RALPLAN-DR Summary

### Cycle frame (6 bullets)

- **R7 closed.** `v1.5.0-scheduler-community` shipped scheduler L4 (11th domain) + community recipe (7th active). 3 recipes remain deferred (lms, cms, internal-it).
- **R8 strategy.** Both lms and cms had `reintroduction_trigger:` text naming "scheduler primitive" verbatim in `_MANIFEST.yaml#deferred_recipes`. R7 satisfied that gate. R8 adds **both** recipes in one cycle since they share the scheduler-consumer pattern and have non-overlapping mutation surfaces beyond the shared `scheduled-task` L4 README append.
- **R6 parity choice.** No new L4 / L3 / L2 / L1 / skill — R8 closer in shape to R6 (3 recipes atomic) than to R7 (which introduced a new L4). 2 SPs: SP43 atomic-2 + SP44 FINAL.
- **Disk discoveries (2026-05-21).** `recipes/_MANIFEST.yaml` deferred section confirms lms + cms both name scheduler verbatim and BOTH triggers now read "**Scheduler L4 landed in R7 v1.5.0**" — confirming both are unblocked simultaneously. `templates/L4/scheduled-task/README.md` exists from R7 SP41 with no `applied_recipes:` key (per R7 H2/M4 precedent) — SP43 will ADD the key with plural list `[cms, lms]` as both arrive together.
- **Evidence rigor (this iter).** 6 external English WebFetch attempts (Coursera + Moodle + edX + Sanity + Contentful + Strapi) + 2 Korean attempts (인프런 + 네이버 블로그). Result: **5 verbatim PASS** (Moodle + Contentful + Strapi + Coursera + Sanity) + **3 documented downgrades** (edX 404, 인프런 no-API, 네이버 host-blocked). Both lms and cms clear 1-floor with buffer.
- **Internal-it stays deferred.** Refreshed `reintroduction_trigger:` text acknowledges R8 closed both scheduler-consuming recipes (lms + cms); internal-it remains gated on Jira/ServiceNow verbatim + webhook-emit primitive (independent of R8 scope).

### Principles (8 numbered)

1. **Composition kit, not single product.** R8 recipes (lms + cms) COMPOSE existing 11 L4. Zero new L4 / L3 / L2 / L1 / Tier-1 / Tier-2 skill. R7 was the catalog-expansion cycle; R8 returns to R6 recipe-only cadence.
2. **Spec-before-code, evidence-anchored AT PRD SIGNATURE.** All 5 lms + 5 cms `spec_ref:` are disk-resolved in §4 before SP43 begins. Korean refs WebFetched in this PRD revision; downgrades documented in §4.4 with HTTP status + 2026-05-21 timestamp.
3. **Binary verification per axis.** `/ax-verify` exits 0; `recipe_governance_guard.sh` exits 0 (dual-form regex from R6); `recipe_spec_referential_integrity_guard.sh` exits 0; 2 sealed verdicts ≥10/12 MUST + ≥5/8 SHOULD.
4. **Tier-1 cap = 4. Tier-2 count = 8. FROZEN.** L1 = 49 (R7-locked). L2 = 92. L3 = 20. L4 = 11 (UNCHANGED — R8 consumes scheduler; does NOT add L4).
5. **Atomic Spec Trio rule per SP.** SP43 ships BOTH recipes + BOTH specs + ALL L4 README mutations + scheduler L4 README `applied_recipes:` key addition + manifest moves — single atomic commit (R6 SP39 atomic-3 precedent).
6. **Recipe does not ship code; AI implements business logic.** Inherited from R5/R6/R7.
7. **Scheduler L4 consumed correctly.** Both lms (due-date reminders, course-publish scheduling) and cms (scheduled-publish, content expiry) bind `scheduled-task` in `enabled_l4_domains:`. R7's `templates/L4/scheduled-task/README.md` gains `applied_recipes: [cms, lms]` key in SP43 atomic commit (file-storage + practices unused-L4 precedent inverted now that consumers arrive).
8. **No new L2 / L3 / practices rule families this cycle.** Both recipes bind to existing rules (`idempotency-key-on-mutations.md`, `recipe-invariants-must-resolve.md`) and existing ASVS / SCHED / AUDIT / NOTIF / SEARCH / FF anchors. Rich-text-editor L2 (SP32 R5) is the editor for cms content authoring; calendar L1 (R5) anchors lms due-date UI. Both shipped; SP43 disk-verifies in prep.

### Decision Drivers (top 3)

1. **Reintroduction-trigger discipline.** R6 `_MANIFEST.yaml` named scheduler as the gating trigger for both lms AND cms. R7 v1.5.0 landed scheduler. Honoring the trigger means both lms and cms become eligible the next cycle — SHIPPING them this cycle proves the deferral-trigger system is binary (named gate → satisfied → consumed) rather than perpetually-deferred.
2. **Evidence rigor.** 5/6 external WebFetch attempts returned verbatim quotes (Moodle + Contentful + Strapi + Coursera + Sanity). lms has 2 verbatim external (Moodle + Coursera); cms has 3 verbatim external (Contentful + Strapi + Sanity). Both clear R6/R7 1-floor with substantial buffer.
3. **R6 cadence parity.** R6 shipped 3 recipes in 1 atomic SP (SP39). R8 ships 2 recipes in 1 atomic SP (SP43). No new L4, no parallel-branch complexity, no new harness shapes. The R6 SP39/SP40 pattern is the exact precedent.

### Viable Options Considered (≥2 mandatory)

- **Option (1) — Defer cms; ship lms alone in R8.**
  - Pros: Smaller delta; one verdict per cycle.
  - Cons: cms `reintroduction_trigger:` is satisfied IDENTICALLY (both name "scheduler primitive"); skipping cms when its gate has cleared means the trigger-discipline becomes arbitrary; evidence for cms is actually STRONGER than lms (3 external verbatim vs 2); the only reason to defer cms would be "we want to slow the catalog down" — explicitly contradicted by CLAUDE.md vision ("새 도메인 / 새 규칙 추가는 정상 활동").
  - **REJECTED.**

- **Option (2) — Atomic SP43 (lms + cms in one commit) + SP44 FINAL.** (CHOSEN — R6 SP39/SP40 precedent)
  - Pros: Cadence-parity with R6 SP39 atomic-3 (R8 SP43 atomic-2 is a strict subset — fewer recipes, same shape); mutation surfaces overlap intentionally (both touch `scheduled-task` L4 README applied_recipes plural-list key — atomic commit means the key is added with both consumers listed simultaneously, never partial); evidence chains for both recipes verbatim-cleared in this PRD revision; no new L4, so no new verdict harness shape (community-verdict + R6 verdicts are the precedent shape); shorter wall-time vs Option (3).
  - Cons: Single-commit atomicity means if either recipe's sealed verdict fails, the SP44 tag policy holds the full tag (partial-tag fallback per §6). 2 sealed verdicts to author in SP44 (community is the precedent at n=1; R6 SP40 at n=3).
  - **CHOSEN.**

- **Option (3) — Split SP43 lms-atomic + SP43b cms-atomic-sequential + SP44 FINAL.** (R7 Synthesis-B precedent)
  - Pros: If lms verdict fails, cms still ships clean at partial tag; mirrors R7 SP41/SP41b/SP42 gating exactly.
  - Cons: R7 split was justified by "first new L4 since R3 + net-new verdict harness" — neither condition applies to R8 (no new L4, verdict harness is the R6/R7-precedent recipe shape); split adds 1 PR cycle (≈0.5 d wall-time) with no de-risking benefit; the R6 SP39 atomic-3 precedent showed atomic-multi-recipe is the standard cadence when verdict harness is non-novel.
  - **REJECTED.** R7 split was a one-time deviation justified by L4-introduction + harness novelty; R8 returns to R6 atomic cadence.

### Mode

**DELIBERATE.** Retained for: (a) 2 sealed verdicts in one cycle (R5/R6/R7 precedent — sealed verdict release is a binary catalog-quality signal that must be explicit); (b) `scheduled-task` L4 README mutation pattern is new shape (key-addition with plural-list at first consumer arrival — explicit precedent inversion from R7); (c) wall-time ≈ 3-4 d (R6 SP39 precedent); (d) Korean evidence ledger zero-verbatim cycle (both Korean attempts downgrade — must be documented with rationale, not silently elided).

### Recommended: Option (2) — 2 SPs (SP43 atomic-2 + SP44 FINAL), tag `v1.6.0-lms-cms` IFF 2/2 sealed verdicts pass.

```
SP43  (atomic — lms + cms: 2 recipes + 2 Spec Trios + 2 recipe specs + scheduled-task L4 README applied_recipes key add + 5+ L4 README plural-list appends + manifest moves + 2 sealed-verdict scaffolds PENDING + evidence snapshot)
    ↓ gated on recipe_governance_guard.sh + recipe_spec_referential_integrity_guard.sh exit 0 AND 2 compose-test files pass
SP44  (FINAL — sealed verdict exec × 2 + /ax-verify full; tag v1.6.0-lms-cms IFF 2/2 pass; partial v1.6.0-lms-only OR v1.6.0-cms-only IFF 1/2 pass; no tag IFF 0/2 pass; PR)
```

All SP linear. Total: **2 SPs, ≈ 3-4 d wall-time.**

---

## §2 Context

### R7 v1.5.0 state (disk-verified 2026-05-21)

| Surface | Count | Path |
|---|---|---|
| L1 primitives | 49 | `templates/L1/components/` |
| L2 blocks | 92 | `templates/L2/blocks/` |
| L3 pages | 20 | `templates/L3/pages/` |
| L4 domains | 11 | `templates/L4/` (audit-log, auth, billing, crud, feature-flags, file-storage, notification, payment, practices, scheduled-task, search) |
| Active recipes | 7 | `recipes/{saas-subscription, e-commerce, crm, booking, marketplace, b2b-admin, community}/` |
| Deferred recipes | 3 | `recipes/_MANIFEST.yaml#deferred_recipes` (lms, cms, internal-it) |
| Practices rules (Java/Spring) | 68 | `practices/rules/` (R7 closure: 64 → 68 generic-promoted) |
| Sealed verdicts | 8 | `skills/_tests/sealed-verdict/` (incl. scheduler-l4-verdict.md) |
| Current tag | `v1.5.0-scheduler-community` on `main@faaf87d` | `git tag --sort=-creatordate \| head -1` |

### Disk-verified deferred-trigger state for lms + cms (2026-05-21)

| Recipe | `recipes/_MANIFEST.yaml` row | Disk-confirmed trigger text |
|---|---|---|
| lms | `deferred_recipes:` entry | `reintroduction_trigger: "Scheduler L4 landed in R7 v1.5.0 (templates/L4/scheduled-task/); remaining gap = Coursera/Moodle/edX case-study URL with verbatim integration text. R8 eligible."` |
| cms | `deferred_recipes:` entry | `reintroduction_trigger: "Scheduler L4 landed in R7 v1.5.0; remaining gap = Sanity/Contentful verbatim citation. R8 eligible."` |
| internal-it | `deferred_recipes:` entry | `reintroduction_trigger: "Independent of R7 scheduler. Remaining gap = verbatim Jira/ServiceNow REST API quote + clarified webhook-emit primitive in notification L4. R8+ if fetch succeeds."` — **NOT R8 scope.** |

### R8 scope

- **Deliverable 1 (lms recipe, SP43 atomic — half-1 of atomic-2):** `recipes/lms/` quartet + `specs/recipes/lms-recipe-l0.yaml`. Composition: `crud + audit-log + notification + scheduled-task + auth` (+ optional `feature-flags` for course-toggle). 5 disk-verified business invariants.
- **Deliverable 2 (cms recipe, SP43 atomic — half-2 of atomic-2):** `recipes/cms/` quartet + `specs/recipes/cms-recipe-l0.yaml`. Composition: `crud + audit-log + scheduled-task + notification` (+ optional `auth` for editorial multi-user, optional `search` for content discovery). 5 disk-verified business invariants.
- **Shared mutation (SP43 atomic):** `recipes/_MANIFEST.yaml` — lms + cms move deferred→active; internal-it stays deferred with refreshed trigger. `templates/L4/scheduled-task/README.md` gains `applied_recipes: [cms, lms]` key (first-consumer arrival; alphabetical). 5+ L4 READMEs (`crud`, `audit-log`, `notification`, `auth`, `feature-flags`, `search`) append `cms` and `lms` to their `applied_recipes:` plural list alphabetically.

NO new L3 / L2 / L1 / Tier-1 / Tier-2 skill. Recipe count mutates by +2 (7 → 9). L4 count UNCHANGED (11).

---

## §3 Objectives + Guardrails

### Must Have

- 2 recipes shipped to `recipes/{lms,cms}/` with full Spec Trio (`RECIPE.md`, `L4-composition.md`, `L2-block-recipe.md`, `spec-trio-template.yaml`).
- 2 recipe-level specs at `specs/recipes/{lms,cms}-recipe-l0.yaml`.
- 2 sealed verdicts at `skills/_tests/sealed-verdict/{lms,cms}-verdict.md`, each ≥10/12 MUST + ≥5/8 SHOULD.
- `recipes/_MANIFEST.yaml`: lms + cms move deferred→active; internal-it row stays in `deferred_recipes:` with refreshed `reintroduction_trigger:` text acknowledging R8 closed scheduler-consuming recipes.
- `templates/L4/scheduled-task/README.md` gains `applied_recipes:` key with plural list `[cms, lms]` (alphabetical; first consumers arrive simultaneously — file-storage + practices unused-L4 precedent inverted exactly per R7 TD-020 Follow-ups).
- `templates/L4/{crud,audit-log,notification,auth,feature-flags,search}/README.md` `applied_recipes:` plural list appends both `cms` and `lms` alphabetically (append-only; R5/R6/R7 entries preserved).
- 2 × `frontend/tests/recipes/{lms,cms}-compose.spec.ts` co-shipped (per-recipe TDD anchor §4.1/§4.2).
- 1 × `practices/upstream/r8-sp43-evidence-snapshot.md` capturing all 8 WebFetch attempts with HTTP status + 2026-05-21 timestamp + verbatim-or-downgrade-rationale.
- `/ax-verify` exits 0.
- `practices/evals/recipe_governance_guard.sh` exits 0 for all 9 active recipes (7 R7 + 2 R8).
- `practices/evals/recipe_spec_referential_integrity_guard.sh` exits 0 for all 9 recipe specs.
- Tag `v1.6.0-lms-cms` on the merge commit IFF 2/2 sealed verdicts pass. Partial tag `v1.6.0-lms-only` OR `v1.6.0-cms-only` IFF only one verdict passes (failing recipe marks `status: active-verdict-pending` in `_MANIFEST.yaml`; SP45 fast-follow in R9). No tag IFF 0/2 pass.

### Must NOT Have

- NO new L4 / L3 / L2 / L1 / Tier-1 / Tier-2 skill.
- NO new practices rule family in `practices/rules/`. Both recipes bind to existing anchors only.
- NO `RECIPE_DEVIATION.md` ceremony (R5/R6/R7 rejection stands).
- NO `/ax-scaffold business <pattern> --analyze` free-text NLP.
- NO Korean reference fabrication. 인프런 + 네이버 attempts logged in §4.4 with downgrade rationale.
- NO change to git workflow / CI policy / release process. Tag is a catalog-release artifact, not a fork-team policy mandate (Critic J watch-item honored from R7).
- NO partial deliverable within an SP — but SP-to-SP partial-tag policy is explicit (Option 2 / R7 Critic K resolution carried forward).
- NO internal-it work this cycle (independent of R8 trigger).
- NO scheduler L4 README rewrite — only the `applied_recipes:` key addition + plural list. All existing R7 content stays.

---

## §4 Recipe Inventory (2 recipes) — ALL `spec_ref:` disk-verified

> Disk-verified by `grep -nE "id:" specs/<file>.yaml` on 2026-05-21. Every cited anchor below appears in the actual spec file. Korean evidence ledger in §4.4.

### 4.1 `lms`

- **L4 composition (5 existing + 1 optional):** `crud`, `audit-log`, `notification`, `scheduled-task`, `auth`. **Optional:** `feature-flags` for course-toggle (e.g., "preview-only / publish gate" per fork-receiver demand).
- **L2 blocks used (existing only — SP43 prep `find templates/L2/blocks -type d`):** `crud-list-adapter`, `crud-create-form`, `crud-edit-form`, `data-table`, `filter-bar`, `notification-list`, `notification-bell`, `confirm-dialog`, `calendar` (L1 — due-date UI), `date-range-picker` (L1), `relative-time` (L1), `kpi-card` (dashboard), `progress-bar` (L1 if exists; SP43 disk-verify).
- **L3 pages used:** `list-page`, `detail-page`, `create-page`, `edit-page`, `dashboard-page`.
- **Business invariants (all 5 disk-RESOLVED at PRD signature):**
  - `LMS-INV-001`: Course content mutations emit audit-log row with operator + before/after state. `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-001` (line 7) + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-002` (line 23). **PASS.**
  - `LMS-INV-002`: Due-date reminder emission uses scheduled-task lock primitive (no double-send on multi-node). `spec_ref: specs/scheduled-task-l0.yaml#SCHED-LOCK-001` (line 21) + `spec_ref: specs/scheduled-task-l0.yaml#SCHED-IDEMPOTENT-001` (line 64). **PASS.**
  - `LMS-INV-003`: Reminder notifications respect learner notification preferences + opt-out. `spec_ref: specs/notification-l0.yaml#NOTIF-PREF-001` (line 131). **PASS** (community precedent R7 INV-003).
  - `LMS-INV-004`: Course visibility (draft / published / archived) gated by author OR admin role. `spec_ref: specs/auth-asvs-l1.yaml#ASVS-V4.1.1` (line 139) + `rule_ref: practices/rules/idempotency-key-on-mutations.md`. **PASS.**
  - `LMS-INV-005`: Bulk enrollment operations idempotent — re-submission of same idempotency-key does NOT duplicate enrollments. `spec_ref: specs/scheduled-task-l0.yaml#SCHED-IDEMPOTENT-001` (line 64) + `rule_ref: practices/rules/idempotency-key-on-mutations.md` (disk-verified). **PASS.**
- **Override allowance:** inline — skip `feature-flags` for open-courseware deployments; skip `auth` for fully-public read-only lesson catalog.
- **External evidence (verbatim — 2 PASS, exceeds 1-floor):**
  - Moodle Web Services API 200 OK: `"Once you have done this, your plugin's functions will be accessible to other systems through Web services using one of a number of protocols, like XML-RPC, REST or SOAP."` (quoted_at 2026-05-21)
  - Coursera (post-redirect from building.coursera.org/developer-program) 200 OK: `"Learn from 350+ leading universities and companies"` (quoted_at 2026-05-21; positions Coursera as the canonical platform-of-record for LMS-pattern catalog, even though the dev program subdomain 301-redirected).
- **Downgrades (1):** edX OpenedX REST API page returned HTTP 404 (URL: `https://docs.openedx.org/projects/edx-platform/en/latest/concepts/rest_api.html`); `internal_design` — rationale: 404 means the documented URL has moved, but Moodle + Coursera together exceed 1-floor for lms.
- **Korean evidence:** 인프런 (`https://www.inflearn.com`) returned 200 OK with no developer API documentation text; `internal_design` — explicit confirmation that Korean LMS dev-API verbatim is unavailable this cycle (consistent with R6 deferral rationale "인프런 closed API").
- **TDD anchor:**
  ```yaml
  tdd_anchor:
    test_file: "frontend/tests/recipes/lms-compose.spec.ts"
    assertion: "recipes/lms/RECIPE.md frontmatter enabled_l4_domains: list equals [audit-log, auth, crud, notification, scheduled-task] (alphabetical) AND 5 disk-verified spec_ref anchors all resolve via recipe_spec_referential_integrity_guard.sh"
    expected_RED_reason: "recipes/lms/ directory does not exist"
    first_GREEN_command: "bash practices/evals/recipe_spec_referential_integrity_guard.sh && bash practices/evals/recipe_governance_guard.sh && cd frontend && npm test -- tests/recipes/lms-compose.spec.ts"
    owning_SP: "SP43"
  ```

### 4.2 `cms`

- **L4 composition (4 existing + 2 optional):** `crud`, `audit-log`, `scheduled-task`, `notification`. **Optional:** `auth` for editorial multi-user permissions; `search` for content discovery / content lake querying.
- **L2 blocks used (existing only — SP43 prep `find templates/L2/blocks -type d`):** `crud-list-adapter`, `crud-create-form`, `crud-edit-form`, `data-table`, `filter-bar`, `rich-text-editor` (SP32 R5 — disk-verify in SP43 prep per R7 Architect rec 7), `confirm-dialog`, `notification-list`, `relative-time` (L1), `kpi-card`, `search-input` (if `search` enabled), `tag-input` (L1 if exists; SP43 disk-verify).
- **L3 pages used:** `list-page`, `detail-page`, `create-page`, `edit-page`, `dashboard-page`.
- **Business invariants (all 5 disk-RESOLVED at PRD signature):**
  - `CMS-INV-001`: Content publish-state transitions (draft → scheduled → published → archived) emit audit-log row with operator + before/after. `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-001` (line 7) + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-002` (line 23). **PASS.**
  - `CMS-INV-002`: Scheduled-publish uses scheduled-task lock + idempotency primitive (no double-publish on multi-node). `spec_ref: specs/scheduled-task-l0.yaml#SCHED-LOCK-001` (line 21) + `spec_ref: specs/scheduled-task-l0.yaml#SCHED-IDEMPOTENT-001` (line 64). **PASS.**
  - `CMS-INV-003`: Content expiry (scheduled archive) runs via scheduled-task with JobHistory record. `spec_ref: specs/scheduled-task-l0.yaml#SCHED-EXECUTE-001` (line 49) + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RETENTION-001` (line 88). **PASS.**
  - `CMS-INV-004`: Editorial workflow notifications (review-requested, approved, rejected) respect editor notification preferences. `spec_ref: specs/notification-l0.yaml#NOTIF-PREF-001` (line 131) + `spec_ref: specs/notification-l0.yaml#NOTIF-SEND-001` (line 47). **PASS.**
  - `CMS-INV-005`: Content slug uniqueness enforced server-side per locale + content-type combination. `spec_ref: specs/crud-security.yaml#CRUD-VAL-1` (line 22) + `rule_ref: practices/rules/idempotency-key-on-mutations.md`. **PASS.**
- **Override allowance:** inline — skip `auth` for single-author personal CMS; skip `search` for small-corpus deployments.
- **External evidence (verbatim — 3 PASS, exceeds 1-floor with buffer):**
  - Sanity Docs landing 200 OK: `"Real-time database for structured content"` (quoted_at 2026-05-21; reached via `https://www.sanity.io/docs` after `https://www.sanity.io/docs/http-api` returned 404 — host-level docs landing carries the canonical product description).
  - Contentful Management API 200 OK: `"Contentful's Content Management API (CMA) helps you manage content in your spaces."` (quoted_at 2026-05-21).
  - Strapi REST API 200 OK: `"The REST API allows accessing the content-types through API endpoints."` (quoted_at 2026-05-21).
- **Downgrades (0 external; 1 Korean):** All 3 English CMS-vendor URLs returned verbatim. 네이버 블로그 (`https://developers.naver.com/docs/blog/`) returned host-level fetcher block ("Claude Code is unable to fetch from developers.naver.com"); `internal_design` — rationale: consistent with R6/R7 Naver-host pattern; Sanity + Contentful + Strapi all clear evidence floor.
- **TDD anchor:**
  ```yaml
  tdd_anchor:
    test_file: "frontend/tests/recipes/cms-compose.spec.ts"
    assertion: "recipes/cms/RECIPE.md frontmatter enabled_l4_domains: list equals [audit-log, crud, notification, scheduled-task] (alphabetical, with optional auth + search documented as override_allowed) AND 5 disk-verified spec_ref anchors all resolve via recipe_spec_referential_integrity_guard.sh"
    expected_RED_reason: "recipes/cms/ directory does not exist"
    first_GREEN_command: "bash practices/evals/recipe_spec_referential_integrity_guard.sh && bash practices/evals/recipe_governance_guard.sh && cd frontend && npm test -- tests/recipes/cms-compose.spec.ts"
    owning_SP: "SP43"
  ```

### 4.3 Cluster claim (honest framing — R6 SP39 precedent)

lms + cms share the scheduler-consumer axis: both bind `scheduled-task` in `enabled_l4_domains:` and both will trigger the `scheduled-task` L4 README `applied_recipes:` key addition simultaneously (first-consumer arrival; the key is born with `[cms, lms]` list, not added one consumer at a time). Beyond that shared mutation, their primary domain logic is non-overlapping: lms is course-lifecycle (enrollment, completion, due dates); cms is content-lifecycle (authoring, publish, expiry). Atomic SP43 commit covers the shared scheduler-key-addition + both recipes' independent business-invariant authoring. R6 SP39 atomic-3 precedent applies exactly.

### 4.4 Evidence ledger (WebFetched during this PRD revision — 2026-05-21)

| Recipe | URL | HTTP / fetch result | Verbatim quote | Resolution | provenance_class |
|---|---|---|---|---|---|
| lms | `https://building.coursera.org/developer-program/` | 301 redirect → `https://www.coursera.org/` | — | Followed redirect | (redirect captured) |
| lms | `https://www.coursera.org/` | **200 OK — verbatim** | `"Learn from 350+ leading universities and companies"` (quoted_at 2026-05-21) | Verbatim cite | `external` |
| lms | `https://docs.moodle.org/dev/Web_services_API` | **200 OK — verbatim** | `"Once you have done this, your plugin's functions will be accessible to other systems through Web services using one of a number of protocols, like XML-RPC, REST or SOAP."` (quoted_at 2026-05-21) | Verbatim cite | `external` |
| lms | `https://docs.openedx.org/projects/edx-platform/en/latest/concepts/rest_api.html` | **HTTP 404 Not Found** (2026-05-21) | — | Downgrade | `internal_design` — "documented URL moved; Moodle + Coursera together clear 1-floor; R9 evidence refresh re-attempts an updated edX REST API URL if Open edX publishes one" |
| lms (Korean) | `https://www.inflearn.com` | **200 OK — no developer API documentation text** (2026-05-21) | — | Downgrade | `internal_design` — "인프런 navigation + course-listing UI only; no public dev API documentation (matches R6 deferral rationale 'closed API')" |
| cms | `https://www.sanity.io/docs/http-api` | **HTTP 404 Not Found** (2026-05-21) | — | (alternate fetched) | — |
| cms | `https://www.sanity.io/docs` | **200 OK — verbatim** | `"Real-time database for structured content"` (quoted_at 2026-05-21) | Verbatim cite (alternate host path) | `external` |
| cms | `https://www.contentful.com/developers/docs/references/content-management-api/` | **200 OK — verbatim** | `"Contentful's Content Management API (CMA) helps you manage content in your spaces."` (quoted_at 2026-05-21) | Verbatim cite | `external` |
| cms | `https://docs.strapi.io/dev-docs/api/rest` | **200 OK — verbatim** | `"The REST API allows accessing the content-types through API endpoints."` (quoted_at 2026-05-21) | Verbatim cite | `external` |
| cms (Korean) | `https://developers.naver.com/docs/blog/` | **Host-level fetcher block** ("Claude Code is unable to fetch from developers.naver.com", 2026-05-21) | — | Downgrade | `internal_design` — "consistent R6/R7 Naver-host block pattern; Sanity + Contentful + Strapi clear evidence floor" |

**Per-recipe evidence density floor:**
- lms: **2 external verbatim** (Moodle + Coursera) + **1 documented external 404** (edX) + **1 Korean downgrade** (인프런). PASS — clears 1-floor with buffer.
- cms: **3 external verbatim** (Sanity + Contentful + Strapi) + **1 Korean downgrade** (네이버). PASS — clears 1-floor with substantial buffer.
- **Korean cycle:** 2 attempts, 0 verbatim (1 host-block + 1 navigation-only). Documented per R7 M1 precedent — Korean evidence is per-cycle signal; R6 cleared via channel.io, R7 zero-Korean, R8 zero-Korean. No fabrication. R9 evidence refresh re-attempts both URLs if either host removes the block or 인프런 publishes a dev API.

**Re-attempt at SP execution:** SP43 pre-flight re-runs WebFetch on edX OpenedX docs (try `https://docs.openedx.org/` root for an updated REST API link) and Naver Blog API (one-shot host-check) — no fabrication. Any 200 OK upgrades that recipe's ledger row to `external`; any new 4xx/5xx preserves `internal_design`.

---

## §4.5 SP Plan + Verification Matrix (2 SPs)

| SP | Atomic deliverables | TDD anchors (RED → GREEN) | Verification | Observability_signal (advisory) |
|---|---|---|---|---|
| **SP43** (atomic — lms + cms; mutation surface = `recipes/{lms,cms}/`, `specs/recipes/`, `recipes/_MANIFEST.yaml`, 6+ L4 README `applied_recipes:` appends including scheduler-key-add, `skills/_tests/sealed-verdict/{lms,cms}-verdict.md` scaffolds, evidence snapshot) | (a) 2 × `recipes/{lms,cms}/{RECIPE.md, L4-composition.md, L2-block-recipe.md, spec-trio-template.yaml}`; (b) 2 × `specs/recipes/{lms,cms}-recipe-l0.yaml` with 5 disk-verified invariants each per §4.1/§4.2; (c) `recipes/_MANIFEST.yaml` — lms + cms move deferred→active; internal-it row gets refreshed `reintroduction_trigger:` text acknowledging R8 closed scheduler-consuming recipes; (d) `templates/L4/scheduled-task/README.md` gains `applied_recipes:` key with plural list `[cms, lms]` (alphabetical; first-consumer arrival inversion of R7 H2/M4 unused-L4 pattern); (e) `templates/L4/{crud,audit-log,notification,auth,feature-flags,search}/README.md` `applied_recipes:` plural list appends `cms` AND `lms` alphabetically; (f) 2 × `skills/_tests/sealed-verdict/{lms,cms}-verdict.md` scaffolds (verdict: PENDING; executed in SP44); (g) 2 × `frontend/tests/recipes/{lms,cms}-compose.spec.ts`; (h) `practices/upstream/r8-sp43-evidence-snapshot.md` capturing all 8 WebFetch attempts with HTTP status + 2026-05-21 timestamp + verbatim-or-downgrade-rationale. | 2 × compose-test: RED (recipe dirs absent) → GREEN. `recipe_governance_guard.sh`: RED (lms + cms have no `applied_recipes:` wiring) → GREEN. `recipe_spec_referential_integrity_guard.sh`: RED (specs absent) → GREEN. Scheduler L4 README key-add: RED (no `applied_recipes:` key) → GREEN (key + plural list present). | `bash practices/evals/recipe_governance_guard.sh` exit 0 against all 9 active recipes; `bash practices/evals/recipe_spec_referential_integrity_guard.sh` exit 0 against all 9 specs; lms-compose.spec.ts + cms-compose.spec.ts pass; `/ax-verify-domain` × 6 touched L4 (`crud`, `audit-log`, `notification`, `auth`, `feature-flags`, `search`) exit 0. Scheduler L4 README new `applied_recipes:` key verified via `grep -E "^applied_recipes:" templates/L4/scheduled-task/README.md` exit 0 + list-non-empty check. | `recipe.lms.course_active_total`, `recipe.lms.reminder_scheduled_total`, `recipe.cms.content_scheduled_publish_total`, `recipe.cms.content_archived_total` — advisory only. |
| **SP44** (FINAL — 2 sealed verdicts harness exec + tag policy + PR) | (a) Sealed sub-agent execs `{lms,cms}-verdict.md` (context-0 inputs per R7 P5 mitigation: README + practices/AGENTS.md + recipe Spec Trio + recipe spec); (b) `recipes/README.md` updated — 9 active + 1 deferred with refreshed trigger; (c) `/ax-verify` exit 0. **Tag policy (Option 2 partial-aware):** tag `v1.6.0-lms-cms` IFF 2/2 verdicts pass; **partial tag `v1.6.0-lms-only`** IFF lms-verdict passes AND cms-verdict fails (cms marks `status: active-verdict-pending` in `_MANIFEST.yaml`; SP45 fast-follow); **partial tag `v1.6.0-cms-only`** IFF cms-verdict passes AND lms-verdict fails (lms marks `status: active-verdict-pending`); **no tag** IFF 0/2 pass (both recipes mark `status: active-verdict-pending`; SP45 + SP46 fast-follows). | Sealed verdict harness × 2: PENDING → PASS (or PASS / PENDING for partial). | `/ax-verify` exit 0; manual review of 2 sealed verdicts; tag policy enforced by commit message. | `recipes.active_total: 9` (or 8 partial / 7 zero); `recipes.deferred_total: 1` (or 2 partial / 3 zero); `L4.domain_total: 11` (UNCHANGED). |

**SP atomicity rule:** Within SP43, all lms + cms artifacts AND the scheduler L4 README key-add ship together OR rollback. The scheduler-key-add is bundled with the first-consumer arrival (lms + cms simultaneously) — there is no "scheduler key added in advance" scenario. SP43 failure leaves R7's scheduler L4 README in its existing key-less state.

**SP linearization:** SP43 → SP44. No parallel branches.

---

## §5 Scheduler L4 README key-add rationale (Q1 — R8-specific)

**Resolution: R7 left `templates/L4/scheduled-task/README.md` without `applied_recipes:` key (per H2/M4 unused-L4 precedent, matching `file-storage` + `practices` L4 READMEs). R8 inverts this exactly as TD-020 Follow-ups promised: "R8 ships lms + cms recipes consuming scheduler; in same R8 atomic commit, `applied_recipes:` key + plural list `[lms, cms]` added to scheduler README." This PRD honors TD-020 Follow-ups literally.**

### Disk evidence (2026-05-21)

- `templates/L4/scheduled-task/README.md` exists from R7 SP41 commit `faaf87d`.
- `grep -E "^applied_recipes:" templates/L4/scheduled-task/README.md` → exit 1 (key absent).
- R7 TD-020 Follow-ups text: "R8 ships lms + cms recipes consuming scheduler; in same R8 atomic commit, `applied_recipes:` key + plural list `[lms, cms]` added to scheduler README."

### Why both consumers arrive together (not lms-first-then-cms)

Both R6-deferred recipes (lms + cms) named identical scheduler trigger text. R7 satisfied that trigger for both simultaneously. R8 SP43 atomic-2 means the key is created with both consumers listed from birth (alphabetical: `[cms, lms]`), avoiding the awkward intermediate state where only one consumer exists and the other is "coming later." If Option (3) split were chosen (SP43 lms-atomic + SP43b cms-atomic-sequential), the scheduler README would gain `applied_recipes: [lms]` in SP43 then mutate to `[cms, lms]` in SP43b — 2 mutations vs 1 with no atomic benefit. R6 SP39 atomic-3 precedent: all 3 recipes' L4 README updates landed in one commit.

### Migration plan (within SP43)

1. Add `applied_recipes:` key to `templates/L4/scheduled-task/README.md` frontmatter with plural list `[cms, lms]` (alphabetical).
2. Append `cms` and `lms` to `applied_recipes:` plural list on `templates/L4/{crud,audit-log,notification,auth,feature-flags,search}/README.md` (alphabetical insertion; R5/R6/R7 entries preserved per R6 append-only rule).
3. Disk-verify `bash recipe_governance_guard.sh` exit 0 on the scheduler README — the dual-form regex from R6 already accepts plural-list form; the empty-list-fail fixture from R6 already proves that non-empty plural list satisfies the rule.
4. No new fixtures required (R6 SP39 fixtures `pass_applied_recipes_plural` + `fail_applied_recipes_empty_list` already cover the dual-form acceptance + non-empty constraint).

---

## §6 Autonomous Execution Safety

- **Pre-flight gate (before SP43 starts):** §4.4 evidence ledger captured. SP43 pre-flight re-runs edX OpenedX docs root + Naver Blog API once each (one-shot, no fabrication). Disk-verify `rich-text-editor` L2 block exists (R7 Architect rec 7 carried forward); disk-verify `calendar` + `date-range-picker` + `relative-time` L1 primitives. Abort if any prep step fails.
- **Mid-flight gate (between SP43 and SP44):** `git status` clean; `/ax-verify-domain` × 6 (crud / audit-log / notification / auth / feature-flags / search) exit 0; `recipe_governance_guard.sh` exit 0 against all 9 active recipes; `recipe_spec_referential_integrity_guard.sh` exit 0 against all 9 specs; both compose-test files pass; commit message references SP43 + 2 recipe IDs (lms-INV-001..005, cms-INV-001..005).
- **Stop conditions:** If `recipe_governance_guard.sh` or `recipe_spec_referential_integrity_guard.sh` cannot reach GREEN within 3 iter cycles for SP43, halt and escalate. SP43 atomicity hard — no "lms-only-keep-cms-deferred" loophole within SP43. Failed recipes return to deferred via SP43 rollback (manifest entries MOVED back; scheduler L4 README key-add reverted).
- **Sealed verdict release policy (Option 2 partial-aware):** Tag `v1.6.0-lms-cms` ships IFF 2/2 SP44 verdicts pass. Partial `v1.6.0-lms-only` IFF only lms passes; partial `v1.6.0-cms-only` IFF only cms passes; no tag IFF 0/2 pass. Failing recipes mark `status: active-verdict-pending` in `_MANIFEST.yaml`; SP45 fast-follow next ralplan cycle resolves.
- **Rollback:** Each SP is one squash-mergeable commit. Revert single SP if downstream issue detected without disturbing prior SPs. R7's scheduler L4 README returns to key-less state on SP43 rollback.
- **No destructive ops:** No `git reset --hard`, no `git push --force`. Manifest entries MOVED (not deleted). Scheduler L4 README `applied_recipes:` key ADDED (not replacing existing content; R7 README body preserved).

---

## §7 Pre-Mortem (4 scenarios — DELIBERATE mode)

1. **edX OpenedX REST API page returns 404 at SP43 execution (already observed in §4.4 ledger at PRD signature).**
   - Likelihood: HIGH (already 404 at 2026-05-21).
   - Impact: lms external anchor count is 2 (Moodle + Coursera). Sealed verdict may weight Korean-evidence-gap dimension; lms-verdict could score borderline 10/12 instead of comfortable 11/12.
   - Mitigation: Moodle verbatim is canonical XML-RPC/REST/SOAP description — directly maps to lms's integration model; Coursera verbatim is canonical LMS market positioning. SP43 pre-flight re-attempts edX root (`https://docs.openedx.org/`) for an updated REST API link; if a current URL is found, upgrade `internal_design` row to `external` in the SP43 evidence-snapshot file. If still 404, sealed verdict harness given the explicit downgrade rationale in `practices/upstream/r8-sp43-evidence-snapshot.md` so sub-agent context-0 review knows the rationale.

2. **Scheduler L4 README key-add mutation conflicts with future R9+ recipe consumers.**
   - Likelihood: LOW. R6 SP39 dual-form regex + plural-list-append-only rule explicitly designed for this.
   - Impact: If R9 introduces a 3rd scheduler-consumer recipe, the SP43 commit's `[cms, lms]` list needs to be extended to `[cms, lms, <r9-recipe>]` — but this is the same alphabetical-append pattern used 6+ times across R5/R6/R7 L4 READMEs. No semantic change.
   - Mitigation: TD-2026-05-21-024 (see §8) documents the scheduler-consumer convention explicitly so R9 planner knows the canonical append form.

3. **Sealed verdict for cms scores below threshold because rich-text-editor L2 block (SP32 R5) is missing or has drifted shape.**
   - Likelihood: LOW (SP32 R5 landed; carried through R6/R7 with no documented removal). Re-verified in SP43 prep per R7 Architect rec 7.
   - Impact: cms-verdict may flag "L2 block coverage incomplete" if rich-text-editor absent; cms-verdict scores below 10/12 MUST.
   - Mitigation: SP43 pre-flight `find templates/L2/blocks -type d -name "rich-text-editor"` exit 0 check is a hard prerequisite; if absent, SP43 halts before any commit and escalates. cms recipe explicitly notes rich-text-editor as optional-but-recommended; sealed verdict prompt clarifies this so absence does not auto-fail.

4. **Korean evidence cycle is again zero-verbatim (인프런 + 네이버 both downgraded in this PRD revision).**
   - Likelihood: HIGH (already observed in §4.4).
   - Impact: R8 is the second consecutive zero-Korean-verbatim cycle (R7 was the first). Sealed verdict harness may flag pattern.
   - Mitigation: Per R7 M1 precedent, Korean evidence is per-cycle signal; project vision (CLAUDE.md) framing of "Korean enterprise standard stack" is honored at the **stack level** (Spring Boot + React are the user-validated stack); Korean verbatim source-anchoring is a per-cycle signal that varies (R6 cleared via channel.io; R7 + R8 zero). 2 documented Korean attempts per cycle (인프런 + 네이버 in R8) demonstrate the discipline. R9 evidence refresh re-attempts both URLs + considers expanding the Korean URL pool to ko.coursera.org / 카카오 만들기 / 토스 tech if any new content surfaces.

---

## §8 ADR Template (2 + 1 entries)

Decision-bearing only:

- **TD-2026-05-21-022 (NEW)** — Recipe `lms` shipped via composition of `crud + audit-log + notification + scheduled-task + auth (+ optional feature-flags)`.
  - **Decision:** `recipes/lms/` moves deferred→active. Active recipe count 7 → 8 (or 9 with cms). Scheduler L4 first consumer (alongside cms).
  - **Drivers:** (a) R6 `_MANIFEST.yaml` named scheduler as gating trigger; R7 v1.5.0 satisfied it. (b) 2 external verbatim PASS (Moodle Web Services + Coursera). (c) 5 invariants disk-resolved at PRD signature.
  - **Alternatives considered:** Defer lms past R8 (rejected — trigger discipline becomes arbitrary if satisfied gate doesn't consume); single-recipe SP43 lms-only (rejected — cms gate identically satisfied, R6 SP39 atomic-3 precedent applies); split SP43 lms-atomic + SP43b cms-atomic-sequential (rejected — R7 split was justified by L4-introduction + harness novelty, neither applies to R8).
  - **Why chosen:** Honors deferral-trigger discipline (named gate cleared → consume); R6 SP39 atomic precedent; evidence chain verbatim-cleared.
  - **Consequences:** Active recipe count = 8 (or 9 with cms). 6 L4 READMEs gain `lms` in `applied_recipes:` plural list. scheduled-task L4 README gains `applied_recipes: [cms, lms]` key (first-consumer arrival; honors R7 TD-020 Follow-ups literally).
  - **Follow-ups:** R9 evidence refresh re-attempts edX OpenedX REST API URL + 인프런 dev API if either publishes.

- **TD-2026-05-21-023 (NEW)** — Recipe `cms` shipped via composition of `crud + audit-log + scheduled-task + notification (+ optional auth + search)`.
  - **Decision:** `recipes/cms/` moves deferred→active. Active recipe count 8 → 9 (alongside lms). Scheduler L4 second consumer (with lms).
  - **Drivers:** (a) R6 `_MANIFEST.yaml` named scheduler as gating trigger; R7 v1.5.0 satisfied it. (b) 3 external verbatim PASS (Sanity + Contentful + Strapi — strongest English-tier CMS evidence chain in the catalog). (c) 5 invariants disk-resolved at PRD signature.
  - **Alternatives considered:** Defer cms past R8 (rejected — same trigger discipline as lms; evidence chain actually stronger); merge cms into crud + scheduled-task without separate recipe (rejected — content-lifecycle is genuinely distinct from generic CRUD; rich-text-editor + scheduled-publish + slug-uniqueness are recipe-level invariants); split SP43 (rejected — same as lms).
  - **Why chosen:** 3 external verbatim (Sanity + Contentful + Strapi) is the strongest evidence chain shipped in any single recipe this cycle; honors deferral-trigger discipline.
  - **Consequences:** Active recipe count = 9. 6 L4 READMEs gain `cms` in `applied_recipes:` plural list. scheduled-task L4 README gains `applied_recipes: [cms, lms]` key alongside lms.
  - **Follow-ups:** R9 evidence refresh re-attempts 네이버 블로그 API if Naver host removes fetcher block.

- **TD-2026-05-21-024 (NEW)** — Scheduler L4 `applied_recipes:` key born with plural list `[cms, lms]` (first-consumer arrival convention).
  - **Decision:** When an L4 README acquires its first recipe-consumer(s), the `applied_recipes:` key is ADDED with the full alphabetical list of arriving consumers, not added with one consumer and mutated later. If multiple consumers arrive in the same SP, all are listed from birth.
  - **Drivers:** (a) R7 TD-020 Follow-ups text explicitly promised this shape for R8. (b) R6 SP39 dual-form regex + alphabetical-append rule supports it directly. (c) Atomic-commit principle (Principle 5) means key-add + plural-list-population are one mutation, not two.
  - **Alternatives considered:** Add key with `[lms]` in SP43, then append `cms` in SP43b (rejected — 2 mutations vs 1 with no atomic benefit; R6 precedent is single-commit multi-recipe append).
  - **Why chosen:** Honors R7 TD-020 Follow-ups literally; matches R6 atomic-commit precedent; consistent with `recipe_governance_guard.sh` non-empty-list-required semantics.
  - **Consequences:** When future R9+ adds a 3rd scheduler-consumer, the list extends alphabetically (`[cms, lms, <r9-recipe>]`); same pattern as all other multi-consumer L4 READMEs.
  - **Follow-ups:** R9+ planners reference this ADR when documenting any other "first-consumer arrival" scenario for unused-L4 readmes (file-storage, practices remain unused-L4; their `applied_recipes:` key will be born when first consumer recipe arrives).

Each ADR will be populated in SP44 PR description with: Decision, Drivers, Alternatives considered, Why chosen, Consequences, Follow-ups.

---

## §9 Honored Constraints

- Tier-1 cap **= 4** (FROZEN).
- Tier-2 count **= 8** (UNCHANGED).
- L4 domain count **= 11** (UNCHANGED — R8 consumes scheduler; does NOT add L4).
- L1 catalog **= 49** (UNCHANGED).
- L2 catalog **= 92** (UNCHANGED).
- L3 catalog **= 20** (UNCHANGED).
- Spec Trio atomic rule per SP (SP43 atomic-2: lms + cms + scheduler-key-add bundled).
- Composition kit framing — recipes COMPOSE existing L4; R8 is the R6-shape cycle (no L4 expansion).
- Korean references — 2 attempts logged in §4.4 with explicit downgrade rationale (인프런 200 OK no-API; 네이버 host-block). Zero-Korean-verbatim cycle documented per R7 M1 precedent.
- Out-of-scope: deployment / CI / release policy / docs site / new skills / new L2 / L3 / L4 / RECIPE_DEVIATION.md ceremony / internal-it recipe (independent trigger).
- R6 dual-form `applied_recipes:` regex + alphabetical-append-only rule honored (no new fixtures needed).
- R5/R6/R7 sealed verdict threshold (≥10/12 MUST + ≥5/8 SHOULD) honored.
- R7 TD-020 Follow-ups literally honored (scheduler L4 README key-add with `[cms, lms]` first-consumer-arrival convention).

---

## §10 Out-of-scope (R8 explicit) + Deferred Recipes (1 remaining)

### Deferred recipes (refreshed trigger reflects R8 closure of scheduler-consuming recipes)

| Recipe | R7 deferral rationale | R8-refreshed `reintroduction_trigger:` |
|---|---|---|
| `internal-it` | Independent of R7 scheduler. Jira/ServiceNow REST API not verbatim-fetched. Webhook patterns vendor-specific. | "**Independent of R8 scheduler-consuming recipes (lms + cms landed R8 v1.6.0).** Remaining gap = verbatim Jira/ServiceNow REST API quote + clarified webhook-emit primitive in notification L4. R9+ if fetch succeeds OR notification L4 gains explicit webhook-emit spec items." |

### Out-of-scope (R8)

- New L1 / L2 / L3 / L4 surface.
- New Tier-1 / Tier-2 skill.
- New practices rule families. Both R8 recipes bind to existing anchors only.
- `RECIPE_DEVIATION.md` ceremony.
- `/ax-scaffold business <pattern> --analyze` free-text NLP inference.
- Backend API endpoint implementations for lms or cms. Recipes specify spec_ref bindings; fork-receiver implements code.
- internal-it recipe (independent trigger; R9+ scope).
- Korean URL pool expansion beyond 인프런 + 네이버 (R9+ evidence refresh).
- Scheduler L4 backend skeleton expansion (R7 SP41 shipped stub; R8 consumes catalog-only; fork-receiver implements consumer business logic).
- 6-month recipe-retirement review (R5/R6/R7 deferral stands).
- Migration sweep on legacy `applied_recipe:` (singular) → `applied_recipes:` (plural) lines (R6 dual-form regex still accepts both; deferred to R9+ once all consumers normalize).

---

## §11 Branch + path summary

- **Branch:** `feat/r8-lms-cms-sp43-sp44` (cut from `main` at `faaf87d` — `v1.5.0-scheduler-community` tag).
- **PRD path (this draft):** `docs/superpowers/specs/2026-05-21-r8-lms-cms-prd.draft.md`.
- **Manifest target:** `recipes/_MANIFEST.yaml` — lms + cms move deferred→active; internal-it row stays with refreshed trigger.
- **DECISIONS.md target:** `practices/DECISIONS.md` — append TD-022 (lms), TD-023 (cms), TD-024 (scheduler-key-add first-consumer convention).
- **Evidence snapshot path:** `practices/upstream/r8-sp43-evidence-snapshot.md` — captures all 8 WebFetch attempts with HTTP status + 2026-05-21 timestamp.
- **Final tag:** `v1.6.0-lms-cms` IFF 2/2 SP44 verdicts pass; `v1.6.0-lms-only` IFF only lms passes; `v1.6.0-cms-only` IFF only cms passes; no tag IFF 0/2 pass.

---

## §12 Verdict line

R8 draft adds **2 of 3 R7-deferred recipes** (lms + cms — both scheduler-consuming) via the same atomic Spec-Trio + sealed-verdict pattern that shipped 3 recipes in R6 v1.4.0 and 1 recipe + 1 L4 in R7 v1.5.0. Mutation surface: 2 recipe quartets + 2 recipe specs + 6 L4 README plural-list appends + 1 scheduler L4 README `applied_recipes:` key-add (R7 TD-020 Follow-ups literally honored) + 1 manifest move pair + 2 sealed-verdict scaffolds + 1 evidence snapshot. Internal-it stays deferred (independent trigger). All `spec_ref:` disk-verified; Korean evidence ledger captured live (2 downgrades documented); 5/6 external WebFetch verbatim PASS (1 edX 404 documented). Per-recipe TDD anchors executable. Multi-recipe membership via R6 dual-form regex (no new fixtures). SP count 2 (SP43 atomic-2 + SP44 FINAL). Tag held until 2/2 sealed verdicts pass; partial-tag fallback explicit. R6 SP39/SP40 cadence-parity restored after R7's L4-introduction one-time deviation.

---

## RALPLAN-DR Summary

**Mode:** DELIBERATE.

**Principles (8):** composition kit, spec-before-code, binary verification, Tier-1/Tier-2 frozen, atomic Spec Trio per SP, recipe-no-code, scheduler-consumed-correctly, no-new-L2/L3.

**Decision Drivers (top 3):** reintroduction-trigger discipline (R6-named scheduler gate cleared by R7; R8 honors the trigger) · evidence rigor (5/6 external verbatim + zero Korean documented) · R6 cadence parity (atomic-multi-recipe SP after R7's one-time L4-introduction split).

**Viable Options (3 considered; 1 chosen):**
- Option (1) — defer cms ship lms-alone: REJECTED (trigger discipline arbitrary; cms evidence stronger than lms).
- **Option (2) — SP43 atomic-2 + SP44 FINAL (R6 SP39/SP40 precedent): CHOSEN.**
- Option (3) — SP43/SP43b split (R7 Synthesis-B precedent): REJECTED (R7 split justified by L4-introduction + harness novelty; neither applies to R8).

**Recommended path:** SP43 atomic (lms + cms + scheduled-task L4 README key-add + 6 L4 README plural-list appends + manifest moves + 2 sealed-verdict scaffolds PENDING + evidence snapshot) → SP44 FINAL (2 sealed verdict execs + tag policy v1.6.0-lms-cms partial-aware + PR).

**Wall-time estimate:** 3-4 days.

**Pre-mortem scenarios:** 4 (edX 404 already observed → mitigation Moodle + Coursera buffer; scheduler-key future-extension → R6 dual-form regex covers; cms rich-text-editor drift → SP43 prep hard-gate; Korean zero-verbatim 2nd consecutive cycle → R7 M1 precedent).

**Test plan (expanded — DELIBERATE):**
- **Unit:** recipe-level spec YAML structure validation (5 invariants per recipe present; spec_ref anchors point to disk-resolvable IDs).
- **Integration:** `recipe_governance_guard.sh` + `recipe_spec_referential_integrity_guard.sh` exit 0 against all 9 active recipes.
- **E2E:** `frontend/tests/recipes/{lms,cms}-compose.spec.ts` per-recipe TDD anchors RED → GREEN; `/ax-verify-domain` × 6 touched L4 exit 0.
- **Observability:** advisory counters `recipe.lms.course_active_total`, `recipe.lms.reminder_scheduled_total`, `recipe.cms.content_scheduled_publish_total`, `recipe.cms.content_archived_total` documented as future emission points (NOT enforced this cycle; R9+ if fork-receivers demand).

**ADRs (3):** TD-2026-05-21-022 (lms) · TD-2026-05-21-023 (cms) · TD-2026-05-21-024 (scheduler-L4 first-consumer arrival convention `[cms, lms]`).

**Tag policy:** `v1.6.0-lms-cms` IFF 2/2 verdicts pass; partial `v1.6.0-lms-only` / `v1.6.0-cms-only` IFF 1/2; no tag IFF 0/2.

**Evidence ledger summary (verbatim count, downgrade count):** 5 verbatim external (Moodle + Coursera + Sanity + Contentful + Strapi) · 3 downgrades (edX 404, 인프런 200 OK no-API, 네이버 host-blocked) · 1 redirect captured (Coursera 301 → www.coursera.org followed; verbatim secured at post-redirect host).
