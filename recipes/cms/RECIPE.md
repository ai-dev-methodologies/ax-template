---
pattern: cms
display_name: "CMS (Content Authoring + Scheduled Publish + Editorial Workflow + Expiry)"
schema_version: 1
compatible_with_catalog_version: "v1.6.0-lms-cms"
last_verified_at: "2026-05-21"
enabled_l4_domains:
  - audit-log
  - crud
  - notification
  - scheduled-task
l2_blocks_used:
  - confirm-dialog
  - crud-create-form
  - crud-edit-form
  - crud-list-adapter
  - data-table
  - filter-bar
  - kpi-card
  - notification-list
  - search-input
l3_pages_used:
  - create-page
  - dashboard-page
  - detail-page
  - edit-page
  - list-page
override_allowed:
  # Inline override block — no separate RECIPE_DEVIATION.md file.
  #
  # enabled_l4_domains:
  #   skip: ["auth"]
  #   rationale: "Single-author personal CMS; no multi-role gating needed."
  #   citation: "<internal ticket / PR url>"
  #
  # enabled_l4_domains:
  #   skip: ["search"]
  #   rationale: "Small-corpus CMS deployment; list-page filtering sufficient."
  #   citation: "<internal ticket / PR url>"
---

## Backend Implementation Status

> See [`docs/IMPLEMENTATION-STATUS.md`](../../docs/IMPLEMENTATION-STATUS.md) for the full 12-L4 status taxonomy and fork-receiver expectation alignment (R15+ mandatory section).

| L4 domain | Status | Effort if not impl |
|---|---|---|
| `audit-log` | **spec-only** 📋 | ~5-10 eng-days (implement backend) |
| `crud` | **impl** ✅ | — (ready) |
| `notification` | **spec-only** 📋 | ~5-10 eng-days (implement backend) |
| `scheduled-task` | **skeleton** ⚠️ | ~3-7 eng-days (flesh out .skeleton) |

**Summary**: 1 impl ready · 2 spec-only (implement) · 1 skeleton (flesh out) · est. ~17-24 engineering days for the gap.

**Reading guide**: `impl` = backend Java reference workload ready in `backend/src/main/java/com/ax/template/authblueprint/<domain>/`. `spec-only` = Spec Trio + Next.js stub only; backend NOT included. `skeleton` = `.skeleton` file present; flesh out controller/service yourself. Sealed verdict PASS validates catalog self-discoverability, NOT runnable backend code.


# Recipe: cms

**Business context:** Content lifecycle — authoring with rich text + markdown,
draft/scheduled/published/archived state machine, scheduled-publish via
distributed-lock cron, scheduled archive (content expiry), editorial workflow
notifications (review-requested / approved / rejected), and locale-aware slug
uniqueness. Targets headless-CMS / publication-CMS / knowledge-base surfaces
(Sanity-style, Contentful-style, Strapi-style, brunch-style Korean
publication).

## Enabled L4 Domains

| L4 Domain | Role in this recipe |
|---|---|
| `crud` | Content + Slug CRUD; draft/scheduled/published/archived state machine |
| `audit-log` | Publish-state transitions emit audit row with operator + before/after |
| `scheduled-task` | Scheduled-publish + scheduled-archive cron with distributed lock + idempotency |
| `notification` | Editorial workflow notifications (review-requested / approved / rejected) |

**Optional:** `auth` (single-author personal CMS skip), `search` (small-corpus skip).

## Business Invariants

| ID | Statement | Binding |
|---|---|---|
| CMS-INV-001 | Content publish-state transitions (draft → scheduled → published → archived) emit audit-log | `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-001` + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-002` |
| CMS-INV-002 | Scheduled-publish uses scheduled-task lock + idempotency primitive (no double-publish) | `spec_ref: specs/scheduled-task-l0.yaml#SCHED-LOCK-001` + `spec_ref: specs/scheduled-task-l0.yaml#SCHED-IDEMPOTENT-001` |
| CMS-INV-003 | Content expiry (scheduled archive) runs via scheduled-task with JobHistory | `spec_ref: specs/scheduled-task-l0.yaml#SCHED-EXECUTE-001` + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RETENTION-001` |
| CMS-INV-004 | Editorial workflow notifications (review-requested / approved / rejected) respect editor preferences | `spec_ref: specs/notification-l0.yaml#NOTIF-PREF-001` + `spec_ref: specs/notification-l0.yaml#NOTIF-SEND-001` |
| CMS-INV-005 | Content slug uniqueness enforced server-side per locale + content-type combination | `spec_ref: specs/crud-security.yaml#CRUD-VAL-1` + `rule_ref: practices/rules/idempotency-key-on-mutations.md` |

