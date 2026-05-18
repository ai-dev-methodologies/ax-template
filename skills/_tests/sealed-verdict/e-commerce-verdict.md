---
recipe: e-commerce
verdict_version: "1"
recorded_at: "2026-05-18"
agent_context: "context-0 — given only recipes/e-commerce/RECIPE.md + practices/AGENTS.md"
result:
  must_score: 12
  must_total: 12
  should_score: 7
  should_total: 8
  verdict: PASS
  threshold: "≥10/12 MUST + ≥5/8 SHOULD"
---

# Sealed Verdict — e-commerce

## Sealed Context (sub-agent input)

The sub-agent receives **only** these two files at spawn time:

1. `recipes/e-commerce/RECIPE.md`
2. `practices/AGENTS.md`

No other codebase context. No L4 directory listing. No additional instructions.

## Sub-Agent Prompt

```
You are given two files:
  1. recipes/e-commerce/RECIPE.md — the Business Pattern Recipe manifest
  2. practices/AGENTS.md — the ax-template practices catalog

Using ONLY these two files, reproduce the L4 domain composition for the
e-commerce business pattern. Your answer must list:

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
| M2 | Lists `payment` as an enabled L4 domain | payment ✓ | ✅ |
| M3 | Lists `notification` as an enabled L4 domain | notification ✓ | ✅ |
| M4 | Lists `audit-log` as an enabled L4 domain | audit-log ✓ | ✅ |
| M5 | Lists `search` as an enabled L4 domain | search ✓ | ✅ |
| M6 | Lists `data-table` as an L2 block | data-table ✓ | ✅ |
| M7 | Lists `filter-bar` as an L2 block | filter-bar ✓ | ✅ |
| M8 | Lists `crud-list-adapter` or `crud-create-form` as an L2 block | crud-list-adapter ✓ | ✅ |
| M9 | Lists at least one L3 page template (`list-page`, `detail-page`, `create-page`, `edit-page`, `search-results-page`) | list-page ✓ | ✅ |
| M10 | Names at least one business invariant with a reference (spec_ref or rule_ref) | ECOM-INV-001 → payment-l0.yaml ✓ | ✅ |
| M11 | Does NOT invent L4 domains absent from `enabled_l4_domains:` in RECIPE.md | No hallucinated domains ✓ | ✅ |
| M12 | Identifies payment as requiring idempotency (from AGENTS.md catalog rules) | idempotency-key rule ✓ | ✅ |

**MUST: 12 / 12**

## SHOULD Rubric (8 items)

| # | Criterion | Agent Answer | Pass? |
|---|-----------|-------------|-------|
| S1 | Lists `faceted-filter` block | faceted-filter ✓ | ✅ |
| S2 | Lists `event-stream` block | event-stream ✓ | ✅ |
| S3 | Lists `kpi-card` block | kpi-card ✓ | ✅ |
| S4 | Places `crud` before `payment` in dependency order (products before checkout) | crud → payment ✓ | ✅ |
| S5 | Lists `edit-page` and `search-results-page` as L3 pages | Both ✓ | ✅ |
| S6 | References `audit-log` as append-only / cross-cutting | audit-log cross-cut ✓ | ✅ |
| S7 | Mentions payment webhook / notification coupling | notification on payment event ✓ | ✅ |
| S8 | Lists `detail-page` and `create-page` as L3 pages | detail-page only — create-page missed | ❌ |

**SHOULD: 7 / 8**

## Verdict

```
MUST:   12 / 12  ✅  (threshold: ≥10)
SHOULD:  7 /  8  ✅  (threshold: ≥5)
VERDICT: PASS
```

The sealed sub-agent reproduced the e-commerce L4 composition accurately from
the recipe manifest alone. Minor gap: `create-page` was not explicitly listed
alongside `detail-page` (counted as a single SHOULD miss). All MUST criteria
met — no hallucinated domains, payment idempotency rule referenced from
AGENTS.md catalog.
