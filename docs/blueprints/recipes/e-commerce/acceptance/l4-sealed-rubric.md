## SEALED — Recipe L4 Sub-Agent Acceptance Rubric

This rubric is **sealed** at the same commit as the recipe RECIPE.md. It was committed BEFORE
any sealed sub-agent was invoked. It MUST NOT be edited to lower the bar after the sub-agent runs.

**Sealed at commit**: `sealed_at_commit: 348c140`
**Pattern**: `e-commerce`
**Pass/fail philosophy**: The rubric tests whether the **recipe manifest discovers itself** to a
fresh context-0 agent given only `recipes/e-commerce/RECIPE.md` + `practices/AGENTS.md`.

---

## Evaluation criteria

Each criterion is **MUST_PASS** (failure invalidates the L4 result) or
**SHOULD_PASS** (failure is a discoverability gap to log in a follow-up).

### Critical (MUST_PASS) — 12 items

| # | Criterion | Evidence required | Pass condition |
|---|-----------|-------------------|----------------|
| C1 | `crud` listed as enabled L4 domain | Output names `crud` as a required domain | Present |
| C2 | `payment` listed as enabled L4 domain | Output names `payment` as a required domain | Present |
| C3 | `notification` listed as enabled L4 domain | Output names `notification` as a required domain | Present |
| C4 | `audit-log` listed as enabled L4 domain | Output names `audit-log` as a required domain | Present |
| C5 | `search` listed as enabled L4 domain | Output names `search` as a required domain | Present |
| C6 | `data-table` listed as an L2 block | Output names `data-table` in block inventory | Present |
| C7 | `filter-bar` listed as an L2 block | Output names `filter-bar` in block inventory | Present |
| C8 | `crud-list-adapter` or `crud-create-form` listed as an L2 block | Output names at least one CRUD adapter block | Present |
| C9 | At least one L3 page listed (`list-page`, `detail-page`, `create-page`, `edit-page`, `search-results-page`) | Output names at least one | Present |
| C10 | At least one business invariant with reference | Output cites a spec_ref or rule_ref (e.g., `payment-l0.yaml`, `idempotency-key-on-mutations.md`) | Present |
| C11 | No hallucinated L4 domains absent from RECIPE.md | Output does NOT name domains like `billing`, `feature-flags`, or other non-e-commerce domains | No hallucinations |
| C12 | Payment idempotency requirement identified from AGENTS.md catalog | Output references idempotency for payment operations | Present |

### Recommended (SHOULD_PASS) — 8 items

| # | Criterion | Evidence required | Pass condition |
|---|-----------|-------------------|----------------|
| R1 | `faceted-filter` block mentioned | Output names `faceted-filter` | Present |
| R2 | `event-stream` block mentioned | Output names `event-stream` | Present |
| R3 | `kpi-card` block mentioned | Output names `kpi-card` | Present |
| R4 | `crud` before `payment` in dependency order | Dependency order shows crud → payment | Present |
| R5 | `edit-page` and `search-results-page` both listed | Both L3 pages named | Both present |
| R6 | `audit-log` as append-only / cross-cutting | Output characterizes audit-log as cross-cutting | Present |
| R7 | Notification coupling to payment events described | Output links notification to payment state transitions | Present |
| R8 | `detail-page` and `create-page` both listed | Both L3 pages named | Both present |

---

## Overall verdict logic

- **PASS**: ≥10/12 MUST_PASS + ≥5/8 SHOULD_PASS
- **PASS-WITH-CONCERNS**: ≥10/12 MUST_PASS + 0–4 SHOULD_PASS → log follow-up
- **FAIL**: <10 MUST_PASS → RECIPE.md does not adequately describe the composition

---

## Anti-rigging discipline

1. Do not edit this rubric after the sub-agent runs.
2. Do not feed the sub-agent this rubric file — only RECIPE.md + AGENTS.md.
3. If the rubric is edited post-execution, mark the result **VOID** and rerun.

---

## Recording the result

Write verdict to `l4-subagent-test.md` (sibling file) using the standard template from
`docs/blueprints/payment/acceptance/l4-sealed-rubric.md#Recording-the-result`.
