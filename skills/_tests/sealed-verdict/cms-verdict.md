---
recipe: cms
verdict_version: "1"
recorded_at: "2026-05-21"
agent_context: "context-0 — given only recipes/cms/RECIPE.md + practices/AGENTS.md"
result:
  must_score: null
  must_total: 12
  should_score: null
  should_total: 8
  verdict: PENDING
  threshold: "≥10/12 MUST + ≥5/8 SHOULD"
---

# Sealed Verdict — cms (SP44 PENDING execution)

## Sealed Context (sub-agent input)

The sub-agent receives **only** these two files at spawn time:

1. `recipes/cms/RECIPE.md`
2. `practices/AGENTS.md`

No other codebase context. No L4 directory listing. No additional instructions.

## Sub-Agent Prompt (SP44 will execute)

```
You are given two files:
  1. recipes/cms/RECIPE.md — the Business Pattern Recipe manifest
  2. practices/AGENTS.md — the ax-template practices catalog

Using ONLY these two files, reproduce the L4 domain composition for the
cms business pattern. Your answer must list:

a) Which L4 domains to enable and in what dependency order
b) Which L2 UI blocks to assemble per page
c) Which L3 page templates to use as scaffolding
d) At least 3 business invariants this composition must enforce, with references
e) The scheduled-task primitive used to gate scheduled-publish + scheduled-archive
f) How content slug uniqueness is enforced (which rule + which spec ID)

Do not use any information outside the two provided files.
```

## MUST Rubric (12 items — SP44 fills the answer column)

| # | Criterion | Agent Answer | Pass? |
|---|-----------|-------------|-------|
| M1 | Lists `audit-log` as an enabled L4 domain | (SP44) | — |
| M2 | Lists `crud` as an enabled L4 domain | (SP44) | — |
| M3 | Lists `notification` as an enabled L4 domain | (SP44) | — |
| M4 | Lists `scheduled-task` as an enabled L4 domain | (SP44) | — |
| M5 | Lists `crud-create-form` or `crud-list-adapter` as an L2 block | (SP44) | — |
| M6 | Lists `notification-list` as an L2 block | (SP44) | — |
| M7 | Lists `data-table` or `filter-bar` as an L2 block (editorial queue) | (SP44) | — |
| M8 | Lists at least one L3 page from {list-page, detail-page, create-page, edit-page, dashboard-page} | (SP44) | — |
| M9 | Names ≥3 business invariants with spec_ref OR rule_ref | (SP44) | — |
| M10 | Does NOT invent L4 domains absent from `enabled_l4_domains:` (no payment / billing — note auth/search are optional, listing OR omitting both is acceptable) | (SP44) | — |
| M11 | Identifies scheduled-task lock primitive anchor (SCHED-LOCK-001) for CMS-INV-002 scheduled-publish | (SP44) | — |
| M12 | Identifies the slug-uniqueness server-side validation anchor (CRUD-VAL-1) for CMS-INV-005 | (SP44) | — |

**MUST: PENDING / 12**

## SHOULD Rubric (8 items — SP44 fills the answer column)

| # | Criterion | Agent Answer | Pass? |
|---|-----------|-------------|-------|
| S1 | Lists `confirm-dialog` block (publish / archive / reject) | (SP44) | — |
| S2 | Lists `filter-bar` block (state / locale / content-type chips) | (SP44) | — |
| S3 | Lists `kpi-card` block (dashboard metrics) | (SP44) | — |
| S4 | Lists `search-input` block (optional, only if `search` L4 enabled) | (SP44) | — |
| S5 | Identifies content-expiry binding (SCHED-EXECUTE-001 + AUDIT-RETENTION-001) for CMS-INV-003 | (SP44) | — |
| S6 | Names existing rule (idempotency-key-on-mutations.md) for CMS-INV-005 instead of inventing a new rule | (SP44) | — |
| S7 | Lists `dashboard-page` as an L3 page | (SP44) | — |
| S8 | Mentions editorial notification ordering (NOTIF-PREF-001 honored before fanout, NOTIF-SEND-001 at-least-once dispatch) | (SP44) | — |

**SHOULD: PENDING / 8**

## Verdict

```
MUST:   PENDING / 12
SHOULD: PENDING / 8
VERDICT: PENDING (SP44 executes context-0 sub-agent + records verdict)
```

**INV-005 disposition note (SP44 verification):** The sealed sub-agent must
recognize that CMS-INV-005 binds to `spec_ref: crud-security.yaml#CRUD-VAL-1`
AND `rule_ref: practices/rules/idempotency-key-on-mutations.md` — the standard
two-anchor pair, NOT `co-shipped-rule`. The deliberate framing is documented in
RECIPE.md: `co-shipped-rule` is reserved for genuinely catalog-novel invariants
(R7 community-INV-005 XSS sanitize), while CMS-INV-005 uses the preferred
existing-rule-binding path.

**Evidence density note (SP44 verification):** 3 English verbatim anchors
(Sanity-base + Contentful + Strapi) + 1 topic-relevant English verbatim (Sanity
scheduled-publishing deprecation notice — attests Sanity historically shipped
scheduled-publishing) + 1 Korean verbatim (brunch). Strongest evidence chain in
any single recipe this cycle.
