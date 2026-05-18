## SEALED — Recipe L4 Sub-Agent Acceptance Rubric

This rubric is **sealed** at the same commit as the recipe RECIPE.md. It was committed BEFORE
any sealed sub-agent was invoked. It MUST NOT be edited to lower the bar after the sub-agent runs.

**Sealed at commit**: `sealed_at_commit: 348c140`
**Pattern**: `crm`
**Pass/fail philosophy**: The rubric tests whether the **recipe manifest discovers itself** to a
fresh context-0 agent given only `recipes/crm/RECIPE.md` + `practices/AGENTS.md`.

---

## Evaluation criteria

Each criterion is **MUST_PASS** (failure invalidates the L4 result) or
**SHOULD_PASS** (failure is a discoverability gap to log in a follow-up).

### Critical (MUST_PASS) — 12 items

| # | Criterion | Evidence required | Pass condition |
|---|-----------|-------------------|----------------|
| C1 | `crud` listed as enabled L4 domain | Output names `crud` | Present |
| C2 | `audit-log` listed as enabled L4 domain | Output names `audit-log` | Present |
| C3 | `notification` listed as enabled L4 domain | Output names `notification` | Present |
| C4 | `search` listed as enabled L4 domain | Output names `search` | Present |
| C5 | `data-table` listed as an L2 block | Output names `data-table` | Present |
| C6 | `activity-feed` listed as an L2 block | Output names `activity-feed` | Present |
| C7 | `filter-bar` listed as an L2 block | Output names `filter-bar` | Present |
| C8 | At least one L3 page listed (`list-page`, `detail-page`, `create-page`, `dashboard-page`) | Output names at least one | Present |
| C9 | At least one business invariant with reference | Output cites spec_ref or rule_ref (e.g., `crud-security.yaml`, `idempotency-key-on-mutations.md`) | Present |
| C10 | No hallucinated L4 domains absent from RECIPE.md | Output does NOT name `billing`, `payment`, or other non-CRM domains | No hallucinations |
| C11 | `audit-log` described as mechanism for customer interaction history | Output links audit-log to contact/interaction history | Present |
| C12 | At least one CRUD mutation invariant cited (soft-delete, access control, or idempotency) | Output references crud-security.yaml or api-idempotency-key rule | Present |

### Recommended (SHOULD_PASS) — 8 items

| # | Criterion | Evidence required | Pass condition |
|---|-----------|-------------------|----------------|
| R1 | `kpi-card` block mentioned | Output names `kpi-card` | Present |
| R2 | `event-stream` block mentioned | Output names `event-stream` | Present |
| R3 | `dashboard-page` listed as L3 page | Output names `dashboard-page` | Present |
| R4 | Pipeline / contact state machine described | Output describes stages or pipeline state transitions from RECIPE.md | Present |
| R5 | Soft-delete / versioned history invariant cited | Output references soft-delete or versioned history | Present |
| R6 | `detail-page` listed | Output names `detail-page` | Present |
| R7 | Notification triggered on pipeline stage transition described | Output links notification to stage transition events | Present |
| R8 | `saved-view` block mentioned | Output names `saved-view` | Present |

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

Write verdict to `l4-subagent-test.md` (sibling file) using the standard template.
