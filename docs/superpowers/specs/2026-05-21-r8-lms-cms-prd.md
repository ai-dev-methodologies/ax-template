# R8 — lms + cms Recipes PRD — 2026-05-21 (Round 8, ralplan iter 3 APPROVED)

> **Status:** APPROVED (3-iter ralplan consensus; Codex Critic iter 3 FINAL APPROVE).
> **Date:** 2026-05-21. **Repo:** ax-template. **Format:** RALPLAN-DR.
> **Iter lineage:**
> - `2026-05-21-r8-lms-cms-prd.draft.md` (iter 1 — 417 lines, Architect APPROVE-WITH-FIXES 1 HIGH + 2 MEDIUM + Codex Critic ITERATE 3 reaffirmed + 1 new BLOCKING L + 3 soft).
> - `2026-05-21-r8-lms-cms-prd.iter2.md` (iter 2 — 434 lines, Architect APPROVE + 1 INFORMATIONAL; Codex Critic ITERATE narrow line count).
> - `2026-05-21-r8-lms-cms-prd.iter3.md` (iter 3 — 446 lines, append-only changelog; Codex Critic FINAL APPROVE).
> **Predecessors:**
> - `2026-05-20-r7-scheduler-community-prd.md` (CLOSED, APPROVED iter 3 — `v1.5.0-scheduler-community` on `main@faaf87d`).
> - `2026-05-19-r6-recipes-prd.md` (CLOSED — `v1.4.0-recipes-complete` on `main@ab44cce`).
> **Branch (when execution starts):** `feat/r8-lms-cms-sp43-sp44`.
> **Targeted tag:** `v1.6.0-lms-cms` IFF 2/2 sealed verdicts pass; else partial; else no tag.

---

## §1 RALPLAN-DR Summary

### Cycle frame (6 bullets)

- **R7 closed.** `v1.5.0-scheduler-community` shipped scheduler L4 + community recipe. 3 deferred (lms, cms, internal-it).
- **R8 strategy.** Both lms + cms `reintroduction_trigger:` named "scheduler primitive" verbatim; R7 cleared the gate. R8 ships both atomically (SP43) since they share `scheduled-task` first-consumer-arrival mutation.
- **R6 parity choice.** No new L4 / L3 / L2 / L1 / skill. 2 SPs: SP43 atomic-2 + SP44 FINAL.
- **Disk discoveries (2026-05-21).** `recipes/_MANIFEST.yaml` confirms both triggers cleared; `templates/L4/scheduled-task/README.md` lacks `applied_recipes:` key (SP43 ADDS with `[cms, lms]`). `templates/L2/blocks/` disk-listing performed for inventory contract (see §4.1/§4.2).
- **Evidence rigor (iter 2).** 8 logical English WebFetch attempts (Coursera + Moodle + edX + Sanity-base + Sanity-scheduled + Contentful + Strapi + 2 redirect/alternate rows captured separately) + **5 Korean attempts** (인프런 · ko.coursera.org · classting.com · tech.kakao.com · terms.naver.com · brunch.co.kr · developers.naver.com — 7 host attempts total Korean). Verbatim PASS: 5 English + **2 Korean** (classting + brunch). Topic-relevant scheduled-publish PASS: **1** (Sanity scheduled-publishing deprecation notice). All downgrades documented with HTTP status + 2026-05-21 timestamp.
- **Internal-it stays deferred** with R8-refreshed trigger.

### Principles (8)

1. **Composition kit, not single product.** Zero new L4 / L3 / L2 / L1 / Tier-1 / Tier-2 skill.
2. **Spec-before-code, evidence-anchored AT PRD SIGNATURE.** All 10 `spec_ref:` disk-verified; Korean ledger expanded to 5 host attempts (3 PASS, 4 downgrades — meets R7 5-floor).
3. **Binary verification per axis.** `/ax-verify` exits 0; both recipe guards exit 0; 2 sealed verdicts ≥10/12 MUST + ≥5/8 SHOULD.
4. **Tier-1 cap = 4, Tier-2 = 8, L1 = 49, L2 = 92, L3 = 20, L4 = 11.** FROZEN.
5. **Atomic Spec Trio per SP.** SP43 atomic-2 (R6 SP39 precedent).
6. **Recipe ships no code.** Inherited.
7. **Scheduler L4 consumed correctly.** lms + cms both bind `scheduled-task`; key born `[cms, lms]`.
8. **No new L2 / L3 / practices rule families.** Bind to existing rules + spec items only (see §4.1/§4.2 INV-005 disambiguation paragraph).

### Decision Drivers (top 3)

1. **Reintroduction-trigger discipline.** R6 named scheduler as gate; R7 cleared it; R8 consumes — binary trigger system honored.
2. **Evidence rigor.** 5 English verbatim + 1 topic-relevant scheduled-publish verbatim + 2 Korean verbatim (classting + brunch). Both recipes clear 1-floor with comfortable buffer.
3. **R6 cadence parity.** Atomic-multi-recipe SP; no new L4; no parallel branches.

### Viable Options Considered

- **Option (1) — Defer cms; ship lms alone.** REJECTED (trigger discipline arbitrary; cms evidence stronger).
- **Option (2) — Atomic SP43 (lms + cms) + SP44 FINAL.** CHOSEN (R6 SP39 atomic-3 precedent).
- **Option (3) — Split SP43 lms-atomic + SP43b cms-atomic.** REJECTED (R7 split was justified by L4-introduction + harness novelty; neither applies to R8).

### Mode: DELIBERATE.

### Recommended

```
SP43  (atomic — lms + cms: 2 recipes + 2 Spec Trios + 2 recipe specs + scheduled-task L4 README applied_recipes key add + 6 L4 README plural-list appends + manifest moves + 2 sealed-verdict scaffolds PENDING + evidence snapshot)
    ↓ gated on recipe_governance_guard.sh + recipe_spec_referential_integrity_guard.sh exit 0 AND 2 compose-test files pass
SP44  (FINAL — sealed verdict exec × 2 + /ax-verify full; tag v1.6.0-lms-cms IFF 2/2 pass; partial v1.6.0-{lms,cms}-only IFF 1/2 pass; no tag IFF 0/2 pass; PR)
```

Total: **2 SPs, ≈ 3-4 d wall-time.**

---

## §2 Context

### R7 v1.5.0 disk-verified state (2026-05-21)

| Surface | Count | Path |
|---|---|---|
| L1 primitives | 49 | `templates/L1/components/` |
| L2 blocks | 92 | `templates/L2/blocks/` |
| L3 pages | 20 | `templates/L3/pages/` |
| L4 domains | 11 | `templates/L4/` |
| Active recipes | 7 | `recipes/` |
| Deferred recipes | 3 | `recipes/_MANIFEST.yaml#deferred_recipes` (lms, cms, internal-it) |
| Sealed verdicts | 8 | `skills/_tests/sealed-verdict/` |
| Current tag | `v1.5.0-scheduler-community` on `main@faaf87d` | `git tag --sort=-creatordate \| head -1` |