### INV-005 disambiguation (deliberate framing)

R7 `COMMUNITY-INV-005` invoked `co-shipped-rule: community-html-sanitization`
because XSS-prevention-on-user-HTML had no existing rule anchor — strictly the
catalog-novel path.

R8 **CMS-INV-005 differs:** slug-uniqueness is FULLY COVERED by
`specs/crud-security.yaml#CRUD-VAL-1` (server-side validation rule,
disk-verified) AND by `practices/rules/idempotency-key-on-mutations.md`
(the dedupe-on-mutation discipline, disk-verified). Both anchors resolve via
the standard `spec_ref + rule_ref` pair — co-shipped-rule is NOT used. Same
deliberate framing as LMS-INV-005: prefer existing rule/spec binding when one
exists; reserve `co-shipped-rule` for genuinely catalog-novel invariants.

## Business Observability (advisory — no emitter test enforced this cycle)

| Signal | Type | Notes |
|---|---|---|
| `recipe.cms.content_scheduled_publish_total` | Counter | Scheduled-publish jobs enqueued |
| `recipe.cms.content_archived_total` | Counter | Scheduled-archive jobs executed |
| `recipe.cms.editorial_notification_total{kind}` | Counter | Labeled by review-requested / approved / rejected |

## Evidence

```yaml
evidence:
  - provenance_class: external
    source: "Sanity Docs landing (post-alternate from sanity.io/docs/http-api 404)"
    url: "https://www.sanity.io/docs"
    citation: "Real-time database for structured content"
    quoted_at: "2026-05-21"
    fidelity_note: "Sanity is a canonical headless CMS; the docs landing tagline attests to the structured-content database pattern the CMS recipe encodes."
  - provenance_class: external
    source: "Contentful Content Management API docs"
    url: "https://www.contentful.com/developers/docs/references/content-management-api/"
    citation: "Contentful's Content Management API (CMA) helps you manage content in your spaces."
    quoted_at: "2026-05-21"
    fidelity_note: "Contentful CMA documents the canonical headless-CMS management REST surface the cms recipe CRUD layer mirrors."
  - provenance_class: external
    source: "Strapi REST API docs"
    url: "https://docs.strapi.io/dev-docs/api/rest"
    citation: "The REST API allows accessing the content-types through API endpoints."
    quoted_at: "2026-05-21"
    fidelity_note: "Strapi is the canonical open-source headless CMS; the REST API docs confirm the content-type endpoint pattern."
  - provenance_class: external
    source: "Sanity scheduled-publishing deprecation notice"
    url: "https://www.sanity.io/docs/scheduled-publishing"
    citation: "Scheduled publishing has been deprecated as of October 2025."
    quoted_at: "2026-05-21"
    fidelity_note: "Topic-relevant verbatim — the deprecation notice itself attests Sanity historically shipped scheduled-publishing. CMS-INV-002 binds to the internal scheduled-task L4 primitive, NOT to Sanity's hosted offering; deprecation is orthogonal."
  - provenance_class: external
    source: "Brunch (Korean publication CMS)"
    url: "https://brunch.co.kr/"
    citation: "글이 작품이 되는 공간, 브런치"
    quoted_at: "2026-05-21"
    fidelity_note: "Korean publishing/CMS verbatim (PRD §4.4 H1 closure — first non-zero-Korean-verbatim CMS cycle). Brunch positions itself as a publication platform; the verbatim attests the CMS pattern in Korean enterprise practice."
  - provenance_class: internal_design
    source: "Naver developers blog API docs"
    url: "https://developers.naver.com/docs/blog/"
    rationale: "Host-level fetcher block on 2026-05-21; consistent with R6/R7 Naver-host block pattern."
  - provenance_class: internal_design
    source: "Naver terms (alternate subdomain attempt)"
    url: "https://terms.naver.com/"
    rationale: "Host-level fetcher block on 2026-05-21; pattern is host-wide, not URL-specific. R9 evidence refresh may consider retiring Naver from the standard Korean URL pool."
  - provenance_class: internal_design
    derives_from:
      - "SP15 crud"
      - "SP17 audit-log"
      - "SP26 notification"
      - "SP41 scheduled-task"
    rationale: "CMS composition derives from existing crud, audit-log, notification, and scheduled-task Spec Trios. No new L4 introduced."
```

## Scaffold Usage

```bash
/ax-scaffold business cms my-cms-app
```

This will scaffold all 4 enabled L4 domains into `my-cms-app/` and run
`/ax-verify-domain` for each.
