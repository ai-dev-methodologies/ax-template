---
pattern: lms
display_name: "LMS (Course + Enrollment + Lesson + Due-date Reminders)"
tenant_model: single  # iter-2: explicit declaration per specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001. Recipe ships single-tenant; fork-receivers adopting multi-tenant MUST switch to `tenant_model: multi` AND adopt ISOLATION-001/002/003 + PROPAGATION-001/002.
schema_version: 1
compatible_with_catalog_version: "v1.6.0-lms-cms"
last_verified_at: "2026-05-21"
enabled_l4_domains:
  - audit-log
  - auth
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
  - notification-bell
  - notification-list
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
  #   skip: ["feature-flags"]
  #   rationale: "Open-courseware fork — no toggle gating needed."
  #   citation: "<internal ticket / PR url>"
  #
  # enabled_l4_domains:
  #   skip: ["auth"]
  #   rationale: "Fully-public read-only LMS catalog mirror."
  #   citation: "<internal ticket / PR url>"
---

## Backend Implementation Status

> See [`docs/IMPLEMENTATION-STATUS.md`](../../docs/IMPLEMENTATION-STATUS.md) for the full 12-L4 status taxonomy and fork-receiver expectation alignment (R15+ mandatory section).

| L4 domain | Status | Effort if not impl |
|---|---|---|
| `audit-log` | **spec-only** 📋 | ~5-10 eng-days (implement backend) |
| `auth` | **impl** ✅ | — (ready) |
| `crud` | **impl** ✅ | — (ready) |
| `notification` | **spec-only** 📋 | ~5-10 eng-days (implement backend) |
| `scheduled-task` | **impl** ✅ | — (R49: backend GREEN + admin Next.js surface — task list w/ enable·disable·trigger + per-task history) |

**Summary**: 2 impl ready · 2 spec-only (implement) · 1 skeleton (flesh out) · est. ~17-24 engineering days for the gap.

**Reading guide**: `impl` = backend Java reference workload ready in `backend/src/main/java/com/ax/template/authblueprint/<domain>/`. `spec-only` = Spec Trio + Next.js stub only; backend NOT included. `skeleton` = `.skeleton` file present; flesh out controller/service yourself. Sealed verdict PASS validates catalog self-discoverability, NOT runnable backend code.


# Recipe: lms

**Business context:** Course lifecycle — course catalog, enrollment management,
lesson progression, due-date reminders fanout, instructor + admin role
separation, and learner-completion tracking. Targets K-12 / higher-ed /
corporate-training surfaces (Moodle-style, Coursera-style, classting-style
mobile-first K-12).

## Enabled L4 Domains

| L4 Domain | Role in this recipe |
|---|---|
| `crud` | Course + Lesson + Enrollment CRUD; draft/published/archived states |
| `auth` | Course visibility gated by author OR admin role (ASVS-V4.1.1) |
| `audit-log` | Course content mutations + visibility transitions (operator + before/after) |
| `notification` | Due-date reminder fanout respecting learner preferences |
| `scheduled-task` | Distributed-lock cron for reminder emission + idempotent bulk-enrollment |

**Optional:** `feature-flags` (skip for open-courseware).

## Business Invariants

| ID | Statement | Binding |
|---|---|---|
| LMS-INV-001 | Course content mutations emit audit-log row with operator + before/after | `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-001` + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-002` |
| LMS-INV-002 | Due-date reminder emission uses scheduled-task lock primitive (no double-send on multi-node) | `spec_ref: specs/scheduled-task-l0.yaml#SCHED-LOCK-001` + `spec_ref: specs/scheduled-task-l0.yaml#SCHED-IDEMPOTENT-001` |
| LMS-INV-003 | Reminder notifications respect learner preferences + opt-out | `spec_ref: specs/notification-l0.yaml#NOTIF-PREF-001` |
| LMS-INV-004 | Course visibility (draft/published/archived) gated by author OR admin role | `spec_ref: specs/auth-asvs-l1.yaml#ASVS-V4.1.1` + `rule_ref: practices/rules/idempotency-key-on-mutations.md` |
| LMS-INV-005 | Bulk enrollment idempotent — re-submission of same idempotency-key does NOT duplicate | `spec_ref: specs/scheduled-task-l0.yaml#SCHED-IDEMPOTENT-001` + `rule_ref: practices/rules/idempotency-key-on-mutations.md` |

### INV-005 disambiguation (deliberate framing)