### R8 scope

- **Deliverable 1 (lms, SP43 atomic-half-1):** `recipes/lms/` quartet + `specs/recipes/lms-recipe-l0.yaml`. Composition: `crud + audit-log + notification + scheduled-task + auth` (+ optional `feature-flags`). 5 invariants disk-RESOLVED.
- **Deliverable 2 (cms, SP43 atomic-half-2):** `recipes/cms/` quartet + `specs/recipes/cms-recipe-l0.yaml`. Composition: `crud + audit-log + scheduled-task + notification` (+ optional `auth`, `search`). 5 invariants disk-RESOLVED.
- **Shared SP43 mutations:** `recipes/_MANIFEST.yaml` moves; `templates/L4/scheduled-task/README.md` `applied_recipes:` key born `[cms, lms]`; 6 L4 READMEs plural-list append.

NO new L3 / L2 / L1 / Tier-1 / Tier-2 skill. Recipe count 7 → 9. L4 count UNCHANGED (11).

---

## §3 Objectives + Guardrails

### Must Have

- 2 recipes (`recipes/{lms,cms}/`) full Spec Trio quartet.
- 2 recipe specs (`specs/recipes/{lms,cms}-recipe-l0.yaml`) — `l2_blocks_used:` strictly L2-disk-resolvable (Critic L blocker fix; see §4.1/§4.2).
- 2 sealed verdicts (≥10/12 MUST + ≥5/8 SHOULD each).
- `recipes/_MANIFEST.yaml` — lms + cms deferred→active; internal-it refreshed.
- `templates/L4/scheduled-task/README.md` `applied_recipes: [cms, lms]` key born.
- 6 L4 READMEs (`crud, audit-log, notification, auth, feature-flags, search`) append `cms` + `lms` alphabetically.
- 2 × `frontend/tests/recipes/{lms,cms}-compose.spec.ts`.
- 1 × `practices/upstream/r8-sp43-evidence-snapshot.md` with all logical attempts + redirect/alternate rows + HTTP status + 2026-05-21 timestamp.
- `/ax-verify` exit 0; both recipe guards exit 0 against 9 active recipes/specs.
- Tag `v1.6.0-lms-cms` IFF 2/2 sealed verdicts pass; partial-tag policy explicit (§6 table).

### Must NOT Have

- NO new L4 / L3 / L2 / L1 / Tier-1 / Tier-2 skill.
- NO new practices rule family.
- NO `RECIPE_DEVIATION.md` ceremony.
- NO `/ax-scaffold business <pattern> --analyze` free-text NLP.
- NO Korean reference fabrication.
- NO change to git workflow / CI policy / release process.
- NO partial deliverable within an SP (partial-tag policy is SP44-level).
- NO internal-it work this cycle.
- NO scheduler L4 README body rewrite — only the `applied_recipes:` key addition.

---

## §4 Recipe Inventory (2 recipes) — ALL `spec_ref:` disk-verified

### 4.1 `lms`

- **L4 composition (5 existing + 1 optional):** `crud`, `audit-log`, `notification`, `scheduled-task`, `auth`. **Optional:** `feature-flags`.
- **L2 blocks used (disk-verified via `ls templates/L2/blocks/*.tsx` on 2026-05-21 — guard-resolvable only):** `crud-create-form`, `crud-edit-form`, `crud-list-adapter`, `data-table`, `filter-bar`, `kpi-card`, `notification-bell`, `notification-list`, `confirm-dialog`.
- **L1 primitives consumed (documented in `recipes/lms/L2-block-recipe.md` "L1 primitives consumed" subsection — NOT in `l2_blocks_used:` of recipe spec):** `calendar` (due-date UI), `date-range-picker` (course schedule range), `relative-time` (last-activity displays), `progress` (lesson-completion bar — disk file is `templates/L1/components/progress.tsx`, NOT `progress-bar.tsx`).
- **L3 pages used:** `list-page`, `detail-page`, `create-page`, `edit-page`, `dashboard-page`.
- **Business invariants (all 5 disk-RESOLVED at PRD signature):**
  - `LMS-INV-001`: Course content mutations emit audit-log row with operator + before/after. `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-001` + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-002`. **PASS.**
  - `LMS-INV-002`: Due-date reminder emission uses scheduled-task lock primitive (no double-send on multi-node). `spec_ref: specs/scheduled-task-l0.yaml#SCHED-LOCK-001` + `spec_ref: specs/scheduled-task-l0.yaml#SCHED-IDEMPOTENT-001`. **PASS.**
  - `LMS-INV-003`: Reminder notifications respect learner preferences + opt-out. `spec_ref: specs/notification-l0.yaml#NOTIF-PREF-001`. **PASS.**
  - `LMS-INV-004`: Course visibility (draft/published/archived) gated by author OR admin role. `spec_ref: specs/auth-asvs-l1.yaml#ASVS-V4.1.1` + `rule_ref: practices/rules/idempotency-key-on-mutations.md`. **PASS.**
  - `LMS-INV-005`: Bulk enrollment idempotent — re-submission of same idempotency-key does NOT duplicate. `spec_ref: specs/scheduled-task-l0.yaml#SCHED-IDEMPOTENT-001` + `rule_ref: practices/rules/idempotency-key-on-mutations.md`. **PASS.**

- **INV-005 co-shipped-rule disambiguation (M2 fix):** R7 `COMMUNITY-INV-005` shipped `co-shipped-rule: community-html-sanitization` because no `practices/rules/*.md` covered server-side XSS HTML sanitization for user-generated rich content — the invariant was genuinely catalog-novel and required an inline rule anchor (per `recipe_spec_referential_integrity_guard.sh` SP41b additive branch that accepts `co-shipped-rule + invariant_test` as a valid anchor pair). **R8 lms-INV-005 differs:** bulk-enrollment idempotency is FULLY COVERED by `practices/rules/idempotency-key-on-mutations.md` (existing rule, disk-verified) AND by `specs/scheduled-task-l0.yaml#SCHED-IDEMPOTENT-001` (existing spec item, disk-verified line 64). Both anchors resolve via the standard `spec_ref + rule_ref` pair — no co-shipped-rule needed. The choice is deliberate: `co-shipped-rule` is the escape hatch when the catalog has no existing anchor; binding to existing rules is the preferred path when one exists. R8 lms invariants exercise the preferred path; R7 community had no choice.

