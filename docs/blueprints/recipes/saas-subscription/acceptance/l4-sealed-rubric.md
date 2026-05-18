## SEALED — Recipe L4 Sub-Agent Acceptance Rubric

This rubric is **sealed** at the same commit as the recipe RECIPE.md. It was committed BEFORE
any sealed sub-agent was invoked. It MUST NOT be edited to lower the bar after the sub-agent runs.

**Sealed at commit**: `sealed_at_commit: 348c140`
**Pattern**: `saas-subscription`
**Pass/fail philosophy**: The rubric tests whether the **recipe manifest discovers itself** to a
fresh context-0 agent given only `recipes/saas-subscription/RECIPE.md` + `practices/AGENTS.md`.

---

## Evaluation criteria

Each criterion is **MUST_PASS** (failure invalidates the L4 result) or
**SHOULD_PASS** (failure is a discoverability gap to log in a follow-up).

### Critical (MUST_PASS) — 12 items

| # | Criterion | Evidence required | Pass condition |
|---|-----------|-------------------|----------------|
| C1 | `billing` listed as enabled L4 domain | Output names `billing` as a required domain | Present |
| C2 | `auth` listed as enabled L4 domain | Output names `auth` as a required domain | Present |
| C3 | `feature-flags` listed as enabled L4 domain | Output names `feature-flags` as a required domain | Present |
| C4 | `notification` listed as enabled L4 domain | Output names `notification` as a required domain | Present |
| C5 | `audit-log` listed as enabled L4 domain | Output names `audit-log` as a required domain | Present |
| C6 | `pricing-table` listed as an L2 block | Output names `pricing-table` in block inventory | Present |
| C7 | `plan-comparison` listed as an L2 block | Output names `plan-comparison` in block inventory | Present |
| C8 | `usage-meter` listed as an L2 block | Output names `usage-meter` in block inventory | Present |
| C9 | `invoice-list` listed as an L2 block | Output names `invoice-list` in block inventory | Present |
| C10 | At least one L3 page template listed (`pricing-page`, `settings-overview`, or `admin-overview-page`) | Output names at least one of the three L3 pages | Present |
| C11 | At least one business invariant named with a reference | Output cites a spec_ref or rule_ref (e.g., `billing-l0.yaml`, `billing-event-idempotent.md`) | Present |
| C12 | No hallucinated L4 domains absent from RECIPE.md `enabled_l4_domains:` | Output does NOT list domains like `payments`, `orders`, `inventory`, or other non-saas-subscription domains | No hallucinations |

### Recommended (SHOULD_PASS) — 8 items

| # | Criterion | Evidence required | Pass condition |
|---|-----------|-------------------|----------------|
| R1 | `billing-history` and `feature-flag-toggle` blocks mentioned | Both named in block discussion | Both present |
| R2 | `auth` placed before `billing` in dependency order | Dependency or wiring order shows auth → billing | Present |
| R3 | `audit-log` described as append-only or cross-cutting | Output characterizes audit-log as cross-cutting or event-log domain | Present |
| R4 | Idempotency requirement cited for billing operations | Output references billing idempotency (e.g., `billing-event-idempotent.md`) | Present |
| R5 | `settings-overview` page listed | Output names `settings-overview` as an L3 page | Present |
| R6 | `admin-overview-page` page listed | Output names `admin-overview-page` as an L3 page | Present |
| R7 | `feature-gate` block mentioned | Output names `feature-gate` in block inventory | Present |
| R8 | `kpi-card` block mentioned | Output names `kpi-card` in block inventory | Present |

---

## Overall verdict logic

- **PASS**: ≥10/12 MUST_PASS + ≥5/8 SHOULD_PASS
- **PASS-WITH-CONCERNS**: ≥10/12 MUST_PASS + 0–4 SHOULD_PASS → log follow-up for discoverability gaps
- **FAIL**: <10 MUST_PASS → RECIPE.md does not adequately describe the composition to a context-0 agent

---

## Anti-rigging discipline

1. Do not edit this rubric after the sub-agent runs to lower the bar.
2. Do not feed the sub-agent this rubric file — only RECIPE.md + AGENTS.md.
3. If the rubric is edited post-execution, mark the result **VOID** and rerun.

---

## Recording the result

Write verdict to `l4-subagent-test.md` (sibling file):

```
# L4 Sub-Agent Acceptance Test — saas-subscription

**Sealed prompt commit**: <hash>
**Sealed rubric commit**: 348c140
**Execution date**: YYYY-MM-DD
**Overall verdict**: PASS | PASS-WITH-CONCERNS | FAIL | VOID

## MUST_PASS criteria (12)
- C1 ... PASS | FAIL — <evidence>
...

## SHOULD_PASS criteria (8)
- R1 ... PASS | FAIL — <evidence>
...

## Diagnostic notes
<if any criterion failed>

## Follow-up actions
<if PASS-WITH-CONCERNS or FAIL>
```
