---
pattern: community
display_name: "Community (Posts + Comments + Moderation + Notifications + Search)"
schema_version: 1
compatible_with_catalog_version: "v1.5.0-scheduler-community"
last_verified_at: "2026-05-20"
enabled_l4_domains:
  - audit-log
  - auth
  - crud
  - notification
  - search
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
  #   skip: ["feature-flags"]
  #   rationale: "Open-by-default community; no moderation toggle gating needed."
  #   citation: "<internal ticket / PR url>"
  #
  # enabled_l4_domains:
  #   skip: ["auth"]
  #   rationale: "Fully-public read-only mirror of upstream community for archival."
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
| `search` | **spec-only** 📋 | ~5-10 eng-days (implement backend) |

**Summary**: 2 impl ready · 3 spec-only (implement) · 0 skeleton (flesh out) · est. ~19-26 engineering days for the gap.

**Reading guide**: `impl` = backend Java reference workload ready in `backend/src/main/java/com/ax/template/authblueprint/<domain>/`. `spec-only` = Spec Trio + Next.js stub only; backend NOT included. `skeleton` = `.skeleton` file present; flesh out controller/service yourself. Sealed verdict PASS validates catalog self-discoverability, NOT runnable backend code.


# Recipe: community

**Business context:** Public/semi-public threaded discussion — posts, comments,
reply chains, moderation actions, member-preference-aware reply notifications, and
soft-delete-aware search. Targets Discourse-style forums, internal community
portals, and review/Q&A surfaces.

## Enabled L4 Domains

| L4 Domain | Role in this recipe |
|---|---|
| `crud` | Post + comment CRUD, threading, soft-delete |
| `auth` | Authenticated post creation, rate-limited per ASVS-V2.2.1 |
| `audit-log` | Immutable moderation audit (status change, hide, restore) |
| `notification` | Reply notifications respecting recipient preferences |
| `search` | Authorization-aware full-text search; soft-deleted threads excluded |

## Business Invariants

| ID | Statement | Binding |
|---|---|---|
| COMMUNITY-INV-001 | Post + comment moderation status changes emit audit-log row with operator + before/after | `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-001` |
| COMMUNITY-INV-002 | Soft-deleted threads excluded from search | `spec_ref: specs/search-l0.yaml#SEARCH-AUTHZ-001` |
| COMMUNITY-INV-003 | Reply notifications respect recipient preferences + opt-out | `spec_ref: specs/notification-l0.yaml#NOTIF-PREF-001` |
| COMMUNITY-INV-004 | Authenticated post creation rate-limited per user per minute | `spec_ref: specs/auth-asvs-l1.yaml#ASVS-V2.2.1` + `rule_ref: practices/rules/idempotency-key-on-mutations.md` |
| COMMUNITY-INV-005 | User-generated HTML sanitized server-side before storage (XSS prevention) | `co-shipped-rule: community-html-sanitization` + `invariant_test: frontend/tests/recipes/community-sanitize.spec.ts` |

INV-005 is a **recipe-level invariant** authored inline in
`specs/recipes/community-recipe-l0.yaml` — NOT a new `practices/rules/*.md`
file (honors PRD §1.8 Principle 8 + §10 out-of-scope). The
`recipe_spec_referential_integrity_guard.sh` accepts this anchor shape when
accompanied by an `invariant_test:` pointing to a co-shipped test (R7 SP41b
path-c additive guard extension).

## Business Observability (advisory — no emitter test enforced this cycle)

| Signal | Type | Notes |
|---|---|---|
| `recipe.community.thread_active_total` | Counter | Active (non-soft-deleted) threads count |
| `recipe.community.sanitize_violation_total` | Counter | HTML sanitize blocks (XSS attempts caught) |
| `community.moderation.action_total{type}` | Counter | Moderation actions by type (hide/restore/lock) |

## Evidence

```yaml
evidence:
  - provenance_class: external
    source: "Discourse meta — Discourse API documentation"
    url: "https://meta.discourse.org/t/discourse-api-documentation/22706"
    citation: "Discourse is backed by a complete JSON api. Anything you can do on the site you can also do using the JSON api."
    quoted_at: "2026-05-20"
    fidelity_note: "Discourse is the canonical open-source community forum platform; its REST API surface is the reference for community-recipe endpoint coverage."
  - provenance_class: external
    source: "Reddit GitHub archive — API documentation wiki"
    url: "https://github.com/reddit-archive/reddit/wiki/API"
    citation: "Clients must authenticate with OAuth2 — Clients connecting via OAuth2 may make up to 60 requests per minute."
    quoted_at: "2026-05-20"
    fidelity_note: "Reddit's archived API doc establishes OAuth2 + per-user rate-limit pattern. Modern developers.reddit.com host remains fetcher-blocked; GitHub archive host preserves the same verbatim semantics (M2 closure per PRD §4.4)."
  - provenance_class: internal_design
    source: "PRAW (Reddit Python API Wrapper) docs"
    url: "https://praw.readthedocs.io/en/stable/"
    rationale: "200 OK but page is navigation + headings only; no extractable descriptive verbatim. Retained as inspirational context for client-side rate-limit handling."
  - provenance_class: internal_design
    source: "Reddit Developer Platform (Devvit)"
    url: "https://developers.reddit.com/docs/quickstart"
    rationale: "Fetcher-blocked at 2026-05-20 fetch attempt. Superseded by Reddit GitHub archive verbatim above."
  - provenance_class: internal_design
    source: "디시인사이드 (DCinside) — Korean community"
    url: "https://www.dcinside.com/"
    rationale: "200 OK but no public REST/OAuth dev API documentation; no extractable verbatim. Retained for Korean community context only."
  - provenance_class: internal_design
    source: "클리앙 (Clien) — Korean community"
    url: "https://www.clien.net/service/"
    rationale: "200 OK but no public REST/OAuth dev API documentation; no extractable verbatim. Retained for Korean community context only."
  - provenance_class: internal_design
    source: "Korean tech blog cycle (PRD §4.4 M1 closure)"
    rationale: |
      Zero-Korean-verbatim cycle for R7. 5 Korean engineering blog/host attempts
      logged in PRD §4.4 (toss.tech, d2.naver.com, tech.kakao.com,
      techblog.woowahan.com × 2, engineering.linecorp.com/ko). 4 of 5 returned
      200 OK with no scheduler/community Korean text; 1 (d2.naver.com)
      fetcher-blocked. No Korean verbatim is available without fabrication.
      The Korean enterprise stack framing (React + Spring Boot) remains
      honored at the catalog level; per-cycle Korean verbatim source-anchoring
      is a soft signal that R6 cleared via channel.io and R7 cannot replicate
      this cycle (R6 channel.io was a booking-domain anchor; community is
      structurally a Discourse-style English-platform domain in Korean
      enterprise practice).
  - provenance_class: internal_design
    derives_from:
      - "SP15 crud"
      - "SP17 audit-log"
      - "SP26 notification"
      - "SP26 search"
      - "auth ASVS L1"
    rationale: "Community composition derives from existing crud, audit-log, notification, search, and auth Spec Trios."
```

## Scaffold Usage

```bash
/ax-scaffold business community my-community-app
```

This will scaffold all 5 enabled L4 domains into `my-community-app/` and run
`/ax-verify-domain` for each.