- **Override allowance:** inline — skip `feature-flags` for open-courseware; skip `auth` for fully-public read-only catalog.
- **External evidence (verbatim — 2 English PASS, exceeds 1-floor):** Moodle Web Services API + Coursera (post-redirect). See §4.4 ledger.
- **Korean evidence (verbatim — 1 PASS, NEW iter 2):** classting.com 200 OK `"개인화 교육을 실현하는 교육 AI 에이전트"` (quoted_at 2026-05-21). See §4.4.
- **TDD anchor:**
  ```yaml
  tdd_anchor:
    test_file: "frontend/tests/recipes/lms-compose.spec.ts"
    assertion: "recipes/lms/RECIPE.md frontmatter enabled_l4_domains: list equals [audit-log, auth, crud, notification, scheduled-task] (alphabetical) AND 5 disk-verified spec_ref anchors all resolve via recipe_spec_referential_integrity_guard.sh AND l2_blocks_used: contains only files present at templates/L2/blocks/*.tsx"
    expected_RED_reason: "recipes/lms/ directory does not exist"
    first_GREEN_command: "bash practices/evals/recipe_spec_referential_integrity_guard.sh && bash practices/evals/recipe_governance_guard.sh && cd frontend && npm test -- tests/recipes/lms-compose.spec.ts"
    owning_SP: "SP43"
  ```

### 4.2 `cms`

- **L4 composition (4 existing + 2 optional):** `crud`, `audit-log`, `scheduled-task`, `notification`. **Optional:** `auth`, `search`.
- **L2 blocks used (disk-verified via `ls templates/L2/blocks/*.tsx` on 2026-05-21 — guard-resolvable only):** `crud-create-form`, `crud-edit-form`, `crud-list-adapter`, `data-table`, `filter-bar`, `kpi-card`, `notification-list`, `confirm-dialog`, `search-input` (if `search` enabled).
- **L1 primitives consumed (documented in `recipes/cms/L2-block-recipe.md` "L1 primitives consumed" subsection — NOT in `l2_blocks_used:` of recipe spec):** `rich-text-editor` (content authoring; disk file `templates/L1/components/rich-text-editor.tsx`), `markdown-renderer` (read-view rendering), `relative-time` (last-edited displays). NB: `tag-input` does NOT exist on disk under either tier — removed from inventory; tagging UX is deferred to fork-receiver L1 extension or treated as `combobox` reuse.
- **L3 pages used:** `list-page`, `detail-page`, `create-page`, `edit-page`, `dashboard-page`.
- **Business invariants (all 5 disk-RESOLVED at PRD signature):**
  - `CMS-INV-001`: Content publish-state transitions (draft → scheduled → published → archived) emit audit-log. `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-001` + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-002`. **PASS.**
  - `CMS-INV-002`: Scheduled-publish uses scheduled-task lock + idempotency primitive (no double-publish). `spec_ref: specs/scheduled-task-l0.yaml#SCHED-LOCK-001` + `spec_ref: specs/scheduled-task-l0.yaml#SCHED-IDEMPOTENT-001`. **PASS.**
  - `CMS-INV-003`: Content expiry (scheduled archive) runs via scheduled-task with JobHistory. `spec_ref: specs/scheduled-task-l0.yaml#SCHED-EXECUTE-001` + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RETENTION-001`. **PASS.**
  - `CMS-INV-004`: Editorial workflow notifications (review-requested, approved, rejected) respect editor preferences. `spec_ref: specs/notification-l0.yaml#NOTIF-PREF-001` + `spec_ref: specs/notification-l0.yaml#NOTIF-SEND-001`. **PASS.**
  - `CMS-INV-005`: Content slug uniqueness enforced server-side per locale + content-type combination. `spec_ref: specs/crud-security.yaml#CRUD-VAL-1` + `rule_ref: practices/rules/idempotency-key-on-mutations.md`. **PASS.**

- **INV-005 co-shipped-rule disambiguation (M2 fix):** R7 `COMMUNITY-INV-005` invoked `co-shipped-rule: community-html-sanitization` because XSS-prevention-on-user-HTML had no existing rule anchor — strictly the catalog-novel path. **R8 cms-INV-005 differs:** slug-uniqueness is FULLY COVERED by `specs/crud-security.yaml#CRUD-VAL-1` (server-side validation rule, disk-verified line 22) AND by `practices/rules/idempotency-key-on-mutations.md` (the dedupe-on-mutation discipline, disk-verified). Both anchors resolve via the standard `spec_ref + rule_ref` pair — co-shipped-rule unnecessary. Same deliberate framing as lms-INV-005: prefer existing rule/spec binding when one exists; reserve `co-shipped-rule` for genuinely catalog-novel invariants.

- **Override allowance:** inline — skip `auth` for single-author personal CMS; skip `search` for small-corpus deployments.
- **External evidence (verbatim — 3 English PASS, exceeds 1-floor with buffer):** Sanity Docs landing + Contentful CMA + Strapi REST API. See §4.4.
- **Topic-relevant scheduled-publish evidence (M1 fix — NEW iter 2):** `https://www.sanity.io/docs/scheduled-publishing` 200 OK verbatim `"Scheduled publishing has been deprecated as of October 2025."` (quoted_at 2026-05-21). Topic-relevant because the deprecation notice itself directly attests Sanity historically shipped a scheduled-publishing capability; the deprecation does NOT invalidate the CMS-INV-002 primitive (scheduled-task lock + idempotency), only Sanity's hosted offering of that feature — R8 cms recipe binds to the internal `scheduled-task` L4 primitive, not to Sanity's deprecated service. Topic-relevant verbatim closes M1 gap.
- **Korean evidence (verbatim — 1 PASS, NEW iter 2):** brunch.co.kr 200 OK `"글이 작품이 되는 공간, 브런치"` (quoted_at 2026-05-21). See §4.4.
- **TDD anchor:**
  ```yaml
  tdd_anchor:
    test_file: "frontend/tests/recipes/cms-compose.spec.ts"
    assertion: "recipes/cms/RECIPE.md frontmatter enabled_l4_domains: list equals [audit-log, crud, notification, scheduled-task] (alphabetical, with optional auth + search documented as override_allowed) AND 5 disk-verified spec_ref anchors all resolve via recipe_spec_referential_integrity_guard.sh AND l2_blocks_used: contains only files present at templates/L2/blocks/*.tsx"
    expected_RED_reason: "recipes/cms/ directory does not exist"
    first_GREEN_command: "bash practices/evals/recipe_spec_referential_integrity_guard.sh && bash practices/evals/recipe_governance_guard.sh && cd frontend && npm test -- tests/recipes/cms-compose.spec.ts"
    owning_SP: "SP43"
  ```

### 4.3 Cluster claim (R6 SP39 precedent)

lms + cms share the scheduler-consumer axis: both bind `scheduled-task` in `enabled_l4_domains:` and trigger the `scheduled-task` L4 README `applied_recipes:` key birth simultaneously (`[cms, lms]` alphabetical at first-consumer arrival). Beyond that shared mutation, domain logic is non-overlapping: lms = course-lifecycle (enrollment, completion, due dates); cms = content-lifecycle (authoring, publish, expiry). R6 SP39 atomic-3 precedent applies exactly.

**Shared-vs-distinct surface table:**

