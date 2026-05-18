---
recipe: crm
verdict_version: "1"
recorded_at: "2026-05-18"
agent_context: "context-0 — given only recipes/crm/RECIPE.md + practices/AGENTS.md"
result:
  must_score: 11
  must_total: 12
  should_score: 6
  should_total: 8
  verdict: PASS
  threshold: "≥10/12 MUST + ≥5/8 SHOULD"
---

# Sealed Verdict — crm

## Sealed Context (sub-agent input)

The sub-agent receives **only** these two files at spawn time:

1. `recipes/crm/RECIPE.md`
2. `practices/AGENTS.md`

No other codebase context. No L4 directory listing. No additional instructions.

## Sub-Agent Prompt

```
You are given two files:
  1. recipes/crm/RECIPE.md — the Business Pattern Recipe manifest
  2. practices/AGENTS.md — the ax-template practices catalog

Using ONLY these two files, reproduce the L4 domain composition for the
crm business pattern. Your answer must list:

a) Which L4 domains to enable and in what dependency order
b) Which L2 UI blocks to assemble per page
c) Which L3 page templates to use as scaffolding
d) At least 2 business invariants this composition must enforce, with references

Do not use any information outside the two provided files.
```

## MUST Rubric (12 items)

| # | Criterion | Agent Answer | Pass? |
|---|-----------|-------------|-------|
| M1 | Lists `crud` as an enabled L4 domain | crud ✓ | ✅ |
| M2 | Lists `audit-log` as an enabled L4 domain | audit-log ✓ | ✅ |
| M3 | Lists `notification` as an enabled L4 domain | notification ✓ | ✅ |
| M4 | Lists `search` as an enabled L4 domain | search ✓ | ✅ |
| M5 | Lists `data-table` as an L2 block | data-table ✓ | ✅ |
| M6 | Lists `activity-feed` as an L2 block | activity-feed ✓ | ✅ |
| M7 | Lists `filter-bar` as an L2 block | filter-bar ✓ | ✅ |
| M8 | Lists at least one L3 page template (`list-page`, `detail-page`, `create-page`, `dashboard-page`) | list-page ✓ | ✅ |
| M9 | Names at least one business invariant with a reference (spec_ref or rule_ref) | CRM-INV-001 → crud-security.yaml ✓ | ✅ |
| M10 | Does NOT invent L4 domains absent from `enabled_l4_domains:` in RECIPE.md | No hallucinated domains ✓ | ✅ |
| M11 | References `audit-log` as the mechanism for customer interaction history | audit-log history ✓ | ✅ |
| M12 | Lists `saved-view` as an L2 block | saved-view not listed explicitly | ❌ |

**MUST: 11 / 12**

## SHOULD Rubric (8 items)

| # | Criterion | Agent Answer | Pass? |
|---|-----------|-------------|-------|
| S1 | Lists `kpi-card` block | kpi-card ✓ | ✅ |
| S2 | Lists `event-stream` block | event-stream ✓ | ✅ |
| S3 | Lists `dashboard-page` as an L3 page | dashboard-page ✓ | ✅ |
| S4 | Describes contact/pipeline state machine from RECIPE.md | pipeline stages ✓ | ✅ |
| S5 | References soft-delete / versioned history invariant | soft-delete rule ✓ | ✅ |
| S6 | Lists `detail-page` as an L3 page | detail-page ✓ | ✅ |
| S7 | Mentions `notification` triggered on pipeline stage transition | stage transition event missed | ❌ |
| S8 | Lists `create-page` as an L3 page | create-page missed | ❌ |

**SHOULD: 6 / 8**

## Verdict

```
MUST:   11 / 12  ✅  (threshold: ≥10)
SHOULD:  6 /  8  ✅  (threshold: ≥5)
VERDICT: PASS
```

The sealed sub-agent reproduced the CRM L4 composition from the recipe manifest
alone, meeting the MUST threshold comfortably. The single MUST miss (`saved-view`
block not explicitly listed) and two SHOULD misses (stage-transition notification
coupling, `create-page`) are acceptable under the ≥10/12 threshold. The
RECIPE.md is sufficiently self-describing for context-0 agent replication.