R7 `COMMUNITY-INV-005` shipped `co-shipped-rule: community-html-sanitization`
because no `practices/rules/*.md` covered server-side XSS HTML sanitization for
user-generated rich content — the invariant was genuinely catalog-novel and
required an inline rule anchor (per `recipe_spec_referential_integrity_guard.sh`
SP41b additive branch).

R8 **LMS-INV-005 differs:** bulk-enrollment idempotency is FULLY COVERED by
`practices/rules/idempotency-key-on-mutations.md` (existing rule, disk-verified)
AND by `specs/scheduled-task-l0.yaml#SCHED-IDEMPOTENT-001` (existing spec item,
disk-verified). Both anchors resolve via the standard `spec_ref + rule_ref`
pair — co-shipped-rule is NOT used. The choice is deliberate: `co-shipped-rule`
is the escape hatch reserved for genuinely catalog-novel invariants; binding to
existing rules is the preferred path when one exists.

## Business Observability (advisory — no emitter test enforced this cycle)

| Signal | Type | Notes |
|---|---|---|
| `recipe.lms.course_active_total` | Counter | Active (non-archived) courses count |
| `recipe.lms.reminder_scheduled_total` | Counter | Due-date reminders enqueued by scheduled-task |
| `recipe.lms.enrollment_idempotent_dedupe_total` | Counter | Bulk-enrollment idempotency dedupes caught |

## Evidence

```yaml
evidence:
  - provenance_class: external
    source: "Coursera homepage (post-redirect from building.coursera.org/developer-program/)"
    url: "https://www.coursera.org/"
    citation: "Learn from 350+ leading universities and companies"
    quoted_at: "2026-05-21"
    fidelity_note: "Coursera is the canonical large-scale LMS / MOOC platform; the homepage tagline attests to the cross-institutional course-catalog pattern the LMS recipe encodes."
  - provenance_class: external
    source: "Moodle Web Services API developer docs"
    url: "https://docs.moodle.org/dev/Web_services_API"
    citation: "Once you have done this, your plugin's functions will be accessible to other systems through Web services using one of a number of protocols, like XML-RPC, REST or SOAP."
    quoted_at: "2026-05-21"
    fidelity_note: "Moodle is the canonical open-source LMS; the Web Services API documents the REST surface the LMS recipe CRUD layer mirrors."
  - provenance_class: external
    source: "Classting (Korean K-12 LMS)"
    url: "https://www.classting.com/"
    citation: "개인화 교육을 실현하는 교육 AI 에이전트"
    quoted_at: "2026-05-21"
    fidelity_note: "Korean K-12 LMS anchor (PRD §4.4 H1 closure — first non-zero-Korean-verbatim LMS cycle since R6 channel.io). Classting positions itself as personalised education for Korean K-12; the verbatim attests the LMS pattern in Korean enterprise practice."
  - provenance_class: internal_design
    source: "edX OpenedX REST API docs"
    url: "https://docs.openedx.org/projects/edx-platform/en/latest/concepts/rest_api.html"
    rationale: "HTTP 404 on 2026-05-21 fetch — documented URL moved upstream. Moodle + Coursera + classting clear the 1-floor with substantial buffer; R9 evidence refresh re-attempts."
  - provenance_class: internal_design
    source: "인프런 (Inflearn — Korean e-learning)"
    url: "https://www.inflearn.com"
    rationale: "200 OK on 2026-05-21 but no public developer API documentation extractable; closed-API consistent with R6/R7 Korean closed-platform pattern."
  - provenance_class: internal_design
    source: "tech.kakao.com (Korean tech blog)"
    url: "https://tech.kakao.com/"
    rationale: "200 OK on 2026-05-21 but page is dev-tool-centric; no learning-focused verbatim sentence extractable. Page mentions 지식 공유 / 성장 but no education-platform-specific quote."
  - provenance_class: internal_design
    derives_from:
      - "SP15 crud"
      - "SP17 audit-log"
      - "SP26 notification"
      - "SP41 scheduled-task"
      - "auth ASVS L1"
    rationale: "LMS composition derives from existing crud, audit-log, notification, scheduled-task, and auth Spec Trios. No new L4 introduced."
```

## Scaffold Usage

```bash
/ax-scaffold business lms my-lms-app
```

This will scaffold all 5 enabled L4 domains into `my-lms-app/` and run
`/ax-verify-domain` for each.