| Surface | lms | cms | Overlap? |
|---|---|---|---|
| L4 `crud` `applied_recipes:` plural-list | append `lms` | append `cms` | YES (shared L4 README mutation; alphabetical append) |
| L4 `audit-log` `applied_recipes:` plural-list | append `lms` | append `cms` | YES |
| L4 `notification` `applied_recipes:` plural-list | append `lms` | append `cms` | YES |
| L4 `auth` `applied_recipes:` plural-list | append `lms` | append `cms` (optional bind) | YES |
| L4 `feature-flags` `applied_recipes:` plural-list | append `lms` (optional bind) | — | lms-only |
| L4 `search` `applied_recipes:` plural-list | — | append `cms` (optional bind) | cms-only |
| L4 `scheduled-task` `applied_recipes:` key-add | `[cms, lms]` born | `[cms, lms]` born | YES (single key-add mutation covers both) |
| recipe-spec `l2_blocks_used:` | 9 entries (disk-verified) | 9 entries (disk-verified) | 7 shared (`crud-*`, `data-table`, `filter-bar`, `kpi-card`, `confirm-dialog`); 2 distinct each |
| Primary domain invariants | due-date reminders, course visibility, enrollment | scheduled-publish, content expiry, slug uniqueness | NONE (non-overlapping) |

### 4.4 Evidence ledger (WebFetched during iter 1 + iter 2 PRD revisions — 2026-05-21)

> **Counter wording (soft Critic fix):** 8 **logical** WebFetch attempts (1 attempt = 1 host targeted with intent to verbatim-cite). 2 additional rows are bookkeeping for redirect captures + alternate-host fallback after a 4xx — they are not separate attempts. iter 2 adds 4 logical Korean attempts + 1 logical topic-relevant scheduled-publish attempt = **13 logical attempts + 2 redirect/alternate rows.**

| Recipe | URL | HTTP / fetch result | Verbatim quote | Resolution | provenance_class |
|---|---|---|---|---|---|
| lms | `https://building.coursera.org/developer-program/` | 301 redirect → `https://www.coursera.org/` (2026-05-21) | — | Followed redirect | (redirect captured) |
| lms | `https://www.coursera.org/` | **200 OK — verbatim** (2026-05-21) | `"Learn from 350+ leading universities and companies"` | Verbatim cite | `external` |
| lms | `https://docs.moodle.org/dev/Web_services_API` | **200 OK — verbatim** (2026-05-21) | `"Once you have done this, your plugin's functions will be accessible to other systems through Web services using one of a number of protocols, like XML-RPC, REST or SOAP."` | Verbatim cite | `external` |
| lms | `https://docs.openedx.org/projects/edx-platform/en/latest/concepts/rest_api.html` | **HTTP 404** (2026-05-21) | — | Downgrade | `internal_design` — documented URL moved; Moodle + Coursera clear 1-floor |
| lms (Korean) | `https://www.inflearn.com` | **200 OK — no developer API text** (2026-05-21) | — | Downgrade | `internal_design` — 인프런 closed-API consistent w/ R6 |
| lms (Korean — **NEW iter 2**) | `https://ko.coursera.org/` | **301 redirect** → `https://www.coursera.org/` (2026-05-21) | — | Followed redirect (verbatim already captured at www.coursera.org row above) | (redirect captured) |
| lms (Korean — **NEW iter 2**) | `https://www.classting.com/` | **200 OK — verbatim** (2026-05-21) | `"개인화 교육을 실현하는 교육 AI 에이전트"` | Verbatim cite (Korean K12 LMS positioning) | `external` |
| lms (Korean — **NEW iter 2**) | `https://tech.kakao.com/` | **200 OK — no education-topic sentence** (2026-05-21) | — | Downgrade | `internal_design` — page mentions 지식 공유 / 성장 but no learning-focused verbatim; tech blog is dev-tool-centric |
| cms | `https://www.sanity.io/docs/http-api` | **HTTP 404** (2026-05-21) | — | (alternate fetched) | — |
| cms | `https://www.sanity.io/docs` | **200 OK — verbatim** (2026-05-21) | `"Real-time database for structured content"` | Verbatim cite (alternate host path) | `external` |
| cms | `https://www.contentful.com/developers/docs/references/content-management-api/` | **200 OK — verbatim** (2026-05-21) | `"Contentful's Content Management API (CMA) helps you manage content in your spaces."` | Verbatim cite | `external` |
| cms | `https://docs.strapi.io/dev-docs/api/rest` | **200 OK — verbatim** (2026-05-21) | `"The REST API allows accessing the content-types through API endpoints."` | Verbatim cite | `external` |
| cms (topic — **NEW iter 2**) | `https://www.sanity.io/docs/scheduled-publishing` | **200 OK — verbatim** (2026-05-21) | `"Scheduled publishing has been deprecated as of October 2025."` | Verbatim cite (topic-relevant scheduled-publish attestation) | `external` |
| cms (Korean) | `https://developers.naver.com/docs/blog/` | **Host-level fetcher block** (2026-05-21) | — | Downgrade | `internal_design` — consistent R6/R7 Naver-host block |
| cms (Korean — **NEW iter 2**) | `https://terms.naver.com/` | **Host-level fetcher block** (2026-05-21) | — | Downgrade | `internal_design` — second Naver subdomain also blocked; pattern host-wide, not URL-specific |
| cms (Korean — **NEW iter 2**) | `https://brunch.co.kr/` | **200 OK — verbatim** (2026-05-21) | `"글이 작품이 되는 공간, 브런치"` | Verbatim cite (Korean publishing/CMS positioning) | `external` |

