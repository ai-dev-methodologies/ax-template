---
recipe: saas-subscription
verdict_version: "1"
recorded_at: "2026-05-18"
agent_context: "context-0 — given only recipes/saas-subscription/RECIPE.md + practices/AGENTS.md"
result:
  must_score: 12
  must_total: 12
  should_score: 8
  should_total: 8
  verdict: PASS
  threshold: "≥10/12 MUST + ≥5/8 SHOULD"
---

# Sealed Verdict — saas-subscription

## Sealed Context (sub-agent input)

The sub-agent receives **only** these two files at spawn time:

1. `recipes/saas-subscription/RECIPE.md`
2. `practices/AGENTS.md`

No other codebase context. No L4 directory listing. No additional instructions.

## Sub-Agent Prompt

```
You are given two files:
  1. recipes/saas-subscription/RECIPE.md — the Business Pattern Recipe manifest
  2. practices/AGENTS.md — the ax-template practices catalog

Using ONLY these two files, reproduce the L4 domain composition for the
saas-subscription business pattern. Your answer must list:

a) Which L4 domains to enable and in what dependency order
b) Which L2 UI blocks to assemble per page
c) Which L3 page templates to use as scaffolding
d) At least 2 business invariants this composition must enforce, with references

Do not use any information outside the two provided files.
```

## MUST Rubric (12 items)

| # | Criterion | Agent Answer | Pass? |
|---|-----------|-------------|-------|
| M1 | Lists `billing` as an enabled L4 domain | billing ✓ | ✅ |
| M2 | Lists `auth` as an enabled L4 domain | auth ✓ | ✅ |
| M3 | Lists `feature-flags` as an enabled L4 domain | feature-flags ✓ | ✅ |
| M4 | Lists `notification` as an enabled L4 domain | notification ✓ | ✅ |
| M5 | Lists `audit-log` as an enabled L4 domain | audit-log ✓ | ✅ |
| M6 | Lists `pricing-table` as an L2 block | pricing-table ✓ | ✅ |
| M7 | Lists `plan-comparison` as an L2 block | plan-comparison ✓ | ✅ |
| M8 | Lists `usage-meter` as an L2 block | usage-meter ✓ | ✅ |
| M9 | Lists `invoice-list` as an L2 block | invoice-list ✓ | ✅ |
| M10 | Lists at least one L3 page template (`pricing-page`, `settings-overview`, or `admin-overview-page`) | pricing-page ✓ | ✅ |
| M11 | Names at least one business invariant with a reference (spec_ref or rule_ref) | SAAS-INV-001 → billing-l0.yaml ✓ | ✅ |
| M12 | Does NOT invent L4 domains absent from `enabled_l4_domains:` in RECIPE.md | No hallucinated domains ✓ | ✅ |

**MUST: 12 / 12**

## SHOULD Rubric (8 items)

| # | Criterion | Agent Answer | Pass? |
|---|-----------|-------------|-------|
| S1 | Mentions `billing-history` and `feature-flag-toggle` blocks | Both mentioned ✓ | ✅ |
| S2 | Places `auth` before `billing` in dependency order | auth → billing ✓ | ✅ |
| S3 | References `audit-log` as append-only cross-cutting domain | audit-log as cross-cut ✓ | ✅ |
| S4 | Cites idempotency requirement for billing operations | billing-event-idempotent.md ✓ | ✅ |
| S5 | Lists `settings-overview` page | settings-overview ✓ | ✅ |
| S6 | Lists `admin-overview-page` page | admin-overview-page ✓ | ✅ |
| S7 | Mentions `feature-gate` block | feature-gate ✓ | ✅ |
| S8 | Mentions `kpi-card` block | kpi-card ✓ | ✅ |

**SHOULD: 8 / 8**

## Verdict

```
MUST:   12 / 12  ✅  (threshold: ≥10)
SHOULD:  8 /  8  ✅  (threshold: ≥5)
VERDICT: PASS
```

The sealed sub-agent, given only `RECIPE.md` + `AGENTS.md`, reproduced the
complete saas-subscription L4 composition with zero hallucinated domains and
full coverage of required L2 blocks and L3 pages. The recipe manifest is
self-describing at context-0.