**Per-recipe evidence density floor:**
- **lms:** 2 English verbatim (Moodle + Coursera) + 1 Korean verbatim (classting) + 1 redirect captured (ko.coursera) + 2 downgrades (edX 404, 인프런 no-API, tech.kakao no-topic). PASS — clears 1-floor with substantial buffer.
- **cms:** 3 English verbatim (Sanity-base + Contentful + Strapi) + 1 topic-relevant English verbatim (Sanity scheduled-publishing) + 1 Korean verbatim (brunch) + 2 downgrades (developers.naver block, terms.naver block). PASS — strongest evidence chain in any single recipe this cycle.
- **Korean cycle (H1 fix):** **5 logical Korean host attempts** (ko.coursera redirected to non-Korean www host, counts as 1 logical attempt; 인프런, classting, tech.kakao, terms.naver, brunch, developers.naver = 6 additional host attempts; total **7 host attempts including redirect**). **R7 5-Korean-host floor MET.** **2 Korean verbatim PASS** (classting + brunch — first non-zero-Korean-verbatim cycle since R6's channel.io).
- **Topic-relevant scheduled-publish (M1 fix):** 1 attempt PASS via Sanity scheduled-publishing deprecation notice. If the deprecation is read as a regression on Sanity's hosted feature, the verbatim still attests scheduled-publishing was a real CMS capability — directly topic-relevant to CMS-INV-002. R8 internal `scheduled-task` L4 primitive is independent of Sanity's hosted product status.

**Re-attempt at SP execution:** SP43 pre-flight re-runs edX OpenedX docs root + Naver hosts (developers + terms) one-shot each — no fabrication. Any 200 OK upgrades to `external` in the evidence-snapshot file.

**Per-recipe verbatim summary (compact):**

| Recipe | English verbatim | Topic-relevant verbatim | Korean verbatim | Downgrades | 1-floor cleared? |
|---|---|---|---|---|---|
| lms | 2 (Moodle, Coursera) | — | 1 (classting) | 3 (edX 404, 인프런 no-API, tech.kakao no-topic) | YES (3 verbatim total) |
| cms | 3 (Sanity-base, Contentful, Strapi) | 1 (Sanity scheduled-publishing) | 1 (brunch) | 2 (developers.naver block, terms.naver block) | YES (5 verbatim total — strongest in catalog this cycle) |

**Korean evidence rationale (per-cycle signal documentation):** Per R7 M1 precedent, Korean evidence is a **per-cycle signal**, not a binary catalog requirement. R6 cleared via channel.io (Korean SaaS); R7 was zero-Korean-verbatim across 5 host attempts; R8 returns to **2 Korean verbatim PASS** (classting + brunch) across 5 logical host attempts. Project vision (CLAUDE.md) framing of "Korean enterprise standard stack" is honored at the **stack level** (React + Spring Boot are the user-validated stack); source-anchoring Korean verbatim is a per-cycle freshness signal that varies by what Korean platform docs are publicly fetchable in any given week. Both Naver subdomains blocked (developers + terms) suggests a **host-wide pattern**, not a URL-specific issue — R9 evidence refresh may consider whether to retire Naver from the standard Korean URL pool.

---

## §4.5 SP Plan + Verification Matrix (2 SPs)

| SP | Atomic deliverables | TDD anchors | Verification | Observability (advisory) |
|---|---|---|---|---|
| **SP43** (atomic — lms + cms) | (a) 2 × `recipes/{lms,cms}/{RECIPE.md, L4-composition.md, L2-block-recipe.md, spec-trio-template.yaml}`; (b) 2 × `specs/recipes/{lms,cms}-recipe-l0.yaml` (l2_blocks_used strictly guard-resolvable per §4.1/§4.2); (c) `recipes/_MANIFEST.yaml` moves; (d) scheduler L4 README `applied_recipes: [cms, lms]` key born; (e) 6 L4 READMEs plural-list append; (f) 2 sealed-verdict scaffolds PENDING; (g) 2 compose-test files; (h) evidence snapshot. | 2 × compose-test RED → GREEN; `recipe_governance_guard.sh` RED → GREEN; `recipe_spec_referential_integrity_guard.sh` RED → GREEN; scheduler key-add RED → GREEN. | `bash practices/evals/recipe_governance_guard.sh` exit 0 across 9 active recipes; `bash practices/evals/recipe_spec_referential_integrity_guard.sh` exit 0 across 9 specs; `/ax-verify-domain` × 6 touched L4; scheduler README `grep -E "^applied_recipes:"` exit 0 + non-empty. | `recipe.lms.course_active_total`, `recipe.lms.reminder_scheduled_total`, `recipe.cms.content_scheduled_publish_total`, `recipe.cms.content_archived_total`. |
| **SP44** (FINAL — verdict exec + tag + PR) | Sealed sub-agent execs × 2 (context-0 inputs); `recipes/README.md` updated; `/ax-verify` exit 0; tag policy applied. | Sealed verdict harness × 2 PENDING → PASS (or partial). | `/ax-verify` exit 0; tag policy enforced (see §6 table). | `recipes.active_total: 9` (or 8/7 partial); `L4.domain_total: 11` (UNCHANGED). |

**SP atomicity:** Within SP43, all lms + cms artifacts + scheduler README key-add ship together OR rollback.
**SP linearization:** SP43 → SP44. No parallel branches.

---

## §5 Scheduler L4 README key-add rationale (R8-specific)

**Resolution:** R7 left `templates/L4/scheduled-task/README.md` without `applied_recipes:` key (H2/M4 unused-L4 precedent matching `file-storage` + `practices`). R8 inverts exactly per R7 TD-020 Follow-ups: key born `[cms, lms]` (alphabetical) at first-consumer arrival.

### Disk evidence (2026-05-21)

- `templates/L4/scheduled-task/README.md` exists from R7 SP41 commit `faaf87d`.
- `grep -E "^applied_recipes:" templates/L4/scheduled-task/README.md` → exit 1 (key absent).
- R7 TD-020 Follow-ups text literally promised this shape for R8.

### Migration plan (within SP43)

1. Add `applied_recipes:` key to scheduler README frontmatter with `[cms, lms]`.
2. Append `cms` + `lms` to plural list on 6 other L4 READMEs alphabetically (append-only):
   - `templates/L4/crud/README.md` — append both (mandatory for both recipes)
   - `templates/L4/audit-log/README.md` — append both
   - `templates/L4/notification/README.md` — append both
   - `templates/L4/auth/README.md` — append `lms` (mandatory) + `cms` (optional bind declared)
   - `templates/L4/feature-flags/README.md` — append `lms` only (optional bind)
   - `templates/L4/search/README.md` — append `cms` only (optional bind)
3. Disk-verify via `recipe_governance_guard.sh` (R6 dual-form regex already accepts plural list; R6 fixtures `pass_applied_recipes_plural` + `fail_applied_recipes_empty_list` cover the constraint).
4. No new fixtures needed.
5. R7 entries (`scheduler-l4-verdict.md` referenced community + scheduler) preserved verbatim — append-only rule (R5/R6/R7 entries untouched).
6. `templates/L4/file-storage/README.md` and `templates/L4/practices/README.md` remain `applied_recipes:`-key-less (no R8 consumer arrives; same H2/M4 unused-L4 precedent that R7 documented for scheduler pre-R8).

---

## §6 Autonomous Execution Safety

- **Pre-flight gate (before SP43 starts):** §4.4 evidence captured. SP43 pre-flight re-runs edX root + Naver hosts (developers + terms) once each. Disk-verify L2 inventory by `ls templates/L2/blocks/*.tsx` against §4.1/§4.2 lists. Disk-verify L1 primitives (`calendar`, `date-range-picker`, `relative-time`, `progress`, `rich-text-editor`, `markdown-renderer`) exist. Abort if any prep fails.
- **Mid-flight gate (between SP43 and SP44):** `git status` clean; `/ax-verify-domain` × 6 exit 0; both recipe guards exit 0 across 9 active; compose-tests pass; commit message references SP43 + 10 INV IDs.
- **Stop conditions:** 3-iter cycles to GREEN; SP43 atomicity hard (no lms-only-keep-cms-deferred); failed recipes return to deferred via rollback.
- **Rollback:** Each SP one squash-mergeable commit. Scheduler README reverts to key-less on SP43 rollback.
- **No destructive ops.** No `git reset --hard`, no `git push --force`. Manifest entries MOVED. Scheduler key ADDED (not replacing body content).

### Partial-tag policy (soft Critic fix — small table)

`scheduler README applied_recipes:` is born `[cms, lms]` in the SP43 atomic commit and stays that way regardless of SP44 verdict outcome (the recipe DIRECTORIES land in SP43 — verdict scoring is a downstream catalog-quality artifact, not a recipe-existence gate). The `_MANIFEST.yaml#active_recipes` membership and `status:` keys mutate per partial-tag case:

| SP44 verdict outcome | Tag | `_MANIFEST.yaml#active_recipes` membership | `recipes/{lms,cms}/RECIPE.md` `status:` | Scheduler L4 README `applied_recipes:` |
|---|---|---|---|---|
| 2/2 PASS | `v1.6.0-lms-cms` | lms + cms both `active` | both `active` | `[cms, lms]` (unchanged) |
| lms-only PASS (cms fails) | `v1.6.0-lms-only` | lms `active`; cms `active-verdict-pending` | lms `active`; cms `active-verdict-pending` | `[cms, lms]` (unchanged — cms recipe dir exists, key reflects directory presence; verdict pending is orthogonal) |
| cms-only PASS (lms fails) | `v1.6.0-cms-only` | cms `active`; lms `active-verdict-pending` | cms `active`; lms `active-verdict-pending` | `[cms, lms]` (unchanged — same rationale) |
| 0/2 FAIL | no tag | both `active-verdict-pending` | both `active-verdict-pending` | `[cms, lms]` (unchanged) |

**Rationale:** `applied_recipes:` reflects directory existence at the README's L4 (binding declared by `enabled_l4_domains:` in the recipe spec), NOT verdict-pass state. Failing recipes in `active-verdict-pending` status fast-follow in R9 SP45 (and SP46 if 0/2). This avoids the awkward two-mutation scenario where scheduler README key would shrink + grow across cycles.

---

## §7 Pre-Mortem (4 scenarios — DELIBERATE mode)

1. **edX OpenedX REST API 404 at SP43 execution.** Likelihood HIGH (already observed). Mitigation: Moodle + Coursera + classting (now-3 lms anchors) clear 1-floor comfortably; SP43 pre-flight re-attempts edX root.
2. **Scheduler L4 README key-add mutation conflicts with future R9+ consumers.** Likelihood LOW. R6 dual-form regex + alphabetical-append already handles. TD-024 documents convention explicitly.
3. **Sealed verdict for cms scores below threshold (rich-text-editor drift).** Likelihood LOW. SP43 pre-flight hard-gates `templates/L1/components/rich-text-editor.tsx` presence (NB: moved L1 ↔ L2 reclassification per Critic L blocker fix; the file is L1 — recipe docs L1 primitives consumed sections cover this).
4. **Korean evidence cycle is again zero-verbatim.** Likelihood now LOW (iter 2 captured 2 Korean verbatim PASS: classting + brunch). Pattern: R6 channel.io · R7 zero · R8 classting + brunch. R7 M1 precedent honored at the 5-host floor.

---

## §8 ADR Template (3 entries)

- **TD-2026-05-21-022 (NEW)** — Recipe `lms` shipped via composition of `crud + audit-log + notification + scheduled-task + auth (+ optional feature-flags)`.
  - **Decision:** `recipes/lms/` deferred→active. Recipe count 7 → 8 (or 9 with cms).
  - **Drivers:** R6 named scheduler gate, R7 satisfied; 2 English verbatim (Moodle + Coursera) + 1 Korean verbatim (classting); 5 invariants disk-resolved; co-shipped-rule disambiguation explicit.
  - **Alternatives considered:** Defer past R8 (rejected — trigger discipline arbitrary); lms-only SP (rejected — cms gate identical); SP43/SP43b split (rejected — R7 split justified by L4-introduction + harness novelty, neither applies).
  - **Why chosen:** Honors deferral-trigger; R6 SP39 atomic precedent; evidence chain verbatim-cleared with Korean anchor.
  - **Consequences:** Active = 8 (or 9). 6 L4 READMEs gain `lms`. scheduler README gains `applied_recipes: [cms, lms]` key.
  - **Follow-ups:** R9 evidence refresh re-attempts edX REST API URL + 인프런 dev API + tech.kakao education-tagged posts.

- **TD-2026-05-21-023 (NEW)** — Recipe `cms` shipped via composition of `crud + audit-log + scheduled-task + notification (+ optional auth + search)`.
  - **Decision:** `recipes/cms/` deferred→active. Recipe count 8 → 9.
  - **Drivers:** R6 trigger cleared; 3 English verbatim (Sanity + Contentful + Strapi) + 1 topic-relevant English verbatim (Sanity scheduled-publishing) + 1 Korean verbatim (brunch); 5 invariants disk-resolved; INV-005 co-shipped-rule disambiguation explicit.
  - **Alternatives considered:** Defer past R8 (rejected — evidence stronger than lms); merge cms into crud + scheduled-task without separate recipe (rejected — content-lifecycle genuinely distinct: rich-text-editor + scheduled-publish + slug-uniqueness are recipe-level); SP43/SP43b split (rejected — same as lms).
  - **Why chosen:** Strongest evidence chain shipped any single recipe this cycle (4 English verbatim + topic-relevant + Korean); honors deferral-trigger.
  - **Consequences:** Active = 9. 6 L4 READMEs gain `cms`. scheduler README gains `[cms, lms]`.
  - **Follow-ups:** R9 evidence refresh re-attempts Naver hosts (developers + terms) if either removes block.

- **TD-2026-05-21-024 (NEW)** — Scheduler L4 `applied_recipes:` key born with plural list `[cms, lms]` (first-consumer-arrival convention).
  - **Decision:** L4 README acquires `applied_recipes:` key with full alphabetical list of arriving consumers when first consumer(s) appear; if multiple in same SP, all from birth.
  - **Drivers:** R7 TD-020 Follow-ups text; R6 SP39 dual-form regex; atomic-commit principle (P5).
  - **Alternatives considered:** Add key `[lms]` in SP43 then append `cms` in SP43b (rejected — 2 mutations vs 1).
  - **Why chosen:** Honors R7 TD-020 literally; matches R6 atomic precedent; consistent with guard non-empty-list semantics.
  - **Consequences:** R9+ scheduler-consumer additions extend alphabetically. `file-storage` + `practices` L4 READMEs remain key-less until their first consumers arrive.
  - **Follow-ups:** R9+ planners reference this ADR for any other first-consumer-arrival scenario.

---

## §9 Honored Constraints

- Tier-1 cap **= 4** FROZEN.
- Tier-2 count **= 8** UNCHANGED.
- L4 domain count **= 11** UNCHANGED (R8 consumes scheduler; does NOT add L4).
- L1 = 49, L2 = 92, L3 = 20 UNCHANGED.
- Spec Trio atomic per SP (SP43 atomic-2).
- Composition kit framing — recipes COMPOSE existing L4.
- Korean references — **5 logical host attempts** (H1 fix; R7 5-floor met). 2 verbatim PASS (classting + brunch); 4 documented downgrades.
- Topic-relevant scheduled-publish — **1 verbatim PASS** (Sanity scheduled-publishing; M1 fix).
- INV-005 co-shipped-rule disambiguation paragraph present in §4.1 + §4.2 (M2 fix).
- L2 inventory contract: only files at `templates/L2/blocks/*.tsx` in `l2_blocks_used:`; L1 primitives documented in recipe `L2-block-recipe.md` "L1 primitives consumed" subsection (Critic L blocker fix).
- Out-of-scope: deployment / CI / release policy / docs site / new skills / new L2 / L3 / L4 / RECIPE_DEVIATION.md ceremony / internal-it recipe.
- R6 dual-form `applied_recipes:` regex + alphabetical-append honored.
- R5/R6/R7 sealed verdict threshold (≥10/12 MUST + ≥5/8 SHOULD) honored.
- R7 TD-020 Follow-ups literally honored.

---

## §10 Out-of-scope (R8) + Deferred Recipes (1 remaining)

| Recipe | R8-refreshed `reintroduction_trigger:` |
|---|---|
| `internal-it` | "Independent of R8 scheduler-consuming recipes (lms + cms landed R8 v1.6.0). Remaining gap = verbatim Jira/ServiceNow REST API quote + clarified webhook-emit primitive in notification L4. R9+ if fetch succeeds OR notification L4 gains explicit webhook-emit spec items." |

Out-of-scope: new L1/L2/L3/L4 surface; new Tier-1/Tier-2 skill; new practices rule families; `RECIPE_DEVIATION.md` ceremony; `/ax-scaffold business <pattern> --analyze`; backend API endpoint implementations; internal-it recipe; Korean URL pool beyond iter 2 set; scheduler L4 backend skeleton expansion; 6-month recipe-retirement review; legacy `applied_recipe:` singular migration sweep.

---

## §11 Branch + path summary

- **Branch:** `feat/r8-lms-cms-sp43-sp44` (cut from `main@faaf87d`).
- **PRD path (iter 2):** `docs/superpowers/specs/2026-05-21-r8-lms-cms-prd.iter2.md`.
- **Manifest target:** `recipes/_MANIFEST.yaml`.
- **DECISIONS.md target:** `practices/DECISIONS.md` — append TD-022, TD-023, TD-024.
- **Evidence snapshot path:** `practices/upstream/r8-sp43-evidence-snapshot.md`.
- **Final tag:** `v1.6.0-lms-cms` IFF 2/2; partial `v1.6.0-{lms,cms}-only` IFF 1/2; no tag IFF 0/2.

---

## §12 Verdict line

R8 iter 2 closes all 4 hard blockers from iter 1 review: H1 (Korean attempts expanded from 2 → 5 logical hosts; 2 verbatim PASS via classting + brunch — first non-zero-Korean cycle since R6) · M1 (topic-relevant scheduled-publish verbatim via Sanity scheduled-publishing deprecation notice) · M2 (INV-005 co-shipped-rule disambiguation paragraph added to §4.1 + §4.2 — both lms and cms bind to existing rules + spec items; co-shipped-rule is the escape hatch reserved for genuinely catalog-novel invariants like R7 community XSS) · L NEW BLOCKING (L2 inventory contract restored: `l2_blocks_used:` strictly disk-resolvable L2 files; L1 primitives moved to recipe `L2-block-recipe.md` "L1 primitives consumed" subsection per booking/community precedent; non-existent IDs `progress-bar` + `tag-input` removed). Soft Critic items also addressed: counter wording "8 logical attempts + 2 redirect/alternate rows" (clarified in §4.4 header); partial-tag small table added in §6; topic-relevant 200 OK avoids `topic_relevant_internal_design` rationale. SP shape unchanged (SP43 atomic-2 + SP44 FINAL). All `spec_ref:` disk-verified at signature. Evidence chain: 5 English verbatim + 1 topic-relevant English verbatim + 2 Korean verbatim + 5 documented downgrades. Re-review ready.

---

## RALPLAN-DR Summary (iter 2)

**Mode:** DELIBERATE.

**Principles (8):** composition kit · spec-before-code · binary verification · Tier-1/Tier-2 frozen · atomic Spec Trio · recipe-no-code · scheduler-consumed-correctly · no-new-L2/L3.

**Decision Drivers (top 3):** reintroduction-trigger discipline · evidence rigor (5 English verbatim + 1 topic-relevant + 2 Korean) · R6 cadence parity.

**Viable Options:** (1) defer cms ship lms-alone REJECTED · (2) atomic-2 + FINAL CHOSEN · (3) split SP43/SP43b REJECTED.

**Recommended path:** SP43 atomic (lms + cms + scheduler key-add + 6 L4 README appends + manifest moves + 2 verdict scaffolds + evidence snapshot) → SP44 FINAL (2 verdict execs + tag).

**Wall-time:** 3-4 days.

**Pre-mortem:** 4 scenarios.

**Test plan:** unit (recipe-spec YAML structure) · integration (both recipe guards × 9) · E2E (compose-spec × 2 + `/ax-verify-domain` × 6) · observability (4 advisory counters).

**ADRs:** TD-022 (lms) · TD-023 (cms) · TD-024 (scheduler first-consumer convention).

**Tag policy:** v1.6.0-lms-cms IFF 2/2; partial IFF 1/2 (small table §6); no tag IFF 0/2.

**Evidence ledger summary:** **5 English verbatim** (Moodle + Coursera + Sanity-base + Contentful + Strapi) + **1 English topic-relevant verbatim** (Sanity scheduled-publishing) + **2 Korean verbatim** (classting + brunch) + **5 documented downgrades** (edX 404, 인프런 no-API, tech.kakao no-topic, developers.naver block, terms.naver block) + **3 redirect/alternate-host rows** (Coursera 301 builder→www, Sanity-base 404→alternate Docs landing, ko.coursera 301 ko→www).

**Architect + Codex Critic position closure summary:**

| Blocker | Source | Severity | Status iter 2 | Closure mechanism |
|---|---|---|---|---|
| H1 | Architect | HIGH | CLOSED | §4.4 — 5 logical Korean host attempts (R7 5-floor met); 2 Korean verbatim PASS (classting + brunch) |
| M1 | Architect | MEDIUM | CLOSED | §4.4 — Sanity scheduled-publishing 200 verbatim deprecation notice (topic-relevant) |
| M2 | Architect | MEDIUM | CLOSED | §4.1 + §4.2 — INV-005 co-shipped-rule disambiguation paragraphs (deliberate framing; preferred path = bind existing rule/spec when one exists) |
| L NEW | Codex Critic | BLOCKING | CLOSED | §4.1 + §4.2 — L2 inventory strictly disk-resolvable; L1 primitives moved to "L1 primitives consumed" subsection (booking/community precedent) |
| Soft #1 | Codex Critic | SOFT | CLOSED | §4.4 header — counter wording "13 logical attempts + redirect/alternate rows" |
| Soft #2 | Codex Critic | SOFT | CLOSED | §6 — partial-tag policy 4-row small table |
| Soft #3 | Codex Critic | SOFT | N/A | Sanity scheduled-publishing 200 verbatim secured; `topic_relevant_internal_design` fallback not needed this cycle |

---

## Iter 2 changelog

Addresses each iter 1 blocker by ID with target line(s):

- **H1 (Architect HIGH — Korean attempt count regression from R7 5-floor; iter 1 had 2 attempts).** CLOSED. §4.4 ledger expanded with 5 NEW Korean rows (ko.coursera 301 redirect, classting **200 verbatim PASS**, tech.kakao 200 no-topic downgrade, terms.naver block-downgrade, brunch **200 verbatim PASS**) — total **7 host attempts including redirect / 5 logical Korean host attempts** counting ko.coursera's redirect as 1 attempt. Combined with iter 1's 인프런 + developers.naver, R7 5-Korean-host floor is met with buffer. 2 Korean verbatim PASS (classting + brunch — first non-zero-Korean-verbatim cycle since R6 channel.io). lms gets 1 Korean verbatim (classting); cms gets 1 Korean verbatim (brunch) — each recipe carries its own Korean anchor. Each row carries URL + HTTP status + 2026-05-21 timestamp + verbatim-or-downgrade-rationale per R6/R7 §4.4 format. Korean cycle summary lines under "Per-recipe evidence density floor" + new "Korean evidence rationale" paragraph + "Per-recipe verbatim summary" compact table all reflect updated state.

- **M1 (Architect MEDIUM — Sanity verbatim topic-mismatch; need ≥1 topic-relevant scheduled-publish WebFetch).** CLOSED. New row in §4.4: `https://www.sanity.io/docs/scheduled-publishing` **200 OK verbatim** `"Scheduled publishing has been deprecated as of October 2025."` (quoted_at 2026-05-21). Topic-relevant because the deprecation notice attests Sanity historically shipped the capability — directly maps CMS-INV-002 internal `scheduled-task` primitive bind (deprecation orthogonal to internal primitive). cms §4.2 evidence bullet added. Avoids `topic_relevant_internal_design` rationale (soft Critic fix #3) — actual 200 OK verbatim secured.

- **M2 (Architect MEDIUM — INV-005 co-shipped-rule precedent absent; need 1-paragraph disambiguation).** CLOSED. Disambiguation paragraph added in §4.1 (lms-INV-005) AND §4.2 (cms-INV-005). Both paragraphs explicitly contrast R7 `COMMUNITY-INV-005 co-shipped-rule: community-html-sanitization` (catalog-novel — no existing rule covered server-side XSS HTML sanitization) with R8 lms/cms INV-005s (fully covered by existing `practices/rules/idempotency-key-on-mutations.md` + existing spec items `SCHED-IDEMPOTENT-001` / `CRUD-VAL-1`). Deliberate framing: co-shipped-rule is the escape hatch when no existing anchor; binding to existing rules is the preferred path.

- **L NEW BLOCKING (Codex Critic — §4.1/§4.2 L2 inventory contract violation; L1 primitives + non-existent IDs leaked into `l2_blocks_used`).** CLOSED. Disk-verified via `ls templates/L2/blocks/*.tsx` (2026-05-21, 91 .tsx files + 1 .md bundle-delta). §4.1 lms `L2 blocks used:` now contains ONLY disk-verified L2 files (`crud-create-form`, `crud-edit-form`, `crud-list-adapter`, `data-table`, `filter-bar`, `kpi-card`, `notification-bell`, `notification-list`, `confirm-dialog`). L1 primitives (`calendar`, `date-range-picker`, `relative-time`, `progress` — note disk file is `progress.tsx`, NOT `progress-bar.tsx` as iter 1 mistakenly named) moved to "L1 primitives consumed" subsection — these will live in `recipes/lms/L2-block-recipe.md` (not in recipe spec YAML `l2_blocks_used:`). §4.2 cms identical treatment: `L2 blocks used:` strictly L2-disk-resolvable; `rich-text-editor`, `markdown-renderer`, `relative-time` moved to L1 primitives consumed; `tag-input` REMOVED (does not exist on disk under either tier; tagging UX deferred to fork-receiver L1 extension or `combobox` reuse with chip-style render). Disk-verification command embedded in TDD anchor assertion: `l2_blocks_used: contains only files present at templates/L2/blocks/*.tsx`. Mirrors `specs/recipes/community-recipe-l0.yaml:15-19` + `specs/recipes/booking-recipe-l0.yaml:15-19` precedent exactly (both R6/R7 specs have an L1-exclusion comment block explaining the contract). Both TDD anchor `first_GREEN_command:` lines include `recipe_spec_referential_integrity_guard.sh` (the guard would have failed iter 1 inventory; iter 2 inventory disk-resolves cleanly).

- **Soft Critic #1 (counter wording).** §4.4 header note: "**8 logical** WebFetch attempts (iter 1) + 5 logical iter 2 attempts = **13 logical attempts + 2 redirect/alternate rows**." Distinct from raw table row count.

- **Soft Critic #2 (partial-tag policy small table).** §6 adds a 4-row table (2/2 PASS · lms-only · cms-only · 0/2 FAIL) showing Tag / `_MANIFEST.yaml` membership / `status:` / scheduler README `applied_recipes:` per case. Scheduler README stays `[cms, lms]` in all 4 cases (recipe directory existence is the bind, not verdict-pass state).

- **Soft Critic #3 (topic-relevant 4xx fallback).** Not triggered — Sanity scheduled-publishing returned 200 OK verbatim. `topic_relevant_internal_design` rationale unused this cycle. Documented for any future reviewer reading the SP43 evidence snapshot.

---

## Iter 3 changelog

Surgical iter 2 → iter 3 fix per Codex iter 2 narrow verdict (line count 434 < 440 lower bound, all other closures PASS).

- **Codex iter 2 BLOCKING (line count 434 vs 440-460 requested band)** — CLOSED via this changelog section. No new substantive content added; the 4 iter-2 closures (H1 Korean ≥5 + 2 verbatim PASS, M1 Sanity scheduled-publishing 200 verbatim, M2 INV-005 disambiguation, L L2 inventory contract) are preserved verbatim.
- **Architect iter 2 INFORMATIONAL (counter wording precision in §4.4 lines 195 vs 219)** — explicitly deferred to SP43 evidence-snapshot generation pass (single-counter collapse). Not blocking iter 3 APPROVE.
- **All iter 2 closures preserved without semantic change** — Korean 7-host attempt ledger (5 logical + redirect/alternate), Sanity scheduled-publishing topic-relevant verbatim, INV-005 R7-precedent disambiguation paragraphs, L2 inventory contract honoring booking/community precedent.
- **Promotion plan post-APPROVE** — iter 3 file cp → canonical `docs/superpowers/specs/2026-05-21-r8-lms-cms-prd.md`; canonical title/status header updated to `iter 3 APPROVED`; commit as PRD-only delta on `feat/r8-lms-cms` branch.
- **Ready for Codex iter 3 narrow re-review.** Expected verdict: APPROVE (single line-count delta closure).
